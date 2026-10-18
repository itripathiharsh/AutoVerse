package com.autoverse.launcher.core.launcher

import com.autoverse.launcher.core.model.AppCategory
import com.autoverse.launcher.core.model.InstalledApp

/**
 * Pure list operations over installed apps. Kept free of Android framework
 * calls so behavior (filtering rules, ordering, favorites handling) is
 * unit-testable on the JVM.
 */
object AppCatalog {

    /**
     * Normalizes the raw PackageManager query result: drops AutoVerse itself
     * (re-launching the launcher from its own drawer loops), keeps at most one
     * entry per package (the first resolver wins, which matches the platform's
     * preferred launcher activity), and sorts by label, case-insensitively.
     */
    fun prepare(raw: List<InstalledApp>, ownPackage: String): List<InstalledApp> =
        raw.asSequence()
            .filter { it.packageName != ownPackage }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()

    /** Case-insensitive substring match on label or package name. */
    fun search(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return apps
        return apps.filter {
            it.label.contains(trimmed, ignoreCase = true) ||
                it.packageName.contains(trimmed, ignoreCase = true)
        }
    }

    /**
     * Phase 15 — category filter over prepared apps. [AppCategory.ALL] is the
     * "no filter" sentinel and passes the list through untouched.
     */
    fun filterByCategory(apps: List<InstalledApp>, category: AppCategory): List<InstalledApp> =
        if (category == AppCategory.ALL) apps else apps.filter { it.category == category }

    /**
     * Phase 15 — combined query + category filter for the drawer. Search runs
     * first because it only ever narrows the list; the order of [prepare] is
     * preserved.
     */
    fun filter(
        apps: List<InstalledApp>,
        query: String,
        category: AppCategory,
    ): List<InstalledApp> = filterByCategory(search(apps, query), category)

    /**
     * Phase 15 — most recently installed apps for the Newly Installed panel,
     * newest first. Apps whose install time could not be read (0) are
     * excluded — the panel only ever shows data the platform actually
     * reported. Ties break on lowercase label so the order is stable across
     * refreshes.
     */
    fun newlyInstalled(apps: List<InstalledApp>, limit: Int): List<InstalledApp> {
        if (limit <= 0) return emptyList()
        return apps.asSequence()
            .filter { it.firstInstallTime > 0L }
            .sortedWith(
                compareByDescending<InstalledApp> { it.firstInstallTime }
                    .thenBy { it.label.lowercase() },
            )
            .take(limit)
            .toList()
    }

    /** Maps favorite keys (launch order) onto currently installed apps. */
    fun resolveFavorites(
        favorites: List<String>,
        installed: List<InstalledApp>,
    ): List<InstalledApp> {
        if (favorites.isEmpty() || installed.isEmpty()) return emptyList()
        val byKey = installed.associateBy { it.key }
        return favorites.mapNotNull { byKey[it] }
    }

    /** Removes favorite keys that point at apps that are no longer installed. */
    fun pruneFavorites(
        favorites: List<String>,
        installed: List<InstalledApp>,
    ): List<String> {
        if (favorites.isEmpty()) return emptyList()
        val installedKeys = installed.asSequence().map { it.key }.toHashSet()
        return favorites.filter { it in installedKeys }
    }
}
