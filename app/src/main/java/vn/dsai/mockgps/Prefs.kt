package vn.dsai.mockgps

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class Favorite(val name: String, val point: LatLng)

class Prefs(ctx: Context) {
    companion object {
        const val DEFAULT_ROUTING = "https://routing.openstreetmap.de/routed-{profile}/route/v1/driving/"
    }

    private val sp = ctx.applicationContext.getSharedPreferences("mockgps", Context.MODE_PRIVATE)

    var lastPoint: LatLng?
        get() {
            val lat = sp.getString("last_lat", null)?.toDoubleOrNull() ?: return null
            val lng = sp.getString("last_lng", null)?.toDoubleOrNull() ?: return null
            return LatLng(lat, lng)
        }
        set(v) = sp.edit {
            if (v == null) {
                remove("last_lat"); remove("last_lng")
            } else {
                putString("last_lat", v.lat.toString()); putString("last_lng", v.lng.toString())
            }
        }

    var favorites: List<Favorite>
        get() = runCatching {
            val arr = JSONArray(sp.getString("favorites", "[]"))
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Favorite(o.getString("name"), LatLng(o.getDouble("lat"), o.getDouble("lng")))
            }
        }.getOrDefault(emptyList())
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(JSONObject().put("name", it.name).put("lat", it.point.lat).put("lng", it.point.lng)) }
            sp.edit { putString("favorites", arr.toString()) }
        }

    fun findFavorite(name: String): Favorite? = favorites.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    var routeSpeedKmh: Float
        get() = sp.getFloat("route_speed", Travel.MOTORBIKE.kmh)
        set(v) = sp.edit { putFloat("route_speed", v) }

    var travel: Travel
        get() = Travel.entries.firstOrNull { it.name == sp.getString("travel", null) } ?: Travel.MOTORBIKE
        set(v) = sp.edit { putString("travel", v.name) }

    var routeByRoad: Boolean
        get() = sp.getBoolean("route_by_road", true)
        set(v) = sp.edit { putBoolean("route_by_road", v) }

    var routeEnd: RouteEnd
        get() = RouteEnd.entries.firstOrNull { it.name == sp.getString("route_end", null) } ?: RouteEnd.STOP
        set(v) = sp.edit { putString("route_end", v.name) }

    var joystickEnabled: Boolean
        get() = sp.getBoolean("joystick", false)
        set(v) = sp.edit { putBoolean("joystick", v) }

    var joystickKmh: Float
        get() = sp.getFloat("joystick_kmh", 5f)
        set(v) = sp.edit { putFloat("joystick_kmh", v) }

    /** Reported horizontal accuracy in meters; each fix varies ±30% around it. */
    var accuracyM: Float
        get() = sp.getFloat("accuracy_m", 5f)
        set(v) = sp.edit { putFloat("accuracy_m", v) }

    var altitudeM: Float
        get() = sp.getFloat("altitude_m", 10f)
        set(v) = sp.edit { putFloat("altitude_m", v) }

    /** Small random walk around the chosen point, like a real receiver. */
    var jitter: Boolean
        get() = sp.getBoolean("jitter", true)
        set(v) = sp.edit { putBoolean("jitter", v) }

    var intervalMs: Int
        get() = sp.getInt("interval_ms", 1000)
        set(v) = sp.edit { putInt("interval_ms", v) }

    var mockGps: Boolean
        get() = sp.getBoolean("mock_gps", true)
        set(v) = sp.edit { putBoolean("mock_gps", v) }

    var mockNetwork: Boolean
        get() = sp.getBoolean("mock_network", true)
        set(v) = sp.edit { putBoolean("mock_network", v) }

    /** Platform fused provider and Google Play services' fused provider. */
    var mockFused: Boolean
        get() = sp.getBoolean("mock_fused", true)
        set(v) = sp.edit { putBoolean("mock_fused", v) }

    /** "auto", "light" or "dark". */
    var mapTheme: String
        get() = sp.getString("map_theme", "auto") ?: "auto"
        set(v) = sp.edit { putString("map_theme", v) }

    /** OSRM endpoint; {profile} becomes foot, bike or car. */
    var routingServer: String
        get() = sp.getString("routing_server", null)?.takeIf { it.isNotBlank() } ?: DEFAULT_ROUTING
        set(v) = sp.edit { putString("routing_server", v.trim()) }

    /** Last camera position as lat, lng, zoom. */
    var camera: Triple<Double, Double, Double>?
        get() {
            val parts = sp.getString("camera", null)?.split(',')?.mapNotNull { it.toDoubleOrNull() } ?: return null
            return if (parts.size == 3) Triple(parts[0], parts[1], parts[2]) else null
        }
        set(v) = sp.edit { if (v == null) remove("camera") else putString("camera", "${v.first},${v.second},${v.third}") }

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit { putBoolean("onboarded", v) }

    var joystickX: Int
        get() = sp.getInt("joystick_x", -1)
        set(v) = sp.edit { putInt("joystick_x", v) }

    var joystickY: Int
        get() = sp.getInt("joystick_y", -1)
        set(v) = sp.edit { putInt("joystick_y", v) }
}
