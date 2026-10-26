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
        assertEquals("", navigationFailureMessage("Waze", NavigationOutcome.REQUESTED))
        assertEquals(
            "No navigation app is available",
            navigationFailureMessage("Waze", NavigationOutcome.NO_PROVIDER),
        )
        assertEquals(
            "Waze can't open that request",
            navigationFailureMessage("Waze", NavigationOutcome.ACTIVITY_NOT_FOUND),
        )
        assertEquals(
            "Google Maps isn't installed on this device",
            navigationFailureMessage("Google Maps", NavigationOutcome.NOT_INSTALLED),
        )
        assertEquals(
            "Google Maps is disabled. Enable it in system settings.",
            navigationFailureMessage("Google Maps", NavigationOutcome.APP_DISABLED),
        )
        assertEquals(
            "Enter a destination first",
            navigationFailureMessage("Google Maps", NavigationOutcome.INVALID_DESTINATION),
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

    @Test
    fun `every outcome except REQUESTED has a non-empty explanation`() {
        NavigationOutcome.entries
            .filter { it != NavigationOutcome.REQUESTED }
            .forEach { assert(navigationFailureMessage("Maps", it).isNotBlank()) { "$it has no message" } }
    }

    @Test
    fun `being paused after the request proves the target took the screen`() {
        assertEquals(
            MapsForegroundVerdict.LEFT_AUTOVERSE,
            foregroundVerdict(pausedAfterRequest = true, inMultiWindow = false),
        )
        assertEquals(
            MapsForegroundVerdict.LEFT_AUTOVERSE,
            foregroundVerdict(pausedAfterRequest = true, inMultiWindow = true),
        )
    }

    @Test
    fun `staying resumed in full screen means nothing came to the front`() {
        assertEquals(
            MapsForegroundVerdict.STAYED_ON_AUTOVERSE,
            foregroundVerdict(pausedAfterRequest = false, inMultiWindow = false),
        )
    }

    @Test
    fun `split-screen can't confirm visibility either way`() {
        assertEquals(
            MapsForegroundVerdict.UNKNOWN_MULTI_WINDOW,
            foregroundVerdict(pausedAfterRequest = false, inMultiWindow = true),
        )
    }

    @Test
    fun `stayed-in-front message is honest about the request`() {
        assertEquals(
            "Google Maps was asked to open but didn't come to the front. Try again.",
            stayedInFrontMessage("Google Maps"),
        )
    }
}
