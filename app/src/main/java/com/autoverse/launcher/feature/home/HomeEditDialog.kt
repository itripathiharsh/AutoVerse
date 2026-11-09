package com.autoverse.launcher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.model.HomeLayout
import com.autoverse.launcher.core.model.HomeMapSize

/**
 * Edit home: switch each Home panel on or off, put the map left or right,
 * and pick its size. Every tap applies immediately so the driver sees the
 * result behind the panel; "Done" closes it. At least one panel stays on.
 */
@Composable
fun HomeEditDialog(
    layout: HomeLayout,
    onChange: (HomeLayout) -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Dialog(onDismissRequest = onDone) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(AutoVerseShapes.small)
                .background(colors.surface)
                .padding(AutoVerseSpacing.lg),
        ) {
            Text("Edit home", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Text(
                "Choose what you see on Home. Changes apply right away.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(AutoVerseSpacing.md))

            val onlyOneLeft = listOf(layout.showMap, layout.showMusic, layout.showSpeed).count { it } == 1
            PanelSwitch(Icons.Outlined.Map, "Map & navigation", layout.showMap, enabled = !(onlyOneLeft && layout.showMap)) {
                onChange(layout.copy(showMap = it))
            }
            PanelSwitch(Icons.Outlined.MusicNote, "Music", layout.showMusic, enabled = !(onlyOneLeft && layout.showMusic)) {
                onChange(layout.copy(showMusic = it))
            }
            PanelSwitch(Icons.Outlined.Speed, "Speed & calls", layout.showSpeed, enabled = !(onlyOneLeft && layout.showSpeed)) {
                onChange(layout.copy(showSpeed = it))
            }

            if (layout.showMap && (layout.showMusic || layout.showSpeed)) {
                Spacer(Modifier.height(AutoVerseSpacing.md))
                Text("Map side", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Spacer(Modifier.height(AutoVerseSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
                    Choice("Left", !layout.mapOnRight) { onChange(layout.copy(mapOnRight = false)) }
                    Choice("Right", layout.mapOnRight) { onChange(layout.copy(mapOnRight = true)) }
                }
                Spacer(Modifier.height(AutoVerseSpacing.md))
                Text("Map size", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Spacer(Modifier.height(AutoVerseSpacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
                    HomeMapSize.entries.forEach { size ->
                        Choice(size.displayName, layout.mapSize == size) { onChange(layout.copy(mapSize = size)) }
                    }
                }
            }

            Spacer(Modifier.height(AutoVerseSpacing.lg))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextAction(text = "Reset", onClick = { onChange(HomeLayout()) })
                Spacer(Modifier.weight(1f))
                PrimaryAction(text = "Done", onClick = onDone, modifier = Modifier.width(160.dp))
            }
        }
    }
}

@Composable
private fun PanelSwitch(icon: ImageVector, label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AutoVerseSpacing.touchComfort)
            .clickable(enabled = enabled) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(AutoVerseSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            if (!enabled) {
                Text("At least one panel stays on", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.accent),
        )
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.onAccent else colors.textPrimary,
        modifier = Modifier
            .clip(AutoVerseShapes.pill)
            .background(if (selected) colors.accent else colors.surfaceHighlight)
            .border(1.dp, if (selected) colors.accent else colors.outline, AutoVerseShapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = AutoVerseSpacing.lg, vertical = AutoVerseSpacing.sm),
    )
}
