package com.autoverse.launcher.core.routing

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * BRouter road data comes in 5°×5° tiles named after their south-west
 * corner, e.g. Delhi (28.6 N, 77.2 E) is in "E75_N25". Pure for tests.
 */
fun routingTileName(latitude: Double, longitude: Double): String {
    val lon = (floor(longitude / 5.0) * 5).toInt()
    val lat = (floor(latitude / 5.0) * 5).toInt()
    val lonPart = if (lon < 0) "W${-lon}" else "E$lon"
    val latPart = if (lat < 0) "S${-lat}" else "N$lat"
    return "${lonPart}_$latPart"
}

/**
 * Every tile a route between [a] and [b] can touch: the bounding box of the
 * two points grown by [marginDegrees] (roads rarely run straight) — at most
 * [MAX_ROUTE_TILES] tiles so a cross-country request can't silently download
 * gigabytes.
 */
fun routingTilesBetween(a: GeoPoint, b: GeoPoint, marginDegrees: Double = 0.3): List<String> {
    val minLat = max(-89.9, min(a.latitude, b.latitude) - marginDegrees)
    val maxLat = min(89.9, max(a.latitude, b.latitude) + marginDegrees)
    val minLon = max(-179.9, min(a.longitude, b.longitude) - marginDegrees)
    val maxLon = min(179.9, max(a.longitude, b.longitude) + marginDegrees)
    val tiles = linkedSetOf<String>()
    var lat = floor(minLat / 5) * 5
    while (lat <= maxLat) {
        var lon = floor(minLon / 5) * 5
        while (lon <= maxLon) {
            tiles += routingTileName(lat + 0.1, lon + 0.1)
            lon += 5
        }
        lat += 5
    }
    return tiles.toList()
}

const val MAX_ROUTE_TILES = 12

const val ROUTING_TILE_BASE_URL = "https://brouter.de/brouter/segments4/"

fun routingTileUrl(name: String): String = "$ROUTING_TILE_BASE_URL$name.rd5"
