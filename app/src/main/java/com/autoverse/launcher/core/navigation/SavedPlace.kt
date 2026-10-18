package com.autoverse.launcher.core.navigation

/** Longest accepted saved-place label; longer input is clamped, not rejected. */
const val MAX_SAVED_PLACE_LABEL_LENGTH = 60

/** How many saved places the launcher keeps (oldest dropped on save). */
const val MAX_SAVED_PLACES = 12

/**
 * Phase 18 — a user-saved navigation destination with a display label.
 * Saved locally on the device only; AutoVerse never uploads, resolves or
 * rewrites the destination.
 */
data class SavedPlace(
    val label: String,
    val destination: NavigationDestination,
)

/**
 * Label normalization: trimmed, clamped to [MAX_SAVED_PLACE_LABEL_LENGTH],
 * falling back to the destination text so every saved place stays
 * identifiable even when the user saved without typing a label.
 */
fun normalizedSavedPlaceLabel(label: String, destination: NavigationDestination): String {
    val trimmed = label.trim().take(MAX_SAVED_PLACE_LABEL_LENGTH)
    return trimmed.ifBlank { navigationDestinationText(destination) }
}

/**
 * New saved-places state after saving [destination] under [label]: an
 * existing entry with the same destination text is replaced (label updates
 * in place), the new entry appends, and at most [limit] entries remain.
 */
fun withSavedPlace(
    current: List<SavedPlace>,
    label: String,
    destination: NavigationDestination,
    limit: Int = MAX_SAVED_PLACES,
): List<SavedPlace> {
    val text = navigationDestinationText(destination)
    val entry = SavedPlace(normalizedSavedPlaceLabel(label, destination), destination)
    val deduped = current.filterNot { navigationDestinationText(it.destination) == text }
    return (deduped + entry).takeLast(limit)
}

/** New saved-places state after removing every entry equal to [place]. */
fun withoutSavedPlace(current: List<SavedPlace>, place: SavedPlace): List<SavedPlace> =
    current.filterNot { it == place }
