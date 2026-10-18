package com.autoverse.launcher.core.model

/**
 * Phase 15 — drawer filter categories.
 *
 * [ALL] is the filter sentinel ("no category selected") and is never assigned
 * to an app by the classifier; every concrete app lands in exactly one of the
 * six real categories, with [OTHERS] as the guaranteed fallback. [label] is
 * the user-facing chip text and the single source of truth for the wording in
 * the drawer (tests pin it, so a rename is a deliberate, visible change).
 */
enum class AppCategory(val label: String) {
    ALL("All Apps"),
    NAVIGATION("Navigation"),
    MUSIC_AUDIO("Music & Audio"),
    COMMUNICATION("Communication"),
    VIDEO("Video"),
    TOOLS("Tools"),
    OTHERS("Others"),
}
