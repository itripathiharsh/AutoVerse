package com.autoverse.launcher.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 5 — presentation derivation. Copy stays factual in every mode, and
 * ACTIVE metadata falls back cleanly when track fields are missing. The
 * position extrapolation never invents progress for unknown durations.
 */
class MediaPresentationTest {

    private fun active(
        packageName: String = "com.example.music",
        appLabel: String? = "Sample Music",
        title: String? = "Sample Track",
        artist: String? = "Sample Artist",
        album: String? = "Sample Album",
        status: PlaybackStatus = PlaybackStatus.PLAYING,
        controls: MediaControls = MediaControls.NONE,
        progress: MediaProgress? = null,
    ) = MediaUiState.Active(
        packageName = packageName,
        appLabel = appLabel,
        title = title,
        artist = artist,
        album = album,
        artworkUri = null,
        embeddedArtwork = null,
        status = status,
        controls = controls,
        progress = progress,
    )

    @Test
    fun `access required carries headline message and footer`() {
        val presentation = mediaPresentation(MediaUiState.AccessRequired)
        assertEquals(MediaPresentationMode.ACCESS_REQUIRED, presentation.mode)
        assertEquals("Media access is off", presentation.headline)
        assertNotNull(presentation.message)
        assertNotNull(presentation.footer)
    }

    @Test
    fun `no session matches the placeholder copy`() {
        val presentation = mediaPresentation(MediaUiState.NoSession)
        assertEquals(MediaPresentationMode.NO_SESSION, presentation.mode)
        assertEquals("No media playing", presentation.headline)
        assertNull(presentation.footer)
    }

    @Test
    fun `connecting offers no resolution yet`() {
        val presentation = mediaPresentation(MediaUiState.Connecting)
        assertEquals(MediaPresentationMode.CONNECTING, presentation.mode)
        assertEquals("Looking for media\u2026", presentation.headline)
        assertNull(presentation.message)
        assertNull(presentation.footer)
    }

    @Test
    fun `error passes the factual message through`() {
        val presentation = mediaPresentation(MediaUiState.Error("Session read failed"))
        assertEquals(MediaPresentationMode.ERROR, presentation.mode)
        assertEquals("Media unavailable", presentation.headline)
        assertEquals("Session read failed", presentation.message)
    }

    @Test
    fun `headline prefers title then app label then package segment`() {
        assertEquals("Sample Track", mediaPresentation(active()).headline)
        assertEquals("Sample Music", mediaPresentation(active(title = null)).headline)
        assertEquals(
            "Music",
            mediaPresentation(active(title = null, appLabel = null)).headline,
        )
    }

    @Test
    fun `blank and unknown titles are treated as absent`() {
        assertEquals("Sample Music", mediaPresentation(active(title = "   ")).headline)
        assertEquals("Sample Music", mediaPresentation(active(title = "<unknown>")).headline)
    }

    @Test
    fun `subline joins artist and album and omits missing parts`() {
        assertEquals("Sample Artist \u00b7 Sample Album", mediaPresentation(active()).subline)
        assertEquals("Sample Artist", mediaPresentation(active(album = null)).subline)
        assertEquals("Sample Album", mediaPresentation(active(artist = null)).subline)
        assertNull(mediaPresentation(active(artist = null, album = null)).subline)
    }

    @Test
    fun `active carries status controls and progress`() {
        val controls = mediaControlsOf(
            setOf(MediaAction.PLAY, MediaAction.NEXT),
            PlaybackStatus.PAUSED,
        )
        val progress = MediaProgress(positionMs = 10_000L, durationMs = 40_000L)
        val presentation = mediaPresentation(
            active(
                status = PlaybackStatus.PAUSED,
                controls = controls,
                progress = progress,
            ),
        )
        assertEquals(PlaybackStatus.PAUSED, presentation.status)
        assertEquals(controls, presentation.controls)
        assertEquals(progress, presentation.progress)
    }

    @Test
    fun `cleanText trims blanks and unknown markers`() {
        assertEquals("Title", cleanText("  Title  "))
        assertNull(cleanText(null))
        assertNull(cleanText("   "))
        assertNull(cleanText("<unknown>"))
        assertNull(cleanText("<UNKNOWN>"))
    }

    @Test
    fun `session display name humanizes the package segment`() {
        assertEquals("Music Player", sessionDisplayName("com.example.music_player"))
        assertEquals("Music", sessionDisplayName("com.example.music"))
        assertEquals("com.example.", sessionDisplayName("com.example."))
    }

    @Test
    fun `progress is null while the duration is unknown`() {
        assertNull(mediaProgressOf(5_000L, 0L, 1f, playing = true, 1L, 2L))
        assertNull(mediaProgressOf(5_000L, -3L, 1f, playing = true, 1L, 2L))
    }

    @Test
    fun `progress extrapolates only while playing at a positive speed`() {
        val playing = mediaProgressOf(
            positionMs = 10_000L,
            durationMs = 60_000L,
            playbackSpeed = 1f,
            playing = true,
            lastPositionUpdateTimeMs = 1_000L,
            nowElapsedRealtimeMs = 3_500L,
        )
        assertEquals(12_500L, playing?.positionMs)

        val paused = mediaProgressOf(
            positionMs = 10_000L,
            durationMs = 60_000L,
            playbackSpeed = 1f,
            playing = false,
            lastPositionUpdateTimeMs = 1_000L,
            nowElapsedRealtimeMs = 3_500L,
        )
        assertEquals(10_000L, paused?.positionMs)
    }

    @Test
    fun `progress is clamped to the track bounds`() {
        val beyond = mediaProgressOf(59_000L, 60_000L, 2f, playing = true, 1_000L, 6_000L)
        assertEquals(60_000L, beyond?.positionMs)

        val negative = mediaProgressOf(-500L, 60_000L, 1f, playing = false, 0L, 100L)
        assertEquals(0L, negative?.positionMs)
    }
}
