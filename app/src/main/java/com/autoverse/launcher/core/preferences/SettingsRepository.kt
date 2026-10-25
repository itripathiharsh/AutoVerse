package com.autoverse.launcher.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.PerformanceProfile
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "autoverse_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class SettingsRepository(private val context: Context) {

    /**
     * Decoding is read-only and per-key safe: an unreadable file degrades to
     * defaults via the corruption handler or the [IOException] catch below,
     * and neither path rewrites the file, so a transient failure can not
     * overwrite choices that are merely unreadable right now.
     */
    val settings: Flow<AppSettings> = context.settingsDataStore.data
        .catch { failure ->
            if (failure is IOException) emit(emptyPreferences()) else throw failure
        }
        .map { preferences ->
            val defaults = AppSettings()
            AppSettings(
                themeId = themeIdFromStorage(preferences[KEY_THEME]),
                appearanceMode = appearanceModeFromStorage(preferences[KEY_APPEARANCE_MODE]),
                reducedMotion = preferences[KEY_REDUCED_MOTION] ?: defaults.reducedMotion,
                profile = profileFromStorage(preferences[KEY_PROFILE]),
                preferSmoothness = preferences[KEY_PREFER_SMOOTHNESS] ?: defaults.preferSmoothness,
                clock24h = preferences[KEY_CLOCK_24H] ?: defaults.clock24h,
                textScale = textScaleFromStorage(preferences[KEY_TEXT_SCALE]),
                navigationPackage = preferences[KEY_NAVIGATION_PACKAGE],
                premiumUnlocked = preferences[KEY_PREMIUM_UNLOCKED] ?: defaults.premiumUnlocked,
                onboardingComplete = preferences[KEY_ONBOARDING_COMPLETE] ?: defaults.onboardingComplete,
                offlineVoiceEnabled = preferences[KEY_OFFLINE_VOICE] ?: defaults.offlineVoiceEnabled,
                themeSoundsEnabled = preferences[KEY_THEME_SOUNDS] ?: defaults.themeSoundsEnabled,
            )
        }

    suspend fun setPremiumUnlocked(unlocked: Boolean) {
        context.settingsDataStore.edit { it[KEY_PREMIUM_UNLOCKED] = unlocked }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.settingsDataStore.edit { it[KEY_ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setOfflineVoiceEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_OFFLINE_VOICE] = enabled }
    }

    suspend fun setThemeSoundsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_THEME_SOUNDS] = enabled }
    }

    suspend fun setTheme(themeId: AutoVerseThemeId) {
        context.settingsDataStore.edit { it[KEY_THEME] = themeId.name }
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        context.settingsDataStore.edit { it[KEY_APPEARANCE_MODE] = mode.name }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_REDUCED_MOTION] = enabled }
    }

    suspend fun setProfile(profile: PerformanceProfile) {
        context.settingsDataStore.edit { it[KEY_PROFILE] = profile.name }
    }

    suspend fun setPreferSmoothness(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_PREFER_SMOOTHNESS] = enabled }
    }

    suspend fun setClock24h(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_CLOCK_24H] = enabled }
    }

    /** Clamps on write as well, so hand-built callers can not store junk. */
    suspend fun setTextScale(scale: Float) {
        context.settingsDataStore.edit { it[KEY_TEXT_SCALE] = textScaleFromStorage(scale) }
    }

    /** Null restores the unattended system-default handoff. */
    suspend fun setNavigationPackage(packageName: String?) {
        context.settingsDataStore.edit { preferences ->
            if (packageName == null) {
                preferences.remove(KEY_NAVIGATION_PACKAGE)
            } else {
                preferences[KEY_NAVIGATION_PACKAGE] = packageName
            }
        }
    }

    /**
     * Restores every preference to its factory default. Favorites live in a
     * separate DataStore and are user content, so they are intentionally
     * kept (FR-SET-003 applies to settings, not to launcher data).
     */
    suspend fun resetToDefaults() {
        context.settingsDataStore.edit { it.clear() }
    }

    /**
     * Applies pending schema migrations once at startup. Checks the stamp
     * first so the file is only rewritten when an upgrade is actually due;
     * a failed read or migration leaves the file untouched because decoding
     * never depends on the version stamp. Returns true when the store could
     * be read (and, if due, migrated), false when the backing file was
     * unreadable — diagnostics surface that as a degraded store health.
     */
    suspend fun migrateIfNeeded(): Boolean {
        val current = try {
            context.settingsDataStore.data.first()
        } catch (_: IOException) {
            return false
        }
        val storedVersion = current[KEY_SETTINGS_SCHEMA] ?: 1
        if (storedVersion >= CURRENT_SETTINGS_SCHEMA) return true
        context.settingsDataStore.edit { preferences -> migratePreferences(preferences) }
        return true
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_id")
        val KEY_APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val KEY_PROFILE = stringPreferencesKey("profile_id")
        val KEY_PREFER_SMOOTHNESS = booleanPreferencesKey("prefer_smoothness")
        val KEY_CLOCK_24H = booleanPreferencesKey("clock_24h")
        val KEY_TEXT_SCALE = floatPreferencesKey("text_scale")
        val KEY_NAVIGATION_PACKAGE = stringPreferencesKey("navigation_package")
        val KEY_PREMIUM_UNLOCKED = booleanPreferencesKey("premium_unlocked")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val KEY_OFFLINE_VOICE = booleanPreferencesKey("offline_voice")
        val KEY_THEME_SOUNDS = booleanPreferencesKey("theme_sounds")
    }
}
