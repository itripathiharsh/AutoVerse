package com.autoverse.launcher.feature.settings

/**
 * Phase 16 — every settings category, in navigation order. Each category maps
 * to exactly one pane of the settings screen and one entry in the category
 * navigation. [label] is the user-visible name used by navigation and search.
 *
 * Pure Kotlin so the catalog, the search, and their tests stay JVM-runnable
 * without an activity; icons are attached in the UI layer only.
 */
enum class SettingsCategory(val label: String) {
    APPEARANCE("Appearance"),
    NAVIGATION("Navigation"),
    MEDIA_AUDIO("Media & Audio"),
    APPS_SHORTCUTS("Apps & Shortcuts"),
    PERFORMANCE("Performance"),
    NOTIFICATIONS("Notifications"),
    ABOUT("About"),
}

/**
 * One searchable settings row: the category it lives in plus the words a user
 * would plausibly type to find it. [subtitle] doubles as the one-line summary
 * shown under the title in search results.
 */
data class SettingsEntry(
    val category: SettingsCategory,
    val title: String,
    val subtitle: String,
)

/**
 * The searchable index of everything the settings screen exposes. Kept in
 * sync with the real controls by hand; SettingsCatalogTest pins the shape
 * (every category present, no duplicate titles, every entry searchable).
 */
val SETTINGS_CATALOG: List<SettingsEntry> = listOf(
    SettingsEntry(
        category = SettingsCategory.APPEARANCE,
        title = "Theme",
        subtitle = "Cobalt, Obsidian, Neon Pulse, Atelier, or Apex palette",
    ),
    SettingsEntry(
        category = SettingsCategory.APPEARANCE,
        title = "Appearance mode",
        subtitle = "System, Day, or Night",
    ),
    SettingsEntry(
        category = SettingsCategory.APPEARANCE,
        title = "Text size",
        subtitle = "Scales interface text without changing layout density",
    ),
    SettingsEntry(
        category = SettingsCategory.APPEARANCE,
        title = "Adaptive layout",
        subtitle = "Automatic — detected from window size and font scale",
    ),
    SettingsEntry(
        category = SettingsCategory.APPEARANCE,
        title = "24-hour clock",
        subtitle = "Time format on the dashboard and top bar",
    ),
    SettingsEntry(
        category = SettingsCategory.APPEARANCE,
        title = "Reduced motion",
        subtitle = "Calmer transitions, no decorative animation",
    ),
    SettingsEntry(
        category = SettingsCategory.NAVIGATION,
        title = "Navigation app",
        subtitle = "System default, Google Maps, Waze, or a discovered app",
    ),
    SettingsEntry(
        category = SettingsCategory.NAVIGATION,
        title = "Embedded map",
        subtitle = "Experimental dashboard map preview",
    ),
    SettingsEntry(
        category = SettingsCategory.MEDIA_AUDIO,
        title = "Media access",
        subtitle = "Notification-listener access that powers the media card",
    ),
    SettingsEntry(
        category = SettingsCategory.MEDIA_AUDIO,
        title = "Media card",
        subtitle = "Playback controls and artwork on the dashboard",
    ),
    SettingsEntry(
        category = SettingsCategory.APPS_SHORTCUTS,
        title = "Favorite apps",
        subtitle = "Shown on the dashboard and in the app drawer",
    ),
    SettingsEntry(
        category = SettingsCategory.APPS_SHORTCUTS,
        title = "App drawer",
        subtitle = "Where favorites are added and removed",
    ),
    SettingsEntry(
        category = SettingsCategory.PERFORMANCE,
        title = "Performance profile",
        subtitle = "Constrained, Balanced, or Enhanced",
    ),
    SettingsEntry(
        category = SettingsCategory.PERFORMANCE,
        title = "Prefer smoothness",
        subtitle = "Bias toward frame rate over visual depth",
    ),
    SettingsEntry(
        category = SettingsCategory.PERFORMANCE,
        title = "Effective workload",
        subtitle = "Which profile is actually running right now",
    ),
    SettingsEntry(
        category = SettingsCategory.PERFORMANCE,
        title = "Diagnostics",
        subtitle = "Frame timing and performance audit",
    ),
    SettingsEntry(
        category = SettingsCategory.NOTIFICATIONS,
        title = "Notification access",
        subtitle = "Grant or revoke AutoVerse's notification listener",
    ),
    SettingsEntry(
        category = SettingsCategory.NOTIFICATIONS,
        title = "Media sessions",
        subtitle = "AutoVerse reads only the active media session",
    ),
    SettingsEntry(
        category = SettingsCategory.ABOUT,
        title = "Version",
        subtitle = "Version name, build number, and release stage",
    ),
    SettingsEntry(
        category = SettingsCategory.ABOUT,
        title = "Licenses",
        subtitle = "Open-source attribution for shipped components",
    ),
    SettingsEntry(
        category = SettingsCategory.ABOUT,
        title = "Privacy",
        subtitle = "No telemetry, analytics, or advertising",
    ),
    SettingsEntry(
        category = SettingsCategory.ABOUT,
        title = "Reset settings",
        subtitle = "Restore defaults; favorite apps are preserved",
    ),
)

/**
 * Case-insensitive search across title, subtitle, and category label. A blank
 * query returns no results — callers show the normal category content instead
 * of an empty result list.
 */
fun searchSettings(
    query: String,
    catalog: List<SettingsEntry> = SETTINGS_CATALOG,
): List<SettingsEntry> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    val needle = trimmed.lowercase()
    return catalog.filter { entry ->
        entry.title.lowercase().contains(needle) ||
            entry.subtitle.lowercase().contains(needle) ||
            entry.category.label.lowercase().contains(needle)
    }
}
