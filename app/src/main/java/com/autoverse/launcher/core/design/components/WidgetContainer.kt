package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/** Accent icon + spaced label, used to title every major card/section. */
@Composable
fun SectionLabel(
    icon: ImageVector?,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.inlineGap),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AutoVerseIcons.inline),
                tint = colors.accent,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Labeled surface section. This is the standard container for any widget or
 * settings group; Phase 3 adds an explicit fallback for widgets that cannot fit.
 *
 * Home panels pass [onToggleCollapsed] to get a plain expand/collapse chevron
 * in the header. A collapsed panel renders the header row only — the body is
 * removed from composition immediately (no animation, no animated sizing) and
 * the layout reflows around it on the same frame.
 */
@Composable
fun WidgetContainer(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    collapsed: Boolean = false,
    onToggleCollapsed: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AutoVerseSpacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    CockpitCard(modifier = modifier, onClick = onClick, contentPadding = contentPadding) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.inlineGap),
        ) {
            SectionLabel(icon = icon, label = title, modifier = Modifier.weight(1f, fill = false))
            if (onToggleCollapsed != null) {
                IconButton(onClick = onToggleCollapsed) {
                    Icon(
                        imageVector = if (collapsed) {
                            Icons.Outlined.KeyboardArrowDown
                        } else {
                            Icons.Outlined.KeyboardArrowUp
                        },
                        contentDescription = if (collapsed) {
                            "Expand $title"
                        } else {
                            "Collapse $title"
                        },
                        tint = colors.textSecondary,
                    )
                }
            }
        }
        if (collapsed) return@CockpitCard
        if (subtitle != null) {
            Spacer(Modifier.size(AutoVerseSpacing.xxs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textFaint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        content()
    }
}
