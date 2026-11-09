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
    val (limits, cameras) = props?.optJSONArray("messages")?.let { roadTagsAlongRoute(points, it) } ?: (null to emptyList())
    return Route.of(points, times, maneuvers, limits, cameras)
}

/**
 * BRouter's "messages" table lists, per stretch of road, the position where
 * the stretch ends plus its way and node tags. Each stretch's `maxspeed` is
 * spread over the route points up to that position, and nodes tagged
 * `highway=speed_camera` become camera positions.
 */
internal fun roadTagsAlongRoute(points: List<GeoPoint>, messages: org.json.JSONArray): Pair<IntArray, List<Int>> {
    val limits = IntArray(points.size)
    val cameras = mutableListOf<Int>()
    if (messages.length() < 2 || points.isEmpty()) return limits to cameras
    val header = messages.getJSONArray(0)
    var lonCol = -1; var latCol = -1; var wayCol = -1; var nodeCol = -1
    for (i in 0 until header.length()) {
        when (header.optString(i)) {
            "Longitude" -> lonCol = i
            "Latitude" -> latCol = i
            "WayTags" -> wayCol = i
            "NodeTags" -> nodeCol = i
        }
    }
    if (lonCol < 0 || latCol < 0 || wayCol < 0) return limits to cameras
    var from = 0
    for (r in 1 until messages.length()) {
        val row = messages.getJSONArray(r)
        val lon = row.optString(lonCol).toDoubleOrNull()?.div(1_000_000.0) ?: continue
        val lat = row.optString(latCol).toDoubleOrNull()?.div(1_000_000.0) ?: continue
        // Nearest route point at or after the previous stretch's end.
        var best = from
        var bestD = Double.MAX_VALUE
        for (i in from until points.size) {
            val d = distanceMeters(points[i], GeoPoint(lat, lon))
            if (d < bestD) { bestD = d; best = i }
            if (d > bestD + 2_000) break
        }
        val limit = maxspeedFromTags(row.optString(wayCol))
        // Points arriving on this stretch: after the previous stretch's end (the very first point belongs to the first stretch).
        val start = if (r == 1) 0 else from + 1
        for (i in start..best) limits[i] = limit
        if (nodeCol >= 0 && row.optString(nodeCol).contains("highway=speed_camera")) cameras += best
        from = best
    }
    return limits to cameras
}

/** "highway=primary maxspeed=50 surface=asphalt" → 50; 0 when not tagged or not numeric. */
fun maxspeedFromTags(tags: String): Int =
    Regex("(?:^|\\s)maxspeed=(\\d{1,3})(?:\\s|$)").find(tags)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 5..150 } ?: 0

/**
 * BRouter reports clockwise (left-hand traffic, as in India) roundabouts with
 * negative exit numbers. A count above 9 means a huge circle such as
 * Connaught Place where "take exit 28" would only confuse — the driver then
 * hears "Enter the roundabout" and follows the next instruction.
 */
fun speakableRoundaboutExit(raw: Int): Int = kotlin.math.abs(raw).takeIf { it in 1..9 } ?: 0
