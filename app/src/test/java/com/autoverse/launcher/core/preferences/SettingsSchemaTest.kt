package com.autoverse.launcher.core.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 10 — settings schema versioning. Verifies migrations run in order,
 * stamp once, preserve existing values, and never touch a file written by a
 * newer app version. Decoding is version-independent, so these tests also
 * pin the "malformed/unknown file still reads as defaults" contract.
 */
class SettingsSchemaTest {

    private val themeKey = stringPreferencesKey("theme_id")
    private val clockKey = booleanPreferencesKey("clock_24h")

    @Test
    fun `current schema is version 1`() {
        assertEquals(1, CURRENT_SETTINGS_SCHEMA)
    }

    @Test
    fun `file without a stamp is treated as schema 1 and left unstamped`() {
        val preferences = mutablePreferencesOf(themeKey to "APEX")

        assertFalse(migratePreferences(preferences))

        assertEquals("APEX", preferences[themeKey])
        assertFalse(preferences.contains(KEY_SETTINGS_SCHEMA))
    }

    @Test
    fun `file already on the current schema is left alone`() {
        val preferences = mutablePreferencesOf(
            KEY_SETTINGS_SCHEMA to CURRENT_SETTINGS_SCHEMA,
        )

        assertFalse(migratePreferences(preferences))

        assertEquals(CURRENT_SETTINGS_SCHEMA, preferences[KEY_SETTINGS_SCHEMA])
    }

    @Test
    fun `unstamped file runs migrations above schema 1`() {
        val preferences = mutablePreferencesOf()
        var ran = false
        val migrations: Map<Int, (MutablePreferences) -> Unit> = mapOf(
            2 to { ran = true },
        )

        assertTrue(migratePreferences(preferences, migrations, targetVersion = 2))

        assertTrue(ran)
        assertEquals(2, preferences[KEY_SETTINGS_SCHEMA])
    }

    @Test
    fun `pending migrations run in ascending order and stamp the target`() {
        val order = mutableListOf<Int>()
        val smoothnessKey = booleanPreferencesKey("prefer_smoothness")
        val migrations: Map<Int, (MutablePreferences) -> Unit> = mapOf(
            2 to { prefs ->
                order += 2
                prefs[themeKey] = "OBSIDIAN"
            },
            3 to { prefs ->
                order += 3
                prefs[smoothnessKey] = false
            },
        )
        val preferences = mutablePreferencesOf(KEY_SETTINGS_SCHEMA to 1)

        val changed = migratePreferences(preferences, migrations, targetVersion = 3)

        assertTrue(changed)
        assertEquals(listOf(2, 3), order)
        assertEquals(3, preferences[KEY_SETTINGS_SCHEMA])
        assertEquals("OBSIDIAN", preferences[themeKey])
        assertEquals(false, preferences[smoothnessKey])
    }

    @Test
    fun `migration preserves existing user values`() {
        val preferences = mutablePreferencesOf(KEY_SETTINGS_SCHEMA to 1, clockKey to true)
        val migrations: Map<Int, (MutablePreferences) -> Unit> = mapOf(
            2 to { prefs -> prefs[clockKey] = prefs[clockKey] ?: true },
        )

        migratePreferences(preferences, migrations, targetVersion = 2)

        assertEquals(true, preferences[clockKey])
    }

    @Test
    fun `file from a newer schema is not stamped down`() {
        val preferences = mutablePreferencesOf(KEY_SETTINGS_SCHEMA to 7)

        assertFalse(migratePreferences(preferences, targetVersion = 2))

        assertEquals(7, preferences[KEY_SETTINGS_SCHEMA])
    }

    @Test
    fun `schema 0 file reaches the target`() {
        val preferences = mutablePreferencesOf(KEY_SETTINGS_SCHEMA to 0)

        assertTrue(migratePreferences(preferences))

        assertEquals(CURRENT_SETTINGS_SCHEMA, preferences[KEY_SETTINGS_SCHEMA])
    }
}
