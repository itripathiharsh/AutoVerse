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

    companion object {
        const val PHOTON_PUBLIC_URL = "https://photon.komoot.io"
        const val MIN_QUERY_LENGTH = 3
    }
}

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
