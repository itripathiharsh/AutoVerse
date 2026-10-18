package com.autoverse.launcher.core.design.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens. Every composable reads colors through
 * [com.autoverse.launcher.core.design.LocalAutoVerseColors] — literals live here only.
 */
@Immutable
data class AutoVerseColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceHighlight: Color,
    val outline: Color,
    val outlineStrong: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textFaint: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)

/**
 * Cobalt at night — the AutoVerse flagship palette. Deep charcoal surfaces
 * with the restrained electric blue drawn from the brand mark. Every ratio
 * is verified by ContrastTest.
 */
val CobaltDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF090C11),
    backgroundBottom = Color(0xFF0C1017),
    surface = Color(0xFF11151D),
    surfaceRaised = Color(0xFF161B25),
    surfaceHighlight = Color(0xFF1C2330),
    outline = Color(0xFF262E3C),
    outlineStrong = Color(0xFF354054),
    accent = Color(0xFF4A90F7),
    accentSoft = Color(0x334A90F7),
    onAccent = Color(0xFF071523),
    textPrimary = Color(0xFFE8EDF4),
    textSecondary = Color(0xFF9AA7B8),
    textFaint = Color(0xFF78879C),
    disabledContainer = Color(0xFF1B2230),
    disabledContent = Color(0xFF5C6978),
    success = Color(0xFF57D3A1),
    warning = Color(0xFFE8B04B),
    danger = Color(0xFFF0716A),
)

/**
 * Cobalt by day — cool daylight variant of the flagship palette. The accent
 * deepens so white-on-accent and accent-on-light both stay legible, and the
 * soft fill is pre-blended to keep compositing cheap.
 */
val CobaltDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFF4F7FB),
    backgroundBottom = Color(0xFFE9EEF6),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF2F5FA),
    surfaceHighlight = Color(0xFFE3EAF4),
    outline = Color(0xFFD2DAE6),
    outlineStrong = Color(0xFFB9C4D4),
    accent = Color(0xFF1E63E9),
    accentSoft = Color(0xFFDCE7FB),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0B1526),
    textSecondary = Color(0xFF3E4C61),
    textFaint = Color(0xFF55647A),
    disabledContainer = Color(0xFFE2E8F2),
    disabledContent = Color(0xFF94A1B3),
    success = Color(0xFF0E7A4B),
    warning = Color(0xFF8A5B00),
    danger = Color(0xFFC33B32),
)

/** Obsidian at night — the original flagship palette. */
val ObsidianDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF05070B),
    backgroundBottom = Color(0xFF0C1018),
    surface = Color(0xFF0F141C),
    surfaceRaised = Color(0xFF141B26),
    surfaceHighlight = Color(0xFF1B2432),
    outline = Color(0xFF232E3E),
    outlineStrong = Color(0xFF2F3D51),
    accent = Color(0xFFF2B857),
    accentSoft = Color(0x33F2B857),
    onAccent = Color(0xFF201607),
    textPrimary = Color(0xFFE9EFF7),
    textSecondary = Color(0xFF9DACC0),
    textFaint = Color(0xFF7E8CA0),
    disabledContainer = Color(0xFF1A2230),
    disabledContent = Color(0xFF5A6678),
    success = Color(0xFF6FCF97),
    warning = Color(0xFFF2C94C),
    danger = Color(0xFFEB6E63),
)

/**
 * Obsidian by day — daylight-legible variant of the flagship palette.
 * Accent deepens to bronze so white text on accent and accent on white
 * both stay at or above 4.5:1. Pre-blended (non-alpha) soft fills keep
 * rendering cheap — no live blending in the compositor.
 */
val ObsidianDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFF4F6F9),
    backgroundBottom = Color(0xFFE8ECF2),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF7F9FC),
    surfaceHighlight = Color(0xFFEDF1F7),
    outline = Color(0xFFD8DFE8),
    outlineStrong = Color(0xFFC3CDDA),
    accent = Color(0xFF8F5E0F),
    accentSoft = Color(0xFFF5E8D0),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF101721),
    textSecondary = Color(0xFF48566B),
    textFaint = Color(0xFF556577),
    disabledContainer = Color(0xFFE3E8EF),
    disabledContent = Color(0xFF98A2B3),
    success = Color(0xFF1F8A4C),
    warning = Color(0xFF966800),
    danger = Color(0xFFC0392B),
)

/** Neon Pulse at night — deep blue-black surfaces with restrained electric cyan. */
val NeonPulseDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF04070C),
    backgroundBottom = Color(0xFF0A1018),
    surface = Color(0xFF0C141E),
    surfaceRaised = Color(0xFF101B28),
    surfaceHighlight = Color(0xFF16273A),
    outline = Color(0xFF1E3040),
    outlineStrong = Color(0xFF2A4256),
    accent = Color(0xFF35E0FF),
    accentSoft = Color(0x3335E0FF),
    onAccent = Color(0xFF041E26),
    textPrimary = Color(0xFFE8F6FC),
    textSecondary = Color(0xFFA8C0D0),
    textFaint = Color(0xFF8AA2B4),
    disabledContainer = Color(0xFF131E2B),
    disabledContent = Color(0xFF5A6E80),
    success = Color(0xFF5BD99F),
    warning = Color(0xFFF2C94C),
    danger = Color(0xFFEB6E63),
)

