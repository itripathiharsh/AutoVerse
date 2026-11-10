package com.autoverse.launcher.core.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/** One place inside a shared link: a name, coordinates, or both. */
data class SharedPoint(val name: String?, val latitude: Double? = null, val longitude: Double? = null) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

/** What a shared text / link tells us about a place (or a whole route). */
sealed interface SharedLocation {
    /** Exact coordinates (with a name when the link carried one). */
    data class Point(val name: String?, val latitude: Double, val longitude: Double) : SharedLocation

    /** A short link that must be opened once to see where it points (maps.app.goo.gl). */
    data class ShortLink(val name: String?, val url: String) : SharedLocation

    /**
     * Only a place name / address — search for it. [near] (lat, lng) is the
     * map the link was showing, to bias the search; null = near the car.
     */
    data class Query(val text: String, val near: Pair<Double, Double>? = null) : SharedLocation

    /**
     * Directions shared from Google Maps: drive to [destination] by way of
     * [stops], in order. The start point is always the car, so the link's
     * origin is dropped. Points without coordinates are found by name, near
     * [near] (the map centre of the link) when known.
     */
    data class Route(
        val stops: List<SharedPoint>,
        val destination: SharedPoint,
        val near: Pair<Double, Double>? = null,
    ) : SharedLocation
}

private val URL_REGEX = Regex("https?://[^\\s<>\"]+", RegexOption.IGNORE_CASE)
private val SHORT_HOSTS = setOf("maps.app.goo.gl", "goo.gl", "g.co", "share.google")
private val GOOGLE_HOST = Regex("^(?:www\\.|maps\\.)?google\\.[a-z]{2,3}(?:\\.[a-z]{2})?$")
private val LABEL_PREFIX =
    Regex("^(?:get directions to|directions to|route to|navigate to|drive to)\\s*:?\\s*", RegexOption.IGNORE_CASE)
private val CURRENT_LOCATION_NAMES = setOf("your location", "my location", "current location", "my current location")
private val DECIMAL = "[-+]?\\d+(?:\\.\\d+)?"

/**
 * Reads a place from whatever Google Maps (or any app) shares or opens:
 * "geo:" URIs, full Google Maps URLs (places, searches, directions), short
 * links, or plain text. Pure, so every link shape is unit-tested.
 */
fun parseSharedLocation(raw: String): SharedLocation? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    if (text.startsWith("geo:", ignoreCase = true)) return parseGeoUri(text)

    val urls = URL_REGEX.findAll(text).map { it.value.trimEnd('.', ',', ')', ']', '\'', ';') }.toList()
    val url = urls.firstOrNull(::looksLikeMapsLink) ?: urls.firstOrNull()
    val label = shareLabel(text, url)

    if (url != null) {
        val target = unwrapRedirect(url) ?: url
        if (isShortLink(target)) return SharedLocation.ShortLink(label, target)
        parseMapsUrl(target, label)?.let { return it }
        return label?.let { SharedLocation.Query(it) }
    }
    Regex("geo:\\S+", RegexOption.IGNORE_CASE).find(text)?.let { m -> parseGeoUri(m.value)?.let { return it } }
    // Plain "26.93, 81.20" or a place name.
    coordinatePair(text)?.let { (lat, lon) -> return SharedLocation.Point(null, lat, lon) }
    val query = label ?: text.lines().first().trim()
    return SharedLocation.Query(query.take(200))
}

/**
 * The place name Google Maps puts around a link: the first line before it
 * ("Asandra\nUttar Pradesh 225001\nhttps://…" → "Asandra"), or after it when
 * the link comes first. "Directions to X" → "X".
 */
internal fun shareLabel(text: String, url: String?): String? {
    val before = if (url != null) text.substringBefore(url) else text
    val after = if (url != null) text.substringAfter(url, "") else ""
    fun firstLine(s: String) = s.lines().map { it.trim() }.firstOrNull {
        it.isNotEmpty() && !URL_REGEX.containsMatchIn(it) && !isBoilerplate(it)
    }
    val line = firstLine(before) ?: firstLine(after) ?: return null
    return line.replace(LABEL_PREFIX, "").trim().trimEnd(':').trim().take(200).takeIf { it.isNotEmpty() }
}

