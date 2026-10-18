package com.autoverse.launcher.core.diagnostics

import android.os.Build

/**
 * How an access item matters to the launcher:
 * [REQUIRED] breaks a core surface when missing, [SPECIAL] needs a trip to
 * system settings to change, [OPTIONAL] is an extra that degrades gracefully.
 */
enum class AccessKind { REQUIRED, SPECIAL, OPTIONAL }

/**
 * The real state of one access item. [NOT_APPLICABLE] means the launcher does
 * not request or use this access on the current platform — it is not a
 * failure.
 */
enum class AccessState { GRANTED, NOT_GRANTED, NOT_APPLICABLE }

/** One row of the permissions card / sheet. */
data class AccessItem(
    val label: String,
    val kind: AccessKind,
    val state: AccessState,
    val detail: String,
)

/** Roll-up shown on the permissions summary card. */
enum class PermissionsSummary { ALL_GOOD, OPTIONAL_OFF, ACTION_NEEDED }

/**
 * Builds the access report from real signals only. The launcher deliberately
 * requests no location permission (the embedded map prototype does not use
 * location), so Location is reported as not applicable rather than missing;
 * Bluetooth becomes not-applicable from API 31 up because the legacy
 * permission is intentionally not requested there and the link indicator
 * simply hides. Pure and unit-tested.
 */
fun buildAccessReport(
    sdkInt: Int,
    networkGranted: Boolean,
    notificationAccessGranted: Boolean,
): List<AccessItem> = listOf(
    AccessItem(
        label = "Network",
        kind = AccessKind.REQUIRED,
        state = if (networkGranted) AccessState.GRANTED else AccessState.NOT_GRANTED,
        detail = "Required to reach the network for the embedded map preview.",
    ),
    AccessItem(
        label = "Notification access",
        kind = AccessKind.SPECIAL,
        state = if (notificationAccessGranted) {
            AccessState.GRANTED
        } else {
            AccessState.NOT_GRANTED
        },
        detail = "Lets the media card read what other players are playing.",
    ),
    AccessItem(
        label = "Location",
        kind = AccessKind.OPTIONAL,
        state = AccessState.NOT_APPLICABLE,
        detail = "Not requested — AutoVerse does not use location.",
    ),
    AccessItem(
        label = "Bluetooth",
        kind = AccessKind.OPTIONAL,
        state = if (sdkInt < Build.VERSION_CODES.S) {
            AccessState.GRANTED
        } else {
            AccessState.NOT_APPLICABLE
        },
        detail = if (sdkInt < Build.VERSION_CODES.S) {
            "Install-time permission used by the Bluetooth link indicator."
        } else {
            "Not requested on this Android version; the link indicator stays hidden."
        },
    ),
)

/**
 * Rolls the report up: any missing required or special access needs action,
 * an unmet optional item is a soft notice, everything else is fine.
 */
fun summarizePermissions(items: List<AccessItem>): PermissionsSummary = when {
    items.any { item ->
        item.state == AccessState.NOT_GRANTED &&
            (item.kind == AccessKind.REQUIRED || item.kind == AccessKind.SPECIAL)
    } -> PermissionsSummary.ACTION_NEEDED

    items.any { it.state == AccessState.NOT_GRANTED } -> PermissionsSummary.OPTIONAL_OFF
    else -> PermissionsSummary.ALL_GOOD
}

/** Items that count for the "N of M granted" line; not-applicable does not. */
fun applicableAccessItems(items: List<AccessItem>): List<AccessItem> =
    items.filter { it.state != AccessState.NOT_APPLICABLE }

fun grantedAccessCount(items: List<AccessItem>): Int =
    items.count { it.state == AccessState.GRANTED }
