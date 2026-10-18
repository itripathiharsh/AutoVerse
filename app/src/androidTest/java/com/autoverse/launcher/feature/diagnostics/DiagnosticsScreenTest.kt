package com.autoverse.launcher.feature.diagnostics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.connectivity.LinkState
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.diagnostics.AccessItem
import com.autoverse.launcher.core.diagnostics.DiagnosticEvent
import com.autoverse.launcher.core.diagnostics.DiagnosticLevel
import com.autoverse.launcher.core.diagnostics.FrameSample
import com.autoverse.launcher.core.diagnostics.SystemSnapshot
import com.autoverse.launcher.core.diagnostics.buildAccessReport
import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.NavigationProvider
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import com.autoverse.launcher.core.performance.DeviceProfile
import com.autoverse.launcher.core.performance.FrameTimeTracker
import com.autoverse.launcher.core.performance.PerformanceTier
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Phase 17 — diagnostics behavior pins. The test window is smaller than the
 * WIDE render size, so that layout is driven through [Modifier.requiredSize]
 * and asserted with existence checks; interactions on nodes the window clips
 * go through the semantics onClick action rather than touch injection.
 * COMPACT tests render at the exact window width.
 */
class DiagnosticsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Clearly fictional sample device — values are never treated as real. */
    private val sampleProfile = DeviceProfile(
        manufacturer = "Sample Motors",
        model = "HU-9000",
        androidRelease = "14",
        sdkInt = 34,
        totalRamMb = 8_192,
        cpuCores = 8,
        lowRamDevice = false,
        densityDpi = 240,
        screenWidthDp = 1_280,
        screenHeightDp = 800,
        tier = PerformanceTier.ENHANCED,
    )

    private val sampleSnapshot = FrameTimeTracker.Snapshot(
        fps = 59.4f,
        frameCount = 2_140L,
        jankCount = 6L,
        sessionId = 1L,
        atMillis = 1_700_000_000_000L,
    )

    private val sampleHistory = listOf(
        FrameSample(fps = 58f, atMillis = 1_700_000_000_000L),
        FrameSample(fps = 60f, atMillis = 1_700_000_001_000L),
        FrameSample(fps = 59f, atMillis = 1_700_000_002_000L),
    )

    private val sampleSystemSnapshot = SystemSnapshot(
        processCpuPercentOfCore = 18.4f,
        processMemoryPssMb = 214,
        deviceMemoryAvailableMb = 2_300,
        deviceMemoryTotalMb = 8_192,
        storageAvailableMb = 21_450,
    )

    private val sampleEvents = listOf(
        DiagnosticEvent(
            id = 3L,
            timeMillis = 1_700_000_012_000L,
            level = DiagnosticLevel.WARN,
            message = "An app could not be opened",
        ),
        DiagnosticEvent(
            id = 2L,
            timeMillis = 1_700_000_002_000L,
            level = DiagnosticLevel.INFO,
            message = "App list loaded",
        ),
        DiagnosticEvent(
            id = 1L,
            timeMillis = 1_700_000_001_000L,
            level = DiagnosticLevel.INFO,
            message = "Launcher started",
        ),
    )

    private val sampleAccessItems = buildAccessReport(
        sdkInt = 34,
        networkGranted = true,
        notificationAccessGranted = true,
    )

    private fun setDiagnostics(
        widthDp: Int = 1280,
        heightDp: Int = 800,
        profile: DeviceProfile = sampleProfile,
        selectedProfile: PerformanceProfile = PerformanceProfile.BALANCED,
        frameSnapshot: FrameTimeTracker.Snapshot = sampleSnapshot,
        frameHistory: List<FrameSample> = sampleHistory,
        frameTrackingActive: Boolean = true,
        systemSnapshot: SystemSnapshot? = sampleSystemSnapshot,
        events: List<DiagnosticEvent> = sampleEvents,
        appListState: AppListState = AppListState.Ready(emptyList()),
        mediaState: MediaUiState = MediaUiState.NoSession,
        navigationProvider: ResolvedNavigationProvider = ResolvedNavigationProvider(
            provider = NavigationProvider.SystemDefault,
            preferredMissing = false,
        ),
        settingsStoreReadable: Boolean? = true,
        accessItems: List<AccessItem> = sampleAccessItems,
        notificationAccessGranted: Boolean = true,
        settings: AppSettings = AppSettings(themeId = AutoVerseThemeId.OBSIDIAN),
        refreshing: Boolean = false,
        onRefresh: () -> Unit = {},
        onClearLog: () -> Unit = {},
        onBack: () -> Unit = {},
        onOpenAppearanceSettings: () -> Unit = {},
        onOpenPerformanceSettings: () -> Unit = {},
        onOpenPermissionsSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                Box(Modifier.requiredSize(widthDp.dp, heightDp.dp)) {
                    DiagnosticsScreen(
                        clockText = "09:41",
                        dateText = "Wednesday, 15 October",
                        connectivity = ConnectivityStatus(
                            wifi = LinkState.CONNECTED,
                            bluetooth = LinkState.CONNECTED,
                        ),
                        profile = profile,
                        selectedProfile = selectedProfile,
                        frameSnapshot = frameSnapshot,
                        frameHistory = frameHistory,
                        frameTrackingActive = frameTrackingActive,
                        systemSnapshot = systemSnapshot,
                        diagnosticsEvents = events,
                        appListState = appListState,
                        mediaState = mediaState,
                        navigationProvider = navigationProvider,
                        settingsStoreReadable = settingsStoreReadable,
                        accessItems = accessItems,
                        notificationAccessGranted = notificationAccessGranted,
                        settings = settings,
                        systemAnimationsDisabled = false,
                        refreshing = refreshing,
                        onRefresh = onRefresh,
                        onClearLog = onClearLog,
                        onBack = onBack,
                        onOpenHome = {},
                        onOpenAppDrawer = {},
                        onOpenNavigation = {},
                        onOpenSettings = {},
                        onOpenAppearanceSettings = onOpenAppearanceSettings,
                        onOpenPerformanceSettings = onOpenPerformanceSettings,
                        onOpenPermissionsSettings = onOpenPermissionsSettings,
                    )
                }
            }
        }
    }

    /**
     * Clicks through the semantics action: works for nodes the undersized test
     * window never actually shows (the WIDE render extends past it).
     */
    private fun clickNode(text: String) {
        composeRule.onAllNodesWithText(text).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test
    fun wideLayoutShowsSummaryCardsAndAllPanels() {
        setDiagnostics()

        composeRule.onAllNodesWithText("App status").onFirst().assertExists()
        composeRule.onAllNodesWithText("Performance profile").onFirst().assertExists()
        composeRule.onAllNodesWithText("Theme").onFirst().assertExists()
        composeRule.onAllNodesWithText("Permissions").onFirst().assertExists()
        composeRule.onNodeWithText("System information").assertExists()
        composeRule.onNodeWithText("App components").assertExists()
        composeRule.onNodeWithText("Recent logs").assertExists()
        composeRule.onNodeWithText("Refresh All").assertExists()
    }

    @Test
    fun compactLayoutShowsSummaryAndPanels() {
        setDiagnostics(widthDp = 411, heightDp = 891)

        composeRule.onAllNodesWithText("App status").onFirst().assertExists()
        composeRule.onNodeWithText("App components").assertExists()
        composeRule.onNodeWithText("Recent logs").assertExists()
        composeRule.onNodeWithText("Refresh All").assertExists()
    }

    @Test
    fun refreshAllFiresCallback() {
        val refreshed = AtomicReference<Boolean?>(null)
        setDiagnostics(onRefresh = { refreshed.set(true) })

        clickNode("Refresh All")

        assertEquals(true, refreshed.get())
    }

    @Test
    fun refreshingStateSwapsLabelAndDisablesAction() {
        val refreshed = AtomicReference<Boolean?>(null)
        setDiagnostics(refreshing = true, onRefresh = { refreshed.set(true) })

        composeRule.onNodeWithText("Refreshing…").assertExists()
        composeRule.onAllNodesWithText("Refresh All").assertCountEquals(0)

        clickNode("Refreshing…")

        assertEquals(null, refreshed.get())
    }

    @Test
    fun summaryCardsReflectHealthyState() {
        setDiagnostics()

        composeRule.onNodeWithText("Normal").assertExists()
        composeRule.onNodeWithText("All core surfaces are working.").assertExists()
        composeRule.onNodeWithText("Balanced").assertExists()
        composeRule.onNodeWithText("Running with the stored profile.").assertExists()
        composeRule.onNodeWithText("Obsidian").assertExists()
        composeRule.onNodeWithText("System ·", substring = true).assertExists()
        composeRule.onNodeWithText("All granted").assertExists()
        composeRule.onNodeWithText("2 of 2 granted").assertExists()
    }

    @Test
    fun degradedHealthIsReflectedInStatusCard() {
        setDiagnostics(settingsStoreReadable = false)

        composeRule.onNodeWithText("Degraded").assertExists()
        composeRule.onNodeWithText("Running with fallbacks — see components below.").assertExists()
    }

    @Test
    fun profileDivergenceIsExplained() {
        setDiagnostics(selectedProfile = PerformanceProfile.ENHANCED)

        composeRule.onNodeWithText("Balanced").assertExists()
        composeRule.onNodeWithText("Selected: Enhanced").assertExists()
    }

    @Test
    fun permissionGapShowsActionNeeded() {
        setDiagnostics(
            accessItems = buildAccessReport(
                sdkInt = 34,
                networkGranted = false,
                notificationAccessGranted = true,
            ),
        )

        composeRule.onNodeWithText("Action needed").assertExists()
        composeRule.onNodeWithText("1 of 2 granted").assertExists()
    }

    @Test
    fun readingsAreHonestWhenUnavailable() {
        setDiagnostics(frameTrackingActive = false, systemSnapshot = null)

        composeRule.onNodeWithText("Tracking disabled").assertExists()
        composeRule.onAllNodesWithText("Not readable").assertCountEquals(4)
    }

    @Test
    fun fpsIsRoundedFromLiveSnapshot() {
        setDiagnostics()

        composeRule.onNodeWithText("59 FPS").assertExists()
        composeRule.onAllNodesWithText("Measuring…").assertCountEquals(0)
    }

    @Test
    fun emptyHistoryShowsChartEmptyState() {
        setDiagnostics(frameHistory = emptyList())

        composeRule.onNodeWithText("History builds while this screen samples.").assertExists()
        composeRule.onAllNodesWithText("samples · 1-second windows · newest right", substring = true)
            .assertCountEquals(0)
    }

    @Test
    fun nonEmptyHistoryShowsSampleCaption() {
        setDiagnostics()

        composeRule.onNodeWithText("3 samples · 1-second windows · newest right").assertExists()
    }

    @Test
    fun emptyLogShowsHonestEmptyState() {
        setDiagnostics(events = emptyList())

        composeRule.onNodeWithText("No events recorded yet.").assertExists()
        composeRule.onAllNodesWithText("View all").assertCountEquals(0)
    }

    @Test
    fun logPreviewCapsAtSixAndViewAllOpensFullLog() {
        val events = (8 downTo 1).map { index ->
            DiagnosticEvent(
                id = index.toLong(),
                timeMillis = 1_700_000_000_000L + index * 1_000L,
                level = DiagnosticLevel.INFO,
                message = "Event number $index",
            )
        }
        setDiagnostics(events = events)

        composeRule.onNodeWithText("Event number 8").assertExists()
        composeRule.onNodeWithText("Event number 2").assertDoesNotExist()
        composeRule.onNodeWithText("Event number 1").assertDoesNotExist()

        clickNode("View all")

        composeRule.onNodeWithText("Diagnostic log").assertExists()
        composeRule.onNodeWithText("8 events").assertExists()
        composeRule.onNodeWithText("Event number 1").assertExists()
    }

    @Test
    fun clearLogFiresFromDetail() {
        val cleared = AtomicReference<Boolean?>(null)
        setDiagnostics(onClearLog = { cleared.set(true) })

        clickNode("View all")
        clickNode("Clear log")

        assertEquals(true, cleared.get())
    }

    @Test
    fun detailBackClosesOverlayWithoutFiringScreenBack() {
        val backPressed = AtomicReference<Boolean?>(null)
        setDiagnostics(onBack = { backPressed.set(true) })

        clickNode("View all")
        composeRule.onNodeWithText("Diagnostic log").assertExists()
        composeRule.onAllNodesWithText("WARN").onFirst().assertExists()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("Diagnostic log").assertDoesNotExist()
        assertEquals(null, backPressed.get())
    }

    @Test
    fun summaryCardActionsRouteToTheirCategories() {
        val performance = AtomicReference<Boolean?>(null)
        val theme = AtomicReference<Boolean?>(null)
        val permissions = AtomicReference<Boolean?>(null)
        setDiagnostics(
            onOpenPerformanceSettings = { performance.set(true) },
            onOpenAppearanceSettings = { theme.set(true) },
            onOpenPermissionsSettings = { permissions.set(true) },
        )

        composeRule.onAllNodesWithText("Change")[0]
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onAllNodesWithText("Change")[1]
            .performSemanticsAction(SemanticsActions.OnClick)
        clickNode("Review")

        assertEquals(true, performance.get())
        assertEquals(true, theme.get())
        assertEquals(true, permissions.get())
    }

    @Test
    fun componentTilesComeFromLiveState() {
        setDiagnostics(
            appListState = AppListState.Loading,
            mediaState = MediaUiState.Connecting,
            settingsStoreReadable = null,
        )

        composeRule.onNodeWithText("Reading installed apps…").assertExists()
        composeRule.onNodeWithText("Reading media session state…").assertExists()
        composeRule.onNodeWithText("Checked during startup.").assertExists()
        composeRule.onNodeWithText("System default is used for hand-offs.").assertExists()
        composeRule.onNodeWithText("Off by default; the preview is an experimental prototype.")
            .assertExists()
    }

    @Test
    fun mediaPermissionGapIsReflected() {
        setDiagnostics(notificationAccessGranted = false)

        composeRule.onNodeWithText(
            "Grant notification access so the media card can read sessions.",
        ).assertExists()
    }

    @Test
    fun systemInfoShowsRealProfileValues() {
        setDiagnostics()

        composeRule.onNodeWithText("Sample Motors HU-9000").assertExists()
        composeRule.onNodeWithText("14 (API 34)").assertExists()
        composeRule.onNodeWithText("8192 MB").assertExists()
        composeRule.onNodeWithText("1280 × 800 dp").assertExists()
        composeRule.onNodeWithText("240 dpi").assertExists()
        composeRule.onNodeWithText("Enhanced").assertExists()
    }

    @Test
    fun logRowsShowLevelLabels() {
        setDiagnostics()

        composeRule.onAllNodesWithText("WARN").onFirst().assertExists()
        composeRule.onAllNodesWithText("INFO").onFirst().assertExists()
    }
}
