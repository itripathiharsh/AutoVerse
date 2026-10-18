package com.autoverse.launcher.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 17 — pins the pure halves of [SystemSampler]: `/proc/self/stat`
 * parsing around hostile comm fields, and the two-sample CPU delta math
 * including every null-degradation path.
 */
class SystemSamplerTest {

    // --- /proc/self/stat parsing -------------------------------------------

    @Test
    fun `stat parsing sums utime and stime after the last parenthesis`() {
        // Fields after ')': state(0) .. utime(11), stime(12).
        val line = "1234 (com.autoverse.launcher) S 1 2 3 4 5 6 7 8 9 10 100 25"
        assertEquals(125L, parseCpuStatTotalTicks(line))
    }

    @Test
    fun `stat parsing survives spaces and parentheses inside comm`() {
        val line = "42 (weird (name) here) R 1 2 3 4 5 6 7 8 9 10 7 3"
        assertEquals(10L, parseCpuStatTotalTicks(line))
    }

    @Test
    fun `malformed stat lines return null`() {
        assertNull(parseCpuStatTotalTicks("no parenthesis at all"))
        assertNull(parseCpuStatTotalTicks("1 (x) S 1 2 3"))
        assertNull(parseCpuStatTotalTicks("1 (x) S 1 2 3 4 5 6 7 8 9 10 nope 3"))
    }

    // --- CPU delta math ------------------------------------------------------

    @Test
    fun `cpu percent needs a previous reading`() {
        assertNull(cpuPercentOfCore(null, null, 200L, 2_000_000_000L, 100L))
        assertNull(cpuPercentOfCore(100L, null, 200L, 2_000_000_000L, 100L))
        assertNull(cpuPercentOfCore(null, 0L, 200L, 2_000_000_000L, 100L))
    }

    @Test
    fun `cpu percent rejects a non-positive tick rate`() {
        assertNull(cpuPercentOfCore(100L, 0L, 200L, 1_000_000_000L, 0L))
        assertNull(cpuPercentOfCore(100L, 0L, 200L, 1_000_000_000L, -100L))
    }

    @Test
    fun `cpu percent rejects non-positive intervals and negative deltas`() {
        assertNull(cpuPercentOfCore(100L, 1_000_000_000L, 200L, 1_000_000_000L, 100L))
        assertNull(cpuPercentOfCore(200L, 1_000_000_000L, 100L, 2_000_000_000L, 100L))
    }

    @Test
    fun `cpu percent maps deltas proportionally to one core`() {
        // 50 ticks of 100 over one second = half a core.
        assertEquals(50f, cpuPercentOfCore(100L, 0L, 150L, 1_000_000_000L, 100L)!!, 0.0001f)
        // A full core for one second is 100%.
        assertEquals(100f, cpuPercentOfCore(0L, 0L, 100L, 1_000_000_000L, 100L)!!, 0.0001f)
        // Multi-threaded peaks can exceed one core.
        assertEquals(250f, cpuPercentOfCore(0L, 0L, 250L, 1_000_000_000L, 100L)!!, 0.0001f)
        // 18 ticks over a quarter second = 72%.
        assertEquals(72f, cpuPercentOfCore(0L, 0L, 18L, 250_000_000L, 100L)!!, 0.0001f)
    }

    @Test
    fun `zero tick delta reads as zero percent not null`() {
        assertEquals(0f, cpuPercentOfCore(100L, 0L, 100L, 1_000_000_000L, 100L)!!, 0.0001f)
    }
}
