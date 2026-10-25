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
import com.autoverse.launcher.core.design.tokens.FortressDarkColors
import com.autoverse.launcher.core.design.tokens.FortressDayColors
import com.autoverse.launcher.core.design.tokens.GothamDarkColors
import com.autoverse.launcher.core.design.tokens.GothamDayColors
import com.autoverse.launcher.core.design.tokens.HotRodDarkColors
import com.autoverse.launcher.core.design.tokens.HotRodDayColors
import com.autoverse.launcher.core.design.tokens.NeonPulseDarkColors
import com.autoverse.launcher.core.design.tokens.NeonPulseDayColors
import com.autoverse.launcher.core.design.tokens.NeonTokyoDarkColors
import com.autoverse.launcher.core.design.tokens.NeonTokyoDayColors
import com.autoverse.launcher.core.design.tokens.SakuraDarkColors
import com.autoverse.launcher.core.design.tokens.SakuraDayColors
import com.autoverse.launcher.core.design.tokens.ShonenDarkColors
import com.autoverse.launcher.core.design.tokens.ShonenDayColors
import com.autoverse.launcher.core.design.tokens.WebRushDarkColors
import com.autoverse.launcher.core.design.tokens.WebRushDayColors
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

private val SakuraSpec = ThemeSpec(
    id = AutoVerseThemeId.SAKURA,
    displayName = AutoVerseThemeId.SAKURA.displayName,
    darkColors = SakuraDarkColors,
    lightColors = SakuraDayColors,
)

private val NeonTokyoSpec = ThemeSpec(
    id = AutoVerseThemeId.NEON_TOKYO,
    displayName = AutoVerseThemeId.NEON_TOKYO.displayName,
    darkColors = NeonTokyoDarkColors,
    lightColors = NeonTokyoDayColors,
)

private val ShonenSpec = ThemeSpec(
    id = AutoVerseThemeId.SHONEN,
    displayName = AutoVerseThemeId.SHONEN.displayName,
    darkColors = ShonenDarkColors,
    lightColors = ShonenDayColors,
)

private val GothamSpec = ThemeSpec(
    id = AutoVerseThemeId.GOTHAM,
    displayName = AutoVerseThemeId.GOTHAM.displayName,
    darkColors = GothamDarkColors,
    lightColors = GothamDayColors,
)

private val HotRodSpec = ThemeSpec(
    id = AutoVerseThemeId.HOT_ROD,
    displayName = AutoVerseThemeId.HOT_ROD.displayName,
    darkColors = HotRodDarkColors,
    lightColors = HotRodDayColors,
)

private val FortressSpec = ThemeSpec(
    id = AutoVerseThemeId.FORTRESS,
    displayName = AutoVerseThemeId.FORTRESS.displayName,
    darkColors = FortressDarkColors,
    lightColors = FortressDayColors,
)

private val WebRushSpec = ThemeSpec(
    id = AutoVerseThemeId.WEB_RUSH,
    displayName = AutoVerseThemeId.WEB_RUSH.displayName,
    darkColors = WebRushDarkColors,
    lightColors = WebRushDayColors,
)

/**
 * Every theme the launcher can render, in settings-picker order. The registry
 * must cover each [AutoVerseThemeId] exactly once — ThemeSpecTest enforces it.
 */
val AutoVerseThemeSpecs: List<ThemeSpec> =
    listOf(CobaltSpec, ObsidianSpec, NeonPulseSpec, AtelierSpec, ApexSpec,
        SakuraSpec, NeonTokyoSpec, ShonenSpec, GothamSpec, HotRodSpec,
        FortressSpec, WebRushSpec)

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
