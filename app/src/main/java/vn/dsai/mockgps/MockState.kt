package vn.dsai.mockgps

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow

enum class Mode { POINT, ROUTE }

enum class RouteEnd(@StringRes val label: Int) {
    STOP(R.string.route_end_stop),
    LOOP(R.string.route_end_loop),
    PINGPONG(R.string.route_end_pingpong),
}

enum class Travel(@StringRes val label: Int, val kmh: Float, val osrm: String) {
    FOOT(R.string.route_travel_foot, 5f, "foot"),
    BIKE(R.string.route_travel_bike, 15f, "bike"),
    MOTORBIKE(R.string.route_travel_motorbike, 35f, "car"),
    CAR(R.string.route_travel_car, 50f, "car"),
}

data class MockStatus(
    val running: Boolean = false,
    val mode: Mode = Mode.POINT,
    val position: LatLng? = null,
    val bearing: Double = 0.0,
    val speedKmh: Double = 0.0,
    val route: Route? = null,
    val progressM: Double = 0.0,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val routeSpeedKmh: Double = 0.0,
    val joystick: Boolean = false,
    val fused: Boolean = false,
)

/** Shared between the service and the UI, which always run in the same process. */
object MockState {
    val status = MutableStateFlow(MockStatus())

    /** Joystick deflection: x towards east, y towards north, each in -1..1. */
    @Volatile var stick: Pair<Double, Double> = 0.0 to 0.0

    /** Route handed over by the UI so large tracks don't have to travel inside an Intent. */
    @Volatile var pendingRoute: Route? = null
}
