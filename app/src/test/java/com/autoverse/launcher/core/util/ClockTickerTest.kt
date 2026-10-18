package com.autoverse.launcher.core.util

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 9 — the Constrained workload ticks the clock on minute boundaries.
 * The helper must land exactly on the next boundary, never return 0 (that
 * would spin the ticker), and stay within one minute.
 */
class ClockTickerTest {

    @Test
    fun `exact minute boundary waits a full minute`() {
        val time = LocalDateTime.of(2026, 10, 17, 12, 34, 0, 0)
        assertEquals(60_000L, millisUntilNextMinute(time))
    }

    @Test
    fun `half past the minute waits thirty seconds`() {
        val time = LocalDateTime.of(2026, 10, 17, 12, 34, 30, 0)
        assertEquals(30_000L, millisUntilNextMinute(time))
    }

    @Test
    fun `one second before the boundary waits one second`() {
        val time = LocalDateTime.of(2026, 10, 17, 12, 34, 59, 0)
        assertEquals(1_000L, millisUntilNextMinute(time))
    }

    @Test
    fun `one nanosecond before the boundary clamps to one millisecond`() {
        val time = LocalDateTime.of(2026, 10, 17, 12, 34, 59, 999_999_999)
        assertEquals(1L, millisUntilNextMinute(time))
    }

    @Test
    fun `every second of the minute yields a bounded positive wait`() {
        for (second in 0..59) {
            val time = LocalDateTime.of(2026, 10, 17, 23, 59, second, 0)
            val wait = millisUntilNextMinute(time)
            assertTrue("second=$second wait=$wait", wait in 1L..60_000L)
        }
    }

    @Test
    fun `midnight wraps to the next day's boundary`() {
        val time = LocalDateTime.of(2026, 10, 17, 23, 59, 30, 0)
        assertEquals(30_000L, millisUntilNextMinute(time))
    }
}
