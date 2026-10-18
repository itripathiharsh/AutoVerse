package com.autoverse.launcher.core.preferences

import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.PerformanceProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMappingTest {

    @Test
    fun `every theme id round-trips through storage`() {
        AutoVerseThemeId.entries.forEach { id ->
            assertEquals(id, themeIdFromStorage(id.name))
        }
    }

    @Test
    fun `missing stored theme falls back to the app default`() {
        assertEquals(AppSettings().themeId, themeIdFromStorage(null))
    }

    @Test
    fun `unknown stored theme falls back to the app default`() {
        listOf("AURORA", "", "NEON PULSE", "obsidian").forEach { stored ->
            assertEquals("stored='$stored'", AppSettings().themeId, themeIdFromStorage(stored))
        }
    }

    @Test
    fun `every performance profile round-trips through storage`() {
        PerformanceProfile.entries.forEach { profile ->
            assertEquals(profile, profileFromStorage(profile.name))
        }
    }

    @Test
    fun `missing stored profile falls back to the conservative default`() {
        assertEquals(AppSettings().profile, profileFromStorage(null))
        assertEquals(PerformanceProfile.BALANCED, profileFromStorage(null))
    }

    @Test
    fun `unknown stored profile falls back to the conservative default`() {
        listOf("TURBO", "", "balanced", "BALANCED ").forEach { stored ->
            assertEquals("stored='$stored'", AppSettings().profile, profileFromStorage(stored))
        }
    }

    @Test
    fun `every appearance mode round-trips through storage`() {
        AppearanceMode.entries.forEach { mode ->
            assertEquals(mode, appearanceModeFromStorage(mode.name))
        }
    }

    @Test
    fun `missing stored appearance mode falls back to the app default`() {
        assertEquals(AppSettings().appearanceMode, appearanceModeFromStorage(null))
    }

    @Test
    fun `unknown stored appearance mode falls back to the app default`() {
        listOf("AUTO", "", "night", "NIGHT ").forEach { stored ->
            assertEquals(
                "stored='$stored'",
                AppSettings().appearanceMode,
                appearanceModeFromStorage(stored),
            )
        }
    }

    // --- Phase 16: text scale ----------------------------------------------

    @Test
    fun `stored text scale inside the range is kept`() {
        assertEquals(1.0f, textScaleFromStorage(1.0f), 0.0001f)
        assertEquals(TEXT_SCALE_MIN, textScaleFromStorage(TEXT_SCALE_MIN), 0.0001f)
        assertEquals(TEXT_SCALE_MAX, textScaleFromStorage(TEXT_SCALE_MAX), 0.0001f)
        assertEquals(1.15f, textScaleFromStorage(1.15f), 0.0001f)
    }

    @Test
    fun `missing or non-finite text scale falls back to the default`() {
        assertEquals(AppSettings().textScale, textScaleFromStorage(null), 0.0001f)
        assertEquals(AppSettings().textScale, textScaleFromStorage(Float.NaN), 0.0001f)
    }

    @Test
    fun `out-of-range text scale clamps into the safe range`() {
        assertEquals(TEXT_SCALE_MIN, textScaleFromStorage(0.1f), 0.0001f)
        assertEquals(TEXT_SCALE_MAX, textScaleFromStorage(9.9f), 0.0001f)
        assertEquals(TEXT_SCALE_MIN, textScaleFromStorage(Float.NEGATIVE_INFINITY), 0.0001f)
        assertEquals(TEXT_SCALE_MAX, textScaleFromStorage(Float.POSITIVE_INFINITY), 0.0001f)
    }

    @Test
    fun `text scale percent formats whole numbers`() {
        assertEquals(100, textScalePercent(1.0f))
        assertEquals(85, textScalePercent(TEXT_SCALE_MIN))
        assertEquals(130, textScalePercent(TEXT_SCALE_MAX))
        assertEquals(115, textScalePercent(1.15f))
    }
}
