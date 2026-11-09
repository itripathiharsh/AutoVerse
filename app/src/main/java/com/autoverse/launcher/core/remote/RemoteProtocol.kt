package com.autoverse.launcher.core.remote

import java.net.URLDecoder
import java.security.MessageDigest
import org.json.JSONObject

/**
 * AutoVerse Remote: the phone scans a QR code on the head unit and gets a
 * remote in its browser (no app, iPhone or Android). This file is the pure,
 * JVM-tested part: request parsing, the access check and routing. The socket
 * server lives in [PhoneRemoteServer]; what the remote can see and do is the
 * [RemoteBridge] the app root provides.
 */

/** What the phone may see. Nothing here is sensitive beyond the song and trip. */
data class RemoteState(
    val hasMedia: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val app: String? = null,
    val playing: Boolean = false,
    val navigating: Boolean = false,
    val destination: String? = null,
    val arrival: String? = null,
    val remaining: String? = null,
)

/** Everything the phone may do — a closed set; nothing else is reachable. */
enum class RemoteCommand(val wire: String) {
    TOGGLE("toggle"), NEXT("next"), PREVIOUS("prev"),
    VOLUME_UP("volup"), VOLUME_DOWN("voldown"), END_TRIP("endtrip");

    companion object {
        fun fromWire(value: String?): RemoteCommand? = entries.firstOrNull { it.wire == value }
    }
}

interface RemoteBridge {
    fun state(): RemoteState
    fun command(command: RemoteCommand)
    /** A place name, address, coordinates or Google Maps link to route to. */
    fun navigate(text: String)
}

data class HttpRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: String = "",
)

class HttpResponse(val status: Int, val contentType: String, val body: ByteArray) {
    val reason: String
        get() = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            413 -> "Payload Too Large"
            503 -> "Service Unavailable"
            else -> "Error"
        }

    companion object {
        fun json(status: Int, json: JSONObject) =
            HttpResponse(status, "application/json; charset=utf-8", json.toString().toByteArray())

        fun error(status: Int, message: String) = json(status, JSONObject().put("ok", false).put("error", message))
    }
}

/** Largest request body accepted (a place name or a maps link). */
const val MAX_BODY_BYTES = 4_096

/** Largest request head accepted (request line + headers). */
const val MAX_HEAD_BYTES = 8_192

/** Parses "GET /api/state?x=1 HTTP/1.1\r\nHeader: v\r\n…" (the part before the blank line). */
fun parseRequestHead(head: String): HttpRequest? {
    val lines = head.split("\r\n").filter { it.isNotEmpty() }
    val requestLine = lines.firstOrNull()?.split(' ') ?: return null
    if (requestLine.size < 2) return null
    val method = requestLine[0].uppercase()
    val target = requestLine[1]
    if (!target.startsWith("/")) return null
    val path = target.substringBefore('?')
    val query = parseForm(target.substringAfter('?', ""))
    val headers = lines.drop(1).mapNotNull { line ->
        val i = line.indexOf(':')
        if (i <= 0) null else line.substring(0, i).trim().lowercase() to line.substring(i + 1).trim()
    }.toMap()
    return HttpRequest(method, path, query, headers)
}

/** "a=1&b=two%20words" → map; malformed pairs are skipped. */
fun parseForm(raw: String): Map<String, String> = raw.split('&')
    .filter { it.isNotEmpty() }
    .mapNotNull { pair ->
        val k = pair.substringBefore('=')
        val v = pair.substringAfter('=', "")
        try {
            URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
        } catch (_: Exception) {
            null
        }
    }.toMap()

/** Constant-time comparison, so the code can't be guessed by timing. */
fun tokenMatches(expected: String, given: String?): Boolean =
    given != null && expected.isNotEmpty() &&
        MessageDigest.isEqual(expected.toByteArray(), given.toByteArray())

/**
 * Routes one request. The page itself is public (it holds no data); every
 * /api call must carry the pairing code from the QR — as the
 * X-AutoVerse-Code header (what the page sends) or ?code=.
 */
class RemoteRouter(
    private val code: () -> String,
    private val bridge: () -> RemoteBridge?,
    private val page: () -> ByteArray,
    private val onAuthorized: () -> Unit = {},
) {
    fun handle(request: HttpRequest): HttpResponse {
        if (request.path == "/" || request.path == "/index.html") {
            return if (request.method == "GET") {
                HttpResponse(200, "text/html; charset=utf-8", page())
            } else {
                HttpResponse.error(405, "method")
            }
        }
        if (!request.path.startsWith("/api/")) return HttpResponse.error(404, "not found")
        val given = request.headers["x-autoverse-code"] ?: request.query["code"]
        if (!tokenMatches(code(), given)) return HttpResponse.error(403, "code")
        val b = bridge() ?: return HttpResponse.error(503, "starting")
        onAuthorized()
        return when (request.path) {
            "/api/state" -> HttpResponse.json(200, stateJson(b.state()))
            "/api/cmd" -> {
                if (request.method != "POST") return HttpResponse.error(405, "method")
                val c = RemoteCommand.fromWire(request.query["c"] ?: parseForm(request.body)["c"])
                    ?: return HttpResponse.error(400, "command")
                b.command(c)
                HttpResponse.json(200, JSONObject().put("ok", true))
            }
            "/api/go" -> {
                if (request.method != "POST") return HttpResponse.error(405, "method")
                val text = request.body.trim().take(2_000)
                if (text.isEmpty()) return HttpResponse.error(400, "empty")
                b.navigate(text)
                HttpResponse.json(200, JSONObject().put("ok", true))
            }
            else -> HttpResponse.error(404, "not found")
        }
    }
}

fun stateJson(s: RemoteState): JSONObject = JSONObject()
    .put("ok", true)
    .put("hasMedia", s.hasMedia)
    .put("title", s.title ?: JSONObject.NULL)
    .put("artist", s.artist ?: JSONObject.NULL)
    .put("app", s.app ?: JSONObject.NULL)
    .put("playing", s.playing)
    .put("navigating", s.navigating)
    .put("destination", s.destination ?: JSONObject.NULL)
    .put("arrival", s.arrival ?: JSONObject.NULL)
    .put("remaining", s.remaining ?: JSONObject.NULL)

/** A fresh pairing code: 10 characters, no look-alikes (0/O, 1/l/I). */
fun newPairingCode(random: java.util.Random = java.security.SecureRandom()): String {
    val alphabet = "abcdefghjkmnpqrstuvwxyz23456789"
    return (1..10).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
}

/** The address the QR opens; the code rides in the #fragment so it never appears in logs. */
fun remoteUrl(ip: String, port: Int, code: String): String = "http://$ip:$port/#$code"