private fun isBoilerplate(line: String): Boolean {
    val l = line.lowercase().trimEnd(':', '.', '!')
    return l == "google maps" || l.startsWith("shared from") || l.startsWith("shared via") ||
        l.startsWith("sent from") || l.startsWith("check out this place") || l.startsWith("view on google maps")
}

private fun looksLikeMapsLink(url: String): Boolean {
    val host = hostOf(url)
    return host in SHORT_HOSTS || GOOGLE_HOST.matches(host) || host.contains("maps")
}

/** maps.app.goo.gl/…, goo.gl/maps/…, g.co/kgs/…, share.google/… — must be opened to see the place. */
fun isShortLink(url: String): Boolean {
    val host = hostOf(url)
    return host in SHORT_HOSTS || host.endsWith(".app.goo.gl")
}

/** True for a full Google Maps address (www.google.com/maps/…, maps.google.com/…). */
fun isGoogleMapsUrl(url: String): Boolean {
    if (!url.startsWith("https://", ignoreCase = true) && !url.startsWith("http://", ignoreCase = true)) return false
    val host = hostOf(url)
    if (!GOOGLE_HOST.matches(host)) return false
    return host.startsWith("maps.") || pathOf(url).startsWith("/maps")
}

/**
 * Reads a full Google Maps URL. Order matters: directions first (their
 * `@lat,lng` is just the map centre), then the place pin (!3d…!4d…), then
 * coordinates in q/ll, then a name search biased to the map centre, and the
 * bare map centre only as a last resort.
 */
internal fun parseMapsUrl(url: String, label: String? = null): SharedLocation? {
    val path = pathOf(url)
    val params = queryParams(url)
    val centre = mapCentre(path)

    if (isDirectionsUrl(path, params)) {
        return parseDirections(url, path, params, label, centre) ?: label?.let { SharedLocation.Query(it, centre) }
    }

    pinCoordinates(url)?.let { (lat, lon) ->
        return SharedLocation.Point(label ?: placeNameFromMapsUrl(url) ?: params["q"]?.let(::pointFromText)?.name, lat, lon)
    }

    val q = (params["q"] ?: params["query"])?.let(::pointFromText)
    if (q?.latitude != null && q.longitude != null) return SharedLocation.Point(label ?: q.name, q.latitude, q.longitude)
    (params["ll"] ?: params["center"] ?: params["sll"])?.let(::coordinatePair)?.let { (lat, lon) ->
        return SharedLocation.Point(label ?: q?.name, lat, lon)
    }

    pathSegmentAfter(path, "place")?.let(::pointFromText)?.let { p ->
        if (p.latitude != null && p.longitude != null) return SharedLocation.Point(label, p.latitude, p.longitude)
    }
    pathSegmentAfter(path, "search")?.let(::pointFromText)?.let { p ->
        if (p.latitude != null && p.longitude != null) return SharedLocation.Point(label, p.latitude, p.longitude)
        if (p.name != null) return SharedLocation.Query(p.name, centre)
    }
    q?.name?.let { return SharedLocation.Query(it, centre) }

    // "/maps/place/Name/@lat,lng,17z" without a pin, or "/maps/@lat,lng,14z":
    // Google centres the map on the place.
    val placeName = placeNameFromMapsUrl(url)
    centre?.let { (lat, lon) -> return SharedLocation.Point(label ?: placeName, lat, lon) }
    placeName?.let { return SharedLocation.Query(it) }
    return null
}

private fun isDirectionsUrl(path: String, params: Map<String, String>): Boolean =
    path.contains("/dir/") || path.endsWith("/dir") || "daddr" in params || "saddr" in params ||
        (params["api"] == "1" && "destination" in params)

