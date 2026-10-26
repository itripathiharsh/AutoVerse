package com.autoverse.launcher.core.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Tier 2 — live engine data over a paired ELM327-compatible Bluetooth
 * adapter (Serial Port Profile). This is the only hardware-coupled OBD
 * component: everything above it consumes [ObdConnectionState] and parses
 * through the pure [ObdProtocol].
 *
 * Hardware behaviour is UNVERIFIED until tested with a real adapter
 * (documented in docs/tier2-obd-offline.md); the protocol layer it feeds is
 * fully unit-tested.
 */
class SystemObdRepository(private val context: Context) {

    private var socket: BluetoothSocket? = null

    val hasConnectPermission: Boolean
        get() = if (android.os.Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // legacy BLUETOOTH is install-time on API 23-30
        }

    /** True when the device has a Bluetooth adapter and it is enabled. */
    fun isBluetoothReady(): Boolean {
        if (!hasConnectPermission) return false
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    /**
     * Finds a bonded adapter whose name matches common ELM327 devices.
     * Returns its friendly name, or null when nothing matches.
     */
    @SuppressLint("MissingPermission")
    fun findPairedAdapter(): String? {
        if (!hasConnectPermission) return null
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: return null
        val bonded = try {
            adapter.bondedDevices.orEmpty()
        } catch (_: SecurityException) {
            return null
        }
        return bonded
            .sortedBy { it.name ?: "" }
            .firstOrNull { device ->
                val name = device.name ?: return@firstOrNull false
                OBD_NAME_HINTS.any { hint -> name.uppercase().contains(hint) }
            }
            ?.name
    }

    /**
     * All bonded Bluetooth devices for the user-driven adapter picker.
     * Likely ELM327 clones (name heuristics) sort first and are flagged so
     * the UI can mark them; every bonded device stays selectable because
     * plenty of adapters ship under arbitrary names.
     */
    @SuppressLint("MissingPermission")
    fun listPairedAdapters(): List<ObdAdapter> {
        if (!hasConnectPermission) return emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bonded = try {
            manager?.adapter?.bondedDevices.orEmpty()
        } catch (_: SecurityException) {
            return emptyList()
        }
        return bonded
            .mapNotNull { device ->
                val name = try {
                    device.name
                } catch (_: SecurityException) {
                    null
                } ?: return@mapNotNull null
                ObdAdapter(
                    name = name,
                    address = device.address,
                    likelyObd = OBD_NAME_HINTS.any { name.uppercase().contains(it) },
                )
            }
            .sortedWith(compareByDescending<ObdAdapter> { it.likelyObd }.thenBy { it.name })
    }

    /**
     * Opens the SPP socket to a bonded adapter — the one at [deviceAddress]
     * when given (user selection), otherwise the first name-heuristic match —
     * performs the ELM327 handshake, then polls [pollOrder] round-robin,
     * delivering merged samples to [onSample]. Between polls it honours a
     * user-requested Mode 03 fault-code read via [consumeDtcRequest] and
     * reports the outcome through [onDtcs] (null = read unavailable).
     * Blocks on the caller's dispatcher (IO).
     *
     * Closes the socket when [shouldStop] turns true or on any IO failure.
     * Throws IOException with a readable message for the UI error state.
     */
    @SuppressLint("MissingPermission")
    suspend fun runSession(
        onState: (ObdConnectionState) -> Unit,
        onSample: (ObdSample) -> Unit,
        shouldStop: () -> Boolean,
        deviceAddress: String? = null,
        consumeDtcRequest: () -> Boolean = { false },
        onDtcs: ((List<ObdTroubleCode>?) -> Unit)? = null,
    ): Unit = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: throw IOException("No Bluetooth adapter on this device")
        val device = try {
            val bonded = adapter.bondedDevices.orEmpty()
            if (deviceAddress != null) {
                bonded.firstOrNull { it.address == deviceAddress }
                    ?: throw IOException("That adapter is no longer paired — refresh the list and pick again")
            } else {
                bonded
                    .sortedBy { it.name ?: "" }
                    .firstOrNull { d -> OBD_NAME_HINTS.any { (d.name ?: "").uppercase().contains(it) } }
            }
        } catch (_: SecurityException) {
            null
        } ?: throw IOException(
            if (deviceAddress == null) {
                "No paired OBD adapter found \u2014 pair your ELM327 device in Bluetooth settings first"
            } else {
                "Bluetooth permission was revoked"
            },
        )

        val name = device.name ?: "OBD adapter"
        onState(ObdConnectionState.Connecting(name))
        val sock = try {
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        } catch (_: SecurityException) {
            throw IOException("Bluetooth permission was revoked")
        }
        socket = sock
        try {
            adapter.cancelDiscovery()
        } catch (_: Exception) {
            // Discovery may not be running; never fatal.
        }
        try {
            sock.connect()
        } catch (e: IOException) {
            throw IOException("Could not reach $name \u2014 check the adapter is plugged in", e)
        }

        try {
            sock.outputStream.use { out ->
                sock.inputStream.use { input ->
                    handshake(input, out)
                    onState(ObdConnectionState.Connected(name, ObdSample()))
                    pollLoop(input, out, onSample, shouldStop, consumeDtcRequest, onDtcs)
                }
            }
        } finally {
            try {
                sock.close()
            } catch (_: IOException) {
            }
            socket = null
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        socket = null
    }

    private suspend fun handshake(input: InputStream, out: OutputStream) {
        for (command in ObdProtocol.initCommands) {
            writeCommand(out, command)
            val response = readResponse(input)
            if (command == "ATZ" && response != null && !ObdProtocol.isResetSuccessful(response)) {
                // Some clones answer late; allow one retry before failing.
                writeCommand(out, command)
                val retry = readResponse(input)
                if (retry == null || !ObdProtocol.isResetSuccessful(retry)) {
                    throw IOException("The adapter did not answer its reset (is it an ELM327?)")
                }
            }
        }
    }

    private suspend fun pollLoop(
        input: InputStream,
        out: OutputStream,
        onSample: (ObdSample) -> Unit,
        shouldStop: () -> Boolean,
        consumeDtcRequest: () -> Boolean = { false },
        onDtcs: ((List<ObdTroubleCode>?) -> Unit)? = null,
    ) {
        var sample = ObdSample()
        var index = 0
        while (!shouldStop()) {
            if (onDtcs != null && consumeDtcRequest()) {
                try {
                    writeCommand(out, ObdProtocol.DTC_COMMAND)
                    // Mode 03 answers can trail behind PID responses; give the
                    // ECU extra time before declaring the read unavailable.
                    val response = readResponse(input, DTC_TIMEOUT_MS)
                    onDtcs(response?.let { ObdProtocol.parseDtcs(it) })
                } catch (_: IOException) {
                    throw IOException("Connection to the adapter was lost")
                }
            }
            val pid = ObdProtocol.pollOrder[index % ObdProtocol.pollOrder.size]
            index += 1
            try {
                writeCommand(out, ObdProtocol.pollCommand(pid))
                val response = readResponse(input)
                val value = response?.let { ObdProtocol.parse(pid, it) }
                if (value != null) {
                    sample = mergeSample(sample, pid, value)
                    onSample(sample)
                }
            } catch (_: IOException) {
                throw IOException("Connection to the adapter was lost")
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun writeCommand(out: OutputStream, command: String) {
        out.write((command + "\r").toByteArray(Charsets.US_ASCII))
        out.flush()
    }

    /** Reads until the ELM prompt, a timeout, or the stream ends. */
    private suspend fun readResponse(
        input: InputStream,
        timeoutMs: Long = RESPONSE_TIMEOUT_MS,
    ): String? {
        val buffer = StringBuilder()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (input.available() > 0) {
                val chunk = ByteArray(256)
                val read = input.read(chunk)
                if (read <= 0) break
                buffer.append(String(chunk, 0, read, Charsets.US_ASCII))
                if (buffer.contains('>')) break
            } else {
                delay(READ_IDLE_MS)
            }
        }
        val text = buffer.toString()
        return text.ifBlank { null }
    }

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val POLL_INTERVAL_MS = 150L
        private const val RESPONSE_TIMEOUT_MS = 1_200L
        private const val DTC_TIMEOUT_MS = 3_000L
        private const val READ_IDLE_MS = 20L

        /** Common bonded names across the ELM327 clones that dominate the market. */
        val OBD_NAME_HINTS = listOf("OBD", "ELM", "V-LINK", "VLINK", "VEEPEAK", "Vgate", "IOS-VLINK", "VEE_LINK")
    }
}
