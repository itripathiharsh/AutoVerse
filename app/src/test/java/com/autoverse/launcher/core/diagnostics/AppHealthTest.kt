package com.autoverse.launcher.core.diagnostics

import com.autoverse.launcher.core.launcher.AppListState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 17 — pins the App Status card derivation: unreadable settings and
 * failed scans degrade, still-resolving startup state is partial, and only
 * fully observable working state is normal.
 */
class AppHealthTest {

    private val ready = AppListState.Ready(emptyList())
    private val loading = AppListState.Loading
    private val error = AppListState.Error("scan failed")

    @Test
    fun `unreadable settings and failed scans degrade`() {
        assertEquals(
            AppHealth.DEGRADED,
            appHealthFor(settingsStoreReadable = false, appListState = ready),
        )
        assertEquals(
            AppHealth.DEGRADED,
            appHealthFor(settingsStoreReadable = false, appListState = error),
        )
        assertEquals(
            AppHealth.DEGRADED,
            appHealthFor(settingsStoreReadable = true, appListState = error),
        )
        assertEquals(
            AppHealth.DEGRADED,
            appHealthFor(settingsStoreReadable = null, appListState = error),
        )
    }

    @Test
    fun `still-resolving startup state is partial`() {
        assertEquals(
            AppHealth.PARTIAL,
            appHealthFor(settingsStoreReadable = null, appListState = ready),
        )
        assertEquals(
            AppHealth.PARTIAL,
            appHealthFor(settingsStoreReadable = true, appListState = loading),
        )
    }

    @Test
    fun `fully observable working state is normal`() {
        assertEquals(
            AppHealth.NORMAL,
            appHealthFor(settingsStoreReadable = true, appListState = ready),
        )
    }

    @Test
    fun `every combination stays within the three health states`() {
        val states = listOf<AppListState>(loading, ready, error)
        val readableValues = listOf(null, true, false)
        for (settings in readableValues) {
            for (state in states) {
                val expected = when {
                    settings == false -> AppHealth.DEGRADED
                    state is AppListState.Error -> AppHealth.DEGRADED
                    state is AppListState.Loading || settings == null -> AppHealth.PARTIAL
                    else -> AppHealth.NORMAL
                }
                assertEquals(
                    "settings=$settings state=$state",
                    expected,
                    appHealthFor(settingsStoreReadable = settings, appListState = state),
                )
            }
        }
    }
}
