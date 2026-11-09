package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.R
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/** Top-level destinations the rail marks as selected. */
enum class RailDestination { HOME, APPS, MAPS, NAVIGATION, MUSIC, CAR, SPEEDOMETER, OBD, DIAGNOSTICS, SETTINGS }

/**
 * Opens the Car page (speed, trips, OBD, reminders, speaker test, head-unit
 * check) from any rail; provided at the app root like [LocalOpenMusic].
 */
val LocalOpenCar = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Opens the Music page from any screen's rail. Provided once at the app
 * root so the seven screens that host the rail need no extra parameter;
 * null (previews) hides the entry.
 */
val LocalOpenMusic = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * What the rail's header shows on every screen: the time and outside
 * temperature (replacing the old top bar, so the map gets that height), a
 * dot on Car when a renewal/service is due, and the Edit-home action that
 * appears at the foot of the rail on Home. Provided once at the app root;
 * dynamic, so a clock tick recomposes only the rail header.
 */
data class RailStatus(
    val clock: String = "",
    val temperature: String? = null,
    val carAlert: Boolean = false,
    val onEditHome: (() -> Unit)? = null,
)

val LocalRailStatus = compositionLocalOf { RailStatus() }

/**
 * Total height the labeled rail needs (logo + eight items + paddings). Below
 * it the rail switches to icon-only items instead of clipping labels.
 */
private val LABELED_RAIL_MIN_HEIGHT = 600.dp // measured: 9 labeled items need ~590dp

/** One rail destination: icon, label, click target, selection. */
private data class RailEntry(
    val destination: RailDestination,
    val icon: ImageVector,
    val label: String,
)

/**
 * Driver-facing destinations only. Speed, OBD and the head-unit check live
 * on the Car page; Diagnostics is reached from Settings — not the main menu.
 * (Previews without a Car page keep the old Speed/OBD entries.)
 */
private val RAIL_ENTRIES = listOf(
    RailEntry(RailDestination.HOME, Icons.Outlined.Home, "Home"),
    RailEntry(RailDestination.APPS, Icons.Outlined.Apps, "Apps"),
    RailEntry(RailDestination.MAPS, Icons.Outlined.Map, "Maps"),
    RailEntry(RailDestination.MUSIC, Icons.Outlined.LibraryMusic, "Music"),
    RailEntry(RailDestination.CAR, Icons.Outlined.DirectionsCar, "Car"),
    RailEntry(RailDestination.SPEEDOMETER, Icons.Outlined.Speed, "Speed"),
    RailEntry(RailDestination.OBD, Icons.Outlined.Sensors, "OBD"),
    RailEntry(RailDestination.SETTINGS, Icons.Outlined.Settings, "Settings"),
)

/**
 * The shared left navigation rail: a small brand mark on top, then every
 * top-level destination in a fixed order — Home, Apps, Maps, Speedometer,
 * OBD, Diagnostics, Settings. Fixed compact width, uniform item metrics, and
 * no vertical wordmark. On short windows the items drop their labels (the
 * icon size and touch target are unchanged and the label stays in the
 * semantics) so no destination is ever clipped out of reach.
 */
@Composable
fun AppNavigationRail(
    selected: RailDestination,
    onOpenHome: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDiagnostics: () -> Unit = {},
    onOpenSpeedometer: () -> Unit = {},
    onOpenObd: () -> Unit = {},
) {
    CockpitCard(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = AutoVerseSpacing.sm),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
            // ~66dp per labeled item plus the logo; drop labels only when they can't all fit.
            val itemCount = if (LocalOpenCar.current != null) 6 else RAIL_ENTRIES.size
            val labeled = maxHeight >= (48 + itemCount * 66).dp || maxHeight >= LABELED_RAIL_MIN_HEIGHT
            Column(
                // Scrolls as a last resort (very short screens, large text) so no
                // destination is ever cut off out of reach.
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
            ) {
                val status = LocalRailStatus.current
                if (status.clock.isNotBlank()) RailClock(status) else BrandMark()
                Spacer(Modifier.height(AutoVerseSpacing.xs))
                val openMusic = LocalOpenMusic.current
                val openCar = LocalOpenCar.current
                RAIL_ENTRIES.forEach { entry ->
                    if (entry.destination == RailDestination.MUSIC && openMusic == null) return@forEach
                    if (entry.destination == RailDestination.CAR && openCar == null) return@forEach
                    // With a Car page, Speed and OBD are reached from it, not the menu.
                    if (openCar != null && (entry.destination == RailDestination.SPEEDOMETER || entry.destination == RailDestination.OBD)) return@forEach
                    val onClick = when (entry.destination) {
                        RailDestination.HOME -> onOpenHome
                        RailDestination.APPS -> onOpenAppDrawer
                        RailDestination.MAPS, RailDestination.NAVIGATION -> onOpenNavigation
                        RailDestination.MUSIC -> openMusic ?: {}
                        RailDestination.CAR -> openCar ?: {}
                        RailDestination.SPEEDOMETER -> onOpenSpeedometer
                        RailDestination.OBD -> onOpenObd
                        RailDestination.DIAGNOSTICS -> onOpenDiagnostics
                        RailDestination.SETTINGS -> onOpenSettings
                    }
                    RailItem(
                        icon = entry.icon,
                        label = entry.label,
                        selected = selected == entry.destination ||
                            (entry.destination == RailDestination.MAPS &&
                                selected == RailDestination.NAVIGATION) ||
                            (entry.destination == RailDestination.CAR && openCar != null &&
                                selected in setOf(RailDestination.SPEEDOMETER, RailDestination.OBD, RailDestination.DIAGNOSTICS)),
                        labeled = labeled,
                        onClick = onClick,
                        alert = entry.destination == RailDestination.CAR && status.carAlert,
                    )
                }
                val editHome = status.onEditHome
                if (selected == RailDestination.HOME && editHome != null) {
                    Spacer(Modifier.height(AutoVerseSpacing.sm))
                    RailItem(
                        icon = Icons.Outlined.Edit,
                        label = "Edit",
                        selected = false,
                        labeled = labeled,
                        onClick = editHome,
                    )
                }
            }
        }
    }
}

/** Time (large, tabular) with AM/PM and the temperature beneath, at the head of the rail. */
@Composable
private fun RailClock(status: RailStatus) {
    val colors = LocalAutoVerseColors.current
    val time = status.clock.substringBefore(' ')
    val period = status.clock.substringAfter(' ', "").uppercase()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = AutoVerseSpacing.xxs),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            maxLines = 1,
        )
        val sub = listOfNotNull(period.takeIf { it.isNotBlank() }, status.temperature).joinToString("  ")
        if (sub.isNotBlank()) {
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

/** Small transparent brand mark at the top of the rail — the logo only, no wordmark. */
@Composable
private fun BrandMark() {
    BrandMark(height = 20.dp, modifier = Modifier.padding(top = AutoVerseSpacing.xxs))
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean,
    labeled: Boolean,
    alert: Boolean = false,
) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.tile)
            .background(if (selected) colors.surfaceHighlight else Color.Transparent)
            .then(if (labeled) Modifier else Modifier.semantics { contentDescription = label })
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .heightIn(min = AutoVerseSpacing.touchMin)
            .padding(vertical = AutoVerseSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.foundation.layout.Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AutoVerseIcons.standard),
                tint = if (selected) colors.accent else colors.textSecondary,
            )
            if (alert) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(9.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(colors.warning),
                )
            }
        }
        if (labeled) {
            Spacer(Modifier.height(AutoVerseSpacing.xxs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
