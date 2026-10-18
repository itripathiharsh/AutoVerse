package com.autoverse.launcher.core.media

import androidx.compose.ui.graphics.ImageBitmap

/** Position within a track. Only represented when the duration is known. */
data class MediaProgress(
    val positionMs: Long,
    val durationMs: Long,
) {
    val fraction: Float
        get() = if (durationMs <= 0L) {
            0f
        } else {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
}

/**
 * Media surface state. Every variant is factual: metadata fields stay null
 * when the session does not provide them, so the UI never invents values.
 */
sealed interface MediaUiState {

    /** Notification-listener access is not granted; the user must enable it. */
    data object AccessRequired : MediaUiState

    /** Access granted but no app currently publishes a media session. */
    data object NoSession : MediaUiState

    /** Initial state while the first session snapshot is being read. */
    data object Connecting : MediaUiState

    /** A session is being observed. */
    data class Active(
        val packageName: String,
        val appLabel: String?,
        val title: String?,
        val artist: String?,
        val album: String?,
        val artworkUri: String?,
        val embeddedArtwork: ImageBitmap?,
        val status: PlaybackStatus,
        val controls: MediaControls,
        val progress: MediaProgress?,
    ) : MediaUiState

    /** Session reading failed; [message] is factual and retryable. */
    data class Error(val message: String) : MediaUiState
}
