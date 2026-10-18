package com.autoverse.launcher.core.media

import android.media.session.PlaybackState

/** Normalized playback state mapped from [PlaybackState] constants. */
enum class PlaybackStatus { PLAYING, PAUSED, STOPPED, BUFFERING, ERROR, UNKNOWN }

fun playbackStatusOf(playbackStateCode: Int): PlaybackStatus = when (playbackStateCode) {
    PlaybackState.STATE_PLAYING -> PlaybackStatus.PLAYING
    PlaybackState.STATE_PAUSED -> PlaybackStatus.PAUSED
    PlaybackState.STATE_STOPPED -> PlaybackStatus.STOPPED
    PlaybackState.STATE_BUFFERING -> PlaybackStatus.BUFFERING
    PlaybackState.STATE_ERROR -> PlaybackStatus.ERROR
    else -> PlaybackStatus.UNKNOWN
}

/**
 * True while audio is expected to be moving forward (playing or buffering
 * toward playback). Drives the play/pause toggle icon, the pause gate and
 * progress extrapolation.
 */
val PlaybackStatus.isPlaying: Boolean
    get() = this == PlaybackStatus.PLAYING || this == PlaybackStatus.BUFFERING

/** Factual status caption; null when the state carries no useful meaning. */
fun playbackStatusLabel(status: PlaybackStatus): String? = when (status) {
    PlaybackStatus.PLAYING -> "Playing"
    PlaybackStatus.PAUSED -> "Paused"
    PlaybackStatus.BUFFERING -> "Buffering"
    PlaybackStatus.STOPPED -> "Stopped"
    PlaybackStatus.ERROR -> "Playback error"
    PlaybackStatus.UNKNOWN -> null
}
