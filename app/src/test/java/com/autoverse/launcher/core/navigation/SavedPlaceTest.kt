package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 18 — saved-place state rules: label normalization, deduplication by
 * canonical destination text, in-place label updates, and the size cap. All
 * pure functions; nothing here touches storage or invents destinations.
 */
class SavedPlaceTest {

    @Test
    fun `label is trimmed when present`() {
        val label = normalizedSavedPlaceLabel("  Home  ", NavigationDestination.Query("X"))
        assertEquals("Home", label)
    }

    @Test
    fun `blank label falls back to destination text`() {
        val label = normalizedSavedPlaceLabel("   ", NavigationDestination.Query("Vienna"))
        assertEquals("Vienna", label)
    }

    @Test
    fun `label is clamped to the maximum length`() {
        val long = "a".repeat(MAX_SAVED_PLACE_LABEL_LENGTH + 25)
        val label = normalizedSavedPlaceLabel(long, NavigationDestination.Query("X"))
        assertEquals(MAX_SAVED_PLACE_LABEL_LENGTH, label.length)
    }

    @Test
    fun `blank label with coordinates falls back to coordinate text`() {
        val label = normalizedSavedPlaceLabel(
            "",
            NavigationDestination.Coordinates(48.2082, 16.3738),
        )
        assertEquals("48.2082, 16.3738", label)
    }

    @Test
    fun `saving into an empty list appends the place`() {
        val result = withSavedPlace(emptyList(), "Home", NavigationDestination.Query("Vienna"))
        assertEquals(listOf(SavedPlace("Home", NavigationDestination.Query("Vienna"))), result)
    }

    @Test
    fun `saving the same destination again updates the label in place`() {
        val current = listOf(SavedPlace("Old", NavigationDestination.Query("Vienna")))
        val result = withSavedPlace(current, "New", NavigationDestination.Query("Vienna"))
        assertEquals(listOf(SavedPlace("New", NavigationDestination.Query("Vienna"))), result)
    }

    @Test
    fun `deduplication uses canonical destination text`() {
        val current = listOf(SavedPlace("Home", NavigationDestination.Coordinates(48.0, 16.0)))
        val result = withSavedPlace(
            current,
            "Home again",
            NavigationDestination.Coordinates(48.0, 16.0),
        )
        assertEquals(1, result.size)
        assertEquals("Home again", result.first().label)
    }

    @Test
    fun `size cap drops the oldest entries`() {
        val a = SavedPlace("A", NavigationDestination.Query("A"))
        val b = SavedPlace("B", NavigationDestination.Query("B"))
        val result = withSavedPlace(listOf(a, b), "C", NavigationDestination.Query("C"), limit = 2)
        assertEquals(listOf(b, SavedPlace("C", NavigationDestination.Query("C"))), result)
    }

    @Test
    fun `default cap is the documented constant`() {
        assertEquals(12, MAX_SAVED_PLACES)
    }

    @Test
    fun `remove drops the matching entry and keeps the rest`() {
        val a = SavedPlace("A", NavigationDestination.Query("A"))
        val b = SavedPlace("B", NavigationDestination.Query("B"))
        assertEquals(listOf(b), withoutSavedPlace(listOf(a, b), a))
    }

    @Test
    fun `remove of an unknown entry keeps the list untouched`() {
        val a = SavedPlace("A", NavigationDestination.Query("A"))
        val missing = SavedPlace("Z", NavigationDestination.Query("Z"))
        assertEquals(listOf(a), withoutSavedPlace(listOf(a), missing))
    }

    @Test
    fun `saved place data class compares by label and destination`() {
        val one = SavedPlace("Home", NavigationDestination.Query("Vienna"))
        val two = SavedPlace("Home", NavigationDestination.Query("Vienna"))
        assertEquals(one, two)
        assertTrue(one.hashCode() == two.hashCode())
    }
}
