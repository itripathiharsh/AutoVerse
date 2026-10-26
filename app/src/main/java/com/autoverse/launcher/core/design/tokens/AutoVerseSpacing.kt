package com.autoverse.launcher.core.design.tokens

import androidx.compose.ui.unit.dp

/**
 * Spacing scale (base-4) plus semantic spacing and touch-target tokens.
 * Components and screens never hard-code dp paddings — they use these.
 */
object AutoVerseSpacing {
    // Base scale
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    // Semantic layout spacing
    val screenHorizontal = lg
    val screenVertical = md
    val cardPadding = lg
    val cardPaddingDense = md
    val stackGap = sm
    val gridGap = sm
    val inlineGap = xs
    val chipHorizontal = sm
    val chipVertical = xs

    // Home rail (compact, fixed-width; shared by every screen)
    val railWidth = 76.dp

    // Touch targets (car UI: comfortable minimums)
    val touchMin = 48.dp
    val touchComfort = 56.dp
    val transportPrimary = 64.dp
    val transportSecondary = 52.dp
    val iconChip = 36.dp
    val heroChip = 84.dp
}
