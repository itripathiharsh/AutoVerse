package com.autoverse.launcher.core.launcher

import android.content.ActivityNotFoundException

/** Result of a launch attempt, mapped to user-facing recovery behavior. */
enum class LaunchOutcome { LAUNCHED, NOT_FOUND, BLOCKED, FAILED }

fun classifyLaunchFailure(failure: Throwable): LaunchOutcome = when (failure) {
    is ActivityNotFoundException -> LaunchOutcome.NOT_FOUND
    is SecurityException -> LaunchOutcome.BLOCKED
    else -> LaunchOutcome.FAILED
}

fun launchFailureMessage(label: String, outcome: LaunchOutcome): String = when (outcome) {
    LaunchOutcome.LAUNCHED -> ""
    LaunchOutcome.NOT_FOUND -> "$label is no longer installed"
    LaunchOutcome.BLOCKED -> "Android blocked launching $label"
    LaunchOutcome.FAILED -> "Could not open $label"
}
