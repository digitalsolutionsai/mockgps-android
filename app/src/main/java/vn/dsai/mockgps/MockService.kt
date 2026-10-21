package vn.dsai.mockgps

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Foreground service that keeps pushing a fake location once a second.
 *
 * It is exported behind android.permission.DUMP, so besides this app only `adb shell` can drive it:
 *   adb shell am start-foreground-service -n vn.dsai.mockgps/.MockService \
 *       -a vn.dsai.mockgps.POINT --es lat 10.7769 --es lng 106.7009
 */
class MockService : Service() {
    companion object {
        const val CHANNEL_ID = "mock"
        private const val NOTIF_ID = 1
        private const val TAG = "MockGPS"

        const val ACTION_POINT = "vn.dsai.mockgps.POINT"
        const val ACTION_ROUTE = "vn.dsai.mockgps.ROUTE"
        const val ACTION_PAUSE = "vn.dsai.mockgps.PAUSE"
        const val ACTION_RESUME = "vn.dsai.mockgps.RESUME"
        const val ACTION_SPEED = "vn.dsai.mockgps.SPEED"
        const val ACTION_JOYSTICK = "vn.dsai.mockgps.JOYSTICK"
        const val ACTION_STOP = "vn.dsai.mockgps.STOP"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val rnd = java.util.Random()
    private lateinit var prefs: Prefs
    private lateinit var injector: LocationInjector
    private var ticker: Job? = null
    private var busy: Any? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var overlay: JoystickOverlay? = null

    private var injecting = false
    private var mode = Mode.POINT
    private var base: LatLng? = null
    private var route: Route? = null
    private var progress = 0.0
    private var direction = 1
    private var routeSpeed = 35.0
    private var routeEnd = RouteEnd.STOP
    private var paused = false
    private var finished = false
    private var current: LatLng? = null
    private var heading = 0.0
    private var speedMs = 0.0
    private var altitude = 10.0
    private var accuracy = 5.0
    private var jitterOn = true
    private var interval = 1000L
    private var jitterN = 0.0
    private var jitterE = 0.0
    private var ticks = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        injector = LocationInjector(this)
        routeSpeed = prefs.routeSpeedKmh.toDouble()
        routeEnd = prefs.routeEnd
        altitude = prefs.altitudeM.toDouble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!goForeground()) {
            shutdown()
            return START_NOT_STICKY
        }
        val extras = intent?.extras ?: Bundle()
        when (intent?.action) {
            ACTION_POINT -> handlePoint(extras)
            ACTION_ROUTE -> handleRoute(extras)
            ACTION_PAUSE -> paused = true
            ACTION_RESUME -> {
                paused = false
                if (finished) { finished = false; progress = 0.0; direction = 1 }
            }
            ACTION_SPEED -> extras.num("speed")?.let { routeSpeed = it.coerceIn(0.5, 400.0) }
            ACTION_JOYSTICK -> handleJoystick(extras)
            ACTION_STOP -> {
                shutdown()
                return START_NOT_STICKY
            }
        }
        if (!injecting && busy == null) shutdown() else refresh()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (injecting) injector.stop()
        injecting = false
        hideJoystick()
        wakeLock?.takeIf { it.isHeld }?.release()
        MockState.status.value = MockStatus()
        scope.cancel()
        super.onDestroy()
    }

    // ---- commands -------------------------------------------------------------------------

    private fun handlePoint(extras: Bundle) {
        extras.num("alt")?.let { altitude = it }
        val lat = extras.num("lat")
        val lng = extras.num("lng") ?: extras.num("lon")
        val fav = extras.getString("fav")
        val q = extras.getString("q")
        when {
            lat != null && lng != null -> startPoint(LatLng(lat, lng))
            fav != null -> prefs.findFavorite(fav)?.let { startPoint(it.point) } ?: fail(getString(R.string.error_no_favorite, fav))
            q != null -> runAsync({ getString(R.string.error_search_failed, q, it) }) {
                val place = Places.search(this, q, current).firstOrNull()
                    ?: throw IllegalStateException(getString(R.string.error_detail_no_results))
                startPoint(place.point)
            }
            else -> (base ?: current ?: prefs.lastPoint)?.let { startPoint(it) } ?: fail(getString(R.string.error_missing_coordinates))
        }
    }

    private fun handleRoute(extras: Bundle) {
        extras.num("speed")?.let { routeSpeed = it.coerceIn(0.5, 400.0) }
        extras.getString("end")?.let { e -> RouteEnd.entries.firstOrNull { it.name.equals(e, true) }?.let { routeEnd = it } }
        val raw = extras.getString("points")
        if (raw == null) {
            val r = MockState.pendingRoute ?: return fail(getString(R.string.error_no_route))
            MockState.pendingRoute = null
            return startRoute(r)
        }
        val pts = raw.split(';', '|', '\n').mapNotNull { Geo.parseLatLng(it) }
        if (pts.size < 2) return fail(getString(R.string.error_route_min_points))
        if (extras.getString("by")?.lowercase() != "road") {
            Route.of(pts)?.let { startRoute(it) } ?: fail(getString(R.string.error_route_too_short))
            return
        }
        val travel = Travel.entries.firstOrNull { it.name.equals(extras.getString("travel"), true) } ?: Travel.MOTORBIKE
        runAsync({ getString(R.string.error_route_failed, it) }) {
            val r = Route.of(Places.route(this, pts, travel, prefs.routingServer))
                ?: throw IllegalStateException(getString(R.string.error_detail_route_too_short))
            startRoute(r)
        }
    }

    private fun handleJoystick(extras: Bundle) {
        val want = when (extras.getString("state")?.lowercase()) {
            "on", "show", "1", "true" -> true
            "off", "hide", "0", "false" -> false
            else -> overlay == null
        }
        prefs.joystickEnabled = want
        if (!want) return hideJoystick()
        if (injecting) showJoystick()
        else (current ?: prefs.lastPoint)?.let { startPoint(it) } ?: fail(getString(R.string.error_no_start_position))
    }

    private fun startPoint(p: LatLng) {
        if (!p.valid) return fail(getString(R.string.error_invalid_coordinates))
        mode = Mode.POINT
        base = p
        current = p
        route = null
        paused = false
        finished = false
        ensureInjecting()
        refresh()
    }

    private fun startRoute(r: Route) {
        mode = Mode.ROUTE
        route = r
        progress = 0.0
        direction = 1
        paused = false
        finished = false
        current = r.points.first()
        ensureInjecting()
        refresh()
    }

    /**
     * Runs a network lookup; if it fails and nothing was running yet, the service goes away again.
     * [errorMessage] turns the failure reason into the text shown to the user.
     */
    private fun runAsync(errorMessage: (String) -> String, block: suspend () -> Unit) {
        val token = Any()
        busy = token
        scope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(errorMessage(e.message ?: e.javaClass.simpleName))
            } finally {
                if (busy === token) {
                    busy = null
                    if (!injecting) shutdown()
                }
            }
        }
    }

    // ---- injection loop -------------------------------------------------------------------

    private fun ensureInjecting() {
        if (injecting) return
        if (!Permissions.isMockApp(this)) {
            return fail(getString(R.string.error_not_mock_app))
        }
        if (!prefs.mockGps && !prefs.mockNetwork && !prefs.mockFused) {
            return fail(getString(R.string.error_no_source))
        }
        interval = prefs.intervalMs.toLong().coerceIn(200, 5000)
        accuracy = prefs.accuracyM.toDouble().coerceIn(1.0, 500.0)
        jitterOn = prefs.jitter
        injector.start(prefs.mockGps, prefs.mockNetwork, prefs.mockFused)
        injecting = true
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MockGPS:inject")
            .apply { setReferenceCounted(false); acquire(6 * 60 * 60 * 1000L) }
        if (prefs.joystickEnabled) showJoystick()
        ticker = scope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                step((now - last) / 1000.0)
                last = now
                push()
                ticks++
                if (ticks % (5000 / interval).toInt().coerceAtLeast(1) == 0 && !Permissions.isMockApp(this@MockService)) {
                    fail(getString(R.string.error_mock_app_lost))
                    shutdown()
                    return@launch
                }
                publish()
                if (ticks % 3 == 0) notifyNow()
                delay(interval)
            }
        }
    }

    private fun step(dt: Double) {
        val (sx, sy) = MockState.stick
        val mag = hypot(sx, sy).coerceAtMost(1.0)
        if (mag > 0.08) {
            // Touching the joystick takes over from a running route.
            if (mode == Mode.ROUTE) {
                mode = Mode.POINT
                route = null
                base = current
                refresh()
            }
            val max = prefs.joystickKmh / 3.6
            val b = base ?: return
            base = Geo.offset(b, sy * max * dt, sx * max * dt)
            current = base
            heading = (Math.toDegrees(atan2(sx, sy)) + 360) % 360
            speedMs = max * mag
            return
        }
        when (mode) {
            Mode.POINT -> { current = base; speedMs = 0.0 }
            Mode.ROUTE -> stepRoute(dt)
        }
    }

    private fun stepRoute(dt: Double) {
        val r = route ?: return
        if (paused || finished) { speedMs = 0.0; return }
        val v = routeSpeed / 3.6 * (0.92 + rnd.nextDouble() * 0.16)
        progress += direction * v * dt
        if (progress >= r.length) {
            when (routeEnd) {
                RouteEnd.STOP -> { progress = r.length; finished = true; refresh() }
                RouteEnd.LOOP -> progress %= r.length
                RouteEnd.PINGPONG -> { progress = (2 * r.length - progress).coerceAtLeast(0.0); direction = -1 }
            }
        } else if (progress <= 0 && direction < 0) {
            progress = -progress
            direction = 1
        }
        val (p, b) = r.at(progress)
        current = p
        heading = if (direction > 0) b else (b + 180) % 360
        speedMs = if (finished) 0.0 else v
    }

    private fun push() {
        val p = current ?: return
        // A slow random walk around the true point so a parked phone does not look frozen.
        if (jitterOn) {
            val amp = (if (speedMs > 0.3) 0.15 else 0.4) * accuracy
            jitterN = (jitterN * 0.85 + rnd.nextGaussian() * amp * 0.35).coerceIn(-amp * 1.5, amp * 1.5)
            jitterE = (jitterE * 0.85 + rnd.nextGaussian() * amp * 0.35).coerceIn(-amp * 1.5, amp * 1.5)
        } else {
            jitterN = 0.0
            jitterE = 0.0
        }
        val q = Geo.offset(p, jitterN, jitterE)
        val moving = speedMs > 0.3
        val loc = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = q.lat
            longitude = q.lng
            altitude = this@MockService.altitude + if (jitterOn) rnd.nextGaussian() * 0.4 else 0.0
            accuracy = (this@MockService.accuracy * if (jitterOn) 0.7 + rnd.nextDouble() * 0.6 else 1.0).toFloat()
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            speed = speedMs.toFloat()
            if (moving) bearing = heading.toFloat()
            verticalAccuracyMeters = 4f
            speedAccuracyMetersPerSecond = 0.5f
            if (moving) bearingAccuracyDegrees = 8f
            extras = Bundle().apply { putInt("satellites", 8 + rnd.nextInt(5)) }
        }
        injector.push(loc)
    }

    private fun publish() {
        MockState.status.value = MockStatus(
            running = injecting,
            mode = mode,
            position = current,
            bearing = heading,
            speedKmh = speedMs * 3.6,
            route = if (mode == Mode.ROUTE) route else null,
            progressM = progress,
            paused = paused,
            finished = finished,
            routeSpeedKmh = routeSpeed,
            joystick = overlay != null,
            fused = injector.gmsActive,
        )
    }

    private fun refresh() {
        publish()
        notifyNow()
    }

    private fun shutdown() {
        ticker?.cancel()
        ticker = null
        if (injecting) {
            injector.stop()
            injecting = false
            (current ?: base)?.let { prefs.lastPoint = it }
        }
        hideJoystick()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        MockState.status.value = MockStatus()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fail(msg: String) {
        Log.w(TAG, msg)
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    // ---- joystick -------------------------------------------------------------------------

    private fun showJoystick() {
        if (overlay != null) return
        if (!Permissions.canOverlay(this)) return fail(getString(R.string.error_overlay_permission))
        overlay = JoystickOverlay(
            this, prefs,
            onMove = { x, y -> MockState.stick = x to y },
            onClose = {
                prefs.joystickEnabled = false
                hideJoystick()
                refresh()
            },
        ).also { it.show() }
    }

    private fun hideJoystick() {
        overlay?.hide()
        overlay = null
        MockState.stick = 0.0 to 0.0
    }

    // ---- notification ---------------------------------------------------------------------

    private fun goForeground(): Boolean = try {
        // On Android 14+ this needs the location permission; the UI and `mockgps setup` grant it first.
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
        true
    } catch (e: Exception) {
        Log.e(TAG, "startForeground failed", e)
        fail(getString(R.string.error_foreground_failed, e.message))
        false
    }

    private fun notifyNow() {
        if (injecting || busy != null) getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val r = route
        val title = when {
            !injecting -> getString(R.string.notif_preparing)
            mode == Mode.ROUTE && finished -> getString(R.string.common_arrived)
            mode == Mode.ROUTE && paused -> getString(R.string.common_route_paused)
            mode == Mode.ROUTE -> getString(R.string.common_following_route, routeSpeed.roundToInt())
            speedMs > 0.3 -> getString(R.string.notif_moving_joystick)
            else -> getString(R.string.common_mocking)
        }
        val coords = current?.format().orEmpty()
        val text = if (mode == Mode.ROUTE && r != null) {
            getString(R.string.notif_route_text, coords, Geo.formatDistance(progress), Geo.formatDistance(r.length))
        } else {
            coords
        }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mock)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (injecting) {
            if (mode == Mode.ROUTE) {
                val resume = paused || finished
                b.addAction(0, getString(if (resume) R.string.common_resume else R.string.common_pause), action(if (resume) ACTION_RESUME else ACTION_PAUSE, 1))
            }
            b.addAction(0, getString(if (overlay != null) R.string.notif_action_hide_joystick else R.string.notif_action_joystick), action(ACTION_JOYSTICK, 2))
            b.addAction(0, getString(R.string.common_stop), action(ACTION_STOP, 3))
        }
        return b.build()
    }

    private fun action(action: String, req: Int): PendingIntent = PendingIntent.getService(
        this, req, Intent(this, MockService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Doubles from the app arrive as numbers; values from `adb ... --es` arrive as strings. */
@Suppress("DEPRECATION")
private fun Bundle.num(key: String): Double? = when (val v = get(key)) {
    is Number -> v.toDouble()
    is String -> v.trim().toDoubleOrNull()
    else -> null
}

/** What the UI uses to drive [MockService]. */
object MockControl {
    private fun intent(ctx: Context, action: String) = Intent(ctx, MockService::class.java).setAction(action)

    fun point(ctx: Context, p: LatLng) = ContextCompat.startForegroundService(
        ctx, intent(ctx, MockService.ACTION_POINT).putExtra("lat", p.lat).putExtra("lng", p.lng),
    )

    fun route(ctx: Context, route: Route, speedKmh: Float, end: RouteEnd) {
        MockState.pendingRoute = route
        ContextCompat.startForegroundService(
            ctx, intent(ctx, MockService.ACTION_ROUTE).putExtra("speed", speedKmh.toDouble()).putExtra("end", end.name),
        )
    }

    /** Sends a control action to an already running service; does nothing otherwise. */
    fun send(ctx: Context, action: String, configure: Intent.() -> Unit = {}) {
        if (MockState.status.value.running) ctx.startService(intent(ctx, action).apply(configure))
    }
}
