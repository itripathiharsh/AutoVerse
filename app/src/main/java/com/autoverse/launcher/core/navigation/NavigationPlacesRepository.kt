package com.autoverse.launcher.core.navigation

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

private val Context.navigationPlacesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "autoverse_navigation",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Phase 18 — saved places and recent destinations, local-only by design.
 * One dedicated DataStore file keeps navigation data isolated from launcher
 * data and general settings; an unreadable file degrades to empty lists
 * without rewriting storage. Recents are written only from a handoff that
 * actually opened, never from an attempt.
 */
class NavigationPlacesRepository(private val context: Context) {

    val savedPlaces: Flow<List<SavedPlace>> = context.navigationPlacesDataStore.data
        .catch { failure ->
            if (failure is IOException) emit(emptyPreferences()) else throw failure
        }
        .map { preferences ->
            SavedPlaceCodec.decode(preferences[KEY_SAVED_PLACES])
        }

    val recentDestinations: Flow<List<RecentDestination>> = context.navigationPlacesDataStore.data
        .catch { failure ->
            if (failure is IOException) emit(emptyPreferences()) else throw failure
        }
        .map { preferences ->
            RecentDestinationCodec.decode(preferences[KEY_RECENT_DESTINATIONS])
        }

    /** Saves [destination] under [label] (normalized); newest entries kept. */
    suspend fun savePlace(label: String, destination: NavigationDestination) {
        context.navigationPlacesDataStore.edit { preferences ->
            val current = SavedPlaceCodec.decode(preferences[KEY_SAVED_PLACES])
            preferences[KEY_SAVED_PLACES] = SavedPlaceCodec.encode(
                withSavedPlace(current, label, destination),
            )
        }
    }

    /** Removes every entry equal to [place]; writes only when it existed. */
    suspend fun removePlace(place: SavedPlace) {
        context.navigationPlacesDataStore.edit { preferences ->
            val current = SavedPlaceCodec.decode(preferences[KEY_SAVED_PLACES])
            val updated = withoutSavedPlace(current, place)
            if (updated.size != current.size) {
                preferences[KEY_SAVED_PLACES] = SavedPlaceCodec.encode(updated)
            }
        }
    }

    /** Records a confirmed handoff; writes only when the list actually changes. */
    suspend fun recordRecent(destination: NavigationDestination) {
        context.navigationPlacesDataStore.edit { preferences ->
            val current = RecentDestinationCodec.decode(preferences[KEY_RECENT_DESTINATIONS])
            val updated = recordRecentDestination(current, destination)
            if (updated != current) {
                preferences[KEY_RECENT_DESTINATIONS] = RecentDestinationCodec.encode(updated)
            }
        }
    }

    /** Forgets every recent destination; saved places are untouched. */
    suspend fun clearRecents() {
        context.navigationPlacesDataStore.edit { it.remove(KEY_RECENT_DESTINATIONS) }
    }

    private companion object {
        val KEY_SAVED_PLACES = stringPreferencesKey("saved_places")
        val KEY_RECENT_DESTINATIONS = stringPreferencesKey("recent_destinations")
    }
}
