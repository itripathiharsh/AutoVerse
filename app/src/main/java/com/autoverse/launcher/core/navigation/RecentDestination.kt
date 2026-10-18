package com.autoverse.launcher.core.navigation

/** How many recent destinations the launcher keeps for quick recall. */
const val MAX_RECENT_DESTINATIONS = 8

/**
 * Phase 18 — one destination the user handed off to a navigation app,
 * newest first. Recorded only after a confirmed OPENED outcome: an attempted
 * or failed handoff is never described as history.
 */
data class RecentDestination(
    val destination: NavigationDestination,
)

/**
 * New recents state after handing [destination] off successfully: the
 * destination moves (or is added) to the front, duplicates identified by
 * canonical destination text are removed, and at most [limit] entries remain.
 */
fun recordRecentDestination(
    current: List<RecentDestination>,
    destination: NavigationDestination,
    limit: Int = MAX_RECENT_DESTINATIONS,
): List<RecentDestination> {
    val text = navigationDestinationText(destination)
    val updated = listOf(RecentDestination(destination)) +
        current.filterNot { navigationDestinationText(it.destination) == text }
    return updated.take(limit)
}
