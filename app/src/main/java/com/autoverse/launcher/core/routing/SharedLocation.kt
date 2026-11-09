package com.autoverse.launcher.core.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/** What a shared text / link tells us about a place. */
sealed interface SharedLocation {
    /** Exact coordinates (with a name when the link carried one). */
    data class Point(val name: String?, val latitude: Double, val longitude: Double) : SharedLocation

    /** A short link that must be opened once to see where it points (maps.app.goo.gl). */
    data class ShortLink(val name: String?, val url: String) : SharedLocation

    /** Only a place name / address — search for it. */
    data class Query(val text: String) : SharedLocation
}

private val URL_REGEX = Regex("https?://\\S+")
private val SHORT_HOSTS = listOf("maps.app.goo.gl", "goo.gl/maps", "g.co/kgs")

/**
 * Reads a place from whatever Google Maps (or any app) shares or opens:
 * "geo:" URIs, full Google Maps URLs, short links, or plain text. Pure, so
 * every link shape is unit-tested.
 */
fun parseSharedLocation(raw: String): SharedLocation? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    if (text.startsWith("geo:", ignoreCase = true)) return parseGeoUri(text)

    val url = URL_REGEX.find(text)?.value?.trimEnd('.', ',', ')')
    // Google Maps puts the place name on the first line(s) before the link.
    val label = (if (url != null) text.substringBefore(url) else "")
        .lines().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        ?.takeIf { !it.startsWith("http") }

    if (url != null) {
        coordinatesFromMapsUrl(url)?.let { (lat, lon) ->
            return SharedLocation.Point(label ?: placeNameFromMapsUrl(url), lat, lon)
        }
        if (SHORT_HOSTS.any { url.contains(it, ignoreCase = true) }) return SharedLocation.ShortLink(label, url)
        queryFromMapsUrl(url)?.let { return SharedLocation.Query(it) }
        placeNameFromMapsUrl(url)?.let { return SharedLocation.Query(it) }
        return label?.let { SharedLocation.Query(it) }
    }
    // Plain "26.93, 81.20" or a place name.
    coordinatePair(text)?.let { (lat, lon) -> return SharedLocation.Point(null, lat, lon) }
    return SharedLocation.Query(text.lines().first().take(200))
}

/** geo:26.9,81.2  ·  geo:0,0?q=26.9,81.2(Asandra)  ·  geo:0,0?q=Asandra+Barabanki */
fun parseGeoUri(uri: String): SharedLocation? {
    val body = uri.substringAfter(':')
    val path = body.substringBefore('?')
    val q = body.substringAfter('?', "").split('&')
        .firstOrNull { it.startsWith("q=") }?.removePrefix("q=")?.replace('+', ' ')?.let(::decode)
    if (q != null) {
        val label = Regex("\\(([^)]*)\\)").find(q)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
        coordinatePair(q.substringBefore('('))?.let { (lat, lon) -> return SharedLocation.Point(label, lat, lon) }
        if (q.isNotBlank()) return SharedLocation.Query(q.trim())
    }
    coordinatePair(path.substringBefore(';'))?.let { (lat, lon) ->
        if (lat != 0.0 || lon != 0.0) return SharedLocation.Point(null, lat, lon)
    }
    return null
}

/**
 * Coordinates inside a Google Maps URL. The place pin (!3d…!4d…) beats the
 * map centre (@lat,lng), which can be off to one side of the place.
 */
fun coordinatesFromMapsUrl(url: String): Pair<Double, Double>? {
    val u = decode(url)
    Regex("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)").findAll(u).lastOrNull()?.let {
        return valid(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
    }
    for (key in listOf("q", "query", "ll", "destination", "daddr", "center", "sll")) {
        Regex("[?&]$key=([^&#]+)").find(u)?.groupValues?.get(1)?.let { v ->
            coordinatePair(v.replace('+', ' '))?.let { return it }
        }
    }
    Regex("/search/(-?\\d+\\.\\d+),\\s*\\+?(-?\\d+\\.\\d+)").find(u)?.let {
        return valid(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
    }
    Regex("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)").find(u)?.let {
        return valid(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
    }
    return null
}

/** "/maps/place/Asandra,+Uttar+Pradesh/@…" → "Asandra, Uttar Pradesh". */
fun placeNameFromMapsUrl(url: String): String? =
    Regex("/place/([^/@?]+)").find(url)?.groupValues?.get(1)
        ?.let { decode(it.replace('+', ' ')) }?.trim()?.takeIf { it.isNotEmpty() && coordinatePair(it) == null }

private fun queryFromMapsUrl(url: String): String? {
    val u = decode(url)
    for (key in listOf("q", "query", "destination", "daddr")) {
        Regex("[?&]$key=([^&#]+)").find(u)?.groupValues?.get(1)?.replace('+', ' ')?.trim()
            ?.takeIf { it.isNotEmpty() && coordinatePair(it) == null }?.let { return it }
    }
    return Regex("/search/([^/@?]+)").find(u)?.groupValues?.get(1)?.replace('+', ' ')?.trim()?.takeIf { it.isNotEmpty() }
}

fun coordinatePair(s: String): Pair<Double, Double>? {
    val m = Regex("^\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$").find(s) ?: return null
    return valid(m.groupValues[1].toDouble(), m.groupValues[2].toDouble())
}

private fun valid(lat: Double, lon: Double): Pair<Double, Double>? =
    if (lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)) lat to lon else null

private fun decode(s: String): String = try {
    URLDecoder.decode(s.replace("+", "%2B"), "UTF-8").replace("%2B", "+")
} catch (_: Exception) {
    s
}

/**
 * Opens a short link (maps.app.goo.gl) just far enough to see the full
 * Google Maps address it redirects to — no page is downloaded or parsed.
 */
suspend fun resolveShortLink(url: String): String? = withContext(Dispatchers.IO) {
    var current = url
    repeat(6) {
        val c = (URL(current).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "HEAD"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AutoVerse")
        }
        try {
            val code = c.responseCode
            val next = c.getHeaderField("Location")
            if (code in 300..399 && next != null) {
                current = URL(URL(current), next).toString()
                if (coordinatesFromMapsUrl(current) != null) return@withContext current
            } else {
                return@withContext current
            }
        } finally {
            c.disconnect()
        }
    }
    current
}
