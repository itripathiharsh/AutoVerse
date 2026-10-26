package com.autoverse.launcher.core.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdProtocolTest {

    @Test
    fun `poll commands are the pid with carriage return`() {
        assertEquals("010D\r", ObdProtocol.pollCommand(ObdPid.VEHICLE_SPEED))
        assertEquals("010C\r", ObdProtocol.pollCommand(ObdPid.ENGINE_RPM))
    }

    @Test
    fun `init sequence is reset then echo off linefeeds off auto protocol`() {
        assertEquals(listOf("ATZ", "ATE0", "ATL0", "ATSP0"), ObdProtocol.initCommands)
    }

    @Test
    fun `speed parses from a clean single frame`() {
        // 0x5A = 90 km/h
        val v = ObdProtocol.parse(ObdPid.VEHICLE_SPEED, "41 0D 5A\r>")
        assertEquals(ObdValue("speedKmh", 90), v)
    }

    @Test
    fun `rpm parses two byte formula`() {
        // ((0x1A*256)+0xF8)/4 = 1726.5 -> 1726
        val v = ObdProtocol.parse(ObdPid.ENGINE_RPM, "41 0C 1A F8\r>")
        assertEquals(ObdValue("rpm", 1726), v)
    }

    @Test
    fun `coolant subtracts forty offset`() {
        val v = ObdProtocol.parse(ObdPid.COOLANT_TEMP, "41 05 59")
        assertEquals(ObdValue("coolantC", 49), v) // 0x59=89; 89-40=49
    }

    @Test
    fun `percentages scale byte by 100 over 255`() {
        assertEquals(ObdValue("fuelPct", 62), ObdProtocol.parse(ObdPid.FUEL_LEVEL, "41 2F 9F"))
        assertEquals(ObdValue("loadPct", 39), ObdProtocol.parse(ObdPid.ENGINE_LOAD, "41 04 64"))
        assertEquals(ObdValue("throttlePct", 15), ObdProtocol.parse(ObdPid.THROTTLE_POS, "41 11 27"))
    }

    @Test
    fun `multiframe iso tp response assembles across lines`() {
        val raw = "SEARCHING...\r\r0D\r41 0D 00\r41 0D 3C\r>"
        // Scanner picks the first 41-0D header; bytes continue with 00 then 3C is a
        // separate frame — parser must only trust the bytes of the first frame (00).
        val v = ObdProtocol.parse(ObdPid.VEHICLE_SPEED, raw)
        assertEquals(ObdValue("speedKmh", 0), v)
    }

    @Test
    fun `garbage no data and stops return null`() {
        assertNull(ObdProtocol.parse(ObdPid.VEHICLE_SPEED, "NO DATA"))
        assertNull(ObdProtocol.parse(ObdPid.VEHICLE_SPEED, "STOPPED"))
        assertNull(ObdProtocol.parse(ObdPid.VEHICLE_SPEED, "CAN ERROR"))
        assertNull(ObdProtocol.parse(ObdPid.VEHICLE_SPEED, "UNABLE TO CONNECT"))
        assertNull(ObdProtocol.parse(ObdPid.VEHICLE_SPEED, ""))
        assertNull(ObdProtocol.parse(ObdPid.VEHICLE_SPEED, "ATZ\rELM327 v1.5\r>"))
    }

    @Test
    fun `wrong pid header is ignored`() {
        assertNull(ObdProtocol.parse(ObdPid.VEHICLE_SPEED, "41 0C 1A F8"))
    }

    @Test
    fun `lowercase and echo noise still parse`() {
        val v = ObdProtocol.parse(ObdPid.VEHICLE_SPEED, "010d\r41 0d 32\r>")
        assertEquals(ObdValue("speedKmh", 50), v)
    }

    @Test
    fun `merge folds pid readings into the latest sample`() {
        var s: ObdSample? = null
        s = mergeSample(s, ObdPid.VEHICLE_SPEED, ObdValue("speedKmh", 64))
        s = mergeSample(s, ObdPid.ENGINE_RPM, ObdValue("rpm", 2100))
        assertEquals(ObdSample(rpm = 2100, speedKmh = 64), s)
        s = mergeSample(s, ObdPid.COOLANT_TEMP, ObdValue("coolantC", 88))
        assertEquals(ObdSample(rpm = 2100, speedKmh = 64, coolantC = 88), s)
    }

    @Test
    fun `merge keeps previous value when a pid goes quiet`() {
        val base = ObdSample(rpm = 2100, speedKmh = 64)
        val merged = mergeSample(base, ObdPid.VEHICLE_SPEED, ObdValue("speedKmh", 61))
        assertEquals(61, merged.speedKmh)
        assertEquals(2100, merged.rpm)
    }

    @Test
    fun `reset success detects elm identification`() {
        assertTrue(ObdProtocol.isResetSuccessful("ELM327 v1.5"))
        assertFalse(ObdProtocol.isResetSuccessful("?"))
    }

    @Test
    fun `trouble codes parse from mode 03 response`() {
        // Mode 03 response with P0133 (0x0133)
        val codes = ObdProtocol.parseDtcs("43 01 33 00 00 00 00\r>")
        assertEquals(1, codes?.size)
        assertEquals("P0133", codes?.first()?.code)
        assertTrue(codes?.first()?.description?.contains("O2 sensor", ignoreCase = true) == true)
    }

    @Test
    fun `no trouble codes returns empty list`() {
        assertEquals(emptyList<ObdTroubleCode>(), ObdProtocol.parseDtcs("43 00\r>"))
        assertEquals(emptyList<ObdTroubleCode>(), ObdProtocol.parseDtcs("NO DATA\r>"))
    }

    @Test
    fun `malformed mode 03 response returns null`() {
        assertNull(ObdProtocol.parseDtcs("CAN ERROR\r>"))
        assertNull(ObdProtocol.parseDtcs("UNABLE TO CONNECT\r>"))
        assertNull(ObdProtocol.parseDtcs(""))
    }
}

