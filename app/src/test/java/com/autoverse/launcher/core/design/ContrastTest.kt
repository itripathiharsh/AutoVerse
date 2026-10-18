package com.autoverse.launcher.core.design

import androidx.compose.ui.graphics.Color
import com.autoverse.launcher.core.design.tokens.AutoVerseColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.x relative-luminance contrast checks for every registered theme
 * palette (dark and light). Thresholds: primary text >= 7:1, body text
 * >= 4.5:1, non-text UI and state accents >= 3:1.
 */
class ContrastTest {

    @Test
    fun `every theme text token meets contrast on every surface`() {
        allPalettes().forEach { (label, colors) -> assertTextContrast(label, colors) }
    }

    @Test
    fun `every theme accent and state token stays legible`() {
        allPalettes().forEach { (label, colors) -> assertAccentContrast(label, colors) }
    }

    /**
     * Phase 11 safety pass: accent and warning/danger also appear as small
     * text (TextAction labels, navigation status lines) on card surfaces, not
     * only as icons. Text-grade contrast applies there; success is marker-only
     * (dot/icon) so the non-text minimum applies.
     */
    @Test
    fun `accent and state colors used as text stay readable on card surfaces`() {
        allPalettes().forEach { (label, colors) ->
            listOf(colors.surface, colors.surfaceRaised).forEach { card ->
                assertAtLeast(4.5, colors.accent, card, "$label accent on card")
                assertAtLeast(4.5, colors.warning, card, "$label warning on card")
                assertAtLeast(4.5, colors.danger, card, "$label danger on card")
                assertAtLeast(3.0, colors.success, card, "$label success marker on card")
            }
        }
    }

    private fun allPalettes(): List<Pair<String, AutoVerseColors>> =
        AutoVerseThemeSpecs.flatMap { spec ->
            listOf(
                "${spec.id.name} dark" to spec.darkColors,
                "${spec.id.name} light" to spec.lightColors,
            )
        }

    private fun assertTextContrast(label: String, colors: AutoVerseColors) {
        val surfaces = listOf(colors.backgroundTop, colors.backgroundBottom, colors.surface, colors.surfaceRaised)
        surfaces.forEach { surface ->
            assertAtLeast(7.0, colors.textPrimary, surface, "$label textPrimary on surface")
            assertAtLeast(4.5, colors.textSecondary, surface, "$label textSecondary on surface")
            assertAtLeast(4.5, colors.textFaint, surface, "$label textFaint on surface")
        }
    }

    private fun assertAccentContrast(label: String, colors: AutoVerseColors) {
        assertAtLeast(4.5, colors.onAccent, colors.accent, "$label onAccent on accent")
        assertAtLeast(3.0, colors.accent, colors.backgroundTop, "$label accent on backgroundTop")
        assertAtLeast(3.0, colors.accent, colors.surfaceHighlight, "$label accent on surfaceHighlight")
        assertAtLeast(3.0, colors.success, colors.backgroundTop, "$label success on backgroundTop")
        assertAtLeast(3.0, colors.warning, colors.backgroundTop, "$label warning on backgroundTop")
        assertAtLeast(3.0, colors.danger, colors.backgroundTop, "$label danger on backgroundTop")
    }

    private fun assertAtLeast(minRatio: Double, foreground: Color, background: Color, label: String) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "$label expected >= $minRatio but was ${"%.2f".format(ratio)}",
            ratio >= minRatio,
        )
    }
}

private fun linearChannel(component: Float): Double {
    val value = component.toDouble()
    return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}

private fun luminance(color: Color): Double =
    0.2126 * linearChannel(color.red) +
        0.7152 * linearChannel(color.green) +
        0.0722 * linearChannel(color.blue)

private fun contrastRatio(foreground: Color, background: Color): Double {
    val first = luminance(foreground)
    val second = luminance(background)
    return (max(first, second) + 0.05) / (min(first, second) + 0.05)
}
