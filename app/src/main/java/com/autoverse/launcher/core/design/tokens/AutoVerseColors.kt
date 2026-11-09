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
 * Cobalt at night — the AutoVerse flagship palette. Neutral graphite (not
 * blue-tinted) with clearly stepped levels (~4-5% lightness apart, so cheap
 * TN/IPS panels still separate them off-angle). Blue is reserved for the
 * route, the active item and the one primary action. Outlines are
 * translucent white: an edge catching light, not a grey line. Every ratio
 * is verified by ContrastTest.
 */
val CobaltDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF07080A),
    backgroundBottom = Color(0xFF0A0C0F),
    surface = Color(0xFF121418),
    surfaceRaised = Color(0xFF1A1D22),
    surfaceHighlight = Color(0xFF24282F),
    outline = Color(0x14FFFFFF),
    outlineStrong = Color(0x29FFFFFF),
    accent = Color(0xFF4C93FF),
    accentSoft = Color(0x2E4C93FF),
    onAccent = Color(0xFF06101F),
    textPrimary = Color(0xFFF2F4F7),
    textSecondary = Color(0xFFA3AAB5),
    textFaint = Color(0xFF828A96),
    disabledContainer = Color(0xFF1C1F24),
    disabledContent = Color(0xFF5F6672),
    success = Color(0xFF3DD68C),
    warning = Color(0xFFF5B544),
    danger = Color(0xFFFF6B66),
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

/** Obsidian at night — warm graphite with a champagne-gold accent. */
val ObsidianDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF070708),
    backgroundBottom = Color(0xFF0A0A0C),
    surface = Color(0xFF131416),
    surfaceRaised = Color(0xFF1B1C1F),
    surfaceHighlight = Color(0xFF26272B),
    outline = Color(0x14FFFFFF),
    outlineStrong = Color(0x29FFFFFF),
    accent = Color(0xFFF2B857),
    accentSoft = Color(0x2EF2B857),
    onAccent = Color(0xFF201607),
    textPrimary = Color(0xFFF3F2EF),
    textSecondary = Color(0xFFA9ACB2),
    textFaint = Color(0xFF858993),
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


/**
 * Neon Pulse by day — cool daylight surfaces. The cyan accent deepens to a
 * teal so white-on-accent and accent-on-light both clear 4.5:1/3:1, and the
 * soft fill is pre-blended to keep compositing cheap.
 */

/** Atelier at night — warm charcoal and quiet clay surfaces for calm reading. */
val AtelierDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF0E0C0A),
    backgroundBottom = Color(0xFF12100D),
    surface = Color(0xFF1B1612),
    surfaceRaised = Color(0xFF241E18),
    surfaceHighlight = Color(0xFF2F2820),
    outline = Color(0x16FFF1E2),
    outlineStrong = Color(0x2BFFF1E2),
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
    backgroundTop = Color(0xFF0A0809),
    backgroundBottom = Color(0xFF0D0A0B),
    surface = Color(0xFF171214),
    surfaceRaised = Color(0xFF201A1C),
    surfaceHighlight = Color(0xFF2B2326),
    outline = Color(0x14FFFFFF),
    outlineStrong = Color(0x29FFFFFF),
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
    danger = Color(0xFFFF6166),
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
