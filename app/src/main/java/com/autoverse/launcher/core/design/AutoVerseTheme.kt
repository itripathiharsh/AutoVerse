package com.autoverse.launcher.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.autoverse.launcher.core.design.icons.LocalAutoVerseIconSpec
import com.autoverse.launcher.core.design.icons.themeIconSpecFor
import com.autoverse.launcher.core.design.tokens.AutoVerseTypography
import com.autoverse.launcher.core.design.tokens.LocalAutoVerseMotion
import com.autoverse.launcher.core.design.tokens.motionSpecFor
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId

/**
 * Applies the active [ThemeSpec] tokens. Theme or appearance changes only
 * recompose the tree below this node — nothing is recreated.
 */
@Composable
fun AutoVerseTheme(
    themeId: AutoVerseThemeId = AutoVerseThemeId.COBALT,
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val spec = themeSpecFor(themeId)
    val darkTheme = isDarkTheme(appearanceMode, isSystemInDarkTheme())
    val colors = spec.colorsFor(darkTheme)
    val motion = motionSpecFor(reducedMotion)

    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.textSecondary,
            background = colors.backgroundTop,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.outline,
            error = colors.danger,
            onError = colors.onAccent,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.textSecondary,
            background = colors.backgroundTop,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.outline,
            error = colors.danger,
            onError = colors.onAccent,
        )
    }

    CompositionLocalProvider(
        LocalAutoVerseColors provides colors,
        LocalAutoVerseMotion provides motion,
        LocalAutoVerseIconSpec provides themeIconSpecFor(themeId),
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = AutoVerseTypography,
            content = content,
        )
    }
}
