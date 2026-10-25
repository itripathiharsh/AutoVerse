package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.icons.IconBackgroundStyle
import com.autoverse.launcher.core.design.icons.IconBorderStyle
import com.autoverse.launcher.core.design.icons.IconGlow
import com.autoverse.launcher.core.design.icons.IconSelectedState
import com.autoverse.launcher.core.design.icons.LocalAutoVerseIconSpec
import com.autoverse.launcher.core.design.icons.toShape
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.performance.LocalPerformanceSpec

/**
 * Phase B: the themed container for installed-application icons. The real
 * PackageManager bitmap stays untouched inside; the ACTIVE THEME supplies
 * the container shape, background, border and selected treatment around it
 * (theme PRD §4.5). Falls back to a glyph when no bitmap is available.
 *
 * Selected states are structural — EDGE (accent border), RING (thicker
 * accent border), INDICATOR (dot below the container) — never color-only.
 * Glow renders only on the Enhanced workload profile; Constrained hardware
 * gets flat containers.
 */
@Composable
fun ThemedAppIcon(
    bitmap: ImageBitmap?,
    size: Dp,
    modifier: Modifier = Modifier,
    fallback: ImageVector? = null,
    contentDescription: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val spec = LocalAutoVerseIconSpec.current
    val colors = LocalAutoVerseColors.current
    val performance = LocalPerformanceSpec.current
    val shape = spec.containerShape.toShape()

    val backgroundColor = when (spec.backgroundStyle) {
        IconBackgroundStyle.TINTED -> colors.accentSoft
        IconBackgroundStyle.RAISED -> colors.surfaceRaised
        IconBackgroundStyle.INK -> colors.backgroundTop
    }
    val baseBorder = when (spec.borderStyle) {
        IconBorderStyle.NONE -> null
        IconBorderStyle.ACCENT -> colors.accent
        IconBorderStyle.OUTLINE -> colors.outlineStrong
    }
    val borderColor: Color = when {
        selected && spec.selectedState != IconSelectedState.INDICATOR -> colors.accent
        baseBorder != null -> baseBorder
        else -> Color.Transparent
    }
    val borderWidth = when {
        selected && spec.selectedState == IconSelectedState.RING -> spec.borderWidthDp + 1f
        baseBorder == null && !(selected && spec.selectedState != IconSelectedState.INDICATOR) -> 0f
        else -> spec.borderWidthDp
    }
    val glowElevation = when {
        performance.profile == PerformanceProfile.CONSTRAINED -> 0.dp
        selected && spec.glow == IconGlow.LUMINOUS -> 10.dp
        spec.glow == IconGlow.LUMINOUS -> 6.dp
        spec.glow == IconGlow.RESTRAINED -> 3.dp
        else -> 0.dp
    }
    val contentPadding = (size.value * 0.14f * (1.15f - spec.iconScale)).dp + 3.dp

    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.55f)
            .shadowIf(glowElevation, shape)
            .clip(shape)
            .background(if (enabled) backgroundColor else colors.disabledContainer)
            .then(
                if (borderWidth > 0f) {
                    Modifier.border(borderWidth.dp, borderColor, shape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        } else {
            Icon(
                imageVector = fallback ?: Icons.Outlined.Apps,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                tint = colors.textFaint,
            )
        }
        if (selected && spec.selectedState == IconSelectedState.INDICATOR) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-2).dp)
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(colors.accent),
            )
        }
    }
}

private fun Modifier.shadowIf(elevation: Dp, shape: androidx.compose.ui.graphics.Shape): Modifier =
    if (elevation > 0.dp) this.shadow(elevation = elevation, shape = shape) else this
