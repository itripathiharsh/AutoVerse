package com.autoverse.launcher.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 5 — artwork downsampling math, extended in Phase 9 for the
 * profile-driven targets (256 px Balanced/Enhanced, 128 px Constrained). The
 * sample size must be the smallest power of two that keeps the longest edge
 * at or above the target, so artwork never decodes below its budget and never
 * at full size.
 */
class MediaArtworkSamplingTest {

    @Test
    fun `images within the target decode at full size`() {
        assertEquals(1, sampleSizeFor(width = 100, height = 50, targetMaxPx = 256))
        assertEquals(1, sampleSizeFor(width = 257, height = 257, targetMaxPx = 256))
    }

    @Test
    fun `exact doublings pick the smallest sample that still covers the target`() {
        assertEquals(2, sampleSizeFor(width = 512, height = 512, targetMaxPx = 256))
        assertEquals(4, sampleSizeFor(width = 1024, height = 256, targetMaxPx = 256))
        assertEquals(8, sampleSizeFor(width = 3840, height = 2160, targetMaxPx = 256))
    }

    @Test
    fun `the constrained target samples more aggressively`() {
        assertEquals(2, sampleSizeFor(width = 384, height = 384, targetMaxPx = 128))
        assertEquals(4, sampleSizeFor(width = 512, height = 512, targetMaxPx = 128))
        assertEquals(8, sampleSizeFor(width = 1024, height = 1024, targetMaxPx = 128))
        assertEquals(16, sampleSizeFor(width = 3072, height = 3072, targetMaxPx = 128))
    }

    @Test
    fun `uneven dimensions sample by the longest edge`() {
        assertEquals(2, sampleSizeFor(width = 1000, height = 1, targetMaxPx = 256))
        assertEquals(4, sampleSizeFor(width = 1000, height = 1, targetMaxPx = 128))
    }

    @Test
    fun `degenerate inputs fall back to no sampling`() {
        assertEquals(1, sampleSizeFor(width = 0, height = 100, targetMaxPx = 256))
        assertEquals(1, sampleSizeFor(width = 100, height = -5, targetMaxPx = 256))
        assertEquals(1, sampleSizeFor(width = 100, height = 100, targetMaxPx = 0))
        assertEquals(1, sampleSizeFor(width = 100, height = 100, targetMaxPx = -128))
    }
}
