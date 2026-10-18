package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 18 — saved-places persistence codec: one tab-separated line per place,
 * escaping for TAB/newline/backslash, CRLF tolerance, and skipping of
 * malformed lines so one bad entry never hides the rest.
 */
class SavedPlaceCodecTest {

    @Test
    fun `plain places round-trip`() {
        val places = listOf(
            SavedPlace("Home", NavigationDestination.Query("Vienna")),
            SavedPlace("Office", NavigationDestination.Coordinates(48.2082, 16.3738)),
        )
        assertEquals(places, SavedPlaceCodec.decode(SavedPlaceCodec.encode(places)))
    }

    @Test
    fun `empty and null decode to an empty list`() {
        assertTrue(SavedPlaceCodec.decode("").isEmpty())
        assertTrue(SavedPlaceCodec.decode(null).isEmpty())
        assertEquals("", SavedPlaceCodec.encode(emptyList()))
    }

    @Test
    fun `labels bearing tabs newlines and backslashes round-trip`() {
        val place = SavedPlace("A\tB\nC\\D", NavigationDestination.Query("Vienna"))
        val decoded = SavedPlaceCodec.decode(SavedPlaceCodec.encode(listOf(place)))
        assertEquals(listOf(place), decoded)
    }

    @Test
    fun `query destinations bearing special characters round-trip`() {
        val place = SavedPlace("Cafe", NavigationDestination.Query("Cafe\tDeluxe\nRoute 5"))
        val decoded = SavedPlaceCodec.decode(SavedPlaceCodec.encode(listOf(place)))
        assertEquals(listOf(place), decoded)
    }

    @Test
    fun `coordinate destinations round-trip`() {
        val place = SavedPlace("Home", NavigationDestination.Coordinates(48.2082, 16.3738))
        val decoded = SavedPlaceCodec.decode(SavedPlaceCodec.encode(listOf(place)))
        assertEquals(listOf(place), decoded)
    }

    @Test
    fun `lines without a separator are skipped`() {
        val decoded = SavedPlaceCodec.decode("no separator here\nGood\tVienna")
        assertEquals(listOf(SavedPlace("Good", NavigationDestination.Query("Vienna"))), decoded)
    }

    @Test
    fun `lines with a blank destination are skipped`() {
        val decoded = SavedPlaceCodec.decode("Label\t\nGood\tVienna")
        assertEquals(listOf(SavedPlace("Good", NavigationDestination.Query("Vienna"))), decoded)
    }

    @Test
    fun `lines with an unparsable destination are skipped`() {
        val tooLong = "x".repeat(MAX_DESTINATION_LENGTH + 1)
        val decoded = SavedPlaceCodec.decode("Label\t$tooLong\nGood\tVienna")
        assertEquals(listOf(SavedPlace("Good", NavigationDestination.Query("Vienna"))), decoded)
    }

    @Test
    fun `carriage returns are tolerated`() {
        val decoded = SavedPlaceCodec.decode("Home\tVienna\r\nOffice\tGraz")
        assertEquals(
            listOf(
                SavedPlace("Home", NavigationDestination.Query("Vienna")),
                SavedPlace("Office", NavigationDestination.Query("Graz")),
            ),
            decoded,
        )
    }

    @Test
    fun `blank stored labels fall back to destination text on decode`() {
        val decoded = SavedPlaceCodec.decode("\tVienna")
        assertEquals(listOf(SavedPlace("Vienna", NavigationDestination.Query("Vienna"))), decoded)
    }

    @Test
    fun `stored labels are clamped on decode`() {
        val long = "a".repeat(MAX_SAVED_PLACE_LABEL_LENGTH + 25)
        val decoded = SavedPlaceCodec.decode("$long\tVienna")
        assertEquals(MAX_SAVED_PLACE_LABEL_LENGTH, decoded.first().label.length)
    }

    @Test
    fun `escape and unescape are inverse for line storage characters`() {
        val raw = "tab\there\nnewline\\backslash\rcarriage"
        assertEquals(raw, unescapeCodecField(escapeCodecField(raw)))
    }

    @Test
    fun `unknown escapes keep the backslash literally`() {
        assertEquals("keep\\x this", unescapeCodecField("keep\\x this"))
    }
}
