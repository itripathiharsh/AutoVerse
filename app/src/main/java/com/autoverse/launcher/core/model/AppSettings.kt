package com.autoverse.launcher.core.model

enum class AutoVerseThemeId(val displayName: String, val premium: Boolean = false) {
    COBALT("Cobalt"),
    OBSIDIAN("Obsidian"),
    NEON_PULSE("Neon Pulse"),
    ATELIER("Atelier"),
    APEX("Apex"),
    SAKURA("Sakura", premium = true),
    NEON_TOKYO("Neon Tokyo", premium = true),
    SHONEN("Shonen", premium = true),
    GOTHAM("Gotham", premium = true),
    HOT_ROD("Hot Rod", premium = true),
    FORTRESS("Fortress", premium = true),
    WEB_RUSH("Web Rush", premium = true),
}

enum class PerformanceProfile(val displayName: String) {
    CONSTRAINED("Constrained"),
    BALANCED("Balanced"),
    ENHANCED("Enhanced"),
}

enum class AppearanceMode(val displayName: String) {
    SYSTEM("System"),
    DAY("Day"),
    NIGHT("Night"),
}

data class AppSettings(
    val themeId: AutoVerseThemeId = AutoVerseThemeId.COBALT,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val reducedMotion: Boolean = true,
    val profile: PerformanceProfile = PerformanceProfile.CONSTRAINED,
    /** Phase 9 switch: runs the dashboard with the Constrained workload without changing the profile. */
    val preferSmoothness: Boolean = true,
    val clock24h: Boolean = true,
    /**
     * Phase 16 — user text scale applied on top of the system font scale.
     * Values are clamped by the preferences layer to 0.85x..1.30x so the
     * layout-engine rules stay valid; 1.0f means "system size only".
     */
    val textScale: Float = 1.0f,
    /**
     * Google Maps is the default handoff target (official intents, no API
     * key). A stored preference overrides this; when Maps is not installed
     * the resolver falls back to the system geo chooser and the UI says so.
     */
    val navigationPackage: String? = "com.google.android.apps.maps",
    /** Apps page: the 30% Recently Used panel is collapsed and All Apps uses the width. */
    val appsRecentCollapsed: Boolean = false,
    /** Maps page: the 30% destinations panel is collapsed and the map region uses the width. */
    val mapsPlacesCollapsed: Boolean = false,
    /**
     * Premium themes entitlement. 1.0 ships as a paid app with every theme
     * included, so this defaults to true; a future free-with-upgrade model
     * would default it to false and grant it from Play Billing.
     */
    val premiumUnlocked: Boolean = true,
    /** Tier 1: false until the first-run onboarding finishes. */
    val onboardingComplete: Boolean = false,
    /** Theme system Phase E: synthesized confirmation sounds; off by default. */
    val themeSoundsEnabled: Boolean = false,
)
