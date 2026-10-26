package com.autoverse.launcher.feature.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.MapsLayoutRules
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.AutoVerseTextField
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.DashboardTopBar
import com.autoverse.launcher.core.design.components.MapsLaunchPanel
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.SectionLabel
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.rememberNavigationLaunchFeedback
import com.autoverse.launcher.core.design.mapsPanelsSideBySide
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.navigation.DestinationParseResult
import com.autoverse.launcher.core.navigation.MapsForegroundVerdict
import com.autoverse.launcher.core.navigation.MapsStatus
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.RecentDestination
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.navigation.SavedPlace
import com.autoverse.launcher.core.navigation.navigationDestinationText
import com.autoverse.launcher.core.navigation.parseNavigationDestination
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The Maps page: the Google Maps launch region (~70%) and destinations
 * (~30%). Saved places and recents are real, local data; tapping one hands it
 * straight to Maps. Collapsing destinations gives the map region the full
 * width immediately, the restore control stays visible, and the choice
 * persists. This page never duplicates the Apps catalog.
 */
@Composable
fun NavigationScreen(
    clockText: String,
    dateText: String,
    connectivity: ConnectivityStatus,
    mapsStatus: MapsStatus,
    destinationTargetLabel: String,
    savedPlaces: List<SavedPlace>,
    recentDestinations: List<RecentDestination>,
    query: String,
    onQueryChange: (String) -> Unit,
    onNavigate: suspend (NavigationDestination) -> NavigationOutcome,
    onOpenGoogleMaps: suspend () -> NavigationOutcome,
    onSavePlace: suspend (String, NavigationDestination) -> Boolean,
    onRemovePlace: (SavedPlace) -> Unit,
    onClearRecents: () -> Unit,
    placesCollapsed: Boolean,
    onPlacesCollapsedChange: (Boolean) -> Unit,
    onOpenHome: () -> Unit,
    modifier: Modifier = Modifier,
    speedometerData: SpeedometerData = SpeedometerData(),
    hasLocationPermission: Boolean = true,
    onLaunchVerdict: (String, MapsForegroundVerdict) -> Unit = { _, _ -> },
    onOpenAppDrawer: () -> Unit = {},
    onOpenSpeedometer: () -> Unit = {},
    onOpenObd: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val launchFeedback = rememberNavigationLaunchFeedback(snackbarHostState, onLaunchVerdict)
    var draftLabel by rememberSaveable { mutableStateOf("") }
    val destination = (parseNavigationDestination(query) as? DestinationParseResult.Valid)?.destination

    val navigate: (NavigationDestination) -> Unit = { target ->
        launchFeedback.run(destinationTargetLabel) { onNavigate(target) }
    }
    val openMaps: () -> Unit = { launchFeedback.run("Google Maps", onOpenGoogleMaps) }
    val save: () -> Unit = {
        val target = destination
        if (target != null) {
            val label = draftLabel
            scope.launch {
                val saved = onSavePlace(label, target)
                if (saved) draftLabel = ""
                snackbarHostState.showSnackbar(
                    message = if (saved) {
                        "Saved “${label.trim().ifBlank { navigationDestinationText(target) }}”"
                    } else {
                        "Couldn’t save that place. Try again."
                    },
                    withDismissAction = true,
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val colors = LocalAutoVerseColors.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.backgroundTop)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AutoVerseSpacing.md, vertical = AutoVerseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
        ) {
            DashboardTopBar(
                clockText = clockText,
                dateText = dateText,
                connectivity = connectivity,
                onOpenDiagnostics = onOpenDiagnostics,
                onOpenSettings = onOpenSettings,
            )
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                AppNavigationRail(
                    selected = RailDestination.MAPS,
                    onOpenHome = onOpenHome,
                    onOpenAppDrawer = onOpenAppDrawer,
                    onOpenNavigation = {},
                    onOpenSpeedometer = onOpenSpeedometer,
                    onOpenObd = onOpenObd,
                    onOpenDiagnostics = onOpenDiagnostics,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.width(AutoVerseSpacing.railWidth).fillMaxHeight(),
                )
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val sideBySide = mapsPanelsSideBySide(maxWidth.value.roundToInt())
                    val mapRegion: @Composable (Modifier) -> Unit = { regionModifier ->
                        MapsLaunchPanel(
                            mapsStatus = mapsStatus,
                            destinationTargetLabel = destinationTargetLabel,
                            destinationQuery = query,
                            onDestinationQueryChange = onQueryChange,
                            onNavigate = navigate,
                            onOpenMaps = openMaps,
                            speedometerData = speedometerData,
                            hasLocationPermission = hasLocationPermission,
                            modifier = regionModifier,
                        )
                    }
                    val places: @Composable (Modifier) -> Unit = { panelModifier ->
                        DestinationsPanel(
                            savedPlaces = savedPlaces,
                            recentDestinations = recentDestinations,
                            saveLabel = draftLabel,
                            onSaveLabelChange = { draftLabel = it },
                            canSave = destination != null,
                            onSave = save,
                            onRemovePlace = onRemovePlace,
                            onClearRecents = onClearRecents,
                            onOpen = navigate,
                            onCollapse = { onPlacesCollapsedChange(true) },
                            modifier = panelModifier,
                        )
                    }
                    if (sideBySide) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
                        ) {
                            if (placesCollapsed) {
                                mapRegion(Modifier.weight(1f).fillMaxHeight())
                                RestorePlacesStrip(onRestore = { onPlacesCollapsedChange(false) })
                            } else {
                                mapRegion(Modifier.weight(MapsLayoutRules.MAP_FRACTION).fillMaxHeight())
                                places(Modifier.weight(1f - MapsLayoutRules.MAP_FRACTION).fillMaxHeight())
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
                        ) {
                            mapRegion(Modifier.weight(1f).fillMaxWidth())
                            if (placesCollapsed) {
                                TextAction(text = "Show destinations", onClick = { onPlacesCollapsedChange(false) })
                            } else {
                                places(Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Slim, always-visible restore control while destinations are collapsed. */
@Composable
private fun RestorePlacesStrip(onRestore: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    CockpitCard(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(AutoVerseSpacing.xxs),
    ) {
        IconButton(onClick = onRestore) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "Show destinations",
                tint = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun DestinationsPanel(
    savedPlaces: List<SavedPlace>,
    recentDestinations: List<RecentDestination>,
    saveLabel: String,
    onSaveLabelChange: (String) -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
    onRemovePlace: (SavedPlace) -> Unit,
    onClearRecents: () -> Unit,
    onOpen: (NavigationDestination) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    CockpitCard(modifier = modifier, contentPadding = PaddingValues(AutoVerseSpacing.md)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(icon = Icons.Outlined.Place, label = "DESTINATIONS", modifier = Modifier.weight(1f))
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Hide destinations",
                    tint = colors.textSecondary,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel(icon = Icons.Outlined.Star, label = "SAVED")
            if (savedPlaces.isEmpty()) {
                EmptyHint("Type a destination, then save it here.")
            } else {
                savedPlaces.forEach { place ->
                    SavedPlaceRow(place = place, onOpen = { onOpen(place.destination) }, onRemove = { onRemovePlace(place) })
                }
            }
            Spacer(Modifier.size(AutoVerseSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                AutoVerseTextField(
                    value = saveLabel,
                    onValueChange = onSaveLabelChange,
                    placeholder = "Label (optional)",
                    modifier = Modifier.weight(1f),
                )
                PrimaryAction(text = "Save", onClick = onSave, enabled = canSave)
            }
            Spacer(Modifier.size(AutoVerseSpacing.sm))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(icon = Icons.Outlined.History, label = "RECENT", modifier = Modifier.weight(1f))
                if (recentDestinations.isNotEmpty()) TextAction(text = "Clear", onClick = onClearRecents)
            }
            if (recentDestinations.isEmpty()) {
                EmptyHint("Destinations you navigate to will appear here.")
            } else {
                recentDestinations.forEach { recent ->
                    RecentDestinationRow(destination = recent.destination, onOpen = { onOpen(recent.destination) })
                }
            }
        }
    }
}

@Composable
private fun SavedPlaceRow(
    place: SavedPlace,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    val destinationText = navigationDestinationText(place.destination)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AutoVerseSpacing.touchMin)
            .clip(AutoVerseShapes.tile)
            .clickable(role = Role.Button, onClickLabel = "Navigate to ${place.label}", onClick = onOpen)
            .padding(horizontal = AutoVerseSpacing.xs, vertical = AutoVerseSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        Icon(
            imageVector = Icons.Outlined.Star,
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.inline),
            tint = colors.accent,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (place.label != destinationText) {
                Text(
                    text = destinationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove ${place.label}",
                modifier = Modifier.size(AutoVerseIcons.compact),
                tint = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun RecentDestinationRow(
    destination: NavigationDestination,
    onOpen: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AutoVerseSpacing.touchMin)
            .clip(AutoVerseShapes.tile)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = AutoVerseSpacing.xs, vertical = AutoVerseSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.inline),
            tint = colors.textFaint,
        )
        Text(
            text = navigationDestinationText(destination),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalAutoVerseColors.current.textFaint,
        modifier = Modifier.padding(vertical = AutoVerseSpacing.xs),
    )
}
