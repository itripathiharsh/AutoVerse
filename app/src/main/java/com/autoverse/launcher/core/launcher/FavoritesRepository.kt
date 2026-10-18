package com.autoverse.launcher.core.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "autoverse_favorites",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Favorites persist as one ordered, newline-separated string of app keys.
 * Local-only by design (PRODUCT_OWNER_DECISIONS privacy constraints); a
 * separate DataStore file keeps launcher data isolated from general settings.
 * An unreadable file degrades to an empty list without rewriting storage,
 * and the codec self-heals any decoded value on the next toggle/prune.
 */
class FavoritesRepository(private val context: Context) {

    val favorites: Flow<List<String>> = context.favoritesDataStore.data
        .catch { failure ->
            if (failure is IOException) emit(emptyPreferences()) else throw failure
        }
        .map { preferences ->
            FavoritesCodec.decode(preferences[KEY_FAVORITES])
        }

    suspend fun toggle(appKey: String) {
        context.favoritesDataStore.edit { preferences ->
            val current = FavoritesCodec.decode(preferences[KEY_FAVORITES])
            val updated = if (appKey in current) current - appKey else current + appKey
            preferences[KEY_FAVORITES] = FavoritesCodec.encode(updated)
        }
    }

    suspend fun remove(appKey: String) {
        context.favoritesDataStore.edit { preferences ->
            val current = FavoritesCodec.decode(preferences[KEY_FAVORITES])
            if (appKey in current) {
                preferences[KEY_FAVORITES] = FavoritesCodec.encode(current - appKey)
            }
        }
    }

    /**
     * Phase 15 — replaces the stored order wholesale; used by the drawer's
     * edit flow to persist a draft in one write. Encoding normalizes blanks
     * and duplicates, so a save is always clean.
     */
    suspend fun setFavorites(order: List<String>) {
        context.favoritesDataStore.edit { preferences ->
            preferences[KEY_FAVORITES] = FavoritesCodec.encode(order)
        }
    }

    /** Drops favorites whose app is gone; writes only when something changed. */
    suspend fun pruneStale(installedKeys: Set<String>) {
        context.favoritesDataStore.edit { preferences ->
            val current = FavoritesCodec.decode(preferences[KEY_FAVORITES])
            val pruned = current.filter { it in installedKeys }
            if (pruned.size != current.size) {
                preferences[KEY_FAVORITES] = FavoritesCodec.encode(pruned)
            }
        }
    }

    private companion object {
        val KEY_FAVORITES = stringPreferencesKey("favorite_apps")
    }
}
