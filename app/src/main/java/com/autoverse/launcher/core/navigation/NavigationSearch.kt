package com.autoverse.launcher.core.navigation

/** Where a navigation suggestion came from. */
enum class NavigationSuggestionSource { SAVED_PLACE, RECENT }

/** One row in the destination suggestion list. */
data class NavigationSuggestion(
    val label: String,
    val destination: NavigationDestination,
    val source: NavigationSuggestionSource,
)

/** Most suggestion rows the destination field offers at once. */
const val MAX_NAVIGATION_SUGGESTIONS = 5

/**
 * Phase 18 — deterministic, local-only suggestions for [query]: saved places
 * first (matched by label or destination text), then recent destinations,
 * deduplicated by destination text, never echoing a destination the user has
 * already typed exactly. No geocoding, no network, no invented entries — an
 * empty result is honest and shown as-is.
 */
fun navigationSuggestions(
    query: String,
    savedPlaces: List<SavedPlace>,
    recentDestinations: List<RecentDestination>,
    limit: Int = MAX_NAVIGATION_SUGGESTIONS,
): List<NavigationSuggestion> {
    val trimmed = query.trim()
    if (trimmed.isEmpty() || limit <= 0) return emptyList()

    val results = mutableListOf<NavigationSuggestion>()
    val seenTexts = mutableSetOf<String>()

    fun offer(label: String, destination: NavigationDestination, source: NavigationSuggestionSource) {
        if (results.size >= limit) return
        val text = navigationDestinationText(destination)
        if (text.equals(trimmed, ignoreCase = true)) return
        if (!seenTexts.add(text)) return
        results += NavigationSuggestion(label = label, destination = destination, source = source)
    }

    for (place in savedPlaces) {
        val matches = place.label.contains(trimmed, ignoreCase = true) ||
            navigationDestinationText(place.destination).contains(trimmed, ignoreCase = true)
        if (matches) {
            offer(place.label, place.destination, NavigationSuggestionSource.SAVED_PLACE)
        }
    }
    for (recent in recentDestinations) {
        val text = navigationDestinationText(recent.destination)
        if (text.contains(trimmed, ignoreCase = true)) {
            offer(text, recent.destination, NavigationSuggestionSource.RECENT)
        }
    }
    return results
}
