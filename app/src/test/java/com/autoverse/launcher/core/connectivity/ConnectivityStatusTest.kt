package com.autoverse.launcher.core.connectivity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityStatusTest {

    @Test
    fun `bluetooth is readable below android twelve`() {
        assertTrue(isBluetoothReadable(sdkInt = 23, hasConnectPermission = false))
        assertTrue(isBluetoothReadable(sdkInt = 30, hasConnectPermission = false))
    }

    @Test
    fun `bluetooth needs the connect permission from android twelve`() {
        assertFalse(isBluetoothReadable(sdkInt = 31, hasConnectPermission = false))
        assertFalse(isBluetoothReadable(sdkInt = 34, hasConnectPermission = false))
        assertTrue(isBluetoothReadable(sdkInt = 31, hasConnectPermission = true))
        assertTrue(isBluetoothReadable(sdkInt = 34, hasConnectPermission = true))
    }

    @Test
    fun `unknown links stay hidden until the platform reports them`() {
        val status = ConnectivityStatus()
        assertEquals(LinkState.UNKNOWN, status.wifi)
        assertEquals(LinkState.UNKNOWN, status.bluetooth)
    }
}