/**
 * Directions in any of Google's shapes:
 *  - /maps/dir/Origin/Via/Destination/@centre/data=…!1m5!1m1!1s…!2m2!1d<lng>!2d<lat>…
 *  - /maps/dir/?api=1&origin=…&destination=…&waypoints=a|b
 *  - maps?saddr=…&daddr=B+to:C+to:D (legacy)
 */
private fun parseDirections(
    url: String,
    path: String,
    params: Map<String, String>,
    label: String?,
    centre: Pair<Double, Double>?,
): SharedLocation? {
    // Points in order, the origin first; null = "Your location" / nothing usable.
    val ordered: List<SharedPoint?>
    val segments = dirPathSegments(path)
    if (segments.isNotEmpty()) {
        val coords = directionsWaypointsFromUrl(url)
        val n = maxOf(segments.size, coords.size)
        // Both lists end with the destination, so they are aligned from the end.
        ordered = (0 until n).map { i ->
            val name = segments.getOrNull(i - (n - segments.size))?.let(::decodeText)
            val c = coords.getOrNull(i - (n - coords.size))
            mergePoint(name, c)
        }
    } else {
        val vias = mutableListOf<SharedPoint>()
        params["waypoints"]?.split('|')?.mapNotNullTo(vias) { pointFromText(it) }
        var destination = params["destination"]?.let(::pointFromText)
        params["daddr"]?.let { d ->
            val parts = d.split(Regex("\\s*\\bto:\\s*", RegexOption.IGNORE_CASE)).mapNotNull { pointFromText(it) }
            if (parts.isNotEmpty()) {
                vias += parts.dropLast(1)
                if (destination == null) destination = parts.last()
            }
        }
        if (vias.isEmpty() && destination == null) {
            // Only waypoints from data (rare): use them as they are.
            val coords = directionsWaypointsFromUrl(url)
            ordered = coords.map { c -> c?.let { SharedPoint(null, it.first, it.second) } }
        } else {
            // The explicit origin (origin= / saddr=) is skipped: the car is the start.
            ordered = listOf<SharedPoint?>(null) + vias + destination
        }
    }
    // The first of several points is the origin (usually the driver's own position).
    val points = (if (ordered.size >= 2) ordered.drop(1) else ordered).filterNotNull()
    if (points.isEmpty()) return null
    val last = points.last()
    val destination = if (label != null) last.copy(name = label) else last
    val stops = points.dropLast(1)
    if (stops.isEmpty() && destination.latitude != null && destination.longitude != null) {
        return SharedLocation.Point(destination.name, destination.latitude, destination.longitude)
    }
    return SharedLocation.Route(stops, destination, centre)
}

/** "/maps/dir//Barabanki/@26.9,81.2,12z/data=…" → ["", "Barabanki"] (empty = your location). */
private fun dirPathSegments(path: String): List<String> {
    val i = path.indexOf("/dir/")
    if (i < 0) return emptyList()
    return path.substring(i + "/dir/".length).split('/')
        .takeWhile { !it.startsWith("@") && !it.startsWith("data=") && !it.startsWith("am=") }
        .dropLastWhile { it.isBlank() }
}

private fun mergePoint(name: String?, coords: Pair<Double, Double>?): SharedPoint? {
    val p = name?.let(::pointFromText)
    if (p?.hasCoordinates == true) return p
    if (coords != null) return SharedPoint(p?.name, coords.first, coords.second)
    return p
}

/**
 * "26.9,81.2" · "Name@26.9,81.2" · "loc:26.9,81.2" · "Name (26.9,81.2)" ·
 * "26.9,81.2 (Name)" · "Name". Null for blanks and "Your location".
 */
