package com.autoverse.launcher.feature.diagnostics

import com.autoverse.launcher.core.diagnostics.AppHealth
import com.autoverse.launcher.core.diagnostics.ComponentStatus
import com.autoverse.launcher.core.diagnostics.PermissionsSummary
import com.autoverse.launcher.core.diagnostics.buildAccessReport
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.PerformanceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 17 — pins every diagnostics card string to the exact state it
 * describes: no invented values, unavailable readings stay distinct from
 * zeros, and the profile card leads with what is actually running.
 */
class DiagnosticsSummaryTest {

    // --- Summary cards -------------------------------------------------------

    @Test
    fun `app status texts cover all three health states`() {
        assertEquals("Normal", appStatusLabel(AppHealth.NORMAL))
        assertEquals("Starting", appStatusLabel(AppHealth.PARTIAL))
        assertEquals("Degraded", appStatusLabel(AppHealth.DEGRADED))
        assertEquals("All core surfaces are working.", appStatusDetail(AppHealth.NORMAL))
        assertEquals("Some checks are still resolving.", appStatusDetail(AppHealth.PARTIAL))
        assertEquals(
            "Running with fallbacks — see components below.",
            appStatusDetail(AppHealth.DEGRADED),
        )
    }

    @Test
    fun `profile card leads with the effective profile`() {
        assertEquals("Balanced", profileSummaryText(PerformanceProfile.BALANCED))
        assertEquals("Enhanced", profileSummaryText(PerformanceProfile.ENHANCED))
        assertEquals("Constrained", profileSummaryText(PerformanceProfile.CONSTRAINED))
    }

    @Test
    fun `profile note is null when nothing diverges`() {
        assertNull(
            profileNoteText(
                selected = PerformanceProfile.BALANCED,
                effective = PerformanceProfile.BALANCED,
                preferSmoothness = false,
                reducedMotion = false,
            ),
        )
    }

    @Test
    fun `profile note lists each divergence in a fixed order`() {
        assertEquals(
            "Selected: Enhanced",
            profileNoteText(PerformanceProfile.ENHANCED, PerformanceProfile.BALANCED, false, false),
        )
        assertEquals(
            "Prefer smoothness is on",
            profileNoteText(PerformanceProfile.BALANCED, PerformanceProfile.BALANCED, true, false),
        )
        assertEquals(
            "Reduced motion is on",
            profileNoteText(PerformanceProfile.BALANCED, PerformanceProfile.BALANCED, false, true),
        )
        assertEquals(
            "Selected: Enhanced · Prefer smoothness is on · Reduced motion is on",
            profileNoteText(PerformanceProfile.ENHANCED, PerformanceProfile.BALANCED, true, true),
        )
    }

    @Test
    fun `smoothness note is suppressed on the constrained profile`() {
        assertNull(
            profileNoteText(PerformanceProfile.CONSTRAINED, PerformanceProfile.CONSTRAINED, true, false),
        )
    }

    @Test
    fun `appearance detail names the mode and resolved theme`() {
        assertEquals(
            "Day · dark theme",
            appearanceDetailText(AppearanceMode.DAY, effectiveDark = true),
        )
        assertEquals(
            "System · light theme",
            appearanceDetailText(AppearanceMode.SYSTEM, effectiveDark = false),
        )
    }

    // --- Permissions ---------------------------------------------------------

    @Test
    fun `permissions label covers every roll-up`() {
        assertEquals("All granted", permissionsSummaryLabel(PermissionsSummary.ALL_GOOD))
        assertEquals("Optional off", permissionsSummaryLabel(PermissionsSummary.OPTIONAL_OFF))
        assertEquals("Action needed", permissionsSummaryLabel(PermissionsSummary.ACTION_NEEDED))
    }

    @Test
    fun `permissions detail counts only applicable accesses`() {
        val granted = buildAccessReport(sdkInt = 34, networkGranted = true, notificationAccessGranted = true)
        assertEquals("2 of 2 granted", permissionsDetailText(granted))

        val revoked = buildAccessReport(sdkInt = 34, networkGranted = true, notificationAccessGranted = false)
        assertEquals("1 of 2 granted", permissionsDetailText(revoked))

        val legacy = buildAccessReport(sdkInt = 30, networkGranted = true, notificationAccessGranted = true)
        assertEquals("3 of 3 granted", permissionsDetailText(legacy))
    }

    // --- Performance readings ------------------------------------------------

    @Test
    fun `fps reading keeps disabled and measuring distinct from zero`() {
        assertEquals("Tracking disabled", fpsReadingText(fps = 60f, tracking = false))
        assertEquals("Measuring…", fpsReadingText(fps = 0f, tracking = true))
        assertEquals("Measuring…", fpsReadingText(fps = -1f, tracking = true))
        assertEquals("60 FPS", fpsReadingText(fps = 59.6f, tracking = true))
        assertEquals("58 FPS", fpsReadingText(fps = 57.5f, tracking = true))
    }

    @Test
    fun `cpu reading formats a one-core share or admits unavailability`() {
        assertEquals("Not readable", cpuReadingText(null))
        assertEquals("18.4% of one core", cpuReadingText(18.44f))
        assertEquals("100.0% of one core", cpuReadingText(100f))
        assertEquals("250.0% of one core", cpuReadingText(250f))
    }

    @Test
    fun `memory readings admit unavailability instead of inventing values`() {
        assertEquals("Not readable", memoryReadingText(null))
        assertEquals("214 MB", memoryReadingText(214))

        assertEquals("Not readable", deviceMemoryReadingText(availableMb = null, totalMb = 8_192))
        assertEquals("Not readable", deviceMemoryReadingText(availableMb = 2_300, totalMb = null))
        assertEquals("2.2 GB free of 8.0 GB", deviceMemoryReadingText(availableMb = 2_300, totalMb = 8_192))

        assertEquals("Not readable", storageReadingText(null))
        assertEquals("512 MB free", storageReadingText(512))
        assertEquals("1023 MB free", storageReadingText(1_023))
        assertEquals("1.0 GB free", storageReadingText(1_024))
        assertEquals("20.9 GB free", storageReadingText(21_450))
    }

    // --- Component labels ----------------------------------------------------

    @Test
    fun `component status labels cover every status`() {
        assertEquals("Ready", componentStatusLabel(ComponentStatus.READY))
        assertEquals("Loading", componentStatusLabel(ComponentStatus.LOADING))
        assertEquals("Permission required", componentStatusLabel(ComponentStatus.PERMISSION_REQUIRED))
        assertEquals("Disabled", componentStatusLabel(ComponentStatus.DISABLED))
        assertEquals("Unavailable", componentStatusLabel(ComponentStatus.UNAVAILABLE))
        assertEquals("Error", componentStatusLabel(ComponentStatus.ERROR))
        assertEquals("Not checked", componentStatusLabel(ComponentStatus.NOT_CHECKED))
    }
}
