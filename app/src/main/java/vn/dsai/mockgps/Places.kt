package vn.dsai.mockgps

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.SystemClock
import android.util.Xml
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

data class Place(val name: String, val address: String, val point: LatLng)

/**
 * Address search, reverse lookup and routing. Lookups try the platform Geocoder (Google's backend on
 * Play devices) first, then Photon and Nominatim, because some networks cannot resolve
 * *.openstreetmap.org at all.
 */
object Places {
    private const val OFFICIAL_PACKAGE = "vn.dsai.mockgps"

    /**
     * The free OSM services require an identifying User-Agent with a contact. Builds under another
     * package name (forks) don't send DSAI's contact; put your own here if you publish a fork.
     */
    private fun userAgent(ctx: Context): String {
        val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "dev"
        return if (ctx.packageName == OFFICIAL_PACKAGE) "MockGPS/$version (Android; $OFFICIAL_PACKAGE; +https://dsai.vn; loivo@dsai.vn)"
        else "${ctx.packageName}/$version (Android; fork of MockGPS)"
    }
    private val urlInText = Regex("""https?://\S+""")

    /** Accepts coordinates, a Google Maps / geo: link, shared text containing a link, or an address. */
    suspend fun search(ctx: Context, text: String, near: LatLng?): List<Place> = withContext(Dispatchers.IO) {
        val q = text.trim()
        Geo.parseLatLng(q)?.let { return@withContext listOf(Place("", "", it)) }
        val link = urlInText.find(q)?.value ?: q.takeIf { it.startsWith("geo:") }
        if (link == null) return@withContext lookup(ctx, q, near)

        LinkParser.coords(link)?.let { return@withContext listOf(Place("", "", it)) }
        if (link.startsWith("http")) {
            val full = expand(link)
            LinkParser.coords(full)?.let { return@withContext listOf(Place("", "", it)) }
            LinkParser.query(full)?.let { return@withContext lookup(ctx, it, near) }
        }
        // Text shared from Google Maps is "Name\nAddress\nlink": fall back to the name.
        val rest = q.replace(link, "").trim().lineSequence().firstOrNull { it.isNotBlank() }
        if (rest != null) lookup(ctx, rest, near) else emptyList()
    }

    private suspend fun lookup(ctx: Context, q: String, near: LatLng?): List<Place> {
        var error: Exception? = null
        val sources = listOf<suspend () -> List<Place>>({ geocoder(ctx, q) }, { photon(ctx, q, near) }, { nominatim(ctx, q, near) })
        for (source in sources) {
            try {
                val r = source()
                if (r.isNotEmpty()) return r
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e
            }
        }
        error?.let { throw it }
        return emptyList()
    }

