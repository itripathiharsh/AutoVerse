package com.autoverse.launcher.core.media

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 — transport capability gating. Controls are only offered when the
 * session's action mask supports them and the status makes them meaningful;
 * unsupported controls are disabled rather than hidden.
 */
class MediaActionTest {

    @Test
    fun `play-pause mask expands into both directions`() {
        assertEquals(
            setOf(MediaAction.PLAY, MediaAction.PAUSE),
            mediaActionSet(PlaybackState.ACTION_PLAY_PAUSE),
        )
    }

    @Test
    fun `each platform action maps to its transport action`() {
        assertEquals(setOf(MediaAction.PLAY), mediaActionSet(PlaybackState.ACTION_PLAY))
        assertEquals(setOf(MediaAction.PAUSE), mediaActionSet(PlaybackState.ACTION_PAUSE))
        assertEquals(setOf(MediaAction.NEXT), mediaActionSet(PlaybackState.ACTION_SKIP_TO_NEXT))
        assertEquals(
            setOf(MediaAction.PREVIOUS),
            mediaActionSet(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
        )
    }

    @Test
    fun `empty mask supports nothing and masks combine`() {
        assertEquals(emptySet<MediaAction>(), mediaActionSet(0L))
        assertEquals(
            setOf(MediaAction.PLAY, MediaAction.PAUSE, MediaAction.NEXT),
            mediaActionSet(PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT),
        )
    }

    @Test
    fun `play is gated to non-playing and pause to playing`() {
        val actions = setOf(MediaAction.PLAY, MediaAction.PAUSE)

        val whilePaused = mediaControlsOf(actions, PlaybackStatus.PAUSED)
        assertTrue(whilePaused.canPlay)
        assertFalse(whilePaused.canPause)

        val whilePlaying = mediaControlsOf(actions, PlaybackStatus.PLAYING)
        assertFalse(whilePlaying.canPlay)
        assertTrue(whilePlaying.canPause)

        val whileBuffering = mediaControlsOf(actions, PlaybackStatus.BUFFERING)
        assertTrue(whileBuffering.canPause)
    }

    @Test
    fun `skipping stays available regardless of status`() {
        val controls = mediaControlsOf(
            setOf(MediaAction.NEXT, MediaAction.PREVIOUS),
            PlaybackStatus.UNKNOWN,
        )
        assertTrue(controls.canSkipNext)
        assertTrue(controls.canSkipPrevious)
    }

    @Test
    fun `toggle requires at least one play direction`() {
        assertFalse(MediaControls.NONE.canTogglePlayPause)
        assertFalse(mediaControlsOf(setOf(MediaAction.NEXT), PlaybackStatus.PAUSED).canTogglePlayPause)
        assertTrue(mediaControlsOf(setOf(MediaAction.PLAY), PlaybackStatus.PAUSED).canTogglePlayPause)
        assertTrue(mediaControlsOf(setOf(MediaAction.PAUSE), PlaybackStatus.PLAYING).canTogglePlayPause)
    }
}
