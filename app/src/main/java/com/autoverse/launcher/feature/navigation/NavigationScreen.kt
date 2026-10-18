package com.autoverse.launcher.feature.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.AutoVerseScreen
import com.autoverse.launcher.core.design.components.AutoVerseTextField
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.SectionLabel
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.TransportButton
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.navigationPlacesLayoutFor
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.navigation.DestinationParseResult
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.NavigationProvider
import com.autoverse.launcher.core.navigation.NavigationSuggestion
import com.autoverse.launcher.core.navigation.NavigationSuggestionSource
import com.autoverse.launcher.core.navigation.RecentDestination
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import com.autoverse.launcher.core.navigation.SavedPlace
import com.autoverse.launcher.core.navigation.destinationFailureMessage
import com.autoverse.launcher.core.navigation.navigationDestinationText
import com.autoverse.launcher.core.navigation.navigationFailureMessage
import com.autoverse.launcher.core.navigation.navigationSuggestions
import com.autoverse.launcher.core.navigation.parseNavigationDestination
import com.autoverse.launcher.core.navigation.supportsOpenApp
import com.autoverse.launcher.feature.map.EmbeddedMapSurface
import com.autoverse.launcher.feature.map.MapPocContent
import com.autoverse.launcher.feature.map.MapPocController
import com.autoverse.launcher.feature.map.MapPocState
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Phase 18 — the navigation page. One destination field (free text or
 * coordinates, validated by the existing parser), real suggestions from
 * saved places and confirmed handoffs, and a map-dominant body. The places
 * panel is a child of the body layout: showing or hiding it reflows the map
 * into the freed space without ever recreating the map itself.
 *
 * The embedded map stays off by default; when it is off the body is an
 * intentional surface that hands off to the provider app, never a fake map.
 * No location permission is used and no location data is produced anywhere
 * on this screen.
 */
