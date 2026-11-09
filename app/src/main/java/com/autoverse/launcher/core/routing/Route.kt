package com.autoverse.launcher.core.routing

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A WGS-84 position. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** A place the driver can navigate to: what to call it, and where it is. */
data class NavPlace(
    val name: String,
    val detail: String,
    val latitude: Double,
    val longitude: Double,
) {
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
}

/** What the driver has to do at a maneuver point. */
enum class ManeuverType {
    CONTINUE, TURN_LEFT, TURN_SLIGHT_LEFT, TURN_SHARP_LEFT, TURN_RIGHT, TURN_SLIGHT_RIGHT,
    TURN_SHARP_RIGHT, KEEP_LEFT, KEEP_RIGHT, U_TURN, ROUNDABOUT, EXIT_LEFT, EXIT_RIGHT, ARRIVE,
}

/** One instruction on the route, anchored at [pointIndex] of [Route.points]. */
data class Maneuver(
    val pointIndex: Int,
    val type: ManeuverType,
    /** Roundabout exit number (1-based), 0 when not a roundabout. */
    val roundaboutExit: Int = 0,
)

/**
 * A computed car route: the polyline, per-point cumulative distance and time,
 * and the maneuvers. Distances in metres, times in seconds.
 */
data class Route(
    val points: List<GeoPoint>,
    val cumulativeMeters: DoubleArray,
    val cumulativeSeconds: DoubleArray,
    val maneuvers: List<Maneuver>,
    /** Speed limit (km/h) of the road arriving at each point; 0 = not mapped. */
    val speedLimits: IntArray = IntArray(points.size),
    /** Point indices of mapped speed cameras along the route. */
    val cameras: List<Int> = emptyList(),
) {
    val totalMeters: Double get() = cumulativeMeters.lastOrNull() ?: 0.0
    val totalSeconds: Double get() = cumulativeSeconds.lastOrNull() ?: 0.0

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)

    companion object {
        /**
         * Builds a route from points, optional per-point times (seconds from
         * start, as BRouter reports them) and maneuvers. Missing or broken
         * times fall back to [FALLBACK_SPEED_MPS] so ETA is never blank.
         */
        fun of(
            points: List<GeoPoint>,
            times: List<Double>?,
            maneuvers: List<Maneuver>,
            speedLimits: IntArray? = null,
            cameras: List<Int> = emptyList(),
        ): Route {
            val meters = DoubleArray(points.size)
            for (i in 1 until points.size) {
                meters[i] = meters[i - 1] + distanceMeters(points[i - 1], points[i])
            }
            val seconds = DoubleArray(points.size)
            val usableTimes = times != null && times.size == points.size &&
                times.last() > 0.0 && times.zipWithNext().all { (a, b) -> b >= a }
            for (i in points.indices) {
                seconds[i] = if (usableTimes) times!![i] else meters[i] / FALLBACK_SPEED_MPS
            }
            val withArrival = maneuvers.filter { it.pointIndex in points.indices }
                .sortedBy { it.pointIndex }
                .let { list ->
                    if (points.isNotEmpty() && list.none { it.type == ManeuverType.ARRIVE }) {
                        list + Maneuver(points.lastIndex, ManeuverType.ARRIVE)
                    } else list
                }
            val limits = speedLimits?.takeIf { it.size == points.size } ?: IntArray(points.size)
            return Route(points, meters, seconds, withArrival, limits, cameras.filter { it in points.indices }.distinct().sorted())
        }

        /** ~40 km/h, a fair Indian city average when the router gives no times. */
        const val FALLBACK_SPEED_MPS = 11.0
    }
}

private const val EARTH_RADIUS_M = 6_371_008.8

/** Great-circle distance in metres. */
fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val lat1 = a.latitude * PI / 180
    val lat2 = b.latitude * PI / 180
    val dLat = lat2 - lat1
    val dLon = (b.longitude - a.longitude) * PI / 180
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

/** Short spoken/displayed instruction, e.g. "Turn left", "Take exit 2 at the roundabout". */
fun maneuverText(m: Maneuver): String = when (m.type) {
    ManeuverType.CONTINUE -> "Continue straight"
    ManeuverType.TURN_LEFT -> "Turn left"
    ManeuverType.TURN_SLIGHT_LEFT -> "Bear left"
    ManeuverType.TURN_SHARP_LEFT -> "Turn sharp left"
    ManeuverType.TURN_RIGHT -> "Turn right"
    ManeuverType.TURN_SLIGHT_RIGHT -> "Bear right"
    ManeuverType.TURN_SHARP_RIGHT -> "Turn sharp right"
    ManeuverType.KEEP_LEFT -> "Keep left"
    ManeuverType.KEEP_RIGHT -> "Keep right"
    ManeuverType.U_TURN -> "Make a U-turn"
    ManeuverType.ROUNDABOUT ->
        if (m.roundaboutExit > 0) "At the roundabout, take exit ${m.roundaboutExit}" else "Enter the roundabout"
    ManeuverType.EXIT_LEFT -> "Take the exit on the left"
    ManeuverType.EXIT_RIGHT -> "Take the exit on the right"
    ManeuverType.ARRIVE -> "You have arrived"
}

/** "350 m", "1.2 km", "12 km" — rounded the way a driver reads it. */
fun formatDistance(meters: Double): String = when {
    meters < 50 -> "${((meters / 5).roundToInt() * 5).coerceAtLeast(0)} m"
    meters < 1000 -> "${(meters / 50).roundToInt() * 50} m"
    meters < 10_000 -> String.format(java.util.Locale.US, "%.1f km", meters / 1000)
    else -> "${(meters / 1000).roundToInt()} km"
}

/** Spoken distance: "500 metres", "1.5 kilometres". */
fun spokenDistance(meters: Double): String = when {
    meters < 1000 -> "${(meters / 50).roundToInt().coerceAtLeast(1) * 50} metres"
    else -> {
        val km = (meters / 100).roundToInt() / 10.0
        if (km == km.toInt().toDouble()) "${km.toInt()} kilometre${if (km.toInt() == 1) "" else "s"}"
        else "$km kilometres"
    }
}

/** "1 h 25 min", "18 min", "1 min". */
fun formatDuration(seconds: Double): String {
    val totalMin = (seconds / 60).roundToInt().coerceAtLeast(1)
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "$h h $m min" else "$m min"
}

/** BRouter voice-hint command index (JSON formatter, mode 9) to [ManeuverType]. */
fun maneuverTypeForBRouterCommand(cmd: Int): ManeuverType? = when (cmd) {
    1 -> ManeuverType.CONTINUE
    2 -> ManeuverType.TURN_LEFT
    3 -> ManeuverType.TURN_SLIGHT_LEFT
    4 -> ManeuverType.TURN_SHARP_LEFT
    5 -> ManeuverType.TURN_RIGHT
    6 -> ManeuverType.TURN_SLIGHT_RIGHT
    7 -> ManeuverType.TURN_SHARP_RIGHT
    8 -> ManeuverType.KEEP_LEFT
    9 -> ManeuverType.KEEP_RIGHT
    10, 11, 15 -> ManeuverType.U_TURN
    13, 14 -> ManeuverType.ROUNDABOUT
    17 -> ManeuverType.EXIT_LEFT
    18 -> ManeuverType.EXIT_RIGHT
    else -> null // 12 off-route, 16 beeline: nothing to tell the driver
}
