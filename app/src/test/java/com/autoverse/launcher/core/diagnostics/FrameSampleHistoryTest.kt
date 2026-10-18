package com.autoverse.launcher.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 17 — pins the diagnostics sparkline history: oldest-first order,
 * bounded length, honest clearing, and the default limit.
 */
class FrameSampleHistoryTest {

    @Test
    fun `samples append oldest first and preserve values`() {
        val history = FrameSampleHistory()
        history.add(59.5f, 1_000L)
        history.add(61.0f, 2_000L)

        val samples = history.samples.value
        assertEquals(2, samples.size)
        assertEquals(59.5f, samples[0].fps, 0.0001f)
        assertEquals(1_000L, samples[0].atMillis)
        assertEquals(61f, samples[1].fps, 0.0001f)
        assertEquals(2_000L, samples[1].atMillis)
    }

    @Test
    fun `limit keeps only the newest samples`() {
        val history = FrameSampleHistory(limit = 3)
        (1..5).forEach { history.add(it.toFloat(), it * 1_000L) }

        val samples = history.samples.value
        assertEquals(3, samples.size)
        assertEquals(listOf(3f, 4f, 5f), samples.map { it.fps })
    }

    @Test
    fun `clear empties the history`() {
        val history = FrameSampleHistory()
        history.add(60f, 1_000L)
        history.clear()
        assertTrue(history.samples.value.isEmpty())
    }

    @Test
    fun `default limit is pinned`() {
        assertEquals(60, FrameSampleHistory.DEFAULT_LIMIT)
    }
}
