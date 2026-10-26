package com.autoverse.launcher.core.media

/** Transport command sent to the selected media session. */
enum class MediaCommand { PLAY, PAUSE, NEXT, PREVIOUS }

/** Result of a command attempt, mapped to user-facing recovery behavior. */
enum class MediaCommandOutcome { SENT, NOT_SUPPORTED, ACCESS_LOST, FAILED }

fun classifyMediaCommandFailure(failure: Throwable): MediaCommandOutcome = when (failure) {
    is SecurityException -> MediaCommandOutcome.ACCESS_LOST
    is UnsupportedOperationException -> MediaCommandOutcome.NOT_SUPPORTED
    else -> MediaCommandOutcome.FAILED
}

fun mediaCommandFailureMessage(outcome: MediaCommandOutcome): String = when (outcome) {
    MediaCommandOutcome.SENT -> ""
    MediaCommandOutcome.NOT_SUPPORTED -> "That control isn't available for this player"
    MediaCommandOutcome.ACCESS_LOST -> "Media access was lost. Re-enable it in system settings."
    MediaCommandOutcome.FAILED -> "Couldn't reach the media app. Try again."
}

/** PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM (kept literal so JVM tests need no Android stubs). */
const val ACTION_SKIP_TO_QUEUE_ITEM_FLAG = 1L shl 12

/**
 * Whether the session advertises queue selection. A missing playback state is
 * "unknown", so the request is still sent and its real outcome reported; an
 * explicit action mask without the flag means the player won't honour it.
 */
fun queueSelectionSupported(actions: Long?): Boolean =
    actions == null || actions and ACTION_SKIP_TO_QUEUE_ITEM_FLAG != 0L

/**
 * The play/pause toggle must send the intent that matches what the user
 * sees: pause a session that is playing (or buffering), play otherwise.
 */
fun mediaToggleCommand(status: PlaybackStatus?): MediaCommand =
    if (status?.isPlaying == true) MediaCommand.PAUSE else MediaCommand.PLAY
