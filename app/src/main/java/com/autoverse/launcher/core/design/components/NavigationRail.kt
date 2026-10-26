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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
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
enum class RailDestination { HOME, APPS, MAPS, NAVIGATION, SPEEDOMETER, OBD, DIAGNOSTICS, SETTINGS }

/**
 * Total height the labeled rail needs (logo + seven items + paddings). Below
 * it the rail switches to icon-only items instead of clipping labels.
 */
private val LABELED_RAIL_MIN_HEIGHT = 392.dp

/** One rail destination: icon, label, click target, selection. */
private data class RailEntry(
    val destination: RailDestination,
    val icon: ImageVector,
    val label: String,
)

private val RAIL_ENTRIES = listOf(
    RailEntry(RailDestination.HOME, Icons.Outlined.Home, "Home"),
    RailEntry(RailDestination.APPS, Icons.Outlined.Apps, "Apps"),
    RailEntry(RailDestination.MAPS, Icons.Outlined.Map, "Maps"),
    RailEntry(RailDestination.SPEEDOMETER, Icons.Outlined.Speed, "Speed"),
    RailEntry(RailDestination.OBD, Icons.Outlined.Sensors, "OBD"),
    RailEntry(RailDestination.DIAGNOSTICS, Icons.Outlined.Memory, "Diagnostics"),
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
            val labeled = maxHeight >= LABELED_RAIL_MIN_HEIGHT
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
            ) {
                BrandMark()
                Spacer(Modifier.height(AutoVerseSpacing.xxs))
                RAIL_ENTRIES.forEach { entry ->
                    val onClick = when (entry.destination) {
                        RailDestination.HOME -> onOpenHome
                        RailDestination.APPS -> onOpenAppDrawer
                        RailDestination.MAPS, RailDestination.NAVIGATION -> onOpenNavigation
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
                                selected == RailDestination.NAVIGATION),
                        labeled = labeled,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}

/**
 * Small brand mark at the top of the rail — the logo only, no wordmark. The
 * plate keeps the brand's own charcoal so the chrome half of the mark stays
 * legible in every theme.
 */
@Composable
private fun BrandMark() {
    Image(
        painter = painterResource(R.drawable.autoverse_brand_mark),
        contentDescription = "AutoVerse",
        modifier = Modifier
            .padding(top = AutoVerseSpacing.xxs)
            .size(28.dp),
    )
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean,
    labeled: Boolean,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.standard),
            tint = if (selected) colors.accent else colors.textSecondary,
        )
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
