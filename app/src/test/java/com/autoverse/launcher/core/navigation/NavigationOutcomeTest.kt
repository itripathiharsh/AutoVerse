package com.autoverse.launcher.core.navigation

import android.content.ActivityNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 6 — handoff outcome classification and snackbar wording. Failures
 * must be explained, never a crash or a blank panel.
 */
class NavigationOutcomeTest {

    @Test
    fun `missing handler maps to ACTIVITY_NOT_FOUND`() {
        assertEquals(
            NavigationOutcome.ACTIVITY_NOT_FOUND,
            classifyNavigationLaunchFailure(ActivityNotFoundException()),
        )
    }

    @Test
    fun `any other throwable maps to LAUNCH_FAILED`() {
        assertEquals(
            NavigationOutcome.LAUNCH_FAILED,
            classifyNavigationLaunchFailure(SecurityException()),
        )
        assertEquals(
            NavigationOutcome.LAUNCH_FAILED,
            classifyNavigationLaunchFailure(IllegalStateException("boom")),
        )
    }

    @Test
    fun `messages name the provider and stay outcome-specific`() {
        assertEquals("", navigationFailureMessage("Waze", NavigationOutcome.OPENED))
        assertEquals(
            "No navigation app is available",
            navigationFailureMessage("Waze", NavigationOutcome.NO_PROVIDER),
        )
        assertEquals(
            "Waze couldn't open that destination",
            navigationFailureMessage("Waze", NavigationOutcome.ACTIVITY_NOT_FOUND),
        )
        assertEquals(
            "Waze can't receive destinations from AutoVerse",
            navigationFailureMessage("Waze", NavigationOutcome.NOT_SUPPORTED),
        )
        assertEquals(
            "Couldn't open Waze. Try again.",
            navigationFailureMessage("Waze", NavigationOutcome.LAUNCH_FAILED),
        )
    }
}
