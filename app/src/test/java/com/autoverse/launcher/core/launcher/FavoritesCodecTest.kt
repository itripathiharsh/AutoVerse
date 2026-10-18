package com.autoverse.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4 — persistence format for favorites. Verifies the stored string
 * survives round trips, keeps order, and self-heals from corrupt input.
 */
class FavoritesCodecTest {

    private val keys = listOf(
        "com.alpha.music/com.alpha.music.PlayerActivity",
        "com.bravo.maps/com.bravo.maps.MainActivity",
    )

    @Test
    fun `encode joins with newlines and preserves order`() {
        assertEquals(
            "com.alpha.music/com.alpha.music.PlayerActivity\n" +
                "com.bravo.maps/com.bravo.maps.MainActivity",
            FavoritesCodec.encode(keys),
        )
    }

    @Test
    fun `encode drops blanks, duplicates, and surrounding whitespace`() {
        val first = keys[0]
        val messy = listOf("  $first", first, "", "   ", "com.c/com.c.MainActivity")
        assertEquals(
            "$first\ncom.c/com.c.MainActivity",
            FavoritesCodec.encode(messy),
        )
    }

    @Test
    fun `decode round trips an encoded list`() {
        assertEquals(keys, FavoritesCodec.decode(FavoritesCodec.encode(keys)))
    }

    @Test
    fun `decode of null or blank storage returns empty list`() {
        assertTrue(FavoritesCodec.decode(null).isEmpty())
        assertTrue(FavoritesCodec.decode("").isEmpty())
        assertTrue(FavoritesCodec.decode("\n\n").isEmpty())
    }

    @Test
    fun `decode heals stray separators and windows line endings`() {
        val stored = "\r\n${keys[0]}\n\n${keys[1]}\n \n"
        assertEquals(keys, FavoritesCodec.decode(stored))
    }

    @Test
    fun `decode drops duplicate entries`() {
        val stored = "${keys[0]}\n${keys[0]}\n${keys[1]}"
        assertEquals(keys, FavoritesCodec.decode(stored))
    }
}
