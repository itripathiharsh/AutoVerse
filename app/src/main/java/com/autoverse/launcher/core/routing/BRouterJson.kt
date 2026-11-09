package com.autoverse.launcher.core.routing

import org.json.JSONObject

/**
 * Parses BRouter's GeoJSON track (FormatJson) into a [Route]: the LineString
 * coordinates ([lon, lat, ele?]), the per-point "times" and the "voicehints"
 * rows ([indexInTrack, command, exit, distanceToNext, angle, geometry?]).
 */
fun parseBRouterJson(json: String): Route {
    val root = JSONObject(json)
    val feature = root.getJSONArray("features").getJSONObject(0)
    val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
    val points = ArrayList<GeoPoint>(coords.length())
    for (i in 0 until coords.length()) {
        val c = coords.getJSONArray(i)
        points += GeoPoint(latitude = c.getDouble(1), longitude = c.getDouble(0))
    }
    val props = feature.optJSONObject("properties")
    val times = props?.optJSONArray("times")?.let { arr ->
        List(arr.length()) { arr.getDouble(it) }
    }
    val maneuvers = mutableListOf<Maneuver>()
    props?.optJSONArray("voicehints")?.let { hints ->
        for (i in 0 until hints.length()) {
            val h = hints.getJSONArray(i)
            val type = maneuverTypeForBRouterCommand(h.getInt(1)) ?: continue
            maneuvers += Maneuver(
                pointIndex = h.getInt(0),
                type = type,
                roundaboutExit = speakableRoundaboutExit(h.optInt(2, 0)),
            )
        }
    }
    return Route.of(points, times, maneuvers)
}

/**
 * BRouter reports clockwise (left-hand traffic, as in India) roundabouts with
 * negative exit numbers. A count above 9 means a huge circle such as
 * Connaught Place where "take exit 28" would only confuse — the driver then
 * hears "Enter the roundabout" and follows the next instruction.
 */
fun speakableRoundaboutExit(raw: Int): Int = kotlin.math.abs(raw).takeIf { it in 1..9 } ?: 0
