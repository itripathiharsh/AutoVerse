package com.autoverse.launcher.core.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Address / place search over Photon (komoot's free OpenStreetMap geocoder).
 * Results are biased towards the car's position and limited to India, the
 * launch market. The public server asks for fair use, so the UI debounces
 * typing and searches only from 3 characters. The base URL is swappable for
 * a self-hosted Photon later without touching the UI.
 */
class PlaceSearch(private val baseUrl: String = PHOTON_PUBLIC_URL) {

    suspend fun search(query: String, near: GeoPoint?, limit: Int = 8): List<NavPlace> =
        withContext(Dispatchers.IO) {
            val url = photonSearchUrl(baseUrl, query, near, limit)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "AutoVerse-CarLauncher")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Search service returned ${connection.responseCode}")
                }
                parsePhotonResults(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }

    /** Places of one [category] near [near] (OpenStreetMap amenity filter). */
    suspend fun searchCategory(category: PoiCategory, near: GeoPoint, limit: Int = 8): List<NavPlace> =
        withContext(Dispatchers.IO) {
            val url = photonSearchUrl(baseUrl, category.query, near, limit) + "&osm_tag=" + category.osmTag
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "AutoVerse-CarLauncher")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Search service returned ${connection.responseCode}")
                }
                parsePhotonResults(connection.inputStream.bufferedReader().use { it.readText() })
                    .map { if (it.name.isBlank()) it.copy(name = category.label) else it }
            } finally {
                connection.disconnect()
            }
        }

    companion object {
        const val PHOTON_PUBLIC_URL = "https://photon.komoot.io"
        const val MIN_QUERY_LENGTH = 3
    }
}

/**
 * Stops a driver looks for on the way. [overpassFilter] selects them by
 * OpenStreetMap tags (every mapped station, whatever it's called — CNG by
 * its fuel tag); [query]/[osmTag] are the Photon fallback.
 */
enum class PoiCategory(val label: String, val emoji: String, val query: String, val osmTag: String, val overpassFilter: String) {
    PETROL("Petrol", "⛽", "petrol pump", "amenity:fuel", "[\"amenity\"=\"fuel\"]"),
    CNG("CNG", "🟢", "CNG", "amenity:fuel", "[\"amenity\"=\"fuel\"][\"fuel:cng\"=\"yes\"]"),
    EV("EV charging", "⚡", "charging station", "amenity:charging_station", "[\"amenity\"=\"charging_station\"]"),
    TOILETS("Toilets", "🚻", "toilet", "amenity:toilets", "[\"amenity\"=\"toilets\"]"),
    FOOD("Food", "🍽️", "restaurant", "amenity:restaurant", "[\"amenity\"~\"^(restaurant|fast_food|food_court)$\"]"),
}

/**
 * Category search over OpenStreetMap's Overpass API: all places with the
 * category's tags within [radiusM] of a point, or of a route line (one
 * request for the whole way ahead). Positions are rounded to ~100 m.
 */
class OverpassPoiSearch(private val endpoint: String = OVERPASS_URL) {

    suspend fun near(category: PoiCategory, point: GeoPoint, radiusM: Int = 6_000, limit: Int = 30): List<NavPlace> =
        query(overpassQuery(category, listOf(point), radiusM, limit), category)

    suspend fun alongLine(category: PoiCategory, line: List<GeoPoint>, radiusM: Int = 1_000, limit: Int = 40): List<NavPlace> =
        query(overpassQuery(category, line, radiusM, limit), category)

    private suspend fun query(ql: String, category: PoiCategory): List<NavPlace> = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 45_000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("User-Agent", "AutoVerse-CarLauncher")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        try {
            connection.outputStream.use { it.write(("data=" + URLEncoder.encode(ql, "UTF-8")).toByteArray()) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Places service returned ${connection.responseCode}")
            }
            parseOverpassPlaces(connection.inputStream.bufferedReader().use { it.readText() }, category.label)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
    }
}

/** Overpass QL for [category] around a point or a polyline. Pure for tests. */
fun overpassQuery(category: PoiCategory, line: List<GeoPoint>, radiusM: Int, limit: Int): String {
    val coords = line.joinToString(",") { String.format(Locale.US, "%.3f,%.3f", it.latitude, it.longitude) }
    return "[out:json][timeout:40];nwr${category.overpassFilter}(around:$radiusM,$coords);out center $limit;"
}

/** Overpass JSON → places named by name, brand or operator; detail = brand/area. */
fun parseOverpassPlaces(json: String, fallbackName: String): List<NavPlace> {
    val elements = JSONObject(json).optJSONArray("elements") ?: return emptyList()
    val out = mutableListOf<NavPlace>()
    for (i in 0 until elements.length()) {
        val e = elements.getJSONObject(i)
        val lat = if (e.has("lat")) e.getDouble("lat") else e.optJSONObject("center")?.optDouble("lat") ?: continue
        val lon = if (e.has("lon")) e.getDouble("lon") else e.optJSONObject("center")?.optDouble("lon") ?: continue
        val tags = e.optJSONObject("tags")
        fun tag(k: String) = tags?.optString(k)?.takeIf { it.isNotBlank() }
        val name = tag("name") ?: tag("brand") ?: tag("operator") ?: fallbackName
        val detail = listOfNotNull(
            tag("brand")?.takeIf { it != name },
            tag("addr:street"),
            tag("addr:suburb") ?: tag("addr:city"),
            if (tag("fuel:cng") == "yes") "CNG" else null,
            tag("opening_hours")?.takeIf { it == "24/7" }?.let { "Open 24 h" },
        ).distinct().joinToString(" · ")
        out += NavPlace(name, detail, lat, lon)
    }
    return out
}