    private suspend fun geocoder(ctx: Context, q: String): List<Place> {
        if (!Geocoder.isPresent()) return emptyList()
        val g = Geocoder(ctx, Locale.getDefault())
        val found: List<Address> = if (Build.VERSION.SDK_INT >= 33) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<List<Address>> { cont ->
                    g.getFromLocationName(q, 8, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                        override fun onError(errorMessage: String?) = cont.resume(emptyList())
                    })
                }
            }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            g.getFromLocationName(q, 8).orEmpty()
        }
        return found.filter { it.hasLatitude() && it.hasLongitude() }.map { a ->
            val line = a.getAddressLine(0).orEmpty()
            val name = a.featureName?.takeIf { f -> f.isNotBlank() && !f.all { it.isDigit() } } ?: line.substringBefore(',')
            Place(name, line, LatLng(a.latitude, a.longitude))
        }
    }

    private fun photon(ctx: Context, q: String, near: LatLng?): List<Place> {
        val url = "https://photon.komoot.io/api/?limit=8&q=" + URLEncoder.encode(q, "UTF-8") +
            (near?.let { "&lat=${it.lat}&lon=${it.lng}" } ?: "")
        val features = JSONObject(get(ctx, url)).getJSONArray("features")
        return List(features.length()) { i -> photonPlace(features.getJSONObject(i)) }
    }

    private fun photonPlace(f: JSONObject): Place {
        val c = f.getJSONObject("geometry").getJSONArray("coordinates")
        val p = f.getJSONObject("properties")
        val street = listOf(p.optString("housenumber"), p.optString("street")).filter { it.isNotBlank() }.joinToString(" ")
        val name = p.optString("name").ifBlank { street }
        val address = (listOf(street) + listOf("district", "city", "state", "country").map { p.optString(it) })
            .filter { it.isNotBlank() && it != name }.distinct().joinToString(", ")
        return Place(name.ifBlank { address.substringBefore(',') }, address, LatLng(c.getDouble(1), c.getDouble(0)))
    }

    private fun nominatim(ctx: Context, q: String, near: LatLng?): List<Place> {
        val url = StringBuilder("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=8&accept-language=${Locale.getDefault().toLanguageTag()}&q=")
            .append(URLEncoder.encode(q, "UTF-8"))
        if (near != null) url.append("&viewbox=${near.lng - 0.5},${near.lat + 0.5},${near.lng + 0.5},${near.lat - 0.5}")
        val arr = JSONArray(get(ctx, url.toString()))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val full = o.optString("display_name")
            Place(
                o.optString("name").ifBlank { full.substringBefore(',') },
                full,
                LatLng(o.getString("lat").toDouble(), o.getString("lon").toDouble()),
            )
        }
    }

    suspend fun reverse(ctx: Context, p: LatLng): String? = withContext(Dispatchers.IO) {
        runCatching { reverseGeocoder(ctx, p) }.getOrNull()
            ?: runCatching {
                val f = JSONObject(get(ctx, "https://photon.komoot.io/reverse?lat=${p.lat}&lon=${p.lng}")).getJSONArray("features")
                photonPlace(f.getJSONObject(0)).let { listOf(it.name, it.address).filter(String::isNotBlank).distinct().joinToString(", ") }
            }.getOrNull()
    }

    private suspend fun reverseGeocoder(ctx: Context, p: LatLng): String? {
        if (!Geocoder.isPresent()) return null
        val g = Geocoder(ctx, Locale.getDefault())
        val found: List<Address> = if (Build.VERSION.SDK_INT >= 33) {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine<List<Address>> { cont ->
                    g.getFromLocation(p.lat, p.lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                        override fun onError(errorMessage: String?) = cont.resume(emptyList())
                    })
                }
            }.orEmpty()
        } else {
            @Suppress("DEPRECATION")
            g.getFromLocation(p.lat, p.lng, 1).orEmpty()
        }
        return found.firstOrNull()?.getAddressLine(0)?.takeIf { it.isNotBlank() }
    }

    private val routeLock = Mutex()
    private var lastRouteAt = 0L

    /**
     * Road-snapped path through [waypoints] from an OSRM server ([server] contains {profile}).
     * The public FOSSGIS instance allows at most one request per second, so calls are spaced out.
     */
    suspend fun route(ctx: Context, waypoints: List<LatLng>, travel: Travel, server: String): List<LatLng> = routeLock.withLock {
        val wait = lastRouteAt + 1100 - SystemClock.elapsedRealtime()
        if (wait > 0) delay(wait)
        lastRouteAt = SystemClock.elapsedRealtime()
        withContext(Dispatchers.IO) { fetchRoute(ctx, waypoints, travel, server) }
    }

    private fun fetchRoute(ctx: Context, waypoints: List<LatLng>, travel: Travel, server: String): List<LatLng> {
        val coords = waypoints.joinToString(";") { "${it.lng},${it.lat}" }
        val base = server.replace("{profile}", travel.osrm).let { if (it.endsWith("/")) it else "$it/" }
        val o = JSONObject(get(ctx, "$base$coords?overview=full&geometries=geojson"))
        if (o.optString("code") != "Ok") throw IOException(o.optString("message", ctx.getString(R.string.error_detail_route_not_found)))
        val c = o.getJSONArray("routes").getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
        return List(c.length()) { i -> c.getJSONArray(i).let { LatLng(it.getDouble(1), it.getDouble(0)) } }
    }

    private fun get(ctx: Context, url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", userAgent(ctx))
        try {
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 429) throw IOException(ctx.getString(R.string.error_detail_server_busy))
            if (code !in 200..299) throw IOException("HTTP $code")
            return body
        } finally {
            c.disconnect()
        }
    }

    /** Follows redirects by hand (maps.app.goo.gl short links) and stops at the first hop with coordinates. */
    private fun expand(url: String): String {
        var current = url
        repeat(6) {
            val c = URL(current).openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            c.setRequestProperty("User-Agent", "Mozilla/5.0")
            try {
                val location = c.getHeaderField("Location")
                if (c.responseCode !in 300..399 || location == null) return current
                current = URL(URL(current), location).toString()
                if (LinkParser.coords(current) != null) return current
            } finally {
                c.disconnect()
            }
        }
        return current
    }
}

object LinkParser {
    private val patterns = listOf(
        Regex("""!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)"""),
        Regex("""[?&](?:q|query|ll|center|destination|daddr|sll)=(-?\d+\.\d+)\s*,\s*(-?\d+\.\d+)"""),
        Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)"""),
        Regex("""/maps/(?:search|place|dir)/(-?\d+\.\d+),\s*\+?(-?\d+\.\d+)"""),
        Regex("""^geo:(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)"""),
    )

    fun coords(url: String): LatLng? {
        val u = runCatching { URLDecoder.decode(url, "UTF-8") }.getOrDefault(url)
        for (r in patterns) {
            val m = r.find(u) ?: continue
            val p = LatLng(m.groupValues[1].toDouble(), m.groupValues[2].toDouble())
            if (p.valid && !(p.lat == 0.0 && p.lng == 0.0)) return p
        }
        return null
    }

    /** The place name in links like `...?q=Cho+Ben+Thanh`. */
    fun query(url: String): String? =
        Regex("""[?&]q=([^&]+)""").find(url)?.groupValues?.get(1)
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
}

object Gpx {
    /** Track or route points in order; falls back to waypoints when the file has neither. */
    fun parse(input: InputStream): List<LatLng> {
        val track = ArrayList<LatLng>()
        val wpts = ArrayList<LatLng>()
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(input, null)
        while (p.next() != XmlPullParser.END_DOCUMENT) {
            if (p.eventType != XmlPullParser.START_TAG) continue
            val name = p.name.substringAfter(':')
            if (name != "trkpt" && name != "rtept" && name != "wpt") continue
            val lat = p.getAttributeValue(null, "lat")?.toDoubleOrNull() ?: continue
            val lon = p.getAttributeValue(null, "lon")?.toDoubleOrNull() ?: continue
            (if (name == "wpt") wpts else track) += LatLng(lat, lon)
        }
        return track.ifEmpty { wpts }
    }
}
