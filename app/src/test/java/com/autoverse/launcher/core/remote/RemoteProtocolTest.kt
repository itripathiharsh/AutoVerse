package com.autoverse.launcher.core.remote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteProtocolTest {

    private class FakeBridge : RemoteBridge {
        val commands = mutableListOf<RemoteCommand>()
        val places = mutableListOf<String>()
        override fun state() = RemoteState(hasMedia = true, title = "Kesariya", artist = "Arijit Singh", playing = true)
        override fun command(command: RemoteCommand) { commands += command }
        override fun navigate(text: String) { places += text }
    }

    private val bridge = FakeBridge()
    private var authorized = 0
    private val router = RemoteRouter(
        code = { "abc234defg" },
        bridge = { bridge },
        page = { "<html>remote</html>".toByteArray() },
        onAuthorized = { authorized++ },
    )

    private fun req(method: String, target: String, code: String? = "abc234defg", body: String = ""): HttpRequest {
        val head = "$method $target HTTP/1.1\r\nHost: 192.168.43.20:8765\r\n" +
            (if (code != null) "X-AutoVerse-Code: $code\r\n" else "")
        return parseRequestHead(head)!!.copy(body = body)
    }

    @Test
    fun `request head parses method, path, query and lower-cased headers`() {
        val r = parseRequestHead("POST /api/cmd?c=next&x=a%20b HTTP/1.1\r\nContent-Length: 0\r\nX-AutoVerse-Code: q")!!
        assertEquals("POST", r.method)
        assertEquals("/api/cmd", r.path)
        assertEquals("next", r.query["c"])
        assertEquals("a b", r.query["x"])
        assertEquals("0", r.headers["content-length"])
        assertEquals("q", r.headers["x-autoverse-code"])
        assertNull(parseRequestHead("garbage"))
        assertNull(parseRequestHead("GET noslash HTTP/1.1"))
    }

    @Test
    fun `the page is public but every api call needs the pairing code`() {
        assertEquals(200, router.handle(req("GET", "/", code = null)).status)
        assertEquals(403, router.handle(req("GET", "/api/state", code = null)).status)
        assertEquals(403, router.handle(req("GET", "/api/state", code = "wrong")).status)
        assertEquals(403, router.handle(req("POST", "/api/cmd?c=next", code = "abc234defh")).status)
        assertTrue(bridge.commands.isEmpty())
        assertEquals(0, authorized)
    }

    @Test
    fun `state returns the song and playing flag`() {
        val r = router.handle(req("GET", "/api/state"))
        assertEquals(200, r.status)
        val json = JSONObject(String(r.body))
        assertEquals("Kesariya", json.getString("title"))
        assertTrue(json.getBoolean("playing"))
        assertFalse(json.getBoolean("navigating"))
        assertEquals(1, authorized)
    }

    @Test
    fun `commands map to the closed set and nothing else`() {
        assertEquals(200, router.handle(req("POST", "/api/cmd?c=toggle")).status)
        assertEquals(200, router.handle(req("POST", "/api/cmd", body = "c=volup")).status)
        assertEquals(400, router.handle(req("POST", "/api/cmd?c=rm-rf")).status)
        assertEquals(405, router.handle(req("GET", "/api/cmd?c=next")).status)
        assertEquals(listOf(RemoteCommand.TOGGLE, RemoteCommand.VOLUME_UP), bridge.commands)
    }

    @Test
    fun `a place or maps link is forwarded, empty text is refused`() {
        val link = "https://maps.app.goo.gl/AbC123"
        assertEquals(200, router.handle(req("POST", "/api/go", body = "  $link \n")).status)
        assertEquals(400, router.handle(req("POST", "/api/go", body = "   ")).status)
        assertEquals(listOf(link), bridge.places)
        assertEquals(404, router.handle(req("GET", "/api/files")).status)
        assertEquals(404, router.handle(req("GET", "/etc/passwd")).status)
    }

    @Test
    fun `no bridge yet answers 503, not a crash`() {
        val starting = RemoteRouter(code = { "c0de234567" }, bridge = { null }, page = { ByteArray(0) })
        assertEquals(503, starting.handle(req("GET", "/api/state", code = "c0de234567")).status)
    }

    @Test
    fun `pairing codes are 10 unambiguous characters and change each time`() {
        val a = newPairingCode()
        val b = newPairingCode()
        assertEquals(10, a.length)
        assertTrue(a.none { it in "01ilo" })
        assertNotEquals(a, b)
        assertTrue(tokenMatches(a, a))
        assertFalse(tokenMatches(a, null))
        assertFalse(tokenMatches("", ""))
    }

    @Test
    fun `the QR address keeps the code in the fragment`() {
        assertEquals("http://192.168.43.20:8765/#abc234defg", remoteUrl("192.168.43.20", 8765, "abc234defg"))
    }
}
