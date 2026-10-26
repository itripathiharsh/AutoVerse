package com.autoverse.launcher.core.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallStateLogicTest {

    @Test
    fun `incoming call with permission shows the call panel`() {
        assertTrue(showsIncomingCall(CallUiState.Incoming("+1 555 0100"), permissionGranted = true))
    }

    @Test
    fun `in-call state keeps the call panel until the call ends`() {
        assertTrue(showsIncomingCall(CallUiState.InCall(null), permissionGranted = true))
    }

    @Test
    fun `idle returns the panel to the speedometer`() {
        assertFalse(showsIncomingCall(CallUiState.Idle, permissionGranted = true))
    }

    @Test
    fun `permission denial keeps the speedometer panel`() {
        assertFalse(showsIncomingCall(CallUiState.Incoming("123"), permissionGranted = false))
    }

    @Test
    fun `blank numbers are not displayed`() {
        assertEquals(null, callDisplayNumber(null))
        assertEquals(null, callDisplayNumber("   "))
        assertEquals("555", callDisplayNumber(" 555 "))
    }
}
