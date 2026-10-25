package com.autoverse.launcher.core.design.icons

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import com.autoverse.launcher.core.model.AutoVerseThemeId

/**
 * Phase B of the dynamic theme system: the icon *appearance* language,
 * separated from icon *identity*. A [ThemeIconSpec] describes HOW any icon
 * (logical glyph or a real installed-app bitmap) is presented under one
 * theme: container shape, border, background, glow, scale and the selected
 * treatment. One spec per theme, data-driven — no `when(themeId)` in
 * screens.
 *
 * Selected/disabled states are structural (edge / ring / indicator), never
 * color-only (accessibility rule from the theme PRD).
 */
enum class IconContainerShape {
    ANGULAR_SHIELD, CIRCULAR_RADIAL, SOFT_ROUNDED, HEXAGONAL_NEON, SPORT_ANGULAR, PRECISION_SQUARE,
}

enum class IconBorderStyle { NONE, ACCENT, OUTLINE }

enum class IconBackgroundStyle { TINTED, RAISED, INK }

enum class IconGlow { NONE, RESTRAINED, LUMINOUS }

enum class IconSelectedState { EDGE, RING, INDICATOR }

@Immutable
data class ThemeIconSpec(
    val containerShape: IconContainerShape,
    val borderStyle: IconBorderStyle = IconBorderStyle.OUTLINE,
    val borderWidthDp: Float = 1.5f,
    val backgroundStyle: IconBackgroundStyle = IconBackgroundStyle.TINTED,
    val glow: IconGlow = IconGlow.NONE,
    val iconScale: Float = 1f,
    val selectedState: IconSelectedState = IconSelectedState.EDGE,
) {
    init {
        require(iconScale in 0.85f..1.15f) { "iconScale out of range: $iconScale" }
        require(borderWidthDp in 0.5f..3f) { "borderWidthDp out of range: $borderWidthDp" }
    }

    companion object {
        val Default: ThemeIconSpec = ThemeIconSpec(IconContainerShape.SOFT_ROUNDED)
    }
}

/**
 * The icon language for every registered theme. Mapping to the theme PRD's
 * six identities: Gotham = Dark Knight shields, Hot Rod = Stark radial,
 * Neon Tokyo = hex neon, Apex = precision squares, Web Rush/Shonen = sport
 * angular, Cobalt/Fortress = soft rounded.
 */
