package com.autoverse.launcher.core.media

import android.media.session.PlaybackState

/**
 * Read-only snapshot of one active session, sufficient to choose which
 * session the dashboard should represent.
 */
data class MediaSessionCandidate(
    val key: String,
    val playbackStateCode: Int,
    val hasMetadata: Boolean,
)

/**
 * Picks the session to feature, in this order: a playing session; a
 * paused/buffering session that has metadata; any paused/buffering session;
 * any session with a known non-idle state; finally the first candidate.
 * Ties keep the incoming order.
 */
fun selectMediaSession(candidates: List<MediaSessionCandidate>): MediaSessionCandidate? {
    if (candidates.isEmpty()) return null
    candidates.firstOrNull { it.playbackStateCode == PlaybackState.STATE_PLAYING }?.let { return it }
    candidates.firstOrNull {
        it.hasMetadata && (
            it.playbackStateCode == PlaybackState.STATE_PAUSED ||
                it.playbackStateCode == PlaybackState.STATE_BUFFERING
            )
    }?.let { return it }
    candidates.firstOrNull {
        it.playbackStateCode == PlaybackState.STATE_PAUSED ||
            it.playbackStateCode == PlaybackState.STATE_BUFFERING
    }?.let { return it }
    candidates.firstOrNull { it.playbackStateCode != PlaybackState.STATE_NONE }?.let { return it }
    return candidates.first()
}
