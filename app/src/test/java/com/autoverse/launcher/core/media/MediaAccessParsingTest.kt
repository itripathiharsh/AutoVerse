package com.autoverse.launcher.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 5 — notification-listener access parsing (pre-API 27 path and pure
 * checks). The setting is a colon-separated ComponentName list; parsing must
 * tolerate whitespace, empties, and null.
 */
class MediaAccessParsingTest {

    private val listenerOne = "com.example.one/com.example.one.ListenerService"
    private val listenerTwo = "com.example.two/com.example.two.ListenerService"

    @Test
    fun `colon-separated listeners parse in order`() {
        assertEquals(
            listOf(listenerOne, listenerTwo),
            parseEnabledListeners("$listenerOne:$listenerTwo"),
        )
    }

    @Test
    fun `null empty and blank settings parse to nothing`() {
        assertEquals(emptyList<String>(), parseEnabledListeners(null))
        assertEquals(emptyList<String>(), parseEnabledListeners(""))
        assertEquals(emptyList<String>(), parseEnabledListeners("  :  "))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals(listOf(listenerOne), parseEnabledListeners("  $listenerOne  "))
    }

    @Test
    fun `distinct packages are extracted from component names`() {
        assertEquals(
            setOf("com.example.one", "com.example.two"),
            enabledListenerPackages("$listenerOne:$listenerTwo"),
        )
    }

    @Test
    fun `grant check matches on the package component`() {
        assertTrue(isNotificationAccessGranted(listenerOne, "com.example.one"))
        assertFalse(isNotificationAccessGranted(listenerOne, "com.example.two"))
        assertFalse(isNotificationAccessGranted(null, "com.example.one"))
    }
}
