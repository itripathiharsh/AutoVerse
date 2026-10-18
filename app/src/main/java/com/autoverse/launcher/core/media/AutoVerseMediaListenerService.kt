package com.autoverse.launcher.core.media

import android.service.notification.NotificationListenerService

/**
 * Exists only to satisfy the platform's media-session access model: reading
 * other apps' active sessions requires holding a bound notification
 * listener. This service is intentionally empty — it never reads, stores,
 * logs or forwards notification content.
 */
class AutoVerseMediaListenerService : NotificationListenerService()
