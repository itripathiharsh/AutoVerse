package com.autoverse.launcher.core.launcher

import android.content.Intent
import android.content.pm.ApplicationInfo
import com.autoverse.launcher.core.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 15 — drawer classification table. Pins the declared-category
 * mapping, the intent-category fallback, the multi-match priority, the
 * guaranteed OTHERS fallback, and the probe order that AppRepository and the
 * classifier must agree on. All referenced constants are compile-time values,
 * so this runs as a plain JVM test without an Android runtime.
 */
class AppCategoryClassifierTest {

    private fun classify(
        applicationCategory: Int = ApplicationInfo.CATEGORY_UNDEFINED,
        matched: Set<String> = emptySet(),
    ) = AppCategoryClassifier.classify(applicationCategory, matched)

    // --- Declared ApplicationInfo.category (strongest signal) --------------

    @Test
    fun `declared categories map onto drawer chips`() {
        assertEquals(AppCategory.NAVIGATION, classify(ApplicationInfo.CATEGORY_MAPS))
        assertEquals(AppCategory.MUSIC_AUDIO, classify(ApplicationInfo.CATEGORY_AUDIO))
        assertEquals(AppCategory.VIDEO, classify(ApplicationInfo.CATEGORY_VIDEO))
        assertEquals(AppCategory.COMMUNICATION, classify(ApplicationInfo.CATEGORY_SOCIAL))
        assertEquals(AppCategory.TOOLS, classify(ApplicationInfo.CATEGORY_PRODUCTIVITY))
        assertEquals(AppCategory.TOOLS, classify(ApplicationInfo.CATEGORY_ACCESSIBILITY))
    }

    @Test
    fun `declared category wins over intent categories`() {
        assertEquals(
            AppCategory.NAVIGATION,
            classify(ApplicationInfo.CATEGORY_MAPS, setOf(Intent.CATEGORY_APP_MUSIC)),
        )
        assertEquals(
            AppCategory.MUSIC_AUDIO,
            classify(ApplicationInfo.CATEGORY_AUDIO, setOf(Intent.CATEGORY_APP_MESSAGING)),
        )
    }

    @Test
    fun `categories without a chip fall through to the intent signal`() {
        assertEquals(
            AppCategory.MUSIC_AUDIO,
            classify(ApplicationInfo.CATEGORY_GAME, setOf(Intent.CATEGORY_APP_MUSIC)),
        )
        assertEquals(
            AppCategory.OTHERS,
            classify(ApplicationInfo.CATEGORY_IMAGE, emptySet()),
        )
        assertEquals(
            AppCategory.OTHERS,
            classify(ApplicationInfo.CATEGORY_NEWS, emptySet()),
        )
    }

    // --- Intent-category fallback -------------------------------------------

    @Test
    fun `probed intent categories map onto drawer chips`() {
        assertEquals(AppCategory.NAVIGATION, classify(matched = setOf(Intent.CATEGORY_APP_MAPS)))
        assertEquals(AppCategory.MUSIC_AUDIO, classify(matched = setOf(Intent.CATEGORY_APP_MUSIC)))
        assertEquals(AppCategory.COMMUNICATION, classify(matched = setOf(Intent.CATEGORY_APP_MESSAGING)))
        assertEquals(AppCategory.COMMUNICATION, classify(matched = setOf(Intent.CATEGORY_APP_EMAIL)))
        assertEquals(AppCategory.COMMUNICATION, classify(matched = setOf(Intent.CATEGORY_APP_CONTACTS)))
        assertEquals(AppCategory.TOOLS, classify(matched = setOf(Intent.CATEGORY_APP_CALENDAR)))
        assertEquals(AppCategory.TOOLS, classify(matched = setOf(Intent.CATEGORY_APP_BROWSER)))
        assertEquals(AppCategory.TOOLS, classify(matched = setOf(Intent.CATEGORY_APP_CALCULATOR)))
    }

    @Test
    fun `gallery maps to OTHERS because the drawer has no gallery chip`() {
        assertEquals(AppCategory.OTHERS, classify(matched = setOf(Intent.CATEGORY_APP_GALLERY)))
    }

