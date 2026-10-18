package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/**
 * Dock / shortcut tile: circular icon chip above a centered label, on a
 * tap-enabled tile-shaped [CockpitCard]. Accepts either an [ImageVector]
 * (system action) or an [ImageBitmap] (real app icon, clipped to the chip);
 * image wins when both are set. Labels truncate rather than overflow.
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
            Box(
                modifier = Modifier
                    .size(AutoVerseSpacing.iconChip)
                    .clip(CircleShape)
                    .background(colors.surfaceHighlight),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = icon ?: Icons.Outlined.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(AutoVerseIcons.compact),
                        tint = if (enabled) colors.accent else colors.disabledContent,
                    )
                }
            }
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
