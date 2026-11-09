package com.autoverse.launcher.core.routing

import kotlin.math.PI
import kotlin.math.cos

/** Where the car is along the route right now. */
data class RouteProgress(
    /** Index of the route segment (points[i]..points[i+1]) the car is on. */
    val segmentIndex: Int,
    val traveledMeters: Double,
    val remainingMeters: Double,
    val remainingSeconds: Double,
    /** Perpendicular distance from the car to the route. */
    val offRouteMeters: Double,
    val nextManeuver: Maneuver?,
    val distanceToNextManeuver: Double,
    /** The instruction after [nextManeuver], when it follows closely ("then turn right"). */
    val thenManeuver: Maneuver?,
) {
    val arrived: Boolean get() = remainingMeters <= ARRIVAL_RADIUS_M

    companion object {
        const val ARRIVAL_RADIUS_M = 30.0
    }
}

/** Off by more than this (beyond GPS accuracy) and the car has left the route. */
const val OFF_ROUTE_METERS = 45.0

/** Consecutive off-route fixes before a new route is calculated (filters GPS jumps). */
const val OFF_ROUTE_FIXES_BEFORE_REROUTE = 3

/** "Then …" is shown only when the following instruction comes this soon. */
const val THEN_MANEUVER_WITHIN_M = 150.0

/**
 * Snaps [position] to [route] and measures progress. The search looks a
 * little behind and well ahead of [hintSegment] (the last known segment) so
 * a road that loops back near itself never makes the car jump along the
 * route; pass -1 for a full search (start, or after a reroute).
 */
fun computeProgress(route: Route, position: GeoPoint, hintSegment: Int = -1): RouteProgress {
    val pts = route.points
    if (pts.size < 2) {
        return RouteProgress(0, 0.0, 0.0, 0.0, 0.0, null, 0.0, null)
    }
    val lastSeg = pts.size - 2
    val from = if (hintSegment < 0) 0 else (hintSegment - 3).coerceAtLeast(0)
    val to = if (hintSegment < 0) lastSeg else (hintSegment + 80).coerceAtMost(lastSeg)

    var bestSeg = from
    var bestT = 0.0
    var bestDist = Double.MAX_VALUE
    for (i in from..to) {
        val (t, d) = projectOnSegment(position, pts[i], pts[i + 1])
        if (d < bestDist) {
            bestDist = d
            bestSeg = i
            bestT = t
        }
    }
    val segLen = route.cumulativeMeters[bestSeg + 1] - route.cumulativeMeters[bestSeg]
    val traveled = route.cumulativeMeters[bestSeg] + segLen * bestT
    val remaining = (route.totalMeters - traveled).coerceAtLeast(0.0)

    val segTime = route.cumulativeSeconds[bestSeg + 1] - route.cumulativeSeconds[bestSeg]
    val elapsed = route.cumulativeSeconds[bestSeg] + segTime * bestT
    val remainingSeconds = (route.totalSeconds - elapsed).coerceAtLeast(0.0)

    // Next instruction: the first one at least a few metres ahead of the car.
    val upcoming = route.maneuvers.filter { route.cumulativeMeters[it.pointIndex] > traveled + 3.0 }
    val next = upcoming.firstOrNull()
    val distToNext = next?.let { route.cumulativeMeters[it.pointIndex] - traveled } ?: remaining
    val then = upcoming.getOrNull(1)?.takeIf { second ->
        next != null && route.cumulativeMeters[second.pointIndex] - route.cumulativeMeters[next.pointIndex] <= THEN_MANEUVER_WITHIN_M
    }

    return RouteProgress(
        segmentIndex = bestSeg,
        traveledMeters = traveled,
        remainingMeters = remaining,
        remainingSeconds = remainingSeconds,
        offRouteMeters = bestDist,
        nextManeuver = next,
        distanceToNextManeuver = distToNext.coerceAtLeast(0.0),
        thenManeuver = then,
    )
}

/**
 * Projects [p] onto segment a→b in a local flat (equirectangular) frame —
 * exact enough over a road segment. Returns (t in 0..1, distance in metres).
 */
internal fun projectOnSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): Pair<Double, Double> {
    val mPerDegLat = 111_320.0
    val mPerDegLon = 111_320.0 * cos(a.latitude * PI / 180)
    val ax = 0.0
    val ay = 0.0
    val bx = (b.longitude - a.longitude) * mPerDegLon
    val by = (b.latitude - a.latitude) * mPerDegLat
    val px = (p.longitude - a.longitude) * mPerDegLon
    val py = (p.latitude - a.latitude) * mPerDegLat
    val len2 = bx * bx + by * by
    val t = if (len2 == 0.0) 0.0 else (((px - ax) * bx + (py - ay) * by) / len2).coerceIn(0.0, 1.0)
    val cx = ax + t * bx
    val cy = ay + t * by
    val dx = px - cx
    val dy = py - cy
    return t to kotlin.math.sqrt(dx * dx + dy * dy)
}

/**
 * Voice prompt thresholds: a heads-up well before the turn (further at
 * highway speed) and a "now" prompt close to it. Pure for tests.
 */
enum class PromptStage { FAR, NEAR }

fun promptStageFor(distanceToManeuver: Double, speedKmh: Int?): PromptStage? {
    val far = if ((speedKmh ?: 0) >= 70) 1_000.0 else 400.0
    val near = if ((speedKmh ?: 0) >= 70) 250.0 else 80.0
    return when {
        distanceToManeuver <= near -> PromptStage.NEAR
        distanceToManeuver <= far -> PromptStage.FAR
        else -> null
    }
}
