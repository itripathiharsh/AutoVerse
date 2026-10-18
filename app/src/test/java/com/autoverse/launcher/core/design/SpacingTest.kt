package com.autoverse.launcher.core.design

import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacingTest {

    private val scale = listOf(
        AutoVerseSpacing.xxs,
        AutoVerseSpacing.xs,
        AutoVerseSpacing.sm,
        AutoVerseSpacing.md,
        AutoVerseSpacing.lg,
        AutoVerseSpacing.xl,
        AutoVerseSpacing.xxl,
    )

    @Test
    fun `scale is strictly ascending`() {
        for (index in 1 until scale.size) {
            assertTrue("step $index must be larger than the previous", scale[index] > scale[index - 1])
        }
    }

    @Test
    fun `every scale value is a multiple of the base unit`() {
        scale.forEach { step ->
            assertEquals(0f, step.value % 4f, 0f)
        }
    }

    @Test
    fun `semantic spacing maps back to scale steps`() {
        assertEquals(AutoVerseSpacing.lg, AutoVerseSpacing.screenHorizontal)
        assertEquals(AutoVerseSpacing.md, AutoVerseSpacing.screenVertical)
        assertEquals(AutoVerseSpacing.lg, AutoVerseSpacing.cardPadding)
        assertEquals(AutoVerseSpacing.md, AutoVerseSpacing.cardPaddingDense)
        assertEquals(AutoVerseSpacing.sm, AutoVerseSpacing.stackGap)
        assertEquals(AutoVerseSpacing.sm, AutoVerseSpacing.gridGap)
        assertEquals(AutoVerseSpacing.xs, AutoVerseSpacing.inlineGap)
        assertEquals(AutoVerseSpacing.sm, AutoVerseSpacing.chipHorizontal)
        assertEquals(AutoVerseSpacing.xs, AutoVerseSpacing.chipVertical)
    }

    @Test
    fun `touch targets respect the car-scale ordering`() {
        assertTrue(AutoVerseSpacing.touchMin >= 48.dp)
        assertTrue(AutoVerseSpacing.touchComfort > AutoVerseSpacing.touchMin)
        assertTrue(AutoVerseSpacing.transportPrimary >= AutoVerseSpacing.touchComfort)
        assertTrue(AutoVerseSpacing.transportSecondary >= AutoVerseSpacing.touchMin)
        assertTrue(AutoVerseSpacing.heroChip >= AutoVerseSpacing.iconChip)
    }
}
