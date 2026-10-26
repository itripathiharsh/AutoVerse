package com.autoverse.launcher.core.media

/**
 * One entry of the active player's queue, exactly as the session reported it.
 * [queueId] is the session's own item identifier (used for skip-to-item);
 * labels stay null when the player does not name the entry — nothing is
 * invented.
 */
data class MediaQueueEntry(
    val queueId: Long,
    val title: String?,
    val subtitle: String? = null,
)

/**
 * Bound copy of a session queue for dashboard state: reading a session can
 * expose tens of thousands of items, so the state keeps at most
 * [MAX_QUEUE_ENTRIES] rows (session order preserved) plus the real total,
 * and the UI discloses truncation instead of pretending to show everything.
 */
data class MediaQueueUi(
    val entries: List<MediaQueueEntry>,
    val total: Int,
    val activeQueueId: Long?,
) {
    /** Index of the playing entry within [entries]; -1 when not in the window. */
    val activeIndex: Int
        get() = if (activeQueueId == null) {
            -1
        } else {
            entries.indexOfFirst { it.queueId == activeQueueId }
        }

    val truncated: Boolean
        get() = total > entries.size
}

/** Upper bound on queue rows copied into dashboard state. */
const val MAX_QUEUE_ENTRIES = 200

/**
 * Builds the bounded queue view from the session's own list. Null/blank
 * labels are dropped through [cleanText]; duplicate queue ids collapse to
 * their first occurrence so skip-to-item stays unambiguous.
 */
fun mediaQueueUiOf(
    rawEntries: List<MediaQueueEntry>,
    totalReported: Int?,
    activeQueueId: Long?,
): MediaQueueUi {
    val cleaned = ArrayList<MediaQueueEntry>(rawEntries.size.coerceAtMost(MAX_QUEUE_ENTRIES))
    val seen = HashSet<Long>()
    for (entry in rawEntries) {
        if (cleaned.size >= MAX_QUEUE_ENTRIES) break
        if (!seen.add(entry.queueId)) continue
        cleaned += entry.copy(
            title = cleanText(entry.title),
            subtitle = cleanText(entry.subtitle),
        )
    }
    return MediaQueueUi(
        entries = cleaned,
        total = totalReported?.coerceAtLeast(cleaned.size) ?: cleaned.size,
        activeQueueId = activeQueueId,
    )
}

/** Row label for a queue entry; falls back to a factual positional name. */
fun mediaQueueEntryLabel(entry: MediaQueueEntry, position: Int): String =
    entry.title ?: "Track ${position + 1}"
