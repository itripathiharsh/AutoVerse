package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseBorders
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.performance.LocalPerformanceSpec

/**
 * The base AutoVerse surface: gently layered gradient, hairline border,
 * optional tap with an explicit pressed state (border brightens to accent).
 * The Constrained workload swaps the gradient for a flat fill.
 */
@Composable
fun CockpitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = AutoVerseShapes.card,
    contentPadding: PaddingValues = PaddingValues(AutoVerseSpacing.cardPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    val spec = LocalPerformanceSpec.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Borderless: the panel separates from the near-black background by tone.
    // A barely-there top-down sheen gives depth on capable units.
    val surface = if (spec.flatSurfaces) {
        Modifier.background(colors.surface)
    } else {
        Modifier.background(Brush.verticalGradient(listOf(colors.surfaceRaised, colors.surface)))
    }

    val base = modifier
        .clip(shape)
        .then(surface)

    val clickable = if (onClick != null) {
        base.clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactionSource,
            indication = if (spec.flatSurfaces) null else ripple(),
            onClick = onClick,
        )
    } else {
        base
    }

    Column(
        modifier = clickable
            .then(
                if (onClick != null && enabled && pressed) {
                    Modifier.border(AutoVerseBorders.emphasis, colors.accent, shape)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}
