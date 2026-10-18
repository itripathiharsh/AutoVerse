package com.autoverse.launcher.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 5 — progress fraction. Only rendered when the duration is known,
 * and always clamped so a rogue position can never overflow the bar.
 */
class MediaProgressTest {

    @Test
    fun `fraction is position over duration`() {
        val progress = MediaProgress(positionMs = 30_000L, durationMs = 60_000L)
        assertEquals(0.5f, progress.fraction, 0.0001f)
    }

    @Test
    fun `fraction clamps to the unit range`() {
        assertEquals(1f, MediaProgress(90_000L, 60_000L).fraction, 0f)
        assertEquals(0f, MediaProgress(-10L, 60_000L).fraction, 0f)
    }

    @Test
    fun `unknown duration yields a zero fraction without a crash`() {
        assertEquals(0f, MediaProgress(10_000L, 0L).fraction, 0f)
        assertEquals(0f, MediaProgress(10_000L, -5L).fraction, 0f)
    }
}
