package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 18 — recents rules (newest first, deduplicated by canonical
 * destination text, size-capped) and their persistence codec. Recents are
 * recorded only after a confirmed handoff elsewhere; these tests pin the
 * pure state transitions.
 */
class RecentDestinationTest {

    private fun recent(text: String) = RecentDestination(NavigationDestination.Query(text))

    @Test
    fun `recording into an empty list adds the destination`() {
        val result = recordRecentDestination(emptyList(), NavigationDestination.Query("Vienna"))
        assertEquals(listOf(recent("Vienna")), result)
    }

    @Test
    fun `new destinations go to the front`() {
        val result = recordRecentDestination(
            listOf(recent("A")),
            NavigationDestination.Query("B"),
        )
        assertEquals(listOf(recent("B"), recent("A")), result)
    }

    @Test
    fun `recording an existing destination moves it to the front`() {
        val result = recordRecentDestination(
            listOf(recent("A"), recent("B"), recent("C")),
            NavigationDestination.Query("C"),
        )
        assertEquals(listOf(recent("C"), recent("A"), recent("B")), result)
    }

    @Test
    fun `deduplication uses canonical destination text`() {
        val result = recordRecentDestination(
            listOf(RecentDestination(NavigationDestination.Coordinates(48.0, 16.0))),
            NavigationDestination.Coordinates(48.0, 16.0),
        )
        assertEquals(1, result.size)
    }

    @Test
    fun `size cap drops the oldest entries`() {
        val result = recordRecentDestination(
            listOf(recent("A"), recent("B")),
            NavigationDestination.Query("C"),
            limit = 2,
        )
        assertEquals(listOf(recent("C"), recent("A")), result)
    }

    @Test
    fun `default cap is the documented constant`() {
        assertEquals(8, MAX_RECENT_DESTINATIONS)
    }

    @Test
    fun `plain recents round-trip through the codec`() {
        val recents = listOf(recent("Vienna"), recent("Graz Airport"))
        assertEquals(recents, RecentDestinationCodec.decode(RecentDestinationCodec.encode(recents)))
    }

    @Test
    fun `coordinates round-trip through the codec`() {
        val recents = listOf(
            RecentDestination(NavigationDestination.Coordinates(48.2082, 16.3738)),
        )
        assertEquals(recents, RecentDestinationCodec.decode(RecentDestinationCodec.encode(recents)))
    }

    @Test
    fun `special characters round-trip through the codec`() {
        val recents = listOf(recent("Cafe\tDeluxe\nRoute 5"))
        assertEquals(recents, RecentDestinationCodec.decode(RecentDestinationCodec.encode(recents)))
    }

    @Test
    fun `empty and null decode to an empty list`() {
        assertTrue(RecentDestinationCodec.decode("").isEmpty())
        assertTrue(RecentDestinationCodec.decode(null).isEmpty())
        assertEquals("", RecentDestinationCodec.encode(emptyList()))
    }

    @Test
    fun `malformed lines are skipped`() {
        val tooLong = "x".repeat(MAX_DESTINATION_LENGTH + 1)
        val decoded = RecentDestinationCodec.decode("$tooLong\n\nVienna")
        assertEquals(listOf(recent("Vienna")), decoded)
    }

    @Test
    fun `carriage returns are tolerated`() {
        val decoded = RecentDestinationCodec.decode("Vienna\r\nGraz")
        assertEquals(listOf(recent("Vienna"), recent("Graz")), decoded)
    }
}
