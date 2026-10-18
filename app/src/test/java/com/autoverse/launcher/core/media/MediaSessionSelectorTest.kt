package com.autoverse.launcher.core.media

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 5 — session selection order. The dashboard features at most one
 * session; the choice prefers what the user most likely wants to control
 * and stays deterministic (ties keep the incoming order).
 */
class MediaSessionSelectorTest {

    private fun candidate(
        key: String,
        code: Int = PlaybackState.STATE_NONE,
        hasMetadata: Boolean = false,
    ) = MediaSessionCandidate(key = key, playbackStateCode = code, hasMetadata = hasMetadata)

    @Test
    fun `no candidates selects nothing`() {
        assertNull(selectMediaSession(emptyList()))
    }

    @Test
    fun `playing session wins over paused with metadata`() {
        val paused = candidate("paused", PlaybackState.STATE_PAUSED, hasMetadata = true)
        val playing = candidate("playing", PlaybackState.STATE_PLAYING)
        assertEquals("playing", selectMediaSession(listOf(paused, playing))?.key)
    }

    @Test
    fun `paused with metadata wins over paused without`() {
        val bare = candidate("bare", PlaybackState.STATE_PAUSED)
        val rich = candidate("rich", PlaybackState.STATE_PAUSED, hasMetadata = true)
        assertEquals("rich", selectMediaSession(listOf(bare, rich))?.key)
    }

    @Test
    fun `paused wins over stopped`() {
        val stopped = candidate("stopped", PlaybackState.STATE_STOPPED)
        val paused = candidate("paused", PlaybackState.STATE_PAUSED)
        assertEquals("paused", selectMediaSession(listOf(stopped, paused))?.key)
    }

    @Test
    fun `buffering with metadata is representable`() {
        val buffering = candidate("buffering", PlaybackState.STATE_BUFFERING, hasMetadata = true)
        assertEquals("buffering", selectMediaSession(listOf(buffering))?.key)
    }

    @Test
    fun `known state wins over idle session`() {
        val idle = candidate("idle", PlaybackState.STATE_NONE)
        val errored = candidate("errored", PlaybackState.STATE_ERROR)
        assertEquals("errored", selectMediaSession(listOf(idle, errored))?.key)
    }

    @Test
    fun `ties keep the incoming order`() {
        val first = candidate("first", PlaybackState.STATE_NONE)
        val second = candidate("second", PlaybackState.STATE_NONE)
        assertEquals("first", selectMediaSession(listOf(first, second))?.key)
    }
}
