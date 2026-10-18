package com.autoverse.launcher.core.model

import android.content.ComponentName

/**
 * One launchable activity found on the device. [activityName] is the
 * fully-qualified launcher activity class; [key] matches the
 * ComponentName.flattenToString() format ("package/activity") so favorites
 * persisted as strings remain readable and comparable.
 *
 * [category] is the drawer category derived at scan time (see
 * AppCategoryClassifier); [firstInstallTime] is the package's install
 * timestamp in epoch millis, or 0 when the platform could not report it.
 * Both carry safe defaults so existing call sites (tests, previews) stay
 * source-compatible.
 */
data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: AppCategory = AppCategory.OTHERS,
    val firstInstallTime: Long = 0L,
) {
    val key: String get() = "$packageName/$activityName"

    val component: ComponentName get() = ComponentName(packageName, activityName)
}
