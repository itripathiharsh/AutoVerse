package com.autoverse.launcher.core.model

enum class AutoVerseThemeId(val displayName: String) {
    COBALT("Cobalt"),
    OBSIDIAN("Obsidian"),
    NEON_PULSE("Neon Pulse"),
    ATELIER("Atelier"),
    APEX("Apex"),
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
    /** Phase 7 prototype switch; the embedded map stays off unless opted in. */
    val embeddedMapEnabled: Boolean = false,
)
