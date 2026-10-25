package com.autoverse.launcher.core.media

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.Flow

/**
 * Read-only view of the system's active media sessions. Implementations
 * must emit [MediaUiState.AccessRequired] instead of throwing when the
 * notification-listener grant is missing or revoked.
 */
interface MediaSessionRepository {

    /** Emits the current media surface state and stays current while collected. */
    fun observe(): Flow<MediaUiState>

    /** Sends a transport command to the featured session. */
    suspend fun send(command: MediaCommand): MediaCommandOutcome

    /**
     * Seeks the featured session to [positionMs] from the start of the track.
     * Outcomes mirror [send]; NOT_SUPPORTED when the session rejects seeking.
     */
    suspend fun seekTo(positionMs: Long): MediaCommandOutcome

    /**
     * Loads artwork from a session-provided content URI decoded to at most
     * [targetMaxPx] on the longest edge (the active profile's budget);
     * null on any failure.
     */
    suspend fun loadArtwork(uri: String, targetMaxPx: Int): ImageBitmap?

    /** Drops cached decoded artwork; safe to call under memory pressure. */
    fun trimMemory()
}