internal fun pointFromText(raw: String): SharedPoint? {
    val s = raw.trim().removePrefix("loc:").trim()
    if (s.isEmpty() || s.lowercase() in CURRENT_LOCATION_NAMES) return null
    coordinatePair(s)?.let { (lat, lon) -> return SharedPoint(null, lat, lon) }
    val at = s.lastIndexOf('@')
    if (at > 0) {
        coordinatePair(s.substring(at + 1))?.let { (lat, lon) ->
            return SharedPoint(s.substring(0, at).trim().takeIf { it.isNotEmpty() }, lat, lon)
        }
    }
    Regex("^(.*)\\(([^)]*)\\)\\s*$").find(s)?.let { m ->
        val outside = m.groupValues[1].trim()
        val inside = m.groupValues[2].trim()
        coordinatePair(inside)?.let { (lat, lon) -> return SharedPoint(outside.takeIf { it.isNotEmpty() }, lat, lon) }
        coordinatePair(outside)?.let { (lat, lon) -> return SharedPoint(inside.takeIf { it.isNotEmpty() }, lat, lon) }
    }
    return SharedPoint(s.take(200))
}

private fun directionsWaypointsFromUrl(url: String): List<Pair<Double, Double>?> {
    val data = Regex("/data=([^?#/]+)").find(url)?.groupValues?.get(1)
        ?: rawQueryParams(url)["data"]
        ?: return emptyList()
    return directionsWaypointsFromData(data)
}

private class PbToken(val field: Int, val type: Char, val value: String)
private class PbNode(val field: Int, val type: Char, val value: String, val children: List<PbNode>)

private val PB_TOKEN = Regex("^(\\d+)([a-zA-Z])(.*)$", RegexOption.DOT_MATCHES_ALL)

/**
 * Waypoints of a Google Maps directions `data=` string, in order: each is
 * (lat, lng), or null for one without coordinates (e.g. "Your location",
 * `!1m0`). The string is a flattened protobuf: `!<field><type><value>`, where
 * an `m` token's value counts all tokens nested under it. Directions are a
 * `4m` message holding one `1m` per waypoint; a waypoint's position is a
 * `2m` with `!1d<lng>!2d<lat>` (longitude first).
 */
internal fun directionsWaypointsFromData(data: String): List<Pair<Double, Double>?> {
    val tokens = data.split('!').filter { it.isNotEmpty() }.map { t ->
        PB_TOKEN.matchEntire(t)?.let { m ->
            PbToken(m.groupValues[1].toIntOrNull() ?: -1, m.groupValues[2][0], m.groupValues[3])
        } ?: PbToken(-1, '?', t)
    }
    val tree = pbTree(tokens, 0, tokens.size)
    val block = findDirectionsBlock(tree)
    if (block != null) {
        return block.children.filter { it.field == 1 && it.type == 'm' }.map(::waypointCoordinates)
    }
    // Unknown layout: every !1d<lng>!2d<lat> pair in order.
    return Regex("!1d($DECIMAL)!2d($DECIMAL)").findAll(data).mapNotNull { m ->
        valid(m.groupValues[2].toDouble(), m.groupValues[1].toDouble())
    }.toList()
}

private fun pbTree(tokens: List<PbToken>, from: Int, to: Int): List<PbNode> {
    val out = ArrayList<PbNode>()
    var i = from
    while (i < to) {
        val t = tokens[i]
        if (t.type == 'm') {
            val n = (t.value.toIntOrNull() ?: 0).coerceIn(0, to - i - 1)
            out += PbNode(t.field, t.type, t.value, pbTree(tokens, i + 1, i + 1 + n))
            i += 1 + n
        } else {
            out += PbNode(t.field, t.type, t.value, emptyList())
            i++
        }
    }
    return out
}

private fun findDirectionsBlock(nodes: List<PbNode>): PbNode? {
    for (node in nodes) {
        if (node.type == 'm' && node.field == 4) {
            val waypoints = node.children.count { it.field == 1 && it.type == 'm' }
            val hasMode = node.children.any { it.field == 3 && it.type == 'e' }
            if (waypoints >= 2 || (waypoints >= 1 && hasMode)) return node
        }
        findDirectionsBlock(node.children)?.let { return it }
    }
    return null
}

