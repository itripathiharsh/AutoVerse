package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/**
 * Dock / shortcut tile: themed icon container above a centered label, on a
 * tap-enabled tile-shaped [CockpitCard]. Accepts either an [ImageVector]
 * (system action) or an [ImageBitmap] (real app icon); image wins when both
 * are set. The icon container is themed by the active [ThemeIconSpec] —
 * shape, border and selected treatment come from the theme, the artwork is
 * never recolored. Labels truncate rather than overflow.
 */
@Composable
fun AppDockItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    image: ImageBitmap? = null,
    enabled: Boolean = true,
) {
    val colors = LocalAutoVerseColors.current
    CockpitCard(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = AutoVerseShapes.tile,
        contentPadding = PaddingValues(
            horizontal = AutoVerseSpacing.sm,
            vertical = AutoVerseSpacing.md,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
        ) {
            ThemedAppIcon(
                bitmap = image,
                fallback = icon ?: Icons.Outlined.Apps,
                size = AutoVerseSpacing.iconChip,
                contentDescription = null,
                enabled = enabled,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) colors.textPrimary else colors.disabledContent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
