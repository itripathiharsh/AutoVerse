package com.autoverse.launcher.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 17 — pins the access report on both sides of the Bluetooth API gate
 * and every roll-up path of the permissions summarizer.
 */
class AccessReportTest {

    private val modern = buildAccessReport(
        sdkInt = 34,
        networkGranted = true,
        notificationAccessGranted = true,
    )

    @Test
    fun `report lists the four real items in a fixed order`() {
        assertEquals(
            listOf("Network", "Notification access", "Location", "Bluetooth"),
            modern.map { it.label },
        )
    }

    @Test
    fun `granted report is all good with two applicable items`() {
        assertEquals(
            listOf(
                AccessState.GRANTED,
                AccessState.GRANTED,
                AccessState.NOT_APPLICABLE,
                AccessState.NOT_APPLICABLE,
            ),
            modern.map { it.state },
        )
        assertEquals(PermissionsSummary.ALL_GOOD, summarizePermissions(modern))
        assertEquals(2, applicableAccessItems(modern).size)
        assertEquals(2, grantedAccessCount(modern))
    }

    @Test
    fun `missing network is a required miss needing action`() {
        val items = buildAccessReport(
            sdkInt = 34,
            networkGranted = false,
            notificationAccessGranted = true,
        )
        val network = items.first { it.label == "Network" }
        assertEquals(AccessKind.REQUIRED, network.kind)
        assertEquals(AccessState.NOT_GRANTED, network.state)
        assertEquals(PermissionsSummary.ACTION_NEEDED, summarizePermissions(items))
    }

    @Test
    fun `missing notification access is a special miss needing action`() {
        val items = buildAccessReport(
            sdkInt = 34,
            networkGranted = true,
            notificationAccessGranted = false,
        )
        val notification = items.first { it.label == "Notification access" }
        assertEquals(AccessKind.SPECIAL, notification.kind)
        assertEquals(AccessState.NOT_GRANTED, notification.state)
        assertEquals(PermissionsSummary.ACTION_NEEDED, summarizePermissions(items))
    }

    @Test
    fun `bluetooth follows the API gate`() {
        val legacy = buildAccessReport(
            sdkInt = 30,
            networkGranted = true,
            notificationAccessGranted = true,
        )
        assertEquals(
            AccessState.GRANTED,
            legacy.first { it.label == "Bluetooth" }.state,
        )
        assertEquals(
            AccessState.NOT_APPLICABLE,
            modern.first { it.label == "Bluetooth" }.state,
        )
    }

    @Test
    fun `location is always not applicable`() {
        assertEquals(
            AccessState.NOT_APPLICABLE,
            modern.first { it.label == "Location" }.state,
        )
    }

    @Test
    fun `an optional-only miss stays a soft notice`() {
        val items = listOf(
            AccessItem("Thing", AccessKind.OPTIONAL, AccessState.NOT_GRANTED, "d"),
            AccessItem("Other", AccessKind.REQUIRED, AccessState.GRANTED, "d"),
        )
        assertEquals(PermissionsSummary.OPTIONAL_OFF, summarizePermissions(items))
        assertEquals(1, grantedAccessCount(items))
        assertEquals(2, applicableAccessItems(items).size)
    }
}
