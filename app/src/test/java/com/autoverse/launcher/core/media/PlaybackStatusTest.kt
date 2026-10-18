package com.autoverse.launcher.core.media

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 — normalization of raw [PlaybackState] codes. The mapping must be
 * total: unknown platform codes stay UNKNOWN instead of inventing a state.
 */
class PlaybackStatusTest {

    @Test
    fun `known playback codes map to their status`() {
        assertEquals(PlaybackStatus.PLAYING, playbackStatusOf(PlaybackState.STATE_PLAYING))
        assertEquals(PlaybackStatus.PAUSED, playbackStatusOf(PlaybackState.STATE_PAUSED))
        assertEquals(PlaybackStatus.STOPPED, playbackStatusOf(PlaybackState.STATE_STOPPED))
        assertEquals(PlaybackStatus.BUFFERING, playbackStatusOf(PlaybackState.STATE_BUFFERING))
        assertEquals(PlaybackStatus.ERROR, playbackStatusOf(PlaybackState.STATE_ERROR))
    }

    @Test
    fun `unrepresentable codes map to UNKNOWN`() {
        assertEquals(PlaybackStatus.UNKNOWN, playbackStatusOf(PlaybackState.STATE_NONE))
        assertEquals(PlaybackStatus.UNKNOWN, playbackStatusOf(PlaybackState.STATE_CONNECTING))
        assertEquals(PlaybackStatus.UNKNOWN, playbackStatusOf(999))
    }

    @Test
    fun `isPlaying covers playing and buffering only`() {
        assertTrue(PlaybackStatus.PLAYING.isPlaying)
        assertTrue(PlaybackStatus.BUFFERING.isPlaying)
        assertFalse(PlaybackStatus.PAUSED.isPlaying)
        assertFalse(PlaybackStatus.STOPPED.isPlaying)
        assertFalse(PlaybackStatus.ERROR.isPlaying)
        assertFalse(PlaybackStatus.UNKNOWN.isPlaying)
    }

    @Test
    fun `status labels are factual and UNKNOWN has none`() {
        assertEquals("Playing", playbackStatusLabel(PlaybackStatus.PLAYING))
        assertEquals("Paused", playbackStatusLabel(PlaybackStatus.PAUSED))
        assertEquals("Buffering", playbackStatusLabel(PlaybackStatus.BUFFERING))
        assertEquals("Stopped", playbackStatusLabel(PlaybackStatus.STOPPED))
        assertEquals("Playback error", playbackStatusLabel(PlaybackStatus.ERROR))
        assertNull(playbackStatusLabel(PlaybackStatus.UNKNOWN))
    }
}
