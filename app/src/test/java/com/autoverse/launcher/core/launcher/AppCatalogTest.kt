package com.autoverse.launcher.core.launcher

import com.autoverse.launcher.core.model.AppCategory
import com.autoverse.launcher.core.model.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 4/15 — drawer list rules. Filtering, ordering, one-tile-per-package,
 * search behavior, category filtering, the combined query+category pass, the
 * Newly Installed panel, and favorites resolution are pinned here so the
 * launcher logic is verified without a device.
 */
class AppCatalogTest {

    private fun app(
        packageName: String,
        label: String,
        activity: String = ".MainActivity",
        category: AppCategory = AppCategory.OTHERS,
        firstInstallTime: Long = 0L,
    ) = InstalledApp(packageName, "$packageName$activity", label, category, firstInstallTime)

    private val raw = listOf(
        app("com.bravo.maps", "Bravo Maps"),
        app("com.alpha.music", "alpha music", ".PlayerActivity"),
        app("com.alpha.music", "alpha music duplicate", ".SecondActivity"),
        app("com.alpha.music", "alpha music", ".ThirdActivity"),
        app("com.charlie.phone", "Charlie Phone"),
    )

    // --- prepare ------------------------------------------------------------

    @Test
    fun `prepare keeps first activity per package`() {
        val prepared = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        val alphaEntries = prepared.filter { it.packageName == "com.alpha.music" }
        assertEquals(1, alphaEntries.size)
        assertEquals("com.alpha.music.PlayerActivity", alphaEntries.single().activityName)
    }

    @Test
    fun `prepare excludes the launcher itself`() {
        val withSelf = raw + app("com.autoverse.launcher", "AutoVerse")
        val prepared = AppCatalog.prepare(withSelf, ownPackage = "com.autoverse.launcher")
        assertTrue(prepared.none { it.packageName == "com.autoverse.launcher" })
    }

    @Test
    fun `prepare sorts case-insensitively by label`() {
        val prepared = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        val labels = prepared.map { it.label }
        assertEquals(listOf("alpha music", "Bravo Maps", "Charlie Phone"), labels)
    }

    @Test
    fun `prepare on empty input returns empty list`() {
        assertTrue(AppCatalog.prepare(emptyList(), ownPackage = "com.x").isEmpty())
    }

    // --- search -------------------------------------------------------------

    @Test
    fun `search with blank query returns everything`() {
        val prepared = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        assertEquals(prepared, AppCatalog.search(prepared, ""))
        assertEquals(prepared, AppCatalog.search(prepared, "   "))
    }

    @Test
    fun `search matches labels case-insensitively`() {
        val prepared = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        assertEquals(
            listOf("Bravo Maps"),
            AppCatalog.search(prepared, "BRAVO").map { it.label },
        )
    }

    @Test
    fun `search also matches the package name`() {
        val prepared = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        assertEquals(
            listOf("Charlie Phone"),
            AppCatalog.search(prepared, "charlie").map { it.label },
        )
    }

    @Test
    fun `search with no match returns empty list`() {
        val prepared = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        assertTrue(AppCatalog.search(prepared, "zzz-nothing").isEmpty())
    }

    // --- category filtering (Phase 15) --------------------------------------

    private val categorized = listOf(
        app("com.bravo.maps", "Bravo Maps", category = AppCategory.NAVIGATION, firstInstallTime = 1_000L),
        app(
            "com.alpha.music",
            "alpha music",
            ".PlayerActivity",
            category = AppCategory.MUSIC_AUDIO,
            firstInstallTime = 3_000L,
        ),
        app(
            "com.charlie.phone",
            "Charlie Phone",
            category = AppCategory.COMMUNICATION,
            firstInstallTime = 2_000L,
        ),
        app("com.tools.timer", "Tools Timer", category = AppCategory.TOOLS),
    )

    @Test
    fun `filterByCategory with ALL passes the list through untouched`() {
        assertEquals(categorized, AppCatalog.filterByCategory(categorized, AppCategory.ALL))
    }

    @Test
    fun `filterByCategory keeps only the matching category`() {
        assertEquals(
            listOf("Bravo Maps"),
            AppCatalog.filterByCategory(categorized, AppCategory.NAVIGATION).map { it.label },
        )
        assertEquals(
            listOf("alpha music"),
            AppCatalog.filterByCategory(categorized, AppCategory.MUSIC_AUDIO).map { it.label },
        )
    }

