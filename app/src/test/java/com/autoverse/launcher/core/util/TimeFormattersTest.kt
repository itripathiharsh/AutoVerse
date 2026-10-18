package com.autoverse.launcher.core.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormattersTest {

    @Test
    fun `twelve hour clock trims leading zero`() {
        assertEquals("9:05 AM", formatClock(LocalTime.of(9, 5), clock24h = false, locale = Locale.US))
    }

    @Test
    fun `midnight renders as twelve AM`() {
        assertEquals("12:00 AM", formatClock(LocalTime.MIDNIGHT, clock24h = false, locale = Locale.US))
    }

    @Test
    fun `noon renders as twelve PM`() {
        assertEquals("12:00 PM", formatClock(LocalTime.NOON, clock24h = false, locale = Locale.US))
    }

    @Test
    fun `twenty four hour clock pads hours`() {
        assertEquals("21:30", formatClock(LocalTime.of(21, 30), clock24h = true, locale = Locale.US))
        assertEquals("00:00", formatClock(LocalTime.MIDNIGHT, clock24h = true, locale = Locale.US))
    }

    @Test
    fun `full date is day comma day month`() {
        assertEquals("Sat, 17 Oct", formatFullDate(LocalDate.of(2026, 10, 17), Locale.US))
    }

    @Test
    fun `track time renders minutes and padded seconds`() {
        assertEquals("0:00", formatTrackTime(0L))
        assertEquals("1:36", formatTrackTime(96_000L))
        assertEquals("59:59", formatTrackTime(3_599_000L))
    }

    @Test
    fun `track time switches to hours past an hour`() {
        assertEquals("1:00:00", formatTrackTime(3_600_000L))
        assertEquals("1:01:01", formatTrackTime(3_661_000L))
    }

    @Test
    fun `track time clamps negative input`() {
        assertEquals("0:00", formatTrackTime(-500L))
    }

    @Test
    fun `log timestamp renders hours minutes seconds in the given zone`() {
        assertEquals("22:14:05", formatLogTimestamp(1_700_000_045_000L, ZoneOffset.UTC))
        assertEquals("00:14:05", formatLogTimestamp(1_700_000_045_000L, ZoneOffset.ofHours(2)))
    }

    @Test
    fun `log timestamp pads all three fields`() {
        assertEquals("00:00:00", formatLogTimestamp(0L, ZoneOffset.UTC))
    }
}
