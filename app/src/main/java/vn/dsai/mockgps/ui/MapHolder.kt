package vn.dsai.mockgps.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.coalesce
import org.maplibre.android.style.expressions.Expression.eq
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import vn.dsai.mockgps.Geo
import vn.dsai.mockgps.LatLng
import vn.dsai.mockgps.R
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.maplibre.android.geometry.LatLng as MlLatLng

private fun LatLng.ml() = MlLatLng(lat, lng)
private fun MlLatLng.app() = LatLng(latitude, longitude)
private fun LatLng.point() = Point.fromLngLat(lng, lat)

/**
 * Owns the MapLibre MapView (OpenFreeMap vector tiles) and its overlay layers, so Compose only calls
 * [render], [showMock], [showHere], [moveTo], [fit] and [setInsets]. Calls made before the style has
 * loaded are remembered and applied once it is ready.
 */
class MapHolder(ctx: Context, dark: Boolean) {
    var onTap: (LatLng) -> Unit = {}
    var onLongPress: (LatLng) -> Unit = {}
    var onUserGesture: () -> Unit = {}

    val view = MapView(ctx).apply { onCreate(null) }
    private val density = ctx.resources.displayMetrics.density
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private val pending = mutableListOf<(MapLibreMap) -> Unit>()

    private var selected: LatLng? = null
    private var waypoints: List<LatLng> = emptyList()
    private var route: List<LatLng>? = null
    private var mock: LatLng? = null
    private var mockBearing: Double? = null
    private var here: LatLng? = null
    private var hereRadiusM = 0.0
    private var pulse: ValueAnimator? = null
    private var insetTop = 0
    private var insetBottom = 0
    /** Restarts the camera animation in progress; a padding change cancels it (e.g. the keyboard closing after a search). */
    private var flight: (() -> Unit)? = null

