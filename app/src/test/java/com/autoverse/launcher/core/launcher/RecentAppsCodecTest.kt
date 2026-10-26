package com.autoverse.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentAppsCodecTest {

    @Test
    fun `decoding tolerates blanks duplicates and whitespace`() {
        assertEquals(
            listOf("a", "b"),
            RecentAppsCodec.decode(" a ,,b,a"),
        )
    }

    @Test
    fun `null and blank decode to empty history`() {
        assertTrue(RecentAppsCodec.decode(null).isEmpty())
        assertTrue(RecentAppsCodec.decode("").isEmpty())
    }

    @Test
    fun `recording moves the app to the front`() {
        assertEquals(
            listOf("c", "a", "b"),
            RecentAppsCodec.decode(RecentAppsCodec.record("a,b,c", "c")),
        )
    }

    @Test
    fun `recording a new app prepends`() {
        assertEquals(listOf("x", "a", "b"), RecentAppsCodec.decode(RecentAppsCodec.record("a,b", "x")))
    }

    @Test
    fun `history is bounded to twelve entries`() {
        val full = (1..20).joinToString(",") { "app$it" }
        assertEquals(MAX_RECENT_APPS, RecentAppsCodec.decode(full).size)
        val recorded = RecentAppsCodec.record(full, "new")
        assertEquals(MAX_RECENT_APPS, RecentAppsCodec.decode(recorded).size)
        assertEquals("new", RecentAppsCodec.decode(recorded).first())
    }

    @Test
    fun `encode decode round trips`() {
        val keys = listOf("a", "b", "c")
        assertEquals(keys, RecentAppsCodec.decode(RecentAppsCodec.encode(keys)))
    }
}
