package com.autoverse.launcher.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 17 — pins the bounded diagnostics log: newest-first ordering, stable
 * monotonic ids, capacity drops, truthful clearing, and the message
 * sanitizing that keeps user content and multi-line text out of the UI.
 */
class DiagnosticLogTest {

    private fun log(
        capacity: Int = DiagnosticLog.DEFAULT_CAPACITY,
        clock: () -> Long = { 0L },
    ) = DiagnosticLog(capacity = capacity, clock = clock)

    // --- Recording ----------------------------------------------------------

    @Test
    fun `record keeps newest events first with monotonic ids`() {
        val log = log(clock = { 42L })
        log.record(DiagnosticLevel.INFO, "first")
        log.record(DiagnosticLevel.WARN, "second")

        val entries = log.entries.value
        assertEquals(listOf("second", "first"), entries.map { it.message })
        assertTrue(entries[0].id > entries[1].id)
        assertEquals(42L, entries[0].timeMillis)
    }

    @Test
    fun `capacity drops the oldest entries silently`() {
        val log = log(capacity = 3)
        listOf("a", "b", "c", "d").forEach { log.record(DiagnosticLevel.INFO, it) }

        val entries = log.entries.value
        assertEquals(3, entries.size)
        assertEquals(listOf("d", "c", "b"), entries.map { it.message })
    }

    @Test
    fun `blank messages are dropped after sanitizing`() {
        val log = log()
        log.record(DiagnosticLevel.INFO, "   ")
        log.record(DiagnosticLevel.INFO, "\n\t ")
        assertTrue(log.entries.value.isEmpty())
    }

    @Test
    fun `recorded messages are sanitized on the way in`() {
        val log = log()
        log.record(DiagnosticLevel.INFO, "line one\nline two\tend")
        assertEquals("line one line two end", log.entries.value.single().message)
    }

    @Test
    fun `clear empties the buffer without recording anything`() {
        val log = log()
        log.record(DiagnosticLevel.ERROR, "boom")
        log.clear()
        assertTrue(log.entries.value.isEmpty())

        log.record(DiagnosticLevel.INFO, "after clear")
        assertEquals(listOf("after clear"), log.entries.value.map { it.message })
    }

    // --- Sanitizing ---------------------------------------------------------

    @Test
    fun `sanitize flattens control characters and collapses whitespace`() {
        assertEquals("a b", sanitizeDiagnosticMessage("a\u0007   b"))
        assertEquals("hi", sanitizeDiagnosticMessage("  hi  "))
        assertEquals("", sanitizeDiagnosticMessage(" \n "))
        assertEquals("one two", sanitizeDiagnosticMessage("one\r\ntwo"))
    }

    @Test
    fun `sanitize truncates long messages with an ellipsis`() {
        val exact = "x".repeat(DiagnosticLog.MAX_MESSAGE_LENGTH)
        assertEquals(exact, sanitizeDiagnosticMessage(exact))

        val over = sanitizeDiagnosticMessage("x".repeat(DiagnosticLog.MAX_MESSAGE_LENGTH + 1))
        assertEquals(DiagnosticLog.MAX_MESSAGE_LENGTH, over.length)
        assertTrue(over.endsWith("…"))
        assertEquals("x".repeat(DiagnosticLog.MAX_MESSAGE_LENGTH - 1), over.dropLast(1))
    }

    @Test
    fun `constants are pinned`() {
        assertEquals(120, DiagnosticLog.DEFAULT_CAPACITY)
        assertEquals(160, DiagnosticLog.MAX_MESSAGE_LENGTH)
    }
}
