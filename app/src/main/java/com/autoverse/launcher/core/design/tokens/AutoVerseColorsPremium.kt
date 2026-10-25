package com.autoverse.launcher.core.design.tokens

import androidx.compose.ui.graphics.Color

// PREMIUM THEME PALETTES (0.21.0).
// Every value in this file was computed against the WCAG rules enforced by
// ContrastTest (textPrimary >=7:1 on cards, body/state tokens >=4.5:1,
// markers >=3:1) and verified before being written. accentSoft uses the
// 0x33-alpha convention at night and pre-blended opaque fills by day,
// matching the existing five themes.

/**
 * Sakura (anime day-vibe) — blush-plum surfaces with a cherry-petal accent. Premium theme.
 */
val SakuraDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF120C10),
    backgroundBottom = Color(0xFF170F14),
    surface = Color(0xFF1B1218),
    surfaceRaised = Color(0xFF221720),
    surfaceHighlight = Color(0xFF2A1C26),
    outline = Color(0xFF342431),
    outlineStrong = Color(0xFF44303F),
    accent = Color(0xFFFF8FB8),
    accentSoft = Color(0x33FF8FB8),
    onAccent = Color(0xFF241017),
    textPrimary = Color(0xFFF6ECF1),
    textSecondary = Color(0xFFB5A2AE),
    textFaint = Color(0xFF92808C),
    disabledContainer = Color(0xFF241A21),
    disabledContent = Color(0xFF6E5C67),
    success = Color(0xFF6FD8AC),
    warning = Color(0xFFF0BE62),
    danger = Color(0xFFFF7A8A),
)

/**
 * Sakura by day — white/blush cards with a deep rose accent that keeps 7:1 body text.
 */
val SakuraDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFFFF5F8),
    backgroundBottom = Color(0xFFFFEDF3),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFDF2F6),
    surfaceHighlight = Color(0xFFF9E4EC),
    outline = Color(0xFFF2D3DF),
    outlineStrong = Color(0xFFE4B8CB),
    accent = Color(0xFFC2185B),
    accentSoft = Color(0xFFF5D6E2),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF241019),
    textSecondary = Color(0xFF5D4650),
    textFaint = Color(0xFF75606A),
    disabledContainer = Color(0xFFF3E2E9),
    disabledContent = Color(0xFFB09AA4),
    success = Color(0xFF0E7A4B),
    warning = Color(0xFF8A5B00),
    danger = Color(0xFFB3233B),
)

/**
 * Neon Tokyo (anime night-vibe) — midnight city surfaces with an electric cyan accent. Premium theme.
 */
val NeonTokyoDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF070A10),
    backgroundBottom = Color(0xFF0A0E16),
    surface = Color(0xFF0E131D),
    surfaceRaised = Color(0xFF131925),
    surfaceHighlight = Color(0xFF19202E),
    outline = Color(0xFF232C3D),
    outlineStrong = Color(0xFF303B52),
    accent = Color(0xFF2DD9F5),
    accentSoft = Color(0x332DD9F5),
    onAccent = Color(0xFF04141A),
    textPrimary = Color(0xFFE6F2F6),
    textSecondary = Color(0xFF9DB4C0),
    textFaint = Color(0xFF7E96A3),
    disabledContainer = Color(0xFF16202B),
    disabledContent = Color(0xFF5E7280),
    success = Color(0xFF5FE3B3),
    warning = Color(0xFFF2C14E),
    danger = Color(0xFFFF6B8A),
)

/**
 * Neon Tokyo by day — porcelain surfaces with a deep teal accent.
 */
val NeonTokyoDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFF2FBFD),
    backgroundBottom = Color(0xFFE6F4F8),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFEEF8FB),
    surfaceHighlight = Color(0xFFDFF0F6),
    outline = Color(0xFFCBE2EA),
    outlineStrong = Color(0xFFAFD3DE),
    accent = Color(0xFF0E7490),
    accentSoft = Color(0xFFD1E7ED),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF07161D),
    textSecondary = Color(0xFF3D5560),
    textFaint = Color(0xFF54707C),
    disabledContainer = Color(0xFFE0EEF2),
    disabledContent = Color(0xFF8FA7B0),
    success = Color(0xFF0E7A4B),
    warning = Color(0xFF8A5B00),
    danger = Color(0xFFC2185B),
)

/**
 * Shonen (anime hero) — ink-indigo surfaces with a plus-ultra periwinkle accent. Premium theme.
 */
val ShonenDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF0A0B12),
    backgroundBottom = Color(0xFF0D0F18),
    surface = Color(0xFF12141F),
    surfaceRaised = Color(0xFF171A28),
    surfaceHighlight = Color(0xFF1E2233),
    outline = Color(0xFF2A2F44),
    outlineStrong = Color(0xFF383F5A),
    accent = Color(0xFF9BA8FF),
    accentSoft = Color(0x339BA8FF),
    onAccent = Color(0xFF0B0D22),
    textPrimary = Color(0xFFEDEFF8),
    textSecondary = Color(0xFFA7AECB),
    textFaint = Color(0xFF8690B2),
    disabledContainer = Color(0xFF1C2030),
    disabledContent = Color(0xFF666F8E),
    success = Color(0xFF57D3A1),
    warning = Color(0xFFE8B04B),
    danger = Color(0xFFF0716A),
)

/**
 * Shonen by day — paper-indigo surfaces with a deep hero-violet accent.
 */
val ShonenDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFF3F4FB),
    backgroundBottom = Color(0xFFEAECF7),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF0F1FA),
    surfaceHighlight = Color(0xFFE2E5F7),
    outline = Color(0xFFD0D4EE),
    outlineStrong = Color(0xFFB6BCE3),
    accent = Color(0xFF4F46E5),
    accentSoft = Color(0xFFDBDBF7),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF10132B),
    textSecondary = Color(0xFF434B6B),
    textFaint = Color(0xFF5A6383),
    disabledContainer = Color(0xFFE4E6F3),
    disabledContent = Color(0xFF98A0BE),
    success = Color(0xFF0E7A4B),
    warning = Color(0xFF8A5B00),
    danger = Color(0xFFB3232F),
)

/**
 * Gotham (stealth vigilante) — true-black surfaces with the bat-signal yellow accent. Premium theme.
 */
val GothamDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF060607),
    backgroundBottom = Color(0xFF0A0A0C),
    surface = Color(0xFF101012),
    surfaceRaised = Color(0xFF151517),
    surfaceHighlight = Color(0xFF1C1C1F),
    outline = Color(0xFF26262A),
    outlineStrong = Color(0xFF34343A),
    accent = Color(0xFFF2C744),
    accentSoft = Color(0x33F2C744),
    onAccent = Color(0xFF1C1405),
    textPrimary = Color(0xFFEDEDEA),
    textSecondary = Color(0xFFA8A8A2),
    textFaint = Color(0xFF87877F),
    disabledContainer = Color(0xFF1B1B1E),
    disabledContent = Color(0xFF63635E),
    success = Color(0xFF57D3A1),
    warning = Color(0xFFE89B4B),
    danger = Color(0xFFF06A5E),
)

/**
 * Gotham by day — chalk-grey surfaces with a dark ochre accent.
 */
val GothamDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFF5F5F2),
    backgroundBottom = Color(0xFFECECE8),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF2F2EE),
    surfaceHighlight = Color(0xFFE7E7E1),
    outline = Color(0xFFD8D8D0),
    outlineStrong = Color(0xFFC2C2B8),
    accent = Color(0xFF7A5C00),
    accentSoft = Color(0xFFE2DFD0),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF14140F),
    textSecondary = Color(0xFF4C4C44),
    textFaint = Color(0xFF66665C),
    disabledContainer = Color(0xFFE6E6DF),
    disabledContent = Color(0xFF9C9C92),
    success = Color(0xFF0E7A4B),
    warning = Color(0xFF8F4A00),
    danger = Color(0xFFB3232F),
)

/**
 * Hot Rod (armored hero) — warm gunmetal surfaces with an arc-red accent. Premium theme.
 */
val HotRodDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF0E0909),
    backgroundBottom = Color(0xFF140C0C),
    surface = Color(0xFF1A1111),
    surfaceRaised = Color(0xFF211616),
    surfaceHighlight = Color(0xFF2A1C1B),
    outline = Color(0xFF372424),
    outlineStrong = Color(0xFF47302F),
    accent = Color(0xFFFF6259),
    accentSoft = Color(0x33FF6259),
    onAccent = Color(0xFF220B09),
    textPrimary = Color(0xFFF5EDED),
    textSecondary = Color(0xFFBCA8A6),
    textFaint = Color(0xFF9C8785),
    disabledContainer = Color(0xFF241818),
    disabledContent = Color(0xFF75605E),
    success = Color(0xFF57D3A1),
    warning = Color(0xFFE8B04B),
    danger = Color(0xFFFF9186),
)

/**
 * Hot Rod by day — warm porcelain surfaces with a racing red accent.
 */
val HotRodDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFFAF4F3),
    backgroundBottom = Color(0xFFF4EAE9),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF9F1F0),
    surfaceHighlight = Color(0xFFF3E3E1),
    outline = Color(0xFFEAD6D3),
    outlineStrong = Color(0xFFD8BCB8),
    accent = Color(0xFFC62828),
    accentSoft = Color(0xFFF2D7D6),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1D0F0E),
    textSecondary = Color(0xFF5C4644),
    textFaint = Color(0xFF76605D),
    disabledContainer = Color(0xFFEFE0DE),
    disabledContent = Color(0xFFAC9592),
    success = Color(0xFF0E7A4B),
    warning = Color(0xFF8A5B00),
    danger = Color(0xFFB3233B),
)

// ---- Fortress (the dedicated light identity; PRD "Fortress") ----

/**
 * Fortress at night — slate-blue keep with a sky-steel accent. Premium theme.
 */
val FortressDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF0E1218),
    backgroundBottom = Color(0xFF121722),
    surface = Color(0xFF171D29),
    surfaceRaised = Color(0xFF1D2431),
    surfaceHighlight = Color(0xFF242D3C),
    outline = Color(0xFF2E3848),
    outlineStrong = Color(0xFF3D495C),
    accent = Color(0xFF63B3ED),
    accentSoft = Color(0x3363B3ED),
    onAccent = Color(0xFF0A1929),
    textPrimary = Color(0xFFEDF2F8),
    textSecondary = Color(0xFFA5B4C8),
    textFaint = Color(0xFF8496AB),
    disabledContainer = Color(0xFF1E2532),
    disabledContent = Color(0xFF66788C),
    success = Color(0xFF57D3A1),
    warning = Color(0xFFE8B04B),
    danger = Color(0xFFF0716A),
)

/**
 * Fortress by day — warm white citadel, sky-blue accent. Premium theme.
 */
val FortressDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFF7FAFD),
    backgroundBottom = Color(0xFFEDF3F9),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF1F6FB),
    surfaceHighlight = Color(0xFFE1EBF5),
    outline = Color(0xFFCFDCE8),
    outlineStrong = Color(0xFFB4C7D8),
    accent = Color(0xFF1573B8),
    accentSoft = Color(0xFFD9EAF8),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0C1A26),
    textSecondary = Color(0xFF3D5265),
    textFaint = Color(0xFF556B7E),
    disabledContainer = Color(0xFFE4EDF4),
    disabledContent = Color(0xFF93A8B9),
    success = Color(0xFF0E7A4B),
    warning = Color(0xFF8A5B00),
    danger = Color(0xFFB3233B),
)

// ---- Web Rush (urban sport; PRD "Web Rush") ----

/**
 * Web Rush at night — graphite with racing red and an electric-blue energy
 * line. Premium theme.
 */
val WebRushDarkColors = AutoVerseColors(
    backgroundTop = Color(0xFF0B0C12),
    backgroundBottom = Color(0xFF0F1018),
    surface = Color(0xFF151722),
    surfaceRaised = Color(0xFF1B1E2C),
    surfaceHighlight = Color(0xFF222637),
    outline = Color(0xFF2C3145),
    outlineStrong = Color(0xFF3B415A),
    accent = Color(0xFFFF6B61),
    accentSoft = Color(0x33FF6B61),
    onAccent = Color(0xFF230B09),
    textPrimary = Color(0xFFF4F1F4),
    textSecondary = Color(0xFFB4AEBE),
    textFaint = Color(0xFF948FA0),
    disabledContainer = Color(0xFF1F2029),
    disabledContent = Color(0xFF6E6A7A),
    success = Color(0xFF57D3A1),
    warning = Color(0xFFE8B04B),
    danger = Color(0xFFFF8C99),
)

/**
 * Web Rush by day — porcelain with a deep racing red. Premium theme.
 */
val WebRushDayColors = AutoVerseColors(
    backgroundTop = Color(0xFFFAF7F7),
    backgroundBottom = Color(0xFFF3EEEE),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF8F3F3),
    surfaceHighlight = Color(0xFFEFE4E4),
    outline = Color(0xFFE7D9D9),
    outlineStrong = Color(0xFFD3BFBF),
    accent = Color(0xFFC22733),
    accentSoft = Color(0xFFFADEDE),
    onAccent = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF1A1013),
    textSecondary = Color(0xFF5A4A50),
    textFaint = Color(0xFF746268),
    disabledContainer = Color(0xFFEFE3E3),
    disabledContent = Color(0xFFA9959B),
    success = Color(0xFF0E7A4B),
    warning = Color(0xFF8A5B00),
    danger = Color(0xFFB3232F),
)
