package com.autoverse.launcher.core.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.autoverse.launcher.core.model.InstalledApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PackageManager-backed discovery + icon loading. All heavy work runs on
 * [Dispatchers.IO] from the caller; results are normalized by [AppCatalog].
 */
class AppRepository(private val context: Context) {

    private val iconCache = LruCache<String, ImageBitmap>(ICON_CACHE_ENTRIES)

    /**
     * Queries launcher-intent activities. On API 30+ this is scoped by the
     * manifest <queries> declaration; older versions return all candidates.
     * The int-flags overload works on every supported API level (minSdk 23).
     *
     * Category classification (Phase 15) adds, per load, one CATEGORY_APP_*
     * intent query per probed category plus one getPackageInfo call per
     * package for the install timestamp. Both cheap lookups are individually
     * guarded, so a single failure degrades one app to its documented
     * fallback rather than failing the whole scan.
     */
    fun loadInstalledApps(): List<InstalledApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(launcherIntent, 0)
        val intentCategoriesByPackage = loadIntentCategoryIndex(packageManager)
        val installTimesByPackage = mutableMapOf<String, Long>()
        val raw = resolved.mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            val label = runCatching { info.loadLabel(packageManager).toString() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: activity.packageName
            InstalledApp(
                packageName = activity.packageName,
                activityName = activity.name,
                label = label,
                category = AppCategoryClassifier.classify(
                    applicationCategory = declaredApplicationCategory(activity.applicationInfo),
                    matchedIntentCategories = intentCategoriesByPackage[activity.packageName].orEmpty(),
                ),
                firstInstallTime = installTimesByPackage.getOrPut(activity.packageName) {
                    firstInstallTimeOf(packageManager, activity.packageName)
                },
            )
        }
        return AppCatalog.prepare(raw, context.packageName)
    }

    /**
     * Inverted index of packages matching any probed CATEGORY_APP_* value.
     * One query per category; a query that throws (some OEM package managers
     * reject unknown categories) contributes nothing instead of failing
     * discovery.
     */
    private fun loadIntentCategoryIndex(packageManager: PackageManager): Map<String, Set<String>> {
        val index = mutableMapOf<String, MutableSet<String>>()
        for (category in AppCategoryClassifier.probeCategories) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            val matches = try {
                packageManager.queryIntentActivities(intent, 0)
            } catch (_: Exception) {
                continue
            }
            for (match in matches) {
                val packageName = match.activityInfo?.packageName ?: continue
                index.getOrPut(packageName) { mutableSetOf() }.add(category)
            }
        }
        return index
    }

    /**
     * Declared ApplicationInfo category. The field does not exist before
     * API 26, hence the runtime guard; unresolved info yields
     * CATEGORY_UNDEFINED, which the classifier treats as "no opinion".
     */
    @Suppress("NewApi")
    private fun declaredApplicationCategory(applicationInfo: ApplicationInfo?): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return ApplicationInfo.CATEGORY_UNDEFINED
        }
        return applicationInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED
    }

    /**
     * Install timestamp for the Newly Installed panel; 0 means "unknown"
     * (excluded there). The int-flags getPackageInfo overload is deprecated
     * on API 33+ but remains the only overload available on all supported
     * API levels.
     */
    @Suppress("DEPRECATION")
    private fun firstInstallTimeOf(packageManager: PackageManager, packageName: String): Long =
        runCatching { packageManager.getPackageInfo(packageName, 0).firstInstallTime }
            .getOrDefault(0L)

    /** Loads (and caches) one app icon as an [ImageBitmap]; null on failure. */
    suspend fun loadIcon(app: InstalledApp): ImageBitmap? {
        iconCache.get(app.key)?.let { return it }
        return withContext(Dispatchers.IO) {
            iconCache.get(app.key) ?: try {
                val drawable: Drawable = context.packageManager.getActivityIcon(app.component)
                drawable.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap()
                    .also { iconCache.put(app.key, it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Drops cached icons; they are reloaded lazily on next use. */
    fun trimMemory() {
        iconCache.evictAll()
    }

    private companion object {
        /** ~36 KB per icon at 96px; 64 entries stays under ~2.5 MB. */
        const val ICON_CACHE_ENTRIES = 64
        const val ICON_SIZE_PX = 96
    }
}
