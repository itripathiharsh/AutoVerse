package com.autoverse.launcher.core.diagnostics

import com.autoverse.launcher.core.launcher.AppListState

/**
 * Overall launcher health for the App Status card.
 * [NORMAL] — every core surface is working.
 * [PARTIAL] — something is still resolving (first app scan, startup checks).
 * [DEGRADED] — a core surface failed and the app is running on fallbacks.
 */
enum class AppHealth { NORMAL, PARTIAL, DEGRADED }

/**
 * Derives health from real observed state only: an unreadable settings file
 * or a failed app scan degrade the launcher (favorites and the drawer run on
 * fallbacks); still-loading startup state is partial, never a fake "healthy".
 * Pure and unit-tested.
 */
fun appHealthFor(
    settingsStoreReadable: Boolean?,
    appListState: AppListState,
): AppHealth = when {
    settingsStoreReadable == false -> AppHealth.DEGRADED
    appListState is AppListState.Error -> AppHealth.DEGRADED
    appListState is AppListState.Loading -> AppHealth.PARTIAL
    settingsStoreReadable == null -> AppHealth.PARTIAL
    else -> AppHealth.NORMAL
}
