package com.autoverse.launcher.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaQueueTest {

    @Test
    fun `queue is copied bounded and in session order`() {
        val raw = (1L..300L).map { MediaQueueEntry(queueId = it, title = "T$it") }
        val ui = mediaQueueUiOf(raw, totalReported = 300, activeQueueId = null)
        assertEquals(MAX_QUEUE_ENTRIES, ui.entries.size)
        assertEquals(300, ui.total)
        assertTrue(ui.truncated)
        assertEquals(1L, ui.entries.first().queueId)
    }

    @Test
    fun `duplicate queue ids collapse to first occurrence`() {
        val ui = mediaQueueUiOf(
            rawEntries = listOf(
                MediaQueueEntry(7, "First"),
                MediaQueueEntry(7, "Second"),
                MediaQueueEntry(8, "Third"),
            ),
            totalReported = null,
            activeQueueId = null,
        )
        assertEquals(listOf("First", "Third"), ui.entries.map { it.title })
        assertEquals(2, ui.total)
        assertFalse(ui.truncated)
    }

    @Test
    fun `blank and unknown labels are dropped`() {
        val ui = mediaQueueUiOf(
            rawEntries = listOf(
                MediaQueueEntry(1, "  ", "x"),
                MediaQueueEntry(2, "<unknown>", "y"),
                MediaQueueEntry(3, "Real", ""),
            ),
            totalReported = null,
            activeQueueId = null,
        )
        assertNull(ui.entries[0].title)
        assertNull(ui.entries[1].title)
        assertEquals("Real", ui.entries[2].title)
        assertNull(ui.entries[2].subtitle)
    }

    @Test
    fun `active index tracks the session id and is minus one when absent`() {
        val entries = listOf(MediaQueueEntry(1, "A"), MediaQueueEntry(2, "B"))
        assertEquals(1, mediaQueueUiOf(entries, null, activeQueueId = 2L).activeIndex)
        assertEquals(-1, mediaQueueUiOf(entries, null, activeQueueId = 99L).activeIndex)
        assertEquals(-1, mediaQueueUiOf(entries, null, activeQueueId = null).activeIndex)
    }

    @Test
    fun `total never reports below the copied window`() {
        val raw = (1L..50L).map { MediaQueueEntry(it, "T$it") }
        val ui = mediaQueueUiOf(raw, totalReported = 3, activeQueueId = null)
        assertEquals(50, ui.total)
        assertFalse(ui.truncated)
    }

    @Test
    fun `label falls back to a positional name without inventing a title`() {
        assertEquals("Track 4", mediaQueueEntryLabel(MediaQueueEntry(9, null), position = 3))
        assertEquals("Named", mediaQueueEntryLabel(MediaQueueEntry(9, "Named"), position = 3))
    }
}
