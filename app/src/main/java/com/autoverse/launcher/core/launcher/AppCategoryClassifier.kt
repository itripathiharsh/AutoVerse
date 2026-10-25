package com.autoverse.launcher.core.launcher

import android.content.Intent
import android.content.pm.ApplicationInfo
import com.autoverse.launcher.core.model.AppCategory

/**
 * Phase 15 — pure classifier mapping platform metadata onto a drawer
 * [AppCategory].
 *
 * Three signals are combined, in priority order:
 *
 *  1. The app's declared `ApplicationInfo.category` (API 26+). This is the
 *     strongest signal because the developer set it deliberately.
 *  2. The `CATEGORY_APP_*` intent categories its launcher activity matches.
 *     AppRepository runs one intent query per probed category per app-list
 *     load and hands the per-package union here. This covers apps that never
 *     set a category — notably most games, image, and news apps in the wild.
 *  3. (0.20.1) The app's own label/package keywords. In the wild very few
 *     apps declare `CATEGORY_APP_*` intent categories and many popular ones
 *     (Spotify, Waze, WhatsApp…) do not set a matching declared category on
 *     every build, so without this fallback they all landed in OTHERS. The
 *     keywords are matched against the app's own display name and package —
 *     the app describes itself; nothing is fetched or hardcoded per app.
 *
 * An app matching several intent categories is assigned by a fixed priority:
 * navigation first (a driver should never lose their maps), then music,
 * communication, and tools; gallery maps to [AppCategory.OTHERS] because the
 * drawer has no gallery chip. Keyword checks run in a fixed order (audio,
 * navigation, video, communication, tools) with audio first so
 * "YouTube Music" beats the "youtube" video hint. Nothing throws and nothing
 * is guessed beyond these documented rules: unmatched apps are
 * [AppCategory.OTHERS].
 *
 * All constants referenced here are compile-time (inlined into bytecode), so
 * this object is fully exercisable by plain JVM unit tests.
 */
object AppCategoryClassifier {

    /**
     * The `CATEGORY_APP_*` intent categories probed during app discovery, in
     * probe order. The order is also the multi-match priority: earlier
     * entries win.
     */
    val probeCategories: List<String> = listOf(
        Intent.CATEGORY_APP_MAPS,
        Intent.CATEGORY_APP_MUSIC,
        Intent.CATEGORY_APP_MESSAGING,
        Intent.CATEGORY_APP_EMAIL,
        Intent.CATEGORY_APP_CONTACTS,
        Intent.CATEGORY_APP_CALENDAR,
        Intent.CATEGORY_APP_BROWSER,
        Intent.CATEGORY_APP_CALCULATOR,
        Intent.CATEGORY_APP_GALLERY,
    )

    /**
     * Classifies one app. [applicationCategory] is the declared
     * `ApplicationInfo.category`, or [ApplicationInfo.CATEGORY_UNDEFINED]
     * when unknown (pre-API 26 devices, unresolvable info);
     * [matchedIntentCategories] is the set of probed `CATEGORY_APP_*` values
     * the app matched; [label]/[packageName] feed the keyword fallback (the
     * app's own display name and package id, matched case-insensitively).
     */
    fun classify(
        applicationCategory: Int,
        matchedIntentCategories: Set<String>,
        label: String = "",
        packageName: String = "",
    ): AppCategory {
        categoryOfApplicationCategory(applicationCategory)?.let { return it }
        categoryOfIntentCategories(matchedIntentCategories)?.let { return it }
        return categoryOfNameHints(label, packageName)
    }

    /** Maps a declared `ApplicationInfo` category; null = no opinion. */
    private fun categoryOfApplicationCategory(applicationCategory: Int): AppCategory? =
        when (applicationCategory) {
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.NAVIGATION
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC_AUDIO
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.COMMUNICATION
            ApplicationInfo.CATEGORY_PRODUCTIVITY,
            ApplicationInfo.CATEGORY_ACCESSIBILITY,
            -> AppCategory.TOOLS
            // GAME, IMAGE, NEWS, UNDEFINED, or unknown: no opinion — fall
            // through to the intent-category signal.
            else -> null
        }

    /** First matching probe category wins; null when none matched. */
    private fun categoryOfIntentCategories(matchedIntentCategories: Set<String>): AppCategory? {
        for (candidate in probeCategories) {
            if (candidate in matchedIntentCategories) return categoryOfIntentCategory(candidate)
        }
        return null
    }

    private fun categoryOfIntentCategory(intentCategory: String): AppCategory =
        when (intentCategory) {
            Intent.CATEGORY_APP_MAPS -> AppCategory.NAVIGATION
            Intent.CATEGORY_APP_MUSIC -> AppCategory.MUSIC_AUDIO
            Intent.CATEGORY_APP_MESSAGING,
            Intent.CATEGORY_APP_EMAIL,
            Intent.CATEGORY_APP_CONTACTS,
            -> AppCategory.COMMUNICATION
            Intent.CATEGORY_APP_CALENDAR,
            Intent.CATEGORY_APP_BROWSER,
            Intent.CATEGORY_APP_CALCULATOR,
            -> AppCategory.TOOLS
            // GALLERY: the drawer has no gallery chip — matched apps stay in
            // OTHERS and never reach the keyword fallback.
            else -> AppCategory.OTHERS
        }

    // Keyword fallback — checked against the lowercase label AND package.
    // Order is the priority: audio, navigation, video, communication, tools.
    private val audioHints = listOf(
        "music", "audio", "spotify", "podcast", "radio", "gaana", "saavn",
        "wynk", "audible", "soundcloud", "deezer", "tidal", "pandora",
        "shazam", "boomplay", "anghami",
    )
    private val navigationHints = listOf(
        "map", "nav", "waze", "sygic", "tomtom", "gps", "route", "here we go",
    )
    private val videoHints = listOf(
        "video", "movie", "cinema", "netflix", "prime video", "hotstar",
        "zee5", "sonyliv", "jiotv", "airtel xstream", "mx player", "vlc",
        "plex", "youtube", "tv",
    )
    private val communicationHints = listOf(
        "whatsapp", "telegram", "message", "messenger", "chat", "dialer",
        "phone", "contact", "signal", "viber", "wechat", "slack", "teams",
        "mail", "gmail", "outlook", "email", "meet", "duo", "imo",
    )
    private val toolsHints = listOf(
        "calculator", "calendar", "clock", "file", "manager", "scanner",
        "cleaner", "browser", "chrome", "firefox", "opera", "note",
        "recorder", "flashlight", "compass", "weather", "settings", "store",
        "backup", "vault", "keyboard", "theme", "launcher", "equalizer",
        "tool", "wifi", "bluetooth", "drive", "print", "torch",
    )

    private fun categoryOfNameHints(label: String, packageName: String): AppCategory {
        val haystack = "${label.lowercase()} ${packageName.lowercase()}"
        return when {
            audioHints.any { it in haystack } -> AppCategory.MUSIC_AUDIO
            navigationHints.any { it in haystack } -> AppCategory.NAVIGATION
            videoHints.any { it in haystack } -> AppCategory.VIDEO
            communicationHints.any { it in haystack } -> AppCategory.COMMUNICATION
            toolsHints.any { it in haystack } -> AppCategory.TOOLS
            else -> AppCategory.OTHERS
        }
    }
}
