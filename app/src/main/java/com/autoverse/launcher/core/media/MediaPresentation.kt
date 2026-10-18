package com.autoverse.launcher.core.media

/** Presentation mode of the media card; drives which layout it shows. */
enum class MediaPresentationMode { ACCESS_REQUIRED, NO_SESSION, CONNECTING, ACTIVE, ERROR }

/**
 * Everything the media card needs to render, derived from [MediaUiState].
 * Copy is factual: no invented track names, no fake states.
 */
data class MediaPresentation(
    val mode: MediaPresentationMode,
    val headline: String,
    val subline: String? = null,
    val message: String? = null,
    val footer: String? = null,
    val status: PlaybackStatus? = null,
    val controls: MediaControls = MediaControls.NONE,
    val progress: MediaProgress? = null,
)

fun mediaPresentation(state: MediaUiState): MediaPresentation = when (state) {
    MediaUiState.AccessRequired -> MediaPresentation(
        mode = MediaPresentationMode.ACCESS_REQUIRED,
        headline = "Media access is off",
        message = "Allow media access in system settings to see and control " +
            "playback from other apps. AutoVerse never reads your notifications.",
        footer = "Turn on AutoVerse in the app list, then come back.",
    )

    MediaUiState.NoSession -> MediaPresentation(
        mode = MediaPresentationMode.NO_SESSION,
        headline = "No media playing",
        message = "Start playback in any app and it will appear here.",
    )

    MediaUiState.Connecting -> MediaPresentation(
        mode = MediaPresentationMode.CONNECTING,
        headline = "Looking for media…",
    )

    is MediaUiState.Active -> {
        val artist = cleanText(state.artist)
        val album = cleanText(state.album)
        MediaPresentation(
            mode = MediaPresentationMode.ACTIVE,
            headline = cleanText(state.title)
                ?: cleanText(state.appLabel)
                ?: sessionDisplayName(state.packageName),
            subline = listOfNotNull(artist, album).joinToString(" · ").ifBlank { null },
            status = state.status,
            controls = state.controls,
            progress = state.progress,
        )
    }

    is MediaUiState.Error -> MediaPresentation(
        mode = MediaPresentationMode.ERROR,
        headline = "Media unavailable",
        message = state.message,
    )
}

/** Trims metadata and treats blank or literal `"<unknown>"` values as absent. */
fun cleanText(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed.equals("<unknown>", ignoreCase = true)) return null
    return trimmed
}

/** Fallback display name for a session whose app label is unavailable. */
fun sessionDisplayName(packageName: String): String {
    val segment = packageName.substringAfterLast('.')
    if (segment.isBlank()) return packageName
    return segment
        .split('_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
        .ifBlank { packageName }
}

/**
 * Extrapolates the position the user should see right now, without polling:
 * a playing session at a positive speed advances from the last reported
 * position by the elapsed wall-clock time, clamped to [0, durationMs].
 * Returns null when the duration is unknown, so no progress is invented.
 */
fun mediaProgressOf(
    positionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    playing: Boolean,
    lastPositionUpdateTimeMs: Long,
    nowElapsedRealtimeMs: Long,
): MediaProgress? {
    if (durationMs <= 0L) return null
    var position = positionMs.coerceAtLeast(0L)
    if (playing && playbackSpeed > 0f && lastPositionUpdateTimeMs > 0L) {
        val elapsedMs = (nowElapsedRealtimeMs - lastPositionUpdateTimeMs).coerceAtLeast(0L)
        position += (elapsedMs * playbackSpeed).toLong()
    }
    return MediaProgress(
        positionMs = position.coerceIn(0L, durationMs),
        durationMs = durationMs,
    )
}
