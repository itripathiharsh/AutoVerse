package com.autoverse.launcher.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.autoverse.launcher.core.design.tokens.ApexDarkColors
import com.autoverse.launcher.core.design.tokens.ApexDayColors
import com.autoverse.launcher.core.design.tokens.AtelierDarkColors
import com.autoverse.launcher.core.design.tokens.AtelierDayColors
import com.autoverse.launcher.core.design.tokens.AutoVerseColors
import com.autoverse.launcher.core.design.tokens.CobaltDarkColors
import com.autoverse.launcher.core.design.tokens.CobaltDayColors
import com.autoverse.launcher.core.design.tokens.NeonPulseDarkColors
import com.autoverse.launcher.core.design.tokens.NeonPulseDayColors
import com.autoverse.launcher.core.design.tokens.ObsidianDarkColors
import com.autoverse.launcher.core.design.tokens.ObsidianDayColors
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId

/**
 * A complete, data-driven theme definition. Palette variants are values —
 * switching themes or day/night only swaps tokens, never app structure.
 */
@Immutable
data class ThemeSpec(
    val id: AutoVerseThemeId,
    val displayName: String,
    val darkColors: AutoVerseColors,
    val lightColors: AutoVerseColors,
) {
    fun colorsFor(darkTheme: Boolean): AutoVerseColors =
        if (darkTheme) darkColors else lightColors
}

private val CobaltSpec = ThemeSpec(
    id = AutoVerseThemeId.COBALT,
    displayName = AutoVerseThemeId.COBALT.displayName,
    darkColors = CobaltDarkColors,
    lightColors = CobaltDayColors,
)

private val ObsidianSpec = ThemeSpec(
    id = AutoVerseThemeId.OBSIDIAN,
    displayName = AutoVerseThemeId.OBSIDIAN.displayName,
    darkColors = ObsidianDarkColors,
    lightColors = ObsidianDayColors,
)

private val NeonPulseSpec = ThemeSpec(
    id = AutoVerseThemeId.NEON_PULSE,
    displayName = AutoVerseThemeId.NEON_PULSE.displayName,
    darkColors = NeonPulseDarkColors,
    lightColors = NeonPulseDayColors,
)

private val AtelierSpec = ThemeSpec(
    id = AutoVerseThemeId.ATELIER,
    displayName = AutoVerseThemeId.ATELIER.displayName,
    darkColors = AtelierDarkColors,
    lightColors = AtelierDayColors,
)

private val ApexSpec = ThemeSpec(
    id = AutoVerseThemeId.APEX,
    displayName = AutoVerseThemeId.APEX.displayName,
    darkColors = ApexDarkColors,
    lightColors = ApexDayColors,
)

/**
 * Every theme the launcher can render, in settings-picker order. The registry
 * must cover each [AutoVerseThemeId] exactly once — ThemeSpecTest enforces it.
 */
val AutoVerseThemeSpecs: List<ThemeSpec> =
    listOf(CobaltSpec, ObsidianSpec, NeonPulseSpec, AtelierSpec, ApexSpec)

/** Resolves the spec for [themeId]. Every enum value is registered. */
fun themeSpecFor(themeId: AutoVerseThemeId): ThemeSpec =
    AutoVerseThemeSpecs.first { it.id == themeId }

/** Resolves the effective dark/light choice for an [AppearanceMode]. */
fun isDarkTheme(appearanceMode: AppearanceMode, systemInDark: Boolean): Boolean =
    when (appearanceMode) {
        AppearanceMode.SYSTEM -> systemInDark
        AppearanceMode.DAY -> false
        AppearanceMode.NIGHT -> true
    }

val LocalAutoVerseColors = staticCompositionLocalOf { ObsidianDarkColors }
