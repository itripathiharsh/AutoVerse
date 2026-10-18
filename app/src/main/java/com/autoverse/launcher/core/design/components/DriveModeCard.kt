package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseBorders
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.model.PerformanceProfile

/**
 * Drive Mode: the real rendering-workload selector, mirroring the profile
 * choice in Settings. Every option is a factual setting (no invented vehicle
 * data); the current [selected] profile is marked and one tap switches it.
 */
@Composable
fun DriveModeCard(
    selected: PerformanceProfile,
    modifier: Modifier = Modifier,
    onSelect: (PerformanceProfile) -> Unit = {},
) {
    WidgetContainer(title = "DRIVE MODE", icon = Icons.Outlined.Speed, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs)) {
            PerformanceProfile.entries.forEach { profile ->
                DriveModeOption(
                    label = profile.displayName,
                    description = profileDescription(profile),
                    selected = profile == selected,
                    onSelect = { onSelect(profile) },
                )
            }
        }
    }
}

@Composable
private fun DriveModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.tile)
            .background(if (selected) colors.surfaceHighlight else Color.Transparent)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .heightIn(min = AutoVerseSpacing.touchMin)
            .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(AutoVerseSpacing.sm)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent)
                .border(
                    width = AutoVerseBorders.hairline,
                    color = if (selected) colors.accent else colors.outlineStrong,
                    shape = CircleShape,
                ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textFaint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun profileDescription(profile: PerformanceProfile): String = when (profile) {
    PerformanceProfile.CONSTRAINED -> "Minimal motion, lowest overhead"
    PerformanceProfile.BALANCED -> "Smooth with sensible effects"
    PerformanceProfile.ENHANCED -> "Full motion and richer live surfaces"
}
