package com.autoverse.launcher.core.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.PerformanceProfile

/** On-disk settings schema version; bump together with a new [PREFERENCE_MIGRATIONS] entry. */
const val CURRENT_SETTINGS_SCHEMA = 1

/** Key under which the schema version is stamped. */
internal val KEY_SETTINGS_SCHEMA = intPreferencesKey("schema_version")

/**
 * Ordered schema upgrades: entry `n` transforms a file stored at schema
 * `n - 1` into schema `n`. Schema 1 is the first released layout (Phases
 * 1-9); files with no version key are schema 1, and because every value is
 * decoded by name with a per-key safe default, reaching schema 1 needed no
 * data transformation. Future renames/reshapes register their upgrade here.
 */
val PREFERENCE_MIGRATIONS: Map<Int, (MutablePreferences) -> Unit> = emptyMap()

/**
 * Applies every migration pending for [preferences] and stamps
 * [CURRENT_SETTINGS_SCHEMA]. Returns true when the file was modified, so
 * callers can skip pointless writes. Files newer than [targetVersion] (an
 * app downgrade) are left untouched: read-side decoding ignores unknown
 * keys, so a newer file stays fully usable without being stamped down.
 * Read-side decoding never depends on the stamp, so a failed migration can
 * not corrupt readable settings.
 */
fun migratePreferences(
    preferences: MutablePreferences,
    migrations: Map<Int, (MutablePreferences) -> Unit> = PREFERENCE_MIGRATIONS,
    targetVersion: Int = CURRENT_SETTINGS_SCHEMA,
): Boolean {
    val storedVersion = preferences[KEY_SETTINGS_SCHEMA] ?: 1
    if (storedVersion >= targetVersion) return false
    for (version in (storedVersion + 1)..targetVersion) {
        migrations[version]?.invoke(preferences)
    }
    preferences[KEY_SETTINGS_SCHEMA] = targetVersion
    return true
}

/**
 * Maps the persisted theme id to the theme enum. Missing or unknown values
 * (renamed themes, hand-edited prefs) fall back to the app default so stored
 * settings can never crash a launch. Android-free for JVM tests.
 */
fun themeIdFromStorage(stored: String?): AutoVerseThemeId =
    stored
        ?.let { value -> AutoVerseThemeId.entries.firstOrNull { it.name == value } }
        ?: AppSettings().themeId

/**
 * Maps the persisted appearance mode to the enum. Missing or unknown values
 * fall back to the app default so a renamed or hand-edited value can never
 * crash a launch. Android-free for JVM tests.
 */
fun appearanceModeFromStorage(stored: String?): AppearanceMode =
    stored
        ?.let { value -> AppearanceMode.entries.firstOrNull { it.name == value } }
        ?: AppSettings().appearanceMode

/**
 * Maps the persisted performance profile to the enum. Missing or unknown
 * values fall back to the conservative app default (Balanced with restrained
 * effects), matching the decision that unknown devices start safe. Android-
 * free for JVM tests.
 */
fun profileFromStorage(stored: String?): PerformanceProfile =
    stored
        ?.let { value -> PerformanceProfile.entries.firstOrNull { it.name == value } }
        ?: AppSettings().profile

/** Stored value for an explicit "System default" navigation choice. */
const val NAVIGATION_SYSTEM_DEFAULT_MARKER = ""

/**
 * Maps the persisted navigation preference. An absent key means the user
 * never chose, so the app default (Google Maps) applies — earlier builds
 * returned null here, which silently turned every untouched install into a
 * package-less "System default" whose Open action could launch nothing. The
 * explicit system-default choice is stored as [NAVIGATION_SYSTEM_DEFAULT_MARKER].
 */
fun navigationPackageFromStorage(stored: String?, default: String?): String? = when {
    stored == null -> default
    stored.isBlank() -> null
    else -> stored
}

fun navigationPackageToStorage(packageName: String?): String =
    packageName?.takeIf { it.isNotBlank() } ?: NAVIGATION_SYSTEM_DEFAULT_MARKER

/** Phase 16 — lower bound of the user text scale (0.85x). */
const val TEXT_SCALE_MIN = 0.85f

/** Phase 16 — upper bound of the user text scale (1.30x). */
const val TEXT_SCALE_MAX = 1.30f

/**
 * Maps the persisted text scale to a safe value. Missing, non-finite, or
 * out-of-range values fall back to (or clamp into) the safe 0.85x..1.30x
 * range, so a corrupted preference can never produce an unreadable UI.
 * Android-free for JVM tests.
 */
fun textScaleFromStorage(stored: Float?): Float {
    if (stored == null || stored.isNaN()) return AppSettings().textScale
    return stored.coerceIn(TEXT_SCALE_MIN, TEXT_SCALE_MAX)
}

/** Formats a text scale as the whole percent shown in settings (1.0f -> 100). */
fun textScalePercent(scale: Float): Int = (scale * 100f + 0.5f).toInt()
