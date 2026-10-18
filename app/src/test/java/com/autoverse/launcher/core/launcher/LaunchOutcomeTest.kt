package com.autoverse.launcher.core.launcher

import android.content.ActivityNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 4 — launch failure classification and snackbar wording. The mapping
 * decides recovery behavior: NOT_FOUND also removes the stale favorite.
 */
class LaunchOutcomeTest {

    @Test
    fun `missing activity maps to NOT_FOUND`() {
        assertEquals(
            LaunchOutcome.NOT_FOUND,
            classifyLaunchFailure(ActivityNotFoundException()),
        )
    }

    @Test
    fun `security exception maps to BLOCKED`() {
        assertEquals(
            LaunchOutcome.BLOCKED,
            classifyLaunchFailure(SecurityException()),
        )
    }

    @Test
    fun `any other throwable maps to FAILED`() {
        assertEquals(
            LaunchOutcome.FAILED,
            classifyLaunchFailure(IllegalStateException("boom")),
        )
    }

    @Test
    fun `messages name the app and stay outcome-specific`() {
        assertEquals("", launchFailureMessage("Maps", LaunchOutcome.LAUNCHED))
        assertEquals(
            "Maps is no longer installed",
            launchFailureMessage("Maps", LaunchOutcome.NOT_FOUND),
        )
        assertEquals(
            "Android blocked launching Maps",
            launchFailureMessage("Maps", LaunchOutcome.BLOCKED),
        )
        assertEquals(
            "Could not open Maps",
            launchFailureMessage("Maps", LaunchOutcome.FAILED),
        )
    }
}