    @Test
    fun `unknown intent categories fall back to OTHERS`() {
        assertEquals(AppCategory.OTHERS, classify(matched = setOf("com.example.CUSTOM")))
        assertEquals(
            AppCategory.OTHERS,
            classify(matched = setOf("com.example.CUSTOM", "org.other.THING")),
        )
    }

    @Test
    fun `no signals at all is OTHERS`() {
        assertEquals(AppCategory.OTHERS, classify())
    }

    // --- Multi-match priority ----------------------------------------------

    @Test
    fun `navigation wins any multi-match`() {
        assertEquals(
            AppCategory.NAVIGATION,
            classify(matched = setOf(Intent.CATEGORY_APP_MUSIC, Intent.CATEGORY_APP_MAPS)),
        )
        assertEquals(
            AppCategory.NAVIGATION,
            classify(matched = setOf(Intent.CATEGORY_APP_GALLERY, Intent.CATEGORY_APP_MAPS)),
        )
    }

    @Test
    fun `music outranks communication and tools`() {
        assertEquals(
            AppCategory.MUSIC_AUDIO,
            classify(matched = setOf(Intent.CATEGORY_APP_EMAIL, Intent.CATEGORY_APP_MUSIC)),
        )
        assertEquals(
            AppCategory.MUSIC_AUDIO,
            classify(matched = setOf(Intent.CATEGORY_APP_CALCULATOR, Intent.CATEGORY_APP_MUSIC)),
        )
    }

    @Test
    fun `communication outranks tools`() {
        assertEquals(
            AppCategory.COMMUNICATION,
            classify(matched = setOf(Intent.CATEGORY_APP_CALCULATOR, Intent.CATEGORY_APP_MESSAGING)),
        )
    }

    @Test
    fun `tools outrank gallery`() {
        assertEquals(
            AppCategory.TOOLS,
            classify(matched = setOf(Intent.CATEGORY_APP_GALLERY, Intent.CATEGORY_APP_CALENDAR)),
        )
    }

    // --- Probe list ---------------------------------------------------------

    @Test
    fun `probe order is pinned because it is the priority order`() {
        assertEquals(
            listOf(
                Intent.CATEGORY_APP_MAPS,
                Intent.CATEGORY_APP_MUSIC,
                Intent.CATEGORY_APP_MESSAGING,
                Intent.CATEGORY_APP_EMAIL,
                Intent.CATEGORY_APP_CONTACTS,
                Intent.CATEGORY_APP_CALENDAR,
                Intent.CATEGORY_APP_BROWSER,
                Intent.CATEGORY_APP_CALCULATOR,
                Intent.CATEGORY_APP_GALLERY,
            ),
            AppCategoryClassifier.probeCategories,
        )
    }

    @Test
    fun `every probe category is recognized and only gallery maps to OTHERS`() {
        for (probe in AppCategoryClassifier.probeCategories) {
            val result = classify(matched = setOf(probe))
            assertNotEquals("probe $probe must not be unhandled", AppCategory.ALL, result)
            if (probe == Intent.CATEGORY_APP_GALLERY) {
                assertEquals(AppCategory.OTHERS, result)
            } else {
                assertNotEquals(
                    "probe $probe must map to a real category",
                    AppCategory.OTHERS,
                    result,
                )
            }
        }
    }

    // --- Invariants ---------------------------------------------------------

    @Test
    fun `classifier never returns the ALL sentinel`() {
        val probeSets = listOf(
            emptySet(),
            setOf(Intent.CATEGORY_APP_MAPS),
            setOf(Intent.CATEGORY_APP_MUSIC, Intent.CATEGORY_APP_EMAIL),
            AppCategoryClassifier.probeCategories.toSet(),
            setOf("com.example.CUSTOM"),
        )
        for (declared in -5..15) {
            for (matched in probeSets) {
                val result = AppCategoryClassifier.classify(declared, matched)
                assertTrue(
                    "declared=$declared matched=$matched produced $result",
                    result != AppCategory.ALL,
                )
            }
        }
    }
}
