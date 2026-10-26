package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.navigation.DestinationParseResult
import com.autoverse.launcher.core.navigation.MapsStatus
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.RecentDestination
import com.autoverse.launcher.core.navigation.destinationFailureMessage
import com.autoverse.launcher.core.navigation.navigationDestinationText
import com.autoverse.launcher.core.navigation.parseNavigationDestination

/**
 * The map region on Home (60%) and Maps (70%): a **live, GPU-rendered
 * OpenStreetMap map** (MapLibre Native + free OpenFreeMap vector tiles — no
 * API key, no billing, commercial use allowed) following the car, plus
 * destination entry and the turn-by-turn handoff. The live map is OSM, not
 * Google; turn-by-turn opens in Google Maps (or the user's preferred app),
 * and the panel says exactly that.
 */
@Composable
fun MapsLaunchPanel(
    mapsStatus: MapsStatus,
    destinationTargetLabel: String,
    destinationQuery: String,
    onDestinationQueryChange: (String) -> Unit,
    onNavigate: (NavigationDestination) -> Unit,
    onOpenMaps: () -> Unit,
    modifier: Modifier = Modifier,
    recentDestinations: List<RecentDestination> = emptyList(),
    compact: Boolean = false,
    speedometerData: SpeedometerData = SpeedometerData(),
    hasLocationPermission: Boolean = true,
) {
    val colors = LocalAutoVerseColors.current
    val parseResult = remember(destinationQuery) { parseNavigationDestination(destinationQuery) }
    val mapsMissing = mapsStatus == MapsStatus.NOT_INSTALLED

    CockpitCard(
        modifier = modifier,
        contentPadding = PaddingValues(AutoVerseSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(icon = Icons.Outlined.Map, label = "MAP", modifier = Modifier.weight(1f))
            when (mapsStatus) {
                MapsStatus.INSTALLED -> StatusChip(label = "Handoff ready", tone = StatusTone.SUCCESS, leadingDot = true)
                MapsStatus.NOT_INSTALLED -> StatusChip(label = "Maps not installed", tone = StatusTone.WARNING, leadingDot = true)
                MapsStatus.UNKNOWN -> Unit
            }
        }

        Spacer(Modifier.size(AutoVerseSpacing.xs))

        // The live, free, GPU-rendered map. Weighted so it grows with the
        // region; the minimum keeps it useful on short panels.
        LiveMap(
            speedometerData = speedometerData,
            hasLocationPermission = hasLocationPermission,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 140.dp),
        )

        Spacer(Modifier.size(AutoVerseSpacing.xs))
        Text(
            text = if (mapsMissing) {
                "Live map \u00A9 OpenStreetMap \u00b7 install Google Maps for turn-by-turn"
            } else {
                "Live map \u00A9 OpenStreetMap \u00b7 turn-by-turn opens in $destinationTargetLabel"
            },
            style = MaterialTheme.typography.labelMedium,
            color = colors.textFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xs))

        PrimaryAction(
            text = "Open Google Maps",
            onClick = onOpenMaps,
            enabled = !mapsMissing,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
        ) {
            DestinationField(
                value = destinationQuery,
                onValueChange = onDestinationQueryChange,
                isError = parseResult is DestinationParseResult.Invalid,
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(
                text = "Navigate",
                onClick = {
                    (parseResult as? DestinationParseResult.Valid)?.let { onNavigate(it.destination) }
                },
                enabled = parseResult is DestinationParseResult.Valid,
            )
        }
        val hint = when {
            parseResult is DestinationParseResult.Invalid -> destinationFailureMessage(parseResult.reason)
            destinationTargetLabel != GOOGLE_MAPS_LABEL -> "Destinations open in $destinationTargetLabel"
            else -> null
        }
        if (hint != null) {
            Spacer(Modifier.size(AutoVerseSpacing.xxs))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = if (parseResult is DestinationParseResult.Invalid) colors.danger else colors.textFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (recentDestinations.isNotEmpty() && !compact) {
            RecentDestinationChips(
                recents = recentDestinations,
                onPick = onNavigate,
            )
        }
    }
}

private const val GOOGLE_MAPS_LABEL = "Google Maps"
private const val MAX_RECENT_CHIPS = 4

/** One-tap handoff to a destination the user actually navigated to before. */
@Composable
private fun RecentDestinationChips(
    recents: List<RecentDestination>,
    onPick: (NavigationDestination) -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    LazyRow(
        modifier = Modifier.padding(top = AutoVerseSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        items(recents.take(MAX_RECENT_CHIPS), key = { navigationDestinationText(it.destination) }) { recent ->
            Row(
                modifier = Modifier
                    .clip(AutoVerseShapes.pill)
                    .background(colors.surfaceHighlight)
                    .selectable(selected = false, role = Role.Button, onClick = { onPick(recent.destination) })
                    .heightIn(min = AutoVerseSpacing.touchMin)
                    .padding(horizontal = AutoVerseSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    modifier = Modifier.size(AutoVerseIcons.inline),
                    tint = colors.textFaint,
                )
                Text(
                    text = navigationDestinationText(recent.destination),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp),
                )
            }
        }
    }
}