/**
 * Combines tag-based (Overpass) and name-based (Photon) results: the same
 * station found by both (within ~60 m) appears once, the tagged entry wins
 * because it carries brand, CNG and opening-hours details.
 */
fun mergePlaces(primary: List<NavPlace>, secondary: List<NavPlace>): List<NavPlace> {
    val out = primary.toMutableList()
    for (p in secondary) {
        if (out.none { distanceMeters(it.point, p.point) < 60.0 }) out += p
    }
    return out
}

/** Route points every ~[stepM] for the next [aheadM] — the line an along-route search follows. */
fun routeLineAhead(route: Route, traveledMeters: Double, aheadM: Double = 25_000.0, stepM: Double = 700.0): List<GeoPoint> {
    val out = mutableListOf<GeoPoint>()
    var next = traveledMeters
    for (i in route.points.indices) {
        val m = route.cumulativeMeters[i]
        if (m < traveledMeters) continue
        if (m > traveledMeters + aheadM) break
        if (m >= next) {
            out += route.points[i]
            next = m + stepM
        }
    }
    return out.ifEmpty { listOfNotNull(route.points.lastOrNull()) }.take(60)
}

/** A stop found along the route: how far ahead and how far off the route it is. */
data class RouteStop(val place: NavPlace, val aheadMeters: Double, val detourMeters: Double)

/** Points on the route [aheadKm] ahead of [traveledMeters], for searching "on the way". */
fun samplePointsAhead(route: Route, traveledMeters: Double, aheadKm: List<Double> = listOf(1.5, 6.0, 15.0)): List<GeoPoint> =
    aheadKm.mapNotNull { km ->
        val target = traveledMeters + km * 1000
        if (target > route.totalMeters) return@mapNotNull null
        val i = route.cumulativeMeters.indexOfFirst { it >= target }.takeIf { it >= 0 } ?: return@mapNotNull null
        route.points[i]
    }.ifEmpty { listOfNotNull(route.points.lastOrNull()) }

/**
 * Keeps places that are ahead on the route and within [maxDetourMeters] of
 * it, nearest-ahead first, without duplicates.
 */
fun stopsAlongRoute(route: Route, traveledMeters: Double, candidates: List<NavPlace>, maxDetourMeters: Double = 1_500.0): List<RouteStop> =
    candidates
        .distinctBy { it.name to (it.latitude * 1e4).toInt() }
        .map { p ->
            val pr = computeProgress(route, p.point)
            RouteStop(p, aheadMeters = pr.traveledMeters - traveledMeters, detourMeters = pr.offRouteMeters)
        }
        .filter { it.aheadMeters > 200 && it.detourMeters <= maxDetourMeters }
        .sortedBy { it.aheadMeters }

/** India's bounding box (minLon,minLat,maxLon,maxLat). */
private const val INDIA_BBOX = "68.0,6.5,97.5,37.5"

fun photonSearchUrl(baseUrl: String, query: String, near: GeoPoint?, limit: Int): String {
    val q = URLEncoder.encode(query.trim(), "UTF-8")
    val sb = StringBuilder("$baseUrl/api/?q=$q&limit=$limit&lang=en&bbox=$INDIA_BBOX")
    if (near != null) {
        // Rounded to ~1 km, like the weather lookup: enough to rank nearby
        // results first, never the car's exact position.
        sb.append(String.format(Locale.US, "&lat=%.2f&lon=%.2f", near.latitude, near.longitude))
    }
    return sb.toString()
}

/**
 * Photon GeoJSON → places. The title is the place's own name (or street +
 * house number for an address); the detail line is locality, city and state
 * so "Sector 18" in Noida and in Gurugram can be told apart.
 */
fun parsePhotonResults(json: String): List<NavPlace> {
    val features = JSONObject(json).optJSONArray("features") ?: return emptyList()
    val places = mutableListOf<NavPlace>()
    for (i in 0 until features.length()) {
        val f = features.getJSONObject(i)
        val coords = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
        val p = f.optJSONObject("properties") ?: continue
        val street = p.optString("street").takeIf { it.isNotBlank() }
        val house = p.optString("housenumber").takeIf { it.isNotBlank() }
        val name = p.optString("name").takeIf { it.isNotBlank() }
            ?: listOfNotNull(house, street).joinToString(" ").takeIf { it.isNotBlank() }
            ?: continue
        val detail = listOf("district", "locality", "city", "county", "state")
            .mapNotNull { key -> p.optString(key).takeIf { it.isNotBlank() && it != name } }
            .distinct()
            .take(3)
            .joinToString(", ")
        val place = NavPlace(name, detail, latitude = coords.getDouble(1), longitude = coords.getDouble(0))
        if (places.none { it.name == place.name && it.detail == place.detail }) places += place
    }
    return places
}
