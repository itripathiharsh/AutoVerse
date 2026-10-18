package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 18 — local-only destination suggestions: blank queries offer nothing,
 * saved places rank before recents, results deduplicate by canonical
 * destination text, an exact echo of the query is never offered, and the
 * list is capped. No geocoding, no invented entries.
 */
class NavigationSearchTest {

    private val savedPlaces = listOf(
        SavedPlace("Home", NavigationDestination.Query("Praterstern")),
        SavedPlace("Vienna Center", NavigationDestination.Query("Stephansplatz")),
    )

    private val recents = listOf(
        RecentDestination(NavigationDestination.Query("Vienna Airport")),
        RecentDestination(NavigationDestination.Query("Praterstern")),
    )

    @Test
    fun `blank queries offer nothing`() {
        assertTrue(navigationSuggestions("", savedPlaces, recents).isEmpty())
        assertTrue(navigationSuggestions("   ", savedPlaces, recents).isEmpty())
    }

    @Test
    fun `non-positive limits offer nothing`() {
        assertTrue(navigationSuggestions("vienna", savedPlaces, recents, limit = 0).isEmpty())
    }

    @Test
    fun `saved places match by label case-insensitively`() {
        val results = navigationSuggestions("hom", savedPlaces, recents)
        assertEquals(1, results.size)
        assertEquals("Home", results.first().label)
        assertEquals(NavigationSuggestionSource.SAVED_PLACE, results.first().source)
    }

    @Test
    fun `saved places match by destination text`() {
        val results = navigationSuggestions("prat", savedPlaces, recents)
        val saved = results.first { it.source == NavigationSuggestionSource.SAVED_PLACE }
        assertEquals("Home", saved.label)
        assertEquals(NavigationDestination.Query("Praterstern"), saved.destination)
    }

    @Test
    fun `recents match by destination text and are labelled with it`() {
        val results = navigationSuggestions("airport", savedPlaces, recents)
        assertEquals(1, results.size)
        assertEquals("Vienna Airport", results.first().label)
        assertEquals(NavigationSuggestionSource.RECENT, results.first().source)
    }

    @Test
    fun `saved places rank before recents`() {
        val results = navigationSuggestions("prater", savedPlaces, recents)
        assertEquals(1, results.size)
        assertEquals(NavigationSuggestionSource.SAVED_PLACE, results.first().source)
    }

    @Test
    fun `duplicates across saved and recent are collapsed`() {
        val shared = listOf(SavedPlace("Home", NavigationDestination.Query("Praterstern")))
        val results = navigationSuggestions("prater", shared, recents)
        assertEquals(1, results.size)
        assertEquals(NavigationSuggestionSource.SAVED_PLACE, results.first().source)
    }

    @Test
    fun `an exact echo of the query is never offered`() {
        val echoPlace = listOf(SavedPlace("Home", NavigationDestination.Query("Vienna")))
        assertTrue(navigationSuggestions("Vienna", echoPlace, emptyList()).isEmpty())
        assertTrue(navigationSuggestions("vienna", echoPlace, emptyList()).isEmpty())
    }

    @Test
    fun `results are capped at the limit`() {
        val many = (1..8).map { SavedPlace("Place $it", NavigationDestination.Query("Vienna $it")) }
        val results = navigationSuggestions("vienna", many, emptyList())
        assertEquals(MAX_NAVIGATION_SUGGESTIONS, results.size)
    }

    @Test
    fun `a query with no local match offers nothing`() {
        assertTrue(navigationSuggestions("nowhere", savedPlaces, recents).isEmpty())
    }
}
