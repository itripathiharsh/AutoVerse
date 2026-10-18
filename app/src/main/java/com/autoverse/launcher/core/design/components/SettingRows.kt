package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/** Read-only label/value row for diagnostics-style data. */
@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AutoVerseSpacing.touchMin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
        )
    }
}

/**
 * Single-choice row with radio affordance and a disabled state. The whole row
 * is the target and merges into one accessibility node reporting
 * checked/selected state; the RadioButton itself is not separately focusable.
 * [leading] optionally renders an extra visual (e.g. a theme swatch) before
 * the radio button; it is decorative and must not add interaction.
 */
@Composable
fun SelectRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.row)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .defaultMinSize(minHeight = AutoVerseSpacing.touchMin)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.inlineGap),
    ) {
        if (leading != null) {
            leading()
        }
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) colors.textPrimary else colors.disabledContent,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) colors.textSecondary else colors.disabledContent,
            )
        }
    }
}

/**
 * Label + description row with a trailing switch. The whole row toggles, so
 * the target is the full row width; the Switch is display-only and the row
 * merges into a single accessibility node with switch semantics.
 */
@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.row)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .defaultMinSize(minHeight = AutoVerseSpacing.touchComfort),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) colors.textPrimary else colors.disabledContent,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) colors.textSecondary else colors.disabledContent,
            )
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/**
 * Phase 16 — label + description row with a live value readout and a
 * full-width slider underneath (used for the user text scale). The slider is
 * the only interactive element and keeps its native progress semantics; the
 * readout text mirrors the value visually.
 */
@Composable
fun SliderRow(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    stepCount: Int = 0,
) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AutoVerseSpacing.xxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                color = colors.accent,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            steps = stepCount,
        )
    }
}
