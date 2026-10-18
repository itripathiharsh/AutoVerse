package com.autoverse.launcher.core.design.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.media.MediaAction
import com.autoverse.launcher.core.media.MediaControls
import com.autoverse.launcher.core.media.MediaProgress
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.PlaybackStatus
import com.autoverse.launcher.core.media.mediaControlsOf
import com.autoverse.launcher.core.media.mediaPresentation
import com.autoverse.launcher.core.model.AutoVerseThemeId
import org.junit.Rule
import org.junit.Test

/**
 * Phase 5 — MediaCard rendering per presentation mode: factual copy, action
 * mapping per mode, transport gating (enabled vs disabled, never hidden),
 * progress visibility, and dense-mode trimming.
 */
class MediaCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun activeState(
        title: String? = "Sample Track",
        artist: String? = "Sample Artist",
        status: PlaybackStatus = PlaybackStatus.PLAYING,
        controls: MediaControls = mediaControlsOf(
            setOf(MediaAction.PLAY, MediaAction.PAUSE, MediaAction.NEXT, MediaAction.PREVIOUS),
            status,
        ),
        progress: MediaProgress? = null,
    ) = MediaUiState.Active(
        packageName = "com.example.music",
        appLabel = "Sample Music",
        title = title,
        artist = artist,
        album = null,
        artworkUri = null,
        embeddedArtwork = null,
        status = status,
        controls = controls,
        progress = progress,
    )

    private fun setCard(state: MediaUiState, dense: Boolean = false) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                MediaCard(presentation = mediaPresentation(state), dense = dense)
            }
        }
    }

    @Test
    fun accessRequiredShowsExplanationAndSettingsAction() {
        setCard(MediaUiState.AccessRequired)
        composeRule.onNodeWithText("Media access is off").assertIsDisplayed()
        composeRule.onNodeWithText("Open system settings").assertIsDisplayed()
    }

    @Test
    fun noSessionShowsBrowseAppsAction() {
        setCard(MediaUiState.NoSession)
        composeRule.onNodeWithText("No media playing").assertIsDisplayed()
        composeRule.onNodeWithText("Browse apps").assertIsDisplayed()
    }

    @Test
    fun connectingOffersNoResolutionAction() {
        setCard(MediaUiState.Connecting)
        composeRule.onNodeWithText("Looking for media\u2026").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertDoesNotExist()
        composeRule.onNodeWithText("Browse apps").assertDoesNotExist()
        composeRule.onNodeWithText("Open system settings").assertDoesNotExist()
    }

    @Test
    fun errorShowsFactualMessageAndRetry() {
        setCard(MediaUiState.Error("Couldn\u2019t read media sessions"))
        composeRule.onNodeWithText("Media unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Couldn\u2019t read media sessions").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun activeShowsTrackStatusAndPauseControl() {
        setCard(activeState())
        composeRule.onNodeWithText("Sample Track").assertIsDisplayed()
        composeRule.onNodeWithText("Sample Artist").assertIsDisplayed()
        composeRule.onNodeWithText("Playing").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun pausedShowsPlayControl() {
        setCard(activeState(status = PlaybackStatus.PAUSED))
        composeRule.onNodeWithText("Paused").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun unsupportedTransportControlsAreDisabledNotHidden() {
        setCard(
            activeState(
                status = PlaybackStatus.PAUSED,
                controls = mediaControlsOf(setOf(MediaAction.PLAY), PlaybackStatus.PAUSED),
            ),
        )
        composeRule.onNodeWithContentDescription("Previous").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Previous").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Next").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Next").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Play").assertIsEnabled()
    }

    @Test
    fun missingTitleFallsBackToAppLabel() {
        setCard(activeState(title = null))
        composeRule.onNodeWithText("Sample Music").assertIsDisplayed()
    }

    @Test
    fun progressBarShownWhenDurationIsKnown() {
        setCard(activeState(progress = MediaProgress(positionMs = 60_000L, durationMs = 120_000L)))
        composeRule.onNodeWithTag("mediaProgress").assertExists()
    }

    @Test
    fun progressBarAbsentWhenDurationIsUnknown() {
        setCard(activeState(progress = null))
        composeRule.onNodeWithTag("mediaProgress").assertDoesNotExist()
    }

    @Test
    fun denseModeKeepsActionButDropsMessage() {
        setCard(MediaUiState.AccessRequired, dense = true)
        composeRule.onNodeWithText("Media access is off").assertIsDisplayed()
        composeRule.onNodeWithText("Open system settings").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Allow media access in system settings to see and control " +
                "playback from other apps. AutoVerse never reads your notifications.",
        ).assertDoesNotExist()
    }
}
