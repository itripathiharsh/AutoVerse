package com.autoverse.launcher.core.media

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Whether AutoVerse is allowed to read other apps' media sessions now.
 * Reads the canonical system setting directly (the same source
 * NotificationManager exposes internally), which works on every supported
 * API level; the check is cheap, read-only, and refreshed on resume.
 */
fun isNotificationAccessEnabled(context: Context): Boolean = try {
    val raw = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    )
    isNotificationAccessGranted(raw, context.packageName)
} catch (_: Exception) {
    false
}

/** Splits the colon-separated listener setting into ComponentName strings. */
fun parseEnabledListeners(rawSetting: String?): List<String> =
    rawSetting
        ?.split(':')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

/** Package names of every enabled listener in the raw setting. */
fun enabledListenerPackages(rawSetting: String?): Set<String> =
    parseEnabledListeners(rawSetting)
        .mapNotNull { listener -> listener.substringBefore('/').trim().ifBlank { null } }
        .toSet()

/** Pure overload used below API 27 and by unit tests. */
fun isNotificationAccessGranted(rawSetting: String?, packageName: String): Boolean =
    enabledListenerPackages(rawSetting).contains(packageName)

/** System settings page where the user grants notification-listener access. */
fun notificationAccessSettingsIntent(): Intent =
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
