package com.autoverse.launcher.core.media

import android.media.session.PlaybackState

/**
 * Transport capability a session can expose. PLAY means "can start playback
 * while paused"; PAUSE means "can pause while playing". ACTION_PLAY_PAUSE
 * grants both.
 */
enum class MediaAction { PLAY, PAUSE, NEXT, PREVIOUS }

/** Which transport controls the current session actually supports. */
data class MediaControls(
    val canPlay: Boolean,
    val canPause: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean,
) {
    val canTogglePlayPause: Boolean get() = canPlay || canPause

    companion object {
        val NONE = MediaControls(
            canPlay = false,
            canPause = false,
            canSkipNext = false,
            canSkipPrevious = false,
        )
    }
}

/**
 * Maps a raw [PlaybackState.getActions] mask onto supported [MediaAction]s.
 * ACTION_PLAY_PAUSE is expanded into both PLAY and PAUSE.
 */
fun mediaActionSet(actionsMask: Long): Set<MediaAction> {
    val actions = mutableSetOf<MediaAction>()
    val playPause = actionsMask and PlaybackState.ACTION_PLAY_PAUSE != 0L
    if (playPause || actionsMask and PlaybackState.ACTION_PLAY != 0L) {
        actions += MediaAction.PLAY
    }
    if (playPause || actionsMask and PlaybackState.ACTION_PAUSE != 0L) {
        actions += MediaAction.PAUSE
    }
    if (actionsMask and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) {
        actions += MediaAction.NEXT
    }
    if (actionsMask and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) {
        actions += MediaAction.PREVIOUS
    }
    return actions
}

/**
 * Narrows supported actions by the current playback status: play is only
 * offered while not playing, pause only while playing. Skipping stays
 * available regardless of status when supported.
 */
fun mediaControlsOf(actions: Set<MediaAction>, status: PlaybackStatus): MediaControls {
    val playing = status.isPlaying
    return MediaControls(
        canPlay = !playing && MediaAction.PLAY in actions,
        canPause = playing && MediaAction.PAUSE in actions,
        canSkipNext = MediaAction.NEXT in actions,
        canSkipPrevious = MediaAction.PREVIOUS in actions,
    )
}
