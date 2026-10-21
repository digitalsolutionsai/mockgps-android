package vn.dsai.mockgps

import android.content.Context
import android.icu.text.MeasureFormat
import android.icu.text.NumberFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double) {
    val valid: Boolean get() = lat in -90.0..90.0 && lng in -180.0..180.0

    fun format(): String = String.format(Locale.US, "%.6f, %.6f", lat, lng)
}

object Geo {
    private const val EARTH_RADIUS = 6_371_008.8

    fun distance(a: LatLng, b: LatLng): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS * asin(min(1.0, sqrt(h)))
    }

    /** Initial heading from [a] to [b], 0..360 degrees clockwise from north. */
    fun bearing(a: LatLng, b: LatLng): Double {
        val phi1 = Math.toRadians(a.lat)
        val phi2 = Math.toRadians(b.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val y = sin(dLng) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** Moves [p] by [north] and [east] meters; a flat approximation that is fine for short hops. */
    fun offset(p: LatLng, north: Double, east: Double): LatLng {
        val dLat = north / EARTH_RADIUS
        val dLng = east / (EARTH_RADIUS * cos(Math.toRadians(p.lat)))
        return LatLng(p.lat + Math.toDegrees(dLat), p.lng + Math.toDegrees(dLng))
    }

    fun lerp(a: LatLng, b: LatLng, f: Double) = LatLng(a.lat + (b.lat - a.lat) * f, a.lng + (b.lng - a.lng) * f)

    private val pair = Regex("""^\s*\(?\s*(-?\d{1,3}(?:\.\d+)?)\s*[,;\s]\s*(-?\d{1,3}(?:\.\d+)?)\s*\)?\s*$""")

    /** Parses "10.7769, 106.7009" or "10.7769 106.7009". */
    fun parseLatLng(text: String): LatLng? {
        val m = pair.find(text) ?: return null
        return LatLng(m.groupValues[1].toDouble(), m.groupValues[2].toDouble()).takeIf { it.valid }
    }

    /** "850 m", "3.3 km", "3,3 км"… in the app's language (ICU picks the unit symbol and decimal mark). */
    fun formatDistance(m: Double): String {
        val locale = Locale.getDefault()
        val km = m >= 1000
        val number = NumberFormat.getInstance(locale).apply {
            minimumFractionDigits = if (km) 1 else 0
            maximumFractionDigits = if (km) 1 else 0
        }
        val fmt = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT, number)
        return if (km) fmt.format(Measure(m / 1000, MeasureUnit.KILOMETER))
        else fmt.format(Measure(m.roundToInt(), MeasureUnit.METER))
    }

    fun formatDuration(ctx: Context, seconds: Double): String {
        val min = (seconds / 60).roundToInt().coerceAtLeast(1)
        return if (min < 60) ctx.getString(R.string.duration_minutes, min)
        else ctx.getString(R.string.duration_hours_minutes, min / 60, min % 60)
    }
}

/** A polyline that can be walked along by distance. */
class Route private constructor(val points: List<LatLng>) {
    private val cum = DoubleArray(points.size)

    init {
        for (i in 1 until points.size) cum[i] = cum[i - 1] + Geo.distance(points[i - 1], points[i])
    }

    val length: Double get() = cum.last()

    /** Position and heading after travelling [d] meters from the first point. */
    fun at(d: Double): Pair<LatLng, Double> {
        val x = d.coerceIn(0.0, length)
        var i = cum.binarySearch(x)
        if (i < 0) i = -i - 2
        i = i.coerceIn(0, points.size - 2)
        val seg = cum[i + 1] - cum[i]
        val f = if (seg > 0) (x - cum[i]) / seg else 0.0
        return Geo.lerp(points[i], points[i + 1], f) to Geo.bearing(points[i], points[i + 1])
    }

    companion object {
        /** Drops repeated points; returns null if fewer than two distinct points remain. */
        fun of(raw: List<LatLng>): Route? {
            val pts = ArrayList<LatLng>(raw.size)
            for (p in raw) if (pts.isEmpty() || Geo.distance(pts.last(), p) > 0.3) pts += p
            return if (pts.size >= 2) Route(pts) else null
        }
    }
}
