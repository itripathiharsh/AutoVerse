package com.autoverse.launcher.core.remote

import android.content.Context
import android.os.SystemClock
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny local web server for AutoVerse Remote. Runs only while the driver has
 * turned the remote on, listens on the unit's local network (normally the
 * phone's own hotspot), serves one page and a handful of /api calls guarded
 * by the pairing code. A few threads, a few KB of memory, nothing when off.
 */
class PhoneRemoteServer(private val router: RemoteRouter) {
    private var socket: ServerSocket? = null
    private val workers = Executors.newFixedThreadPool(3)

    /** Starts on the first free port of [PORTS]; returns it, or null when none could be opened. */
    @Synchronized
    fun start(): Int? {
        socket?.let { return it.localPort }
        for (port in PORTS) {
            val s = try {
                ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port), 8)
                }
            } catch (_: Exception) {
                continue
            }
            socket = s
            Thread({ acceptLoop(s) }, "autoverse-remote").apply { isDaemon = true; start() }
            return port
        }
        return null
    }

    @Synchronized
    fun stop() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    private fun acceptLoop(server: ServerSocket) {
        while (!server.isClosed) {
            val client = try {
                server.accept()
            } catch (_: Exception) {
                break
            }
            try {
                workers.execute { serve(client) }
            } catch (_: Exception) {
                client.close()
            }
        }
    }

    private fun serve(client: Socket) {
        client.use { s ->
            try {
                s.soTimeout = 5_000
                val input = s.getInputStream()
                val head = readHead(input) ?: return
                val request = parseRequestHead(head) ?: return respond(s, HttpResponse.error(400, "bad request"))
                val length = request.headers["content-length"]?.toIntOrNull() ?: 0
                if (length > MAX_BODY_BYTES) return respond(s, HttpResponse.error(413, "too large"))
                val body = if (length > 0) String(readExactly(input, length), Charsets.UTF_8) else ""
                respond(s, router.handle(request.copy(body = body)))
            } catch (_: Exception) {
                // A dropped phone connection is normal; nothing to report.
            }
        }
    }

    private fun respond(socket: Socket, response: HttpResponse) {
        val header = "HTTP/1.1 ${response.status} ${response.reason}\r\n" +
            "Content-Type: ${response.contentType}\r\n" +
            "Content-Length: ${response.body.size}\r\n" +
            "Cache-Control: no-store\r\n" +
            "X-Content-Type-Options: nosniff\r\n" +
            "Referrer-Policy: no-referrer\r\n" +
            "Connection: close\r\n\r\n"
        socket.getOutputStream().apply {
            write(header.toByteArray(Charsets.US_ASCII))
            write(response.body)
            flush()
        }
    }

    private fun readHead(input: InputStream): String? {
        val out = ByteArrayOutputStream()
        var matched = 0
        val end = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())
        while (out.size() < MAX_HEAD_BYTES) {
            val b = input.read()
            if (b < 0) return null
            out.write(b)
            matched = if (b.toByte() == end[matched]) matched + 1 else if (b.toByte() == end[0]) 1 else 0
            if (matched == 4) return out.toString(Charsets.ISO_8859_1.name()).dropLast(4)
        }
        return null
    }

    private fun readExactly(input: InputStream, length: Int): ByteArray {
        val buf = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(buf, read, length - read)
            if (n < 0) break
            read += n
        }
        return buf.copyOf(read)
    }

    companion object {
        val PORTS = listOf(8765, 8766, 8767, 8780, 8790)
    }
}

/**
 * Remote on/off, the pairing code and the address the QR shows. One per
 * process (the server outlives screen changes). Off by default — an
 * optional extra the driver turns on from Car → Phone.
 */
class PhoneRemoteController private constructor(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("phone_remote", Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _code = MutableStateFlow(prefs.getString(KEY_CODE, null) ?: newPairingCode().also(::saveCode))
    val code: StateFlow<String> = _code.asStateFlow()

    /** The address to open, or null while off / when the unit has no local network. */
    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url.asStateFlow()

    /** Last time a phone used the remote (elapsedRealtime ms); 0 = never this session. */
    private val _lastPhoneMs = MutableStateFlow(0L)
    val lastPhoneMs: StateFlow<Long> = _lastPhoneMs.asStateFlow()

    @Volatile private var bridge: RemoteBridge? = null
    private var port: Int? = null

    private val server = PhoneRemoteServer(
        RemoteRouter(
            code = { _code.value },
            bridge = { bridge },
            page = { page },
            onAuthorized = { _lastPhoneMs.value = SystemClock.elapsedRealtime() },
        ),
    )

    private val page: ByteArray by lazy {
        app.assets.open("remote/index.html").use { it.readBytes() }
    }

    fun attach(bridge: RemoteBridge?) {
        this.bridge = bridge
    }

    fun setEnabled(on: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, on).apply()
        _enabled.value = on
        apply()
    }

    /** A new code: phones paired with the old one lose access at once. */
    fun newCode() {
        _code.value = newPairingCode().also(::saveCode)
        _lastPhoneMs.value = 0L
        refreshAddress()
    }

    /** Starts/stops the server to match the switch and refreshes the address. */
    fun apply() {
        if (_enabled.value) {
            Thread {
                port = server.start()
                refreshAddress()
            }.start()
        } else {
            server.stop()
            port = null
            _url.value = null
        }
    }

    /** Re-reads the unit's local IP (it changes when the hotspot changes). */
    fun refreshAddress() {
        val p = port
        val ip = localIpv4()
        _url.value = if (_enabled.value && p != null && ip != null) remoteUrl(ip, p, _code.value) else null
    }

    private fun saveCode(code: String) {
        prefs.edit().putString(KEY_CODE, code).apply()
    }

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_CODE = "code"

        @Volatile private var instance: PhoneRemoteController? = null

        fun get(context: Context): PhoneRemoteController =
            instance ?: synchronized(this) { instance ?: PhoneRemoteController(context).also { instance = it } }

        /** The unit's address on Wi-Fi / hotspot (private IPv4), preferring wlan. */
        fun localIpv4(): String? = try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { if (it.name.startsWith("wlan")) 0 else if (it.name.startsWith("eth")) 1 else 2 }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