private val THEME_ICON_SPECS: Map<AutoVerseThemeId, ThemeIconSpec> = mapOf(
    AutoVerseThemeId.COBALT to ThemeIconSpec(
        IconContainerShape.SOFT_ROUNDED,
        borderStyle = IconBorderStyle.OUTLINE,
        backgroundStyle = IconBackgroundStyle.TINTED,
        selectedState = IconSelectedState.RING,
    ),
    AutoVerseThemeId.OBSIDIAN to ThemeIconSpec(
        IconContainerShape.PRECISION_SQUARE,
        borderStyle = IconBorderStyle.OUTLINE,
        backgroundStyle = IconBackgroundStyle.RAISED,
        glow = IconGlow.NONE,
        selectedState = IconSelectedState.EDGE,
    ),
    AutoVerseThemeId.NEON_PULSE to ThemeIconSpec(
        IconContainerShape.HEXAGONAL_NEON,
        borderStyle = IconBorderStyle.ACCENT,
        backgroundStyle = IconBackgroundStyle.TINTED,
        glow = IconGlow.RESTRAINED,
        selectedState = IconSelectedState.RING,
    ),
    AutoVerseThemeId.ATELIER to ThemeIconSpec(
        IconContainerShape.SOFT_ROUNDED,
        borderStyle = IconBorderStyle.OUTLINE,
        backgroundStyle = IconBackgroundStyle.RAISED,
        selectedState = IconSelectedState.EDGE,
    ),
    AutoVerseThemeId.APEX to ThemeIconSpec(
        IconContainerShape.PRECISION_SQUARE,
        borderStyle = IconBorderStyle.OUTLINE,
        borderWidthDp = 1f,
        backgroundStyle = IconBackgroundStyle.RAISED,
        selectedState = IconSelectedState.INDICATOR,
    ),
    AutoVerseThemeId.SAKURA to ThemeIconSpec(
        IconContainerShape.SOFT_ROUNDED,
        borderStyle = IconBorderStyle.ACCENT,
        backgroundStyle = IconBackgroundStyle.TINTED,
        glow = IconGlow.RESTRAINED,
        selectedState = IconSelectedState.RING,
    ),
    AutoVerseThemeId.NEON_TOKYO to ThemeIconSpec(
        IconContainerShape.HEXAGONAL_NEON,
        borderStyle = IconBorderStyle.ACCENT,
        backgroundStyle = IconBackgroundStyle.INK,
        glow = IconGlow.RESTRAINED,
        selectedState = IconSelectedState.RING,
    ),
    AutoVerseThemeId.SHONEN to ThemeIconSpec(
        IconContainerShape.SPORT_ANGULAR,
        borderStyle = IconBorderStyle.ACCENT,
        backgroundStyle = IconBackgroundStyle.TINTED,
        selectedState = IconSelectedState.EDGE,
    ),
    AutoVerseThemeId.GOTHAM to ThemeIconSpec(
        IconContainerShape.ANGULAR_SHIELD,
        borderStyle = IconBorderStyle.ACCENT,
        borderWidthDp = 1.2f,
        backgroundStyle = IconBackgroundStyle.INK,
        glow = IconGlow.RESTRAINED,
        selectedState = IconSelectedState.EDGE,
    ),
    AutoVerseThemeId.HOT_ROD to ThemeIconSpec(
        IconContainerShape.CIRCULAR_RADIAL,
        borderStyle = IconBorderStyle.ACCENT,
        backgroundStyle = IconBackgroundStyle.TINTED,
        glow = IconGlow.LUMINOUS,
        selectedState = IconSelectedState.RING,
    ),
    AutoVerseThemeId.FORTRESS to ThemeIconSpec(
        IconContainerShape.SOFT_ROUNDED,
        borderStyle = IconBorderStyle.OUTLINE,
        borderWidthDp = 1f,
        backgroundStyle = IconBackgroundStyle.RAISED,
        glow = IconGlow.NONE,
        iconScale = 0.95f,
        selectedState = IconSelectedState.INDICATOR,
    ),
    AutoVerseThemeId.WEB_RUSH to ThemeIconSpec(
        IconContainerShape.SPORT_ANGULAR,
        borderStyle = IconBorderStyle.ACCENT,
        backgroundStyle = IconBackgroundStyle.RAISED,
        glow = IconGlow.RESTRAINED,
        selectedState = IconSelectedState.EDGE,
    ),
)

/** Spec for a theme; unknown IDs fall back to the soft default. */
fun themeIconSpecFor(themeId: AutoVerseThemeId): ThemeIconSpec =
    THEME_ICON_SPECS[themeId] ?: ThemeIconSpec.Default

val LocalAutoVerseIconSpec = staticCompositionLocalOf { ThemeIconSpec.Default }

/**
 * Compose shape factories for the closed shape set. Custom outlines use
 * normalized [GenericShape]s so one factory serves every icon size.
 */
fun IconContainerShape.toShape(): Shape = when (this) {
    IconContainerShape.SOFT_ROUNDED -> RoundedCornerShape(percent = 28)
    IconContainerShape.PRECISION_SQUARE -> RoundedCornerShape(percent = 12)
    IconContainerShape.CIRCULAR_RADIAL -> CircleShape
    IconContainerShape.HEXAGONAL_NEON -> GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        moveTo(w * 0.5f, 0f)
        lineTo(w * 0.933f, h * 0.25f)
        lineTo(w * 0.933f, h * 0.75f)
        lineTo(w * 0.5f, h)
        lineTo(w * 0.067f, h * 0.75f)
        lineTo(w * 0.067f, h * 0.25f)
        close()
    }
    IconContainerShape.ANGULAR_SHIELD -> GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        moveTo(w * 0.14f, 0f)
        lineTo(w * 0.86f, 0f)
        lineTo(w * 0.94f, h * 0.18f)
        lineTo(w * 0.94f, h * 0.55f)
        lineTo(w * 0.5f, h)
        lineTo(w * 0.06f, h * 0.55f)
        lineTo(w * 0.06f, h * 0.18f)
        close()
    }
    IconContainerShape.SPORT_ANGULAR -> GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        val cut = 0.18f
        moveTo(w * cut, 0f)
        lineTo(w, 0f)
        lineTo(w, h * (1f - cut))
        lineTo(w * (1f - cut), h)
        lineTo(0f, h)
        lineTo(0f, h * cut)
        close()
    }
}
