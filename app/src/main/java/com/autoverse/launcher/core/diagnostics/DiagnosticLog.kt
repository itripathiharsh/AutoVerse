package com.autoverse.launcher.core.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Severity of a recorded diagnostic event. */
enum class DiagnosticLevel { INFO, WARN, ERROR }

/**
 * One recorded diagnostic event. [id] is monotonic so list keys stay stable
 * when two events share a wall-clock millisecond; [timeMillis] is the
 * recording instant and is formatted for display by the UI layer.
 */
data class DiagnosticEvent(
    val id: Long,
    val timeMillis: Long,
    val level: DiagnosticLevel,
    val message: String,
)

/**
 * Phase 17 — bounded in-memory record of explicit, user-meaningful events.
 *
 * The app has no logging framework and this log is deliberately not one:
 * nothing is written to disk, nothing leaves the device, and only call sites
 * that pass a message record one. Newest events come first; the buffer is
 * capped at [capacity] and silently drops the oldest entries. Clearing the
 * buffer leaves it empty — the clear action itself is not recorded, so
 * "cleared" is a truthful empty state.
 *
 * Messages must never contain user content (titles, app labels) or
 * identifiers (package names, file paths); [record] additionally flattens
 * and truncates whatever it receives so a stray failure message can not leak
 * multi-line or oversized text into the UI.
 */
class DiagnosticLog(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val _entries = MutableStateFlow<List<DiagnosticEvent>>(emptyList())

    /** Recorded events, newest first, at most [capacity] entries. */
    val entries: StateFlow<List<DiagnosticEvent>> = _entries.asStateFlow()

    private var nextId = 0L

    /** Records one event; blank (after sanitizing) messages are dropped. */
    @Synchronized
    fun record(level: DiagnosticLevel, message: String) {
        val sanitized = sanitizeDiagnosticMessage(message)
        if (sanitized.isEmpty()) return
        val event = DiagnosticEvent(
            id = nextId++,
            timeMillis = clock(),
            level = level,
            message = sanitized,
        )
        _entries.value = (listOf(event) + _entries.value).take(capacity)
    }

    /** Empties the buffer; the clear itself is not recorded. */
    @Synchronized
    fun clear() {
        _entries.value = emptyList()
    }

    companion object {
        const val DEFAULT_CAPACITY = 120
        const val MAX_MESSAGE_LENGTH = 160
    }
}

/**
 * Flattens a message to a single line of plain text: line breaks and tabs
 * become spaces, control characters are dropped, whitespace runs collapse,
 * and anything past [DiagnosticLog.MAX_MESSAGE_LENGTH] is truncated with an
 * ellipsis. Pure and Android-free so the redaction rules are unit-tested.
 */
fun sanitizeDiagnosticMessage(raw: String): String {
    val flattened = buildString(raw.length) {
        for (character in raw) {
            when {
                character == '\n' || character == '\r' || character == '\t' -> append(' ')
                character.isISOControl() -> Unit
                else -> append(character)
            }
        }
    }
    val collapsed = flattened.trim().replace(WHITESPACE_RUN, " ")
    return if (collapsed.length <= DiagnosticLog.MAX_MESSAGE_LENGTH) {
        collapsed
    } else {
        collapsed.take(DiagnosticLog.MAX_MESSAGE_LENGTH - 1).trimEnd() + "…"
    }
}

private val WHITESPACE_RUN = Regex("\\s+")
