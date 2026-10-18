package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseBorders
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

enum class StatusTone { NEUTRAL, SUCCESS, WARNING, DANGER }

/**
 * Pill-shaped status indicator. The tone colors the leading icon or dot;
 * the label stays on the text-secondary token for consistent legibility.
 */
@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: StatusTone = StatusTone.NEUTRAL,
    leadingDot: Boolean = false,
) {
    val colors = LocalAutoVerseColors.current
    val markerColor = when (tone) {
        StatusTone.NEUTRAL -> colors.textSecondary
        StatusTone.SUCCESS -> colors.success
        StatusTone.WARNING -> colors.warning
        StatusTone.DANGER -> colors.danger
    }
    Row(
        modifier = modifier
            .clip(AutoVerseShapes.pill)
            .background(colors.surface)
            .border(AutoVerseBorders.hairline, colors.outline, AutoVerseShapes.pill)
            .padding(
                horizontal = AutoVerseSpacing.chipHorizontal,
                vertical = AutoVerseSpacing.chipVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.inlineGap),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AutoVerseIcons.inline),
                tint = markerColor,
            )
        }
        if (leadingDot) {
            Box(
                modifier = Modifier
                    .size(AutoVerseIcons.dot)
                    .clip(CircleShape)
                    .background(markerColor),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