@Composable
fun NavigationScreen(
    resolved: ResolvedNavigationProvider,
    savedPlaces: List<SavedPlace>,
    recentDestinations: List<RecentDestination>,
    embeddedMapEnabled: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    placesPanelVisible: Boolean,
    onTogglePlacesPanel: () -> Unit,
    onNavigate: suspend (NavigationDestination) -> NavigationOutcome,
    onOpenNavigationApp: suspend () -> NavigationOutcome,
    onSavePlace: (String, NavigationDestination) -> Unit,
    onRemovePlace: (SavedPlace) -> Unit,
    onChangeProvider: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    embeddedMapSurface: (@Composable () -> Unit)? = null,
) {
    val provider = resolved.provider
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val controller = remember { MapPocController() }
    val mapState by controller.state.collectAsStateWithLifecycle()
    var reloadToken by remember { mutableIntStateOf(0) }
    var draftLabel by rememberSaveable { mutableStateOf("") }

    val parseResult = remember(query) { parseNavigationDestination(query) }
    val suggestions = remember(query, savedPlaces, recentDestinations) {
        navigationSuggestions(query, savedPlaces, recentDestinations)
    }
    val destination = (parseResult as? DestinationParseResult.Valid)?.destination

    suspend fun reportIfFailed(outcome: NavigationOutcome) {
        if (outcome != NavigationOutcome.OPENED) {
            snackbarHostState.showSnackbar(
                message = navigationFailureMessage(provider.label, outcome),
                withDismissAction = true,
            )
        }
    }

    val handleNavigate: () -> Unit = {
        val target = destination
        if (target != null) {
            scope.launch { reportIfFailed(onNavigate(target)) }
        }
    }
    val handleOpenApp: () -> Unit = {
        scope.launch { reportIfFailed(onOpenNavigationApp()) }
    }
    val handleSave: () -> Unit = {
        val target = destination
        if (target != null) {
            onSavePlace(draftLabel, target)
            draftLabel = ""
        }
    }
    val selectDestination: (NavigationDestination) -> Unit = { target ->
        onQueryChange(navigationDestinationText(target))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        AutoVerseScreen(
            title = "Navigation",
            onBack = onBack,
            scrollable = false,
            modifier = Modifier.padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
            ) {
                SearchCard(
                    provider = provider,
                    preferredMissing = resolved.preferredMissing,
                    query = query,
                    onQueryChange = onQueryChange,
                    parseResult = parseResult,
                    suggestions = suggestions,
                    placesPanelVisible = placesPanelVisible,
                    onTogglePlacesPanel = onTogglePlacesPanel,
                    onChangeProvider = onChangeProvider,
                    onNavigate = handleNavigate,
                    onOpenApp = handleOpenApp,
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val fontScale = LocalDensity.current.fontScale
                    val layout = navigationPlacesLayoutFor(
                        widthDp = maxWidth.value.roundToInt(),
                        heightDp = maxHeight.value.roundToInt(),
                        fontScale = fontScale,
                    )
                    if (layout.sidePanel) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                        ) {
                            MapArea(
                                embeddedMapEnabled = embeddedMapEnabled,
                                providerLabel = provider.label,
                                supportsDestination = provider.supportsDestination,
                                mapState = mapState,
                                onRetry = {
                                    reloadToken += 1
                                    controller.reload()
                                },
                                onOpenApp = handleOpenApp,
                                mapSurface = embeddedMapSurface ?: {
                                    EmbeddedMapSurface(
                                        controller = controller,
                                        reloadToken = reloadToken,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                            if (placesPanelVisible) {
                                PlacesPanel(
                                    savedPlaces = savedPlaces,
                                    recentDestinations = recentDestinations,
                                    saveLabel = draftLabel,
                                    onSaveLabelChange = { draftLabel = it },
                                    canSave = destination != null,
                                    onSave = handleSave,
                                    onRemovePlace = onRemovePlace,
                                    onSelect = selectDestination,
                                    modifier = Modifier
                                        .width(layout.sidePanelWidthDp.dp)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                        ) {
                            MapArea(
                                embeddedMapEnabled = embeddedMapEnabled,
                                providerLabel = provider.label,
                                supportsDestination = provider.supportsDestination,
                                mapState = mapState,
                                onRetry = {
                                    reloadToken += 1
                                    controller.reload()
                                },
                                onOpenApp = handleOpenApp,
                                mapSurface = embeddedMapSurface ?: {
                                    EmbeddedMapSurface(
                                        controller = controller,
                                        reloadToken = reloadToken,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                            if (placesPanelVisible) {
                                PlacesPanel(
                                    savedPlaces = savedPlaces,
                                    recentDestinations = recentDestinations,
                                    saveLabel = draftLabel,
                                    onSaveLabelChange = { draftLabel = it },
                                    canSave = destination != null,
                                    onSave = handleSave,
                                    onRemovePlace = onRemovePlace,
                                    onSelect = selectDestination,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = layout.stackedPanelMaxHeightDp.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCard(
    provider: NavigationProvider,
    preferredMissing: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    parseResult: DestinationParseResult,
    suggestions: List<NavigationSuggestion>,
    placesPanelVisible: Boolean,
    onTogglePlacesPanel: () -> Unit,
    onChangeProvider: () -> Unit,
    onNavigate: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(title = "DESTINATION", icon = Icons.Outlined.Search) {
        if (!provider.supportsDestination) {
            Text(
                text = "This app plans routes inside its own app. Open " +
                    "${provider.label} to enter a destination there.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.size(AutoVerseSpacing.sm))
            PrimaryAction(text = "Open ${provider.label}", onClick = onOpenApp)
        } else {
            val clearAction: (@Composable () -> Unit)? = if (query.isEmpty()) {
                null
            } else {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear destination",
                            modifier = Modifier.size(AutoVerseIcons.compact),
                        )
                    }
                }
            }
            AutoVerseTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "Where to?",
                isError = parseResult is DestinationParseResult.Invalid,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(AutoVerseIcons.compact),
                    )
                },
                trailingIcon = clearAction,
            )
            if (parseResult is DestinationParseResult.Invalid) {
                Spacer(Modifier.size(AutoVerseSpacing.xxs))
                Text(
                    text = destinationFailureMessage(parseResult.reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.danger,
                )
            }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.size(AutoVerseSpacing.xs))
                suggestions.forEach { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        onClick = { onQueryChange(navigationDestinationText(suggestion.destination)) },
                    )
                }
            } else if (query.isNotBlank() && parseResult is DestinationParseResult.Valid) {
                Spacer(Modifier.size(AutoVerseSpacing.xxs))
                Text(
                    text = "No local matches \u2014 your text is handed off as a search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textFaint,
                )
            }
            Spacer(Modifier.size(AutoVerseSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
            ) {
                PrimaryAction(
                    text = "Navigate",
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f),
                    enabled = parseResult is DestinationParseResult.Valid,
                )
                if (provider.supportsOpenApp) {
                    TransportButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open ${provider.label}",
                        onClick = onOpenApp,
                    )
                }
            }
            Spacer(Modifier.size(AutoVerseSpacing.xxs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                Text(
                    text = if (preferredMissing) {
                        "Preferred app isn't installed \u2014 using ${provider.label}"
                    } else {
                        "Preferred \u00b7 ${provider.label}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (preferredMissing) colors.warning else colors.textFaint,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextAction(text = "Change", onClick = onChangeProvider)
                TextAction(
                    text = if (placesPanelVisible) "Hide places panel" else "Show places panel",
                    onClick = onTogglePlacesPanel,
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: NavigationSuggestion,
    onClick: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    val destinationText = navigationDestinationText(suggestion.destination)
    val saved = suggestion.source == NavigationSuggestionSource.SAVED_PLACE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AutoVerseSpacing.touchMin)
            .clip(AutoVerseShapes.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
    ) {
        Icon(
            imageVector = if (saved) Icons.Outlined.Star else Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.inline),
            tint = if (saved) colors.accent else colors.textFaint,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.label,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (suggestion.label != destinationText) {
                Text(
                    text = destinationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MapArea(
    embeddedMapEnabled: Boolean,
    providerLabel: String,
    supportsDestination: Boolean,
    mapState: MapPocState,
    onRetry: () -> Unit,
    onOpenApp: () -> Unit,
    mapSurface: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    if (!embeddedMapEnabled) {
        WidgetContainer(
            title = "NAVIGATION",
            icon = Icons.Outlined.Navigation,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(AutoVerseSpacing.heroChip)
                        .clip(CircleShape)
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(AutoVerseIcons.hero),
                        tint = colors.accent,
                    )
                }
                Spacer(Modifier.size(AutoVerseSpacing.sm))
                Text(
                    text = "Turn-by-turn with $providerLabel",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(AutoVerseSpacing.xs))
                Text(
                    text = if (supportsDestination) {
                        "Type a destination and tap Navigate \u2014 it opens in $providerLabel. " +
                            "The embedded map is off; you can enable it in Settings \u203a Navigation."
                    } else {
                        "This app plans routes inside its own app. " +
                            "The embedded map is off; you can enable it in Settings \u203a Navigation."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(AutoVerseSpacing.md))
                PrimaryAction(text = "Open $providerLabel", onClick = onOpenApp)
            }
        }
    } else {
        WidgetContainer(
            title = "EMBEDDED MAP \u2014 PROTOTYPE",
            subtitle = "MapLibre demo style. Evaluation only \u2014 not a production tile service.",
            icon = Icons.Outlined.Map,
            modifier = modifier,
        ) {
            MapPocContent(
                state = mapState,
                providerLabel = providerLabel,
                onRetry = onRetry,
                onOpenNavigationApp = onOpenApp,
                mapSurface = mapSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlacesPanel(
    savedPlaces: List<SavedPlace>,
    recentDestinations: List<RecentDestination>,
    saveLabel: String,
    onSaveLabelChange: (String) -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
    onRemovePlace: (SavedPlace) -> Unit,
    onSelect: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    WidgetContainer(
        title = "PLACES",
        icon = Icons.Outlined.Place,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel(icon = Icons.Outlined.Star, label = "SAVED PLACES")
            if (savedPlaces.isEmpty()) {
                EmptyHint(
                    text = "Nothing saved yet. Type a destination above, then tap Save.",
                )
            } else {
                savedPlaces.forEach { place ->
                    SavedPlaceRow(
                        place = place,
                        onSelect = { onSelect(place.destination) },
                        onRemove = { onRemovePlace(place) },
                    )
                }
            }
            Spacer(Modifier.size(AutoVerseSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
            ) {
                AutoVerseTextField(
                    value = saveLabel,
                    onValueChange = onSaveLabelChange,
                    placeholder = "Label (optional)",
                    modifier = Modifier.weight(1f),
                )
                PrimaryAction(
                    text = "Save",
                    onClick = onSave,
                    enabled = canSave,
                )
            }
            Spacer(Modifier.size(AutoVerseSpacing.sm))
            SectionLabel(icon = Icons.Outlined.History, label = "RECENT DESTINATIONS")
            if (recentDestinations.isEmpty()) {
                EmptyHint(
                    text = "Destinations you hand off will appear here.",
                )
            } else {
                recentDestinations.forEach { recent ->
                    RecentDestinationRow(
                        destination = recent.destination,
                        onSelect = { onSelect(recent.destination) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPlaceRow(
    place: SavedPlace,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    val destinationText = navigationDestinationText(place.destination)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AutoVerseSpacing.touchMin)
            .clip(AutoVerseShapes.tile)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
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
    onSelect: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AutoVerseSpacing.touchMin)
            .clip(AutoVerseShapes.tile)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
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
