package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/** Top-level destinations the rail marks as selected. */
enum class RailDestination { HOME, APPS, NAVIGATION, DIAGNOSTICS, SETTINGS }

/**
 * Phase 15 — shared left navigation rail: brand plate plus the top-level
 * destinations. Extracted from the Home implementation so the app
 * drawer reuses the identical rail instead of a drifting copy; each host
 * passes its current destination and only that item reports selection.
 * Phase 16: Settings joins Home and Apps as a selectable destination.
 * Phase 17: Diagnostics joins the rail.
 * Phase 18: Navigation becomes a selectable page destination — hosts
 * pass [onOpenNavigation] to switch pages instead of launching the
 * external app, and only the current destination reports selection.
 */
@Composable
fun AppNavigationRail(
    selected: RailDestination,
    onOpenHome: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CockpitCard(
        modifier = modifier,
        contentPadding = PaddingValues(AutoVerseSpacing.sm),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
        ) {
            BrandPlate(logoHeight = 32.dp)
            Spacer(Modifier.height(AutoVerseSpacing.xs))
            RailItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                selected = selected == RailDestination.HOME,
                onClick = onOpenHome,
            )
            RailItem(
                icon = Icons.Outlined.Apps,
                label = "Apps",
                selected = selected == RailDestination.APPS,
                onClick = onOpenAppDrawer,
            )
            RailItem(
                icon = Icons.Outlined.Navigation,
                label = "Navigation",
                selected = selected == RailDestination.NAVIGATION,
                onClick = onOpenNavigation,
            )
            RailItem(
                icon = Icons.Outlined.MonitorHeart,
                label = "Diagnostics",
                selected = selected == RailDestination.DIAGNOSTICS,
                onClick = onOpenDiagnostics,
            )
            RailItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                selected = selected == RailDestination.SETTINGS,
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.tile)
            .background(if (selected) colors.surfaceHighlight else Color.Transparent)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .heightIn(min = AutoVerseSpacing.touchComfort)
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
        Spacer(Modifier.height(AutoVerseSpacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
