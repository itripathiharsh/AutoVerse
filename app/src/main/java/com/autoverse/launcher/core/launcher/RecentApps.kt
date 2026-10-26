package com.autoverse.launcher.core.launcher

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recentAppsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "autoverse_recent_apps",
)

/**
 * Bounded, most-recent-first history of applications launched through
 * AutoVerse. An ordinary launcher cannot observe other apps' foreground
 * usage without privileged access, so this records exactly what this
 * launcher started and claims nothing more. Stored locally only.
 */
class RecentAppsRepository(private val context: Context) {

    /** Ordered most-recent-first launch keys, deduplicated and bounded. */
    val recentKeys: Flow<List<String>> = context.recentAppsDataStore.data
        .map { preferences -> RecentAppsCodec.decode(preferences[KEY_RECENT_APPS]) }

    /** Moves [key] to the front of the history (or adds it), bounded to [MAX_RECENT_APPS]. */
    suspend fun record(key: String) {
        context.recentAppsDataStore.edit { preferences ->
            preferences[KEY_RECENT_APPS] = RecentAppsCodec.record(preferences[KEY_RECENT_APPS], key)
        }
    }

    /** Forgets AutoVerse's launch history. */
    suspend fun clear() {
        context.recentAppsDataStore.edit { it.remove(KEY_RECENT_APPS) }
    }

    companion object {
        private val KEY_RECENT_APPS = stringPreferencesKey("recent_apps")
    }
}

/**
 * Pure codec for the persisted recent-launch history: comma-joined app keys,
 * most recent first. Recording moves an existing key to the front, drops
 * blanks/duplicates, and truncates to [MAX_RECENT_APPS]; decoding is
 * self-healing like the favorites codec.
 */
object RecentAppsCodec {

    fun decode(raw: String?): List<String> =
        raw.orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_RECENT_APPS)

    fun encode(keys: List<String>): String = keys.take(MAX_RECENT_APPS).joinToString(",")

    fun record(raw: String?, launchedKey: String): String {
        val current = decode(raw).filter { it != launchedKey }
        return encode((listOf(launchedKey) + current).take(MAX_RECENT_APPS))
    }
}

const val MAX_RECENT_APPS = 12
