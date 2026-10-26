package com.autoverse.launcher.core.navigation

import android.content.ActivityNotFoundException

/**
 * Result of a navigation handoff attempt. [REQUESTED] only means Android
 * accepted the launch intent — whether the target actually reached the
 * foreground is observed separately ([MapsForegroundVerdict]), never assumed.
 */
enum class NavigationOutcome {
    REQUESTED,
    NOT_INSTALLED,
    APP_DISABLED,
    NO_PROVIDER,
    ACTIVITY_NOT_FOUND,
    NOT_SUPPORTED,
    INVALID_DESTINATION,
    LAUNCH_FAILED,
}

/** What AutoVerse observed after a [NavigationOutcome.REQUESTED] launch. */
enum class MapsForegroundVerdict {
    /** AutoVerse was paused right after the request: another activity took the screen. */
    LEFT_AUTOVERSE,

    /** AutoVerse stayed resumed: the request was accepted but nothing came to the front. */
    STAYED_ON_AUTOVERSE,

    /** Split-screen keeps both apps resumed, so a pause can't prove anything either way. */
    UNKNOWN_MULTI_WINDOW,
}

/** Pure verdict from what the activity lifecycle showed after a launch request. */
fun foregroundVerdict(pausedAfterRequest: Boolean, inMultiWindow: Boolean): MapsForegroundVerdict = when {
    pausedAfterRequest -> MapsForegroundVerdict.LEFT_AUTOVERSE
    inMultiWindow -> MapsForegroundVerdict.UNKNOWN_MULTI_WINDOW
    else -> MapsForegroundVerdict.STAYED_ON_AUTOVERSE
}

fun classifyNavigationLaunchFailure(failure: Throwable): NavigationOutcome = when (failure) {
    is ActivityNotFoundException -> NavigationOutcome.ACTIVITY_NOT_FOUND
    else -> NavigationOutcome.LAUNCH_FAILED
}

fun navigationFailureMessage(
    providerLabel: String,
    outcome: NavigationOutcome,
): String = when (outcome) {
    NavigationOutcome.REQUESTED -> ""
    NavigationOutcome.NOT_INSTALLED -> "$providerLabel isn't installed on this device"
    NavigationOutcome.APP_DISABLED -> "$providerLabel is disabled. Enable it in system settings."
    NavigationOutcome.NO_PROVIDER -> "No navigation app is available"
    NavigationOutcome.ACTIVITY_NOT_FOUND -> "$providerLabel can't open that request"
    NavigationOutcome.NOT_SUPPORTED -> "$providerLabel can't receive destinations from AutoVerse"
    NavigationOutcome.INVALID_DESTINATION -> "Enter a destination first"
    NavigationOutcome.LAUNCH_FAILED -> "Couldn't open $providerLabel. Try again."
}

/** Shown when Android accepted the request but the target never covered AutoVerse. */
fun stayedInFrontMessage(providerLabel: String): String =
    "$providerLabel was asked to open but didn't come to the front. Try again."
