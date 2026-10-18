package com.autoverse.launcher.core.performance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 9 — the pure frame math behind the diagnostics readout. Windows close
 * on the first frame one second or more after the window opened; a frame is
 * janky only when the gap to its predecessor is strictly longer than 33 ms.
 */
class FrameStatsAccumulatorTest {

    @Test
    fun `the first frame primes the window and reports nothing`() {
        val accumulator = FrameStatsAccumulator()
        assertNull(accumulator.record(0L))
        assertEquals(0L, accumulator.frameCount)
        assertEquals(0L, accumulator.jankCount)
    }

    @Test
    fun `fifty frames of twenty milliseconds close the window at fifty fps`() {
        val accumulator = FrameStatsAccumulator()
        accumulator.record(0L)
        var fps: Float? = null
        for (frame in 1..50) {
            fps = accumulator.record(frame * 20_000_000L)
        }
        assertEquals(50f, fps!!, 0.001f)
        assertEquals(50L, accumulator.frameCount)
        assertEquals(0L, accumulator.jankCount)
    }

    @Test
    fun `frames within the one second window report null`() {
        val accumulator = FrameStatsAccumulator()
        accumulator.record(0L)
        for (frame in 1..49) {
            assertNull(accumulator.record(frame * 20_000_000L))
        }
    }

    @Test
    fun `gaps strictly longer than 33 milliseconds are janky`() {
        val accumulator = FrameStatsAccumulator()
        accumulator.record(0L)
        accumulator.record(40_000_000L)
        assertEquals(1L, accumulator.jankCount)

        accumulator.record(40_000_000L + 33_000_000L)
        assertEquals(1L, accumulator.jankCount)

        accumulator.record(40_000_000L + 33_000_000L + 33_000_001L)
        assertEquals(2L, accumulator.jankCount)
        assertEquals(3L, accumulator.frameCount)
    }

    @Test
    fun `the window restarts after closing and reports again per second`() {
        val accumulator = FrameStatsAccumulator()
        accumulator.record(0L)
        var firstWindowFps: Float? = null
        for (frame in 1..50) {
            firstWindowFps = accumulator.record(frame * 20_000_000L)
        }
        assertEquals(50f, firstWindowFps!!, 0.001f)
        assertNull(accumulator.record(51 * 20_000_000L))
        var secondWindowFps: Float? = null
        for (frame in 52..100) {
            secondWindowFps = accumulator.record(frame * 20_000_000L)
        }
        assertEquals(50f, secondWindowFps!!, 0.001f)
        assertEquals(100L, accumulator.frameCount)
        assertEquals(0L, accumulator.jankCount)
    }

    @Test
    fun `reset window drops in flight state but keeps cumulative counters`() {
        val accumulator = FrameStatsAccumulator()
        accumulator.record(0L)
        for (frame in 1..50) {
            accumulator.record(frame * 20_000_000L)
        }
        assertEquals(50L, accumulator.frameCount)

        accumulator.resetWindow()
        assertNull(accumulator.record(10_000_000_000L))
        assertEquals(50L, accumulator.frameCount)
        assertEquals(0L, accumulator.jankCount)

        var fps: Float? = null
        for (frame in 1..50) {
            fps = accumulator.record(10_000_000_000L + frame * 20_000_000L)
        }
        assertEquals(50f, fps!!, 0.001f)
        assertEquals(100L, accumulator.frameCount)
    }
}
