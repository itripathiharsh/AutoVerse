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

/** A bonded Bluetooth device the OBD screen can offer for selection. */
data class ObdAdapter(
    val name: String,
    val address: String,
    /** True when the friendly name matches common ELM327 clone naming. */
    val likelyObd: Boolean,
)

/** One decoded trouble code with its generic SAE description when known. */
data class ObdTroubleCode(val code: String, val description: String? = null)

/** Result of a user-requested Mode 03 fault-code read. */
sealed interface ObdDtcState {
    /** No read has happened on this connection yet. */
    data object Idle : ObdDtcState
    /** A read is in flight against the adapter. */
    data object Reading : ObdDtcState
    /** Read succeeded; [codes] is empty when the ECU reports none stored. */
    data class Codes(val codes: List<ObdTroubleCode>) : ObdDtcState
    /** The adapter or vehicle refused / does not support Mode 03. */
    data class Unavailable(val reason: String) : ObdDtcState
}

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

    /** Mode 03 (read stored diagnostic trouble codes) request. */
    const val DTC_COMMAND: String = "03"

    /**
     * Parses a raw Mode 03 response into decoded trouble codes.
     *
     * Tolerates multi-ECU CAN frames (`43 <count> <pairs…>` repeated per ECU)
     * and the older count-less ISO 9141/KWP form (`43 <pairs…>`). A count byte
     * is detected by the classic heuristic: it must equal the number of
     * remaining byte pairs, so legitimate `00 0x` first codes still decode.
     *
     * Returns:
     * - `emptyList()` for an explicit `NO DATA` (supported, nothing stored);
     * - `null` for `UNABLE TO CONNECT` / bus errors / malformed input — the
     *   UI then reports the read as unavailable instead of guessing.
     */
    fun parseDtcs(raw: String): List<ObdTroubleCode>? {
        if (raw.isBlank()) return null
        val tokens = raw.uppercase()
            .replace(">", " ")
            .split(Regex("[\\s\\r\\n]+"))
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        if (tokens.any { it == "NO" || it == "DATA" } && tokens.none { it == "43" }) return emptyList()
        if (tokens.any { it.contains("UNABLE") || it.contains("CAN") && it.contains("ERROR") || it == "BUSINIT" }) {
            // "CAN ERROR", "BUS INIT..." style failures: not a valid read.
            if (tokens.none { it == "43" }) return null
        }

        val codes = linkedMapOf<String, ObdTroubleCode>()
        var sawValidFrame = false
        var i = 0
        while (i < tokens.size) {
            if (tokens[i] != "43") {
                i += 1
                continue
            }
            val bytes = mutableListOf<Int>()
            var j = i + 1
            while (j < tokens.size && tokens[j].length == 2 && tokens[j].all { it.isDigit() || it in 'A'..'F' }) {
                bytes.add(tokens[j].toInt(16))
                j += 1
            }
            i = if (j > i + 1) j else i + 1
            if (bytes.isEmpty()) continue

            var pairs: List<Int> = bytes
            // CAN count-byte heuristic: first byte equals the pair count of the rest.
            if (bytes.size >= 3 && bytes.size % 2 == 1 && bytes[0] == (bytes.size - 1) / 2) {
                pairs = bytes.drop(1)
                sawValidFrame = true
            } else if (bytes.size == 1 && bytes[0] == 0) {
                // "43 00" — CAN zero-codes answer.
                sawValidFrame = true
                continue
            }
            if (pairs.size % 2 != 0) pairs = pairs.dropLast(1)
            var k = 0
            while (k + 1 < pairs.size) {
                val value = (pairs[k] shl 8) or pairs[k + 1]
                if (value != 0x0000) {
                    val code = decodeDtc(value)
                    if (code != null) {
                        sawValidFrame = true
                        codes.putIfAbsent(code, ObdTroubleCode(code, dtcDescription(code)))
                    }
                }
                k += 2
            }
        }
        if (sawValidFrame) return codes.values.toList()
        // "43" was present but nothing decodable followed → malformed; no
        // "43" at all was already handled by the NO DATA branch above.
        return null
    }

    private fun decodeDtc(value: Int): String? {
        val letter = when (value shr 14) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            3 -> 'U'
            else -> return null
        }
        val digit1 = (value shr 12) and 0b11
        val digit2 = (value shr 8) and 0xF
        val digit3 = (value shr 4) and 0xF
        val digit4 = value and 0xF
        return "%c%d%X%X%X".format(letter, digit1, digit2, digit3, digit4)
    }

    /**
     * Generic SAE powertrain descriptions for common codes. Deliberately a
     * small curated table: unknown codes still display raw (e.g. "P1642"),
     * and every description is labelled informational in the UI — this is
     * not a diagnosis.
     */
    fun dtcDescription(code: String): String? = when (code) {
        "P0010" -> "A camshaft position actuator circuit (bank 1)"
        "P0011" -> "A camshaft position timing over-advanced (bank 1)"
        "P0016" -> "Crankshaft/camshaft position correlation (bank 1 sensor A)"
        "P0087" -> "Fuel rail/system pressure too low"
        "P0100" -> "Mass or volume air flow circuit"
        "P0101" -> "Mass or volume air flow circuit range/performance"
        "P0102" -> "Mass or volume air flow circuit low input"
        "P0113" -> "Intake air temperature sensor 1 circuit high"
        "P0128" -> "Coolant thermostat below regulating temperature"
        "P0131" -> "O2 sensor circuit low voltage (bank 1 sensor 1)"
        "P0133" -> "O2 sensor circuit slow response (bank 1 sensor 1)"
        "P0135" -> "O2 sensor heater circuit (bank 1 sensor 1)"
        "P0171" -> "Fuel trim system too lean (bank 1)"
        "P0172" -> "Fuel trim system too rich (bank 1)"
        "P0174" -> "Fuel trim system too lean (bank 2)"
        "P0175" -> "Fuel trim system too rich (bank 2)"
        "P0201" -> "Injector circuit/open — cylinder 1"
        "P0234" -> "Turbocharger/supercharger overboost condition"
        "P0299" -> "Turbocharger/supercharger underboost"
        "P0300" -> "Random/multiple cylinder misfire detected"
        "P0301" -> "Cylinder 1 misfire detected"
        "P0302" -> "Cylinder 2 misfire detected"
        "P0303" -> "Cylinder 3 misfire detected"
        "P0304" -> "Cylinder 4 misfire detected"
        "P0327" -> "Knock sensor 1 circuit low (bank 1)"
        "P0335" -> "Crankshaft position sensor A circuit"
        "P0340" -> "Camshaft position sensor A circuit (bank 1)"
        "P0401" -> "Exhaust gas recirculation flow insufficient"
        "P0420" -> "Catalyst system efficiency below threshold (bank 1)"
        "P0430" -> "Catalyst system efficiency below threshold (bank 2)"
        "P0442" -> "Evaporative emission system small leak"
        "P0455" -> "Evaporative emission system gross leak"
        "P0500" -> "Vehicle speed sensor A"
        "P0505" -> "Idle air control system"
        "P0562" -> "System voltage low"
        "P0700" -> "Transmission control system (request from TCU)"
        "P2004" -> "Intake manifold runner control stuck open (bank 1)"
        "P2096" -> "Post catalyst fuel trim system too lean (bank 1)"
        "P2181" -> "Cooling system performance"
        "P2295" -> "Fuel pressure regulator 2 control circuit low"
        "U0100" -> "Lost communication with ECM/PCM A"
        "U0101" -> "Lost communication with TCM"
        "C0035" -> "Left front wheel speed sensor circuit"
        "B1318" -> "Battery voltage low"
        else -> null
    }
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