/**
 * Neon Pulse by day — cool daylight surfaces. The cyan accent deepens to a
 * teal so white-on-accent and accent-on-light both clear 4.5:1/3:1, and the
 * soft fill is pre-blended to keep compositing cheap.
 */
val NeonPulseDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFF1F7FA),
    backgroundBottom = Color(0xFFE4EDF3),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF4F9FC),
    surfaceHighlight = Color(0xFFE2EEF5),
    outline = Color(0xFFCFDDE7),
    outlineStrong = Color(0xFFB5C7D3),
    accent = Color(0xFF0B6E8A),
    accentSoft = Color(0xFFD6EBF2),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0B1620),
    textSecondary = Color(0xFF40536A),
    textFaint = Color(0xFF4F6478),
    disabledContainer = Color(0xFFDCE7EF),
    disabledContent = Color(0xFF8FA2B2),
    success = Color(0xFF17804F),
    warning = Color(0xFF8A6500),
    danger = Color(0xFFC0392B),
)

/** Atelier at night — warm charcoal and quiet clay surfaces for calm reading. */
val AtelierDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF100D0A),
    backgroundBottom = Color(0xFF181410),
    surface = Color(0xFF1C1712),
    surfaceRaised = Color(0xFF231D16),
    surfaceHighlight = Color(0xFF2C241B),
    outline = Color(0xFF3A2F24),
    outlineStrong = Color(0xFF4C3E30),
    accent = Color(0xFFE0A87E),
    accentSoft = Color(0x33E0A87E),
    onAccent = Color(0xFF2A1A0C),
    textPrimary = Color(0xFFF4EDE4),
    textSecondary = Color(0xFFC0B1A1),
    textFaint = Color(0xFF9C8C7B),
    disabledContainer = Color(0xFF261F18),
    disabledContent = Color(0xFF6B5D4E),
    success = Color(0xFF76C79B),
    warning = Color(0xFFE8C15A),
    danger = Color(0xFFD97A6C),
)

/**
 * Atelier by day — warm paper tones. The accent is a deep clay so white text
 * on accent and accent on paper both stay legible without glare.
 */
val AtelierDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFFAF6F0),
    backgroundBottom = Color(0xFFF1EAE0),
    surface = Color(0xFFFFFDFA),
    surfaceRaised = Color(0xFFF7F1E8),
    surfaceHighlight = Color(0xFFEDE3D6),
    outline = Color(0xFFE0D4C4),
    outlineStrong = Color(0xFFCBBBA6),
    accent = Color(0xFF9C5A2E),
    accentSoft = Color(0xFFF2E2CC),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF241C14),
    textSecondary = Color(0xFF5C5044),
    textFaint = Color(0xFF6B5E50),
    disabledContainer = Color(0xFFE9E0D3),
    disabledContent = Color(0xFFA79A88),
    success = Color(0xFF1F7A48),
    warning = Color(0xFF8A6200),
    danger = Color(0xFFB23B2E),
)

/** Apex at night — near-black asphalt with a motorsport orange accent. */
val ApexDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF0A0708),
    backgroundBottom = Color(0xFF140C0D),
    surface = Color(0xFF170E10),
    surfaceRaised = Color(0xFF1E1215),
    surfaceHighlight = Color(0xFF28181B),
    outline = Color(0xFF382226),
    outlineStrong = Color(0xFF4A2D33),
    accent = Color(0xFFFF6B2C),
    accentSoft = Color(0x33FF6B2C),
    onAccent = Color(0xFF2A0A05),
    textPrimary = Color(0xFFF7EEEA),
    textSecondary = Color(0xFFC9ADA3),
    textFaint = Color(0xFFA68C80),
    disabledContainer = Color(0xFF241518),
    disabledContent = Color(0xFF70564F),
    success = Color(0xFF63D08C),
    warning = Color(0xFFF2B84C),
    danger = Color(0xFFE5484D),
)

/**
 * Apex by day — warm white with a deep racing red accent. Danger shifts to
 * crimson so it stays distinct from the orange accent hue.
 */
val ApexDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFFAF5F2),
    backgroundBottom = Color(0xFFF1E8E2),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF8F2ED),
    surfaceHighlight = Color(0xFFEFE2DA),
    outline = Color(0xFFE4D4CB),
    outlineStrong = Color(0xFFD0B8AB),
    accent = Color(0xFFC2320F),
    accentSoft = Color(0xFFF7DCCF),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF20140F),
    textSecondary = Color(0xFF5A4740),
    textFaint = Color(0xFF68534B),
    disabledContainer = Color(0xFFEADCD4),
    disabledContent = Color(0xFFA8958A),
    success = Color(0xFF1F7A48),
    warning = Color(0xFF8A5A00),
    danger = Color(0xFFB3261E),
)
