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
    val reducedMotion: Boolean = false,
    val profile: PerformanceProfile = PerformanceProfile.BALANCED,
    /** Phase 9 switch: runs the dashboard with the Constrained workload without changing the profile. */
    val preferSmoothness: Boolean = false,
    val clock24h: Boolean = true,
    /**
     * Phase 16 — user text scale applied on top of the system font scale.
     * Values are clamped by the preferences layer to 0.85x..1.30x so the
     * layout-engine rules stay valid; 1.0f means "system size only".
     */
    val textScale: Float = 1.0f,
    val navigationPackage: String? = null,
    /** Premium entitlement (0.21.0). Local flag for now; Play Billing plugs into this. */
    val premiumUnlocked: Boolean = false,
    /** Tier 1: false until the first-run onboarding finishes. */
    val onboardingComplete: Boolean = false,
    /** Tier 2: opt-in offline wake-word voice ("Hey AutoVerse"). Download-on-first-use model. */
    val offlineVoiceEnabled: Boolean = false,
    /** Theme system Phase E: synthesized confirmation sounds; off by default. */
    val themeSoundsEnabled: Boolean = false,
)
