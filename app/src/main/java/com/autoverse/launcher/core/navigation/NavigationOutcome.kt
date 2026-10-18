package com.autoverse.launcher.core.navigation

import android.content.ActivityNotFoundException

/** Result of a navigation handoff attempt, mapped to recovery behavior. */
enum class NavigationOutcome {
    OPENED,
    NO_PROVIDER,
    ACTIVITY_NOT_FOUND,
    NOT_SUPPORTED,
    LAUNCH_FAILED,
}

fun classifyNavigationLaunchFailure(failure: Throwable): NavigationOutcome = when (failure) {
    is ActivityNotFoundException -> NavigationOutcome.ACTIVITY_NOT_FOUND
    else -> NavigationOutcome.LAUNCH_FAILED
}

fun navigationFailureMessage(
    providerLabel: String,
    outcome: NavigationOutcome,
): String = when (outcome) {
    NavigationOutcome.OPENED -> ""
    NavigationOutcome.NO_PROVIDER -> "No navigation app is available"
    NavigationOutcome.ACTIVITY_NOT_FOUND -> "$providerLabel couldn't open that destination"
    NavigationOutcome.NOT_SUPPORTED -> "$providerLabel can't receive destinations from AutoVerse"
    NavigationOutcome.LAUNCH_FAILED -> "Couldn't open $providerLabel. Try again."
}