private fun waypointCoordinates(waypoint: PbNode): Pair<Double, Double>? {
    fun search(nodes: List<PbNode>): Pair<Double, Double>? {
        for (n in nodes) {
            if (n.type == 'm' && n.field == 2) {
                val lng = n.children.firstOrNull { it.field == 1 && it.type == 'd' }?.value?.toDoubleOrNull()
                val lat = n.children.firstOrNull { it.field == 2 && it.type == 'd' }?.value?.toDoubleOrNull()
                if (lat != null && lng != null) valid(lat, lng)?.let { return it }
            }
            search(n.children)?.let { return it }
        }
        return null
    }
    return search(waypoint.children)
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
 * Destination coordinates of a Google Maps URL (the place pin, never the map
 * centre when a pin or a directions destination exists).
 */
fun coordinatesFromMapsUrl(url: String): Pair<Double, Double>? = when (val s = parseMapsUrl(url)) {
    is SharedLocation.Point -> s.latitude to s.longitude
    is SharedLocation.Route -> s.destination.let { d ->
        if (d.latitude != null && d.longitude != null) d.latitude to d.longitude else null
    }
    else -> null
}

/** Place pin "!3d<lat>!4d<lng>" (the last one: the selected place). */
private fun pinCoordinates(url: String): Pair<Double, Double>? =
    Regex("!3d($DECIMAL)!4d($DECIMAL)").findAll(decode(url)).lastOrNull()?.let {
        valid(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
    }

/** "/maps/…/@26.93,81.20,14z" → the map centre. */
private fun mapCentre(path: String): Pair<Double, Double>? =
    Regex("@($DECIMAL),($DECIMAL)").find(decode(path))?.let {
        valid(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
    }

/** "/maps/place/Asandra,+Uttar+Pradesh/@…" → "Asandra, Uttar Pradesh". */
fun placeNameFromMapsUrl(url: String): String? =
    pathSegmentAfter(pathOf(url), "place")?.takeIf { coordinatePair(it) == null && pointFromText(it)?.hasCoordinates != true }

/** The decoded path segment after "/<key>/", unless it is the map centre or data. */
private fun pathSegmentAfter(path: String, key: String): String? =
    Regex("/$key/([^/]+)").find(path)?.groupValues?.get(1)
        ?.takeIf { !it.startsWith("@") && !it.startsWith("data=") }
        ?.let(::decodeText)?.takeIf { it.isNotEmpty() }

fun coordinatePair(s: String): Pair<Double, Double>? {
    val m = Regex("^\\s*([-+]?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*([-+]?\\d{1,3}(?:\\.\\d+)?)\\s*$").find(s) ?: return null
    return valid(m.groupValues[1].toDouble(), m.groupValues[2].toDouble())
}

private fun valid(lat: Double, lon: Double): Pair<Double, Double>? =
    if (lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)) lat to lon else null

// ---- URL helpers (no android.net.Uri, so they run in unit tests) -----------

private fun hostOf(url: String): String =
    Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://([^/?#:]+)").find(url)?.groupValues?.get(1)?.lowercase() ?: ""

private fun pathOf(url: String): String {
    val rest = url.substringAfter("://", "")
    val i = rest.indexOfAny(charArrayOf('/', '?', '#'))
    if (i < 0 || rest[i] != '/') return ""
    return rest.substring(i).substringBefore('?').substringBefore('#')
}

/** Query parameters, keys lower-cased, values still percent-encoded. First one wins. */
private fun rawQueryParams(url: String): Map<String, String> {
    val query = url.substringAfter('?', "").substringBefore('#')
    if (query.isEmpty()) return emptyMap()
    val out = LinkedHashMap<String, String>()
    for (pair in query.split('&')) {
        if (pair.isEmpty()) continue
        val key = decode(pair.substringBefore('=')).lowercase()
        if (key !in out) out[key] = pair.substringAfter('=', "")
    }
    return out
}

/** Query parameters, decoded ("+" = space). */
private fun queryParams(url: String): Map<String, String> = rawQueryParams(url).mapValues { decodeText(it.value) }

/** Percent-decodes, keeping "+" as is. */
private fun decode(s: String): String = try {
    URLDecoder.decode(s.replace("+", "%2B"), "UTF-8").replace("%2B", "+")
} catch (_: Exception) {
    s
}

/** Percent-decodes form text: "+" is a space. */
private fun decodeText(s: String): String = decode(s.replace('+', ' ')).trim()

// ---- short links -------------------------------------------------------------

/**
 * The real address wrapped inside a redirect address, or null when [url]
 * wraps nothing: consent.google.com/…?continue=<url>, google.com/url?q=<url>,
 * Firebase links with link=<url>, intent://…;S.browser_fallback_url=<url>,
 * intent://www.google.com/maps/…#Intent;scheme=https and comgooglemaps://?q=….
 */
fun unwrapRedirect(url: String): String? {
    var current = url
    var changed = false
    repeat(4) {
        val inner = innerUrl(current) ?: return if (changed) current else null
        current = inner
        changed = true
    }
    return current
}

private fun innerUrl(url: String): String? {
    if (isGoogleMapsUrl(url)) return null // a final Maps address carries its own q= etc.
    val raw = rawQueryParams(url)
    for (key in listOf("continue", "url", "link", "q", "u", "dest", "destination_url")) {
        val v = raw[key]?.let(::decode)?.trim() ?: continue
        if (v.startsWith("http://", true) || v.startsWith("https://", true)) return v
    }
    Regex("S\\.browser_fallback_url=([^;#]+)").find(url)?.let { m ->
        val v = decode(m.groupValues[1]).trim()
        if (v.startsWith("http://", true) || v.startsWith("https://", true)) return v
    }
    if (url.startsWith("intent://", ignoreCase = true)) {
        val scheme = Regex(";scheme=([a-zA-Z]+)").find(url)?.groupValues?.get(1)?.lowercase() ?: "https"
        val body = url.substring("intent://".length).substringBefore("#Intent")
        if (scheme == "http" || scheme == "https") return "$scheme://$body"
    }
    if (url.startsWith("comgooglemaps://", ignoreCase = true)) {
        return "https://maps.google.com/" + url.substring("comgooglemaps://".length).removePrefix("/")
    }
    return null
}

/**
 * The Google Maps address inside an HTML page — for short links that answer
 * 200 with a meta-refresh / JavaScript redirect page instead of a 3xx. Looks
 * at meta refresh, og:url, canonical and location.href/replace first, then any
 * google.com/maps link in the page (JS-escaped forms included). Only an
 * address that actually names a place or route is returned. Pure; unit-tested.
 */
fun mapsUrlFromHtml(html: String): String? {
    val text = html
        .replace("\\/", "/")
        .replace("\\u0026", "&").replace("\\u003d", "=").replace("\\u003D", "=")
        .replace("\\x26", "&").replace("\\x3d", "=").replace("\\x3D", "=")
        .replace("&amp;", "&").replace("&#38;", "&").replace("&#61;", "=").replace("&#x3D;", "=")
    val candidates = ArrayList<String>()
    val tags = Regex("<(?:meta|link)\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(text).map { it.value }.toList()
    fun attr(tag: String, name: String) =
        Regex("\\b$name\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE).find(tag)
            ?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }
    for (tag in tags) {
        val lower = tag.lowercase()
        when {
            lower.contains("refresh") -> attr(tag, "content")
                ?.let { Regex("url\\s*=\\s*['\"]?([^'\"\\s]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
                ?.let(candidates::add)
            lower.contains("og:url") || lower.contains("twitter:url") -> attr(tag, "content")?.let(candidates::add)
            lower.contains("canonical") -> attr(tag, "href")?.let(candidates::add)
        }
    }
    Regex("location(?:\\.href)?\\s*(?:=|\\.replace\\(|\\.assign\\()\\s*['\"]([^'\"]+)['\"]").findAll(text)
        .mapTo(candidates) { it.groupValues[1] }
    Regex("https?://[^\\s<>\"'\\\\]+", RegexOption.IGNORE_CASE).findAll(text)
        .mapTo(candidates) { it.value.trimEnd(')', ';', ',', ']') }

    return candidates.asSequence()
        .map { c -> unwrapRedirect(c) ?: c }
        .filter { isGoogleMapsUrl(it) }
        .firstOrNull { parseMapsUrl(it) != null }
}

private const val RESOLVE_BUDGET_MS = 10_000L
private const val MAX_HOPS = 8
private const val MAX_PAGE_BYTES = 200 * 1024
private const val MOBILE_UA =
    "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

/**
 * Opens a short link (maps.app.goo.gl) just far enough to see the full
 * Google Maps address it points to. Each hop tries HEAD, then GET, without
 * following redirects; wrapped addresses (consent.google.com?continue=…) are
 * unwrapped; a 200 page is read (≤200 KB) for its redirect. Tried with a
 * phone browser's identity first, then a desktop one; 10 s in total.
 * Returns null when no Google Maps address was reached.
 */
suspend fun resolveShortLink(url: String): String? = withContext(Dispatchers.IO) {
    val deadline = System.currentTimeMillis() + RESOLVE_BUDGET_MS
    followShortLink(url, MOBILE_UA, deadline) ?: followShortLink(url, DESKTOP_UA, deadline)
}

private fun followShortLink(start: String, userAgent: String, deadline: Long): String? {
    var current = unwrapRedirect(start) ?: start
    repeat(MAX_HOPS) {
        if (isGoogleMapsUrl(current)) return current
        if (System.currentTimeMillis() >= deadline) return null
        val next = nextHop(current, userAgent, deadline) ?: return null
        current = unwrapRedirect(next) ?: next
    }
    return current.takeIf(::isGoogleMapsUrl)
}

/** Where [url] leads: its redirect target, or the Maps address inside its page. */
private fun nextHop(url: String, userAgent: String, deadline: Long): String? {
    if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) return null
    for (method in listOf("HEAD", "GET")) {
        val remaining = deadline - System.currentTimeMillis()
        if (remaining <= 0) return null
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (_: Exception) {
            return null
        }
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = remaining.coerceAtMost(5_000L).toInt()
            connection.readTimeout = remaining.coerceAtMost(5_000L).toInt()
            connection.requestMethod = method
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            val code = connection.responseCode
            val location = connection.getHeaderField("Location")
            if (code in 300..399 && !location.isNullOrBlank()) return resolveAgainst(url, location.trim())
            connection.getHeaderField("Refresh")
                ?.let { Regex("url\\s*=\\s*(\\S+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
                ?.let { return resolveAgainst(url, it) }
            if (method == "GET" && code in 200..299) {
                return mapsUrlFromHtml(readCapped(connection, MAX_PAGE_BYTES))
            }
            // HEAD without a redirect (200, 404, 405…): ask again with GET.
        } catch (_: Exception) {
            if (method == "GET") return null
        } finally {
            connection.disconnect()
        }
    }
    return null
}

private fun resolveAgainst(base: String, location: String): String =
    if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(location)) location
    else try {
        URL(URL(base), location).toString()
    } catch (_: Exception) {
        location
    }

private fun readCapped(connection: HttpURLConnection, maxBytes: Int): String {
    val out = ByteArrayOutputStream()
    connection.inputStream.use { input ->
        val buf = ByteArray(8 * 1024)
        while (out.size() < maxBytes) {
            val n = input.read(buf, 0, minOf(buf.size, maxBytes - out.size()))
            if (n < 0) break
            out.write(buf, 0, n)
        }
    }
    return out.toString("UTF-8")
}
