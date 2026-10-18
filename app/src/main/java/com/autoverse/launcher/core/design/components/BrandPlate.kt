package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.R
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/**
 * The logo's native charcoal, kept constant on purpose: the wordmark is a
 * light outline and would vanish on day themes, so the plate reproduces the
 * artwork's own background in every theme and appearance instead of tinting
 * or recolouring it.
 */
private val BrandPlateColor = Color(0xFF141B22)

/**
 * Dark brand plate for the AutoVerse logo. Renders the real logo asset at
 * [logoHeight] (aspect preserved) inside a fixed charcoal tile so it stays
 * legible and on-brand across all themes.
 */
@Composable
fun BrandPlate(
    modifier: Modifier = Modifier,
    logoHeight: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .clip(AutoVerseShapes.small)
            .background(BrandPlateColor)
            .padding(
                horizontal = AutoVerseSpacing.sm,
                vertical = AutoVerseSpacing.xs,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.autoverse_logo),
            contentDescription = "AutoVerse",
            modifier = Modifier.height(logoHeight),
            contentScale = ContentScale.Fit,
        )
    }
}
