package com.autoverse.launcher.core.diagnostics

import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.media.MediaControls
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.PlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 17 — pins the component tiles onto the same states the rest of the
 * app renders, so the diagnostics screen can never disagree with them.
 */
class ComponentHealthTest {

    private fun activeSession(appLabel: String?) = MediaUiState.Active(
        packageName = "com.sample.player",
        appLabel = appLabel,
        title = null,
        artist = null,
        album = null,
        artworkUri = null,
        embeddedArtwork = null,
        status = PlaybackStatus.PLAYING,
        controls = MediaControls.NONE,
        progress = null,
    )

    // --- Navigation handoff --------------------------------------------------

    @Test
    fun `navigation handoff is unavailable without a launch target`() {
        val health = navigationHandoffHealth(
            providerLabel = "System default",
            preferredMissing = false,
            hasLaunchTarget = false,
        )
        assertEquals(ComponentStatus.UNAVAILABLE, health.status)
    }

    @Test
    fun `navigation handoff explains a missing preferred app`() {
        val health = navigationHandoffHealth(
            providerLabel = "System default",
            preferredMissing = true,
            hasLaunchTarget = true,
        )
        assertEquals(ComponentStatus.READY, health.status)
        assertEquals(
            "The preferred app isn't installed; System default is used instead.",
            health.detail,
        )
    }

    @Test
    fun `navigation handoff names the provider in use`() {
        val health = navigationHandoffHealth(
            providerLabel = "System default",
            preferredMissing = false,
            hasLaunchTarget = true,
        )
        assertEquals("System default is used for hand-offs.", health.detail)
    }

    // --- Media card ----------------------------------------------------------

    @Test
    fun `media card needs notification access even when a session exists`() {
        val health = mediaSessionHealth(
            notificationAccessGranted = false,
            state = activeSession(appLabel = "Sample Player"),
        )
        assertEquals(ComponentStatus.PERMISSION_REQUIRED, health.status)
    }

    @Test
    fun `media card mirrors access-required, connecting and error states`() {
        assertEquals(
            ComponentStatus.PERMISSION_REQUIRED,
            mediaSessionHealth(notificationAccessGranted = true, state = MediaUiState.AccessRequired).status,
        )
        assertEquals(
            ComponentStatus.LOADING,
            mediaSessionHealth(notificationAccessGranted = true, state = MediaUiState.Connecting).status,
        )
        val error = mediaSessionHealth(
            notificationAccessGranted = true,
            state = MediaUiState.Error("session read failed"),
        )
        assertEquals(ComponentStatus.ERROR, error.status)
        assertEquals("session read failed", error.detail)
    }

    @Test
    fun `media card reports ready states with factual details`() {
        val idle = mediaSessionHealth(notificationAccessGranted = true, state = MediaUiState.NoSession)
        assertEquals(ComponentStatus.READY, idle.status)
        assertEquals("Access granted; no player is active right now.", idle.detail)

        val active = mediaSessionHealth(
            notificationAccessGranted = true,
            state = activeSession(appLabel = "Sample Player"),
        )
        assertEquals(ComponentStatus.READY, active.status)
        assertEquals("Connected to Sample Player.", active.detail)

        val unknownLabel = mediaSessionHealth(
            notificationAccessGranted = true,
            state = activeSession(appLabel = null),
        )
        assertEquals("Connected to a media player.", unknownLabel.detail)
    }

    // --- App drawer ----------------------------------------------------------

    @Test
    fun `app drawer mirrors loading, ready and error states`() {
        assertEquals(
            ComponentStatus.LOADING,
            appDrawerHealth(AppListState.Loading).status,
        )

        val ready = appDrawerHealth(AppListState.Ready(emptyList()))
        assertEquals(ComponentStatus.READY, ready.status)
        assertEquals("0 apps available.", ready.detail)

        val error = appDrawerHealth(AppListState.Error("scan failed"))
        assertEquals(ComponentStatus.ERROR, error.status)
        assertEquals("scan failed", error.detail)
    }

    // --- Settings storage ----------------------------------------------------

    @Test
    fun `settings storage distinguishes unchecked, readable and unreadable`() {
        assertEquals(
            ComponentStatus.NOT_CHECKED,
            settingsStoreHealth(readable = null).status,
        )
        assertEquals(
            ComponentStatus.READY,
            settingsStoreHealth(readable = true).status,
        )
        assertEquals(
            ComponentStatus.ERROR,
            settingsStoreHealth(readable = false).status,
        )
    }

    // --- Frame sampling ------------------------------------------------------

    @Test
    fun `frame sampling is ready only while tracking`() {
        assertEquals(ComponentStatus.READY, frameTrackingHealth(tracking = true).status)
        assertEquals(ComponentStatus.DISABLED, frameTrackingHealth(tracking = false).status)
    }
}