    init {
        view.getMapAsync { m ->
            map = m
            m.uiSettings.apply {
                isLogoEnabled = false
                isTiltGesturesEnabled = false
                attributionGravity = Gravity.TOP or Gravity.START
                compassGravity = Gravity.TOP or Gravity.END
            }
            m.addOnMapClickListener { onTap(it.app()); true }
            m.addOnMapLongClickListener { onLongPress(it.app()); true }
            m.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) onUserGesture()
            }
            // MapView makes itself focusable once ready; after a search clears focus it would then get the
            // Enter key-up from a hardware keyboard and zoom in, cancelling the move to the result.
            view.isFocusable = false
            view.isFocusableInTouchMode = false
            applyInsets()
            m.setStyle(Style.Builder().fromUri(if (dark) STYLE_DARK else STYLE_LIGHT)) { s ->
                runCatching { localizeLabels(s) }.onFailure { Log.w(TAG, "localize labels", it) }
                runCatching { addOverlays(ctx, s) }.onFailure { Log.e(TAG, "map overlays", it) }
                style = s
                drawStatic()
                drawMock()
                drawHere()
            }
            pending.forEach { it(m) }
            pending.clear()
        }
        view.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) applyInsets()
        }
    }

    private fun withMap(block: (MapLibreMap) -> Unit) {
        map?.let(block) ?: pending.add(block)
    }

    val center: LatLng? get() = map?.cameraPosition?.target?.app()

    val camera: Triple<Double, Double, Double>?
        get() = map?.cameraPosition?.let { c -> c.target?.let { Triple(it.latitude, it.longitude, c.zoom) } }

    /** Pixel rows (in this view) covered by the search bar at the top and the sheet at the bottom. */
    fun setInsets(top: Int, bottomEdge: Int) {
        if (top == insetTop && bottomEdge == insetBottom) return
        insetTop = top
        insetBottom = bottomEdge
        applyInsets()
    }

    private fun applyInsets() {
        val m = map ?: return
        val h = view.height
        if (h == 0) return
        val bottom = if (insetBottom > 0) (h - insetBottom).coerceAtLeast(0) else 0
        val interrupted = flight
        m.moveCamera(CameraUpdateFactory.paddingTo(0.0, insetTop.toDouble(), 0.0, bottom.toDouble()))
        interrupted?.invoke()
        val margin = (8 * density).roundToInt()
        m.uiSettings.setAttributionMargins(margin, insetTop + margin, margin, margin)
        m.uiSettings.setCompassMargins(margin, insetTop + margin, margin, margin)
    }

    fun render(selected: LatLng?, waypoints: List<LatLng>, route: List<LatLng>?) {
        this.selected = selected
        this.waypoints = waypoints
        this.route = route
        drawStatic()
    }

    fun showMock(p: LatLng?, bearing: Double?) {
        mock = p
        mockBearing = bearing
        drawMock()
    }

    /**
     * The device's real position. The app only has approximate location, which Android blurs by a
     * kilometre or two, so it is drawn as a shaded circle of [radiusM] rather than a precise dot.
     */
    fun showHere(p: LatLng?, radiusM: Double) {
        here = p
        hereRadiusM = radiusM
        drawHere()
    }

    /** Frames the circle from [showHere] and pulses it so it is easy to spot. */
    fun fitHere() {
        val p = here ?: return
        val r = hereRadiusM.coerceAtLeast(150.0)
        fit(listOf(Geo.offset(p, -r, -r), Geo.offset(p, r, r)))
        pulseHere()
    }

    fun onDestroy() {
        pulse?.cancel()
        pulse = null
    }

    fun moveTo(p: LatLng, minZoom: Double? = null) = withMap { m ->
        val zoom = maxOf(minZoom ?: 0.0, m.cameraPosition.zoom)
        animate(m, CameraUpdateFactory.newLatLngZoom(p.ml(), zoom), 500)
    }

    fun jumpTo(p: LatLng, zoom: Double) = withMap { m ->
        m.moveCamera(CameraUpdateFactory.newLatLngZoom(p.ml(), zoom))
    }

    fun fit(points: List<LatLng>) {
        if (points.size < 2) return
        withMap { m ->
            val bounds = LatLngBounds.Builder().includes(points.map { it.ml() }).build()
            animate(m, CameraUpdateFactory.newLatLngBounds(bounds, (40 * density).roundToInt()), 700)
        }
    }

    private fun animate(m: MapLibreMap, update: CameraUpdate, ms: Int) {
        val restart: () -> Unit = { animate(m, update, ms) }
        flight = restart
        m.animateCamera(update, ms, object : MapLibreMap.CancelableCallback {
            override fun onCancel() = done()
            override fun onFinish() = done()
            private fun done() {
                if (flight === restart) flight = null
            }
        })
    }

    private fun drawStatic() {
        val s = style ?: return
        val line = route?.takeIf { it.size >= 2 }?.let { r -> listOf(Feature.fromGeometry(LineString.fromLngLats(r.map { it.point() }))) }
        s.getSourceAs<GeoJsonSource>(SRC_ROUTE)?.setGeoJson(FeatureCollection.fromFeatures(line.orEmpty()))
        s.getSourceAs<GeoJsonSource>(SRC_WAYPOINTS)?.setGeoJson(
            FeatureCollection.fromFeatures(
                waypoints.mapIndexed { i, p -> Feature.fromGeometry(p.point()).apply { addStringProperty("n", "${i + 1}") } },
            ),
        )
        s.getSourceAs<GeoJsonSource>(SRC_SELECTED)?.setGeoJson(
            FeatureCollection.fromFeatures(listOfNotNull(selected?.let { Feature.fromGeometry(it.point()) })),
        )
    }

    private fun drawMock() {
        val s = style ?: return
        val feature = mock?.let { p ->
            Feature.fromGeometry(p.point()).apply {
                addBooleanProperty("moving", mockBearing != null)
                addNumberProperty("bearing", mockBearing ?: 0.0)
            }
        }
        s.getSourceAs<GeoJsonSource>(SRC_MOCK)?.setGeoJson(FeatureCollection.fromFeatures(listOfNotNull(feature)))
    }

    private fun drawHere() {
        val s = style ?: return
        val ring = here?.let { c ->
            (0..64).map { i ->
                val a = 2 * PI * i / 64
                Geo.offset(c, hereRadiusM * cos(a), hereRadiusM * sin(a)).point()
            }
        }
        val feature = ring?.let { Feature.fromGeometry(Polygon.fromLngLats(listOf(it))) }
        s.getSourceAs<GeoJsonSource>(SRC_HERE)?.setGeoJson(FeatureCollection.fromFeatures(listOfNotNull(feature)))
    }

    private fun pulseHere() {
        val area = style?.getLayer("here-area") ?: return
        val edge = style?.getLayer("here-edge") ?: return
        pulse?.cancel()
        pulse = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 1400
            repeatCount = 2
            addUpdateListener {
                val f = it.animatedValue as Float
                area.setProperties(fillOpacity(HERE_FILL + 0.13f * f))
                edge.setProperties(lineOpacity(HERE_EDGE - 0.6f * f))
            }
            start()
        }
    }

    /** Shows place names in the app language when OSM has them, falling back to the local name. */
    private fun localizeLabels(s: Style) {
        val locale = Locale.getDefault()
        val keys = buildList {
            if (locale.language == "zh") add(if (locale.country in setOf("TW", "HK", "MO") || locale.script == "Hant") "name:zh-Hant" else "name:zh-Hans")
            add("name:${locale.language}")
            if (locale.language == "en") add("name_en")
            add("name")
        }
        val label = coalesce(*keys.map { get(it) }.toTypedArray())
        for (layer in s.layers) {
            if (layer !is SymbolLayer) continue
            val current = layer.textField.expression?.toString() ?: continue
            // Leave road numbers and house numbers alone.
            if ("name" in current && "ref" !in current && "housenumber" !in current) layer.setProperties(textField(label))
        }
    }

    private fun addOverlays(ctx: Context, s: Style) {
        ContextCompat.getDrawable(ctx, R.drawable.ic_pin)?.let { s.addImage(IMG_PIN, it.toBitmap()) }
        s.addImage(IMG_ARROW, arrowBitmap())
        listOf(SRC_HERE, SRC_ROUTE, SRC_WAYPOINTS, SRC_SELECTED, SRC_MOCK).forEach { s.addSource(GeoJsonSource(it)) }

        s.addLayer(FillLayer("here-area", SRC_HERE).withProperties(fillColor(BLUE), fillOpacity(HERE_FILL)))
        s.addLayer(LineLayer("here-edge", SRC_HERE).withProperties(lineColor(BLUE), lineWidth(1.5f), lineOpacity(HERE_EDGE)))

        s.addLayer(
            LineLayer("route-casing", SRC_ROUTE).withProperties(
                lineColor(Color.WHITE), lineWidth(9f),
                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        s.addLayer(
            LineLayer("route", SRC_ROUTE).withProperties(
                lineColor(BLUE), lineWidth(5f),
                lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
        s.addLayer(
            CircleLayer("waypoints", SRC_WAYPOINTS).withProperties(
                circleRadius(11f), circleColor(ORANGE), circleStrokeColor(Color.WHITE), circleStrokeWidth(2.5f),
            ),
        )
        s.addLayer(
            SymbolLayer("waypoint-labels", SRC_WAYPOINTS).withProperties(
                textField(get("n")), textFont(arrayOf("Noto Sans Bold")), textSize(12f), textColor(Color.WHITE),
                textAllowOverlap(true), textIgnorePlacement(true),
            ),
        )
        s.addLayer(
            SymbolLayer("selected", SRC_SELECTED).withProperties(
                iconImage(IMG_PIN), iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                iconAllowOverlap(true), iconIgnorePlacement(true),
            ),
        )
        s.addLayer(CircleLayer("mock-halo", SRC_MOCK).withProperties(circleRadius(22f), circleColor(BLUE), circleOpacity(0.18f)))
        s.addLayer(
            SymbolLayer("mock-heading", SRC_MOCK).withProperties(
                iconImage(IMG_ARROW), iconRotate(get("bearing")),
                iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                iconAllowOverlap(true), iconIgnorePlacement(true),
            ).withFilter(eq(get("moving"), literal(true))),
        )
        s.addLayer(
            CircleLayer("mock", SRC_MOCK).withProperties(
                circleRadius(8f), circleColor(BLUE), circleStrokeColor(Color.WHITE), circleStrokeWidth(3f),
            ),
        )
    }

    /** A small triangle above the centre; rotating the icon swings it around the position dot. */
    private fun arrowBitmap(): Bitmap {
        val size = (44 * density).roundToInt()
        val bmp = createBitmap(size, size)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BLUE }
        val mid = size / 2f
        val w = 7 * density
        c.drawPath(
            Path().apply {
                moveTo(mid, 0f)
                lineTo(mid - w, w * 1.6f)
                lineTo(mid + w, w * 1.6f)
                close()
            },
            p,
        )
        return bmp
    }

    private companion object {
        const val TAG = "MockGPS"
        const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
        const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"
        const val SRC_ROUTE = "app-route"
        const val SRC_WAYPOINTS = "app-waypoints"
        const val SRC_SELECTED = "app-selected"
        const val SRC_MOCK = "app-mock"
        const val SRC_HERE = "app-here"
        const val IMG_PIN = "app-pin"
        const val IMG_ARROW = "app-arrow"
        const val BLUE = 0xFF1A73E8.toInt()
        const val ORANGE = 0xFFF29900.toInt()
        const val HERE_FILL = 0.12f
        const val HERE_EDGE = 0.8f
    }
}
