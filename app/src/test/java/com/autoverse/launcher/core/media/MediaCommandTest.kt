package com.autoverse.launcher.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 — command failure classification and toggle intent. Every failure
 * maps to a recovery path (retry, access flow, or unsupported notice), and
 * the toggle always matches what the user currently sees.
 */
class MediaCommandTest {

    @Test
    fun `failure types map to recovery outcomes`() {
        assertEquals(
            MediaCommandOutcome.ACCESS_LOST,
            classifyMediaCommandFailure(SecurityException()),
        )
        assertEquals(
            MediaCommandOutcome.NOT_SUPPORTED,
            classifyMediaCommandFailure(UnsupportedOperationException()),
        )
        assertEquals(
            MediaCommandOutcome.FAILED,
            classifyMediaCommandFailure(IllegalStateException("boom")),
        )
        assertEquals(
            MediaCommandOutcome.FAILED,
            classifyMediaCommandFailure(RuntimeException()),
        )
    }

    @Test
    fun `only SENT is silent`() {
        assertEquals("", mediaCommandFailureMessage(MediaCommandOutcome.SENT))
        listOf(
            MediaCommandOutcome.NOT_SUPPORTED,
            MediaCommandOutcome.ACCESS_LOST,
            MediaCommandOutcome.FAILED,
        ).forEach { outcome ->
            assertTrue(mediaCommandFailureMessage(outcome).isNotBlank())
        }
    }

    @Test
    fun `toggle pauses while playing or buffering and plays otherwise`() {
        assertEquals(MediaCommand.PAUSE, mediaToggleCommand(PlaybackStatus.PLAYING))
        assertEquals(MediaCommand.PAUSE, mediaToggleCommand(PlaybackStatus.BUFFERING))
        assertEquals(MediaCommand.PLAY, mediaToggleCommand(PlaybackStatus.PAUSED))
        assertEquals(MediaCommand.PLAY, mediaToggleCommand(PlaybackStatus.STOPPED))
        assertEquals(MediaCommand.PLAY, mediaToggleCommand(PlaybackStatus.UNKNOWN))
        assertEquals(MediaCommand.PLAY, mediaToggleCommand(null))
    }
}
