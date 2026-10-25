package com.autoverse.launcher.core.obd

/**
 * Tier 2 — live engine data over an ELM327-compatible Bluetooth adapter.
 *
 * This file is the pure protocol layer: command construction and response
 * parsing only, no Android types, so every rule is exercisable by plain JVM
 * unit tests (ObdProtocolTest). The Bluetooth socket lives in
 * [SystemObdRepository] and is the only hardware-coupled part.
 *
 * Honesty rules (same discipline as diagnostics): a PID the vehicle refuses
 * or does not support parses to null and the UI renders "\u2014"; nothing is
 * invented, nothing is extrapolated.
 */
enum class ObdPid(val command: String) {
    ENGINE_RPM("010C"),
    VEHICLE_SPEED("010D"),
    COOLANT_TEMP("0105"),
    FUEL_LEVEL("012F"),
    ENGINE_LOAD("0104"),
    THROTTLE_POS("0111"),
}

/** One polled reading. Null = the vehicle returned no data for that PID. */
data class ObdSample(
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantC: Int? = null,
    val fuelPct: Int? = null,
    val loadPct: Int? = null,
    val throttlePct: Int? = null,
)

sealed interface ObdConnectionState {
    data object Disconnected : ObdConnectionState
    data object PermissionRequired : ObdConnectionState
    data class Connecting(val deviceName: String) : ObdConnectionState
    data class Connected(val deviceName: String, val sample: ObdSample) : ObdConnectionState
    data class Error(val message: String) : ObdConnectionState
}

object ObdProtocol {

    /** Handshake sent once after the RFCOMM socket opens, in order. */
    val initCommands: List<String> = listOf("ATZ", "ATE0", "ATL0", "ATSP0")

    /** Round-robin poll order: the driver-facing values first. */
    val pollOrder: List<ObdPid> = listOf(
        ObdPid.VEHICLE_SPEED,
        ObdPid.ENGINE_RPM,
        ObdPid.COOLANT_TEMP,
        ObdPid.FUEL_LEVEL,
        ObdPid.ENGINE_LOAD,
        ObdPid.THROTTLE_POS,
    )

    fun pollCommand(pid: ObdPid): String = "${pid.command}\r"

    /**
     * Parses one raw ELM327 response for [pid].
     *
     * Tolerates: echoed commands, "SEARCHING...", "STOPPED...", "NO DATA",
     * multi-line ISO-TP frames, stray spaces/CR/LF and the trailing ">" prompt.
     * Returns null for anything that is not a well-formed positive response
     * (0x41 + PID) with enough data bytes.
     */
    fun parse(pid: ObdPid, raw: String): ObdValue? {
        if (raw.isBlank()) return null
        val service = 0x41
        val pidByte = pid.command.substring(2).toInt(16)
        val wanted = (service shl 8) or pidByte
        val tokens = raw.uppercase()
            .replace(">", " ")
            .split(Regex("[\\s\\r\\n]+"))
            .filter { it.isNotEmpty() }
        // Scan for the "41 <pid>" header, then read that response's data bytes.
        for (i in 0 until tokens.size - 1) {
            if (tokens[i] == "41" && tokens[i + 1] == pid.command.substring(2)) {
                val data = tokens.drop(i + 2).takeWhile { it.length == 2 && it.all { c -> c.isDigit() || c in 'A'..'F' } }
                    .mapNotNull { it.toIntOrNull(16) }
                val value = decode(pid, data) ?: return null
                return value
            }
        }
        return null
    }

    private fun decode(pid: ObdPid, data: List<Int>): ObdValue? {
        if (data.isEmpty()) return null
        return when (pid) {
            ObdPid.ENGINE_RPM ->
                if (data.size >= 2) ObdValue("rpm", ((data[0] * 256) + data[1]) / 4) else null
            ObdPid.VEHICLE_SPEED ->
                ObdValue("speedKmh", data[0])
            ObdPid.COOLANT_TEMP ->
                ObdValue("coolantC", data[0] - 40)
            ObdPid.FUEL_LEVEL ->
                ObdValue("fuelPct", (data[0] * 100) / 255)
            ObdPid.ENGINE_LOAD ->
                ObdValue("loadPct", (data[0] * 100) / 255)
            ObdPid.THROTTLE_POS ->
                ObdValue("throttlePct", (data[0] * 100) / 255)
        }
    }

    /** Detects the ELM327 identification line in an ATZ response. */
    fun isResetSuccessful(raw: String): Boolean =
        raw.uppercase().contains("ELM")
}

/** A single decoded reading with the field name the cockpit consumes. */
data class ObdValue(val field: String, val value: Int)

/**
 * Folds per-PID parses into the latest sample. Unknown/absent PIDs keep the
 * previous value until the adapter says otherwise (they are genuinely
 * unsupported or momentarily unavailable — not zero).
 */
fun mergeSample(
    previous: ObdSample?,
    pid: ObdPid,
    value: ObdValue,
): ObdSample {
    val p = previous ?: ObdSample()
    return when (value.field) {
        "rpm" -> p.copy(rpm = value.value)
        "speedKmh" -> p.copy(speedKmh = value.value)
        "coolantC" -> p.copy(coolantC = value.value)
        "fuelPct" -> p.copy(fuelPct = value.value)
        "loadPct" -> p.copy(loadPct = value.value)
        "throttlePct" -> p.copy(throttlePct = value.value)
        else -> p
    }
}