    @Test
    fun `filterByCategory with no match returns empty list`() {
        assertTrue(AppCatalog.filterByCategory(categorized, AppCategory.VIDEO).isEmpty())
        assertTrue(AppCatalog.filterByCategory(emptyList(), AppCategory.TOOLS).isEmpty())
    }

    // --- combined query + category (Phase 15) -------------------------------

    @Test
    fun `filter with blank query behaves exactly like the category filter`() {
        assertEquals(
            AppCatalog.filterByCategory(categorized, AppCategory.NAVIGATION),
            AppCatalog.filter(categorized, "   ", AppCategory.NAVIGATION),
        )
        assertEquals(
            categorized,
            AppCatalog.filter(categorized, "", AppCategory.ALL),
        )
    }

    @Test
    fun `filter narrows by both query and category`() {
        assertEquals(
            listOf("Bravo Maps"),
            AppCatalog.filter(categorized, "maps", AppCategory.NAVIGATION).map { it.label },
        )
        assertEquals(
            listOf("Bravo Maps"),
            AppCatalog.filter(categorized, "  MAPS  ", AppCategory.ALL).map { it.label },
        )
        assertTrue(AppCatalog.filter(categorized, "maps", AppCategory.TOOLS).isEmpty())
    }

    // --- newly installed (Phase 15) -----------------------------------------

    @Test
    fun `newlyInstalled sorts newest first and respects the limit`() {
        assertEquals(
            listOf("alpha music", "Charlie Phone"),
            AppCatalog.newlyInstalled(categorized, limit = 2).map { it.label },
        )
    }

    @Test
    fun `newlyInstalled excludes apps whose install time was not reported`() {
        assertTrue(
            AppCatalog.newlyInstalled(categorized, limit = 10)
                .none { it.label == "Tools Timer" },
        )
    }

    @Test
    fun `newlyInstalled breaks ties on lowercase label for a stable order`() {
        val tied = listOf(
            app("com.zebra.app", "Zebra App", firstInstallTime = 5_000L),
            app("com.apple.app", "apple app", firstInstallTime = 5_000L),
            app("com.newer.app", "Newer", firstInstallTime = 6_000L),
        )
        assertEquals(
            listOf("Newer", "apple app", "Zebra App"),
            AppCatalog.newlyInstalled(tied, limit = 10).map { it.label },
        )
    }

    @Test
    fun `newlyInstalled with non-positive limit or empty input returns empty list`() {
        assertTrue(AppCatalog.newlyInstalled(categorized, limit = 0).isEmpty())
        assertTrue(AppCatalog.newlyInstalled(categorized, limit = -1).isEmpty())
        assertTrue(AppCatalog.newlyInstalled(emptyList(), limit = 5).isEmpty())
    }

    // --- favorites ----------------------------------------------------------

    @Test
    fun `resolveFavorites preserves stored order, not install order`() {
        val installed = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        val keys = listOf(
            "com.charlie.phone/com.charlie.phone.MainActivity",
            "com.bravo.maps/com.bravo.maps.MainActivity",
        )
        assertEquals(
            listOf("Charlie Phone", "Bravo Maps"),
            AppCatalog.resolveFavorites(keys, installed).map { it.label },
        )
    }

    @Test
    fun `resolveFavorites drops keys that are not installed`() {
        val installed = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        val keys = listOf("com.gone.app/com.gone.app.MainActivity")
        assertTrue(AppCatalog.resolveFavorites(keys, installed).isEmpty())
    }

    @Test
    fun `pruneFavorites keeps only installed keys in original order`() {
        val installed = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        val favorites = listOf(
            "com.gone.app/com.gone.app.MainActivity",
            "com.bravo.maps/com.bravo.maps.MainActivity",
            "com.alpha.music/com.alpha.music.PlayerActivity",
        )
        assertEquals(
            listOf(
                "com.bravo.maps/com.bravo.maps.MainActivity",
                "com.alpha.music/com.alpha.music.PlayerActivity",
            ),
            AppCatalog.pruneFavorites(favorites, installed),
        )
    }

    @Test
    fun `pruneFavorites on empty favorites returns empty list`() {
        val installed = AppCatalog.prepare(raw, ownPackage = "com.autoverse.launcher")
        assertTrue(AppCatalog.pruneFavorites(emptyList(), installed).isEmpty())
    }
}
