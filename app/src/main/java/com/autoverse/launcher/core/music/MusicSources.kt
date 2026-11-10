package com.autoverse.launcher.core.music

/*
 * Pure rules for which installed media-browser services become source chips
 * on the Music page, and for reading the phone-over-Bluetooth source. No
 * Android types here so the rules are covered by plain JVM tests.
 */

/** Chip label of the phone-over-Bluetooth source. */
const val PHONE_SOURCE_LABEL = "Phone (Bluetooth)"

/** Packages that host the system's Bluetooth AVRCP media browser (car / A2DP-sink builds). */
private val BLUETOOTH_PACKAGES = setOf(
    "com.android.bluetooth",
    "com.google.android.bluetooth", // Bluetooth mainline module on newer GMS builds
)

/** Service classes of the AOSP Bluetooth browser across Android versions. */
private val BLUETOOTH_SERVICE_NAMES = listOf(
    "BluetoothMediaBrowserService",
    "A2dpMediaBrowserService",
)

/**
 * Apps that declare a MediaBrowserService but have no music library to
 * offer (assistant, system and navigation components).
 */
private val NON_MUSIC_PACKAGES = setOf(
    "com.google.android.googlequicksearchbox", // Google app / Assistant ("Google")
    "com.google.android.apps.googleassistant",
    "com.google.android.gms", // Google Play services
    "com.google.android.gsf",
    "com.google.android.play.games",
    "com.android.vending",
    "com.android.systemui",
    "com.google.android.projection.gearhead", // Android Auto
    "com.google.android.tts",
    "com.google.android.apps.maps",
    "com.waze",
    "com.google.android.as", // Android System Intelligence
    "com.google.android.apps.turbo",
    "com.google.android.dialer",
    "com.android.dialer",
    "com.android.server.telecom",
    "com.android.settings",
    "com.android.car.media", // the car's own media centre, not a source
    "com.android.car.carlauncher",
)

/** Labels Android reports differently from what drivers see on the phone. */
private val KNOWN_LABELS = mapOf(
    "com.google.android.apps.youtube.music" to "YT Music",
)

/** Package-name segments that say nothing about which app it is. */
private val GENERIC_SEGMENTS = setOf("com", "org", "net", "in", "io", "co", "android", "google", "apps", "app", "media", "player")

fun isBluetoothSource(packageName: String, serviceClass: String): Boolean =
    packageName in BLUETOOTH_PACKAGES ||
        BLUETOOTH_SERVICE_NAMES.any { serviceClass.endsWith(".$it") || serviceClass == it }

/** True for media sessions published by the Bluetooth stack (the phone's player). */
fun isBluetoothPackage(packageName: String): Boolean = packageName in BLUETOOTH_PACKAGES

fun isNonMusicPackage(packageName: String): Boolean = packageName in NON_MUSIC_PACKAGES

/** The name to show for [packageName]: a known override, else the system label, else the package. */
fun musicSourceLabel(packageName: String, systemLabel: String?): String =
    KNOWN_LABELS[packageName]
        ?: systemLabel?.trim()?.takeIf { it.isNotEmpty() && it != packageName }
        ?: packageHint(packageName, "").ifEmpty { packageName }

/** The phone source plus the app sources worth offering. */
data class MusicSources(
    val bluetooth: MusicApp?,
    val apps: List<MusicApp>,
)

/**
 * Splits discovered services into the Bluetooth phone source and app
 * sources: drops this app and known non-music packages, keeps one service
 * per package, and makes every label unique.
 */
fun selectMusicSources(candidates: List<MusicApp>, ownPackage: String): MusicSources {
    val usable = candidates
        .filter { it.packageName != ownPackage }
        .distinctBy { it.packageName }
    val bluetooth = usable.firstOrNull { isBluetoothSource(it.packageName, it.serviceClass) }
    val apps = usable
        .filter { !isBluetoothSource(it.packageName, it.serviceClass) }
        .filter { !isNonMusicPackage(it.packageName) }
        .map { it.copy(label = musicSourceLabel(it.packageName, it.label)) }
    return MusicSources(
        bluetooth = bluetooth?.copy(label = PHONE_SOURCE_LABEL),
        apps = disambiguateLabels(apps).sortedBy { it.label.lowercase() },
    )
}

/**
 * Gives apps that share a label a distinguishing suffix taken from their
 * package name ("YouTube" and "YouTube (Music)"); an app whose package adds
 * nothing keeps the plain label, and any remaining clash gets a number.
 */
fun disambiguateLabels(apps: List<MusicApp>): List<MusicApp> {
    val clashing = apps.groupBy { it.label.lowercase() }.filterValues { it.size > 1 }.keys
    val hinted = apps.map { app ->
        if (app.label.lowercase() !in clashing) return@map app
        val hint = packageHint(app.packageName, app.label)
        if (hint.isEmpty()) app else app.copy(label = "${app.label} ($hint)")
    }
    val seen = mutableMapOf<String, Int>()
    return hinted.map { app ->
        val key = app.label.lowercase()
        val n = (seen[key] ?: 0) + 1
        seen[key] = n
        if (n == 1) app else app.copy(label = "${app.label} $n")
    }
}

/** The most specific package segment not already part of [label], capitalised; "" when none. */
private fun packageHint(packageName: String, label: String): String {
    val compactLabel = label.lowercase().filter { it.isLetterOrDigit() }
    val segment = packageName.split('.')
        .map { it.lowercase() }
        .filter { it.isNotBlank() && it !in GENERIC_SEGMENTS }
        // A 1–2 letter segment ("a", "tv") is never "already in" the label just
        // because those letters appear inside a word of it.
        .lastOrNull { compactLabel.isEmpty() || it.length <= 2 || !compactLabel.contains(it) }
        ?: return ""
    return segment.replaceFirstChar { it.uppercase() }
}

/**
 * Real music apps that often keep their library private from other players
 * (Spotify) or until signed in. They keep their chip even when the library
 * check comes back empty, so the driver can still open them from Music.
 */
val KNOWN_MUSIC_APPS: Set<String> = setOf(
    "com.spotify.music",
    "com.jio.media.jiobeats", // JioSaavn
    "com.gaana",
    "com.bsbportal.music", // Wynk
    "com.amazon.mp3",
    "com.apple.android.music",
    "com.google.android.apps.youtube.music",
    "com.maxmpz.audioplayer", // Poweramp
    "deezer.android.app",
    "com.soundcloud.android",
    "com.hungama.myplay.activity",
)

fun isKnownMusicApp(packageName: String): Boolean = packageName in KNOWN_MUSIC_APPS

/** An app source is worth a chip only when its library root lists something to open or play. */
fun hasMusicContent(rootItems: List<BrowseItem>?): Boolean =
    rootItems?.any { it.browsable || it.playable } == true

// ---- phone over Bluetooth -------------------------------------------------------

/** android.media.session.PlaybackState codes (literal so JVM tests need no Android stubs). */
private const val STATE_NONE = 0
private const val STATE_PLAYING = 3
private const val STATE_BUFFERING = 6
private const val STATE_ERROR = 7

/** What the Bluetooth stack's media session currently reports. */
data class SessionSnapshot(
    /** PlaybackState code, or null when the session has no playback state. */
    val stateCode: Int?,
    val title: String?,
    val artist: String?,
)

/** The phone-over-Bluetooth source as shown on the Music page. */
sealed interface PhoneSourceState {
    data object Checking : PhoneSourceState

    /** No phone connected, or this unit's Bluetooth doesn't share phone music. */
    data object NotConnected : PhoneSourceState

    data class Connected(
        val title: String?,
        val artist: String?,
        val playing: Boolean,
    ) : PhoneSourceState
}

/** The Bluetooth stack explicitly says no phone is there (error state, no track). */
fun phoneReportsDisconnected(session: SessionSnapshot?): Boolean =
    session != null && session.stateCode == STATE_ERROR && session.title.isNullOrBlank()

/**
 * Reads the AOSP Bluetooth browser: with no phone it reports an error
 * playback state ("Bluetooth audio disconnected") and an empty root; a
 * connected phone shows up as a device folder in the root and/or a real
 * playback state and track.
 */
fun phoneSourceState(rootItemCount: Int, session: SessionSnapshot?): PhoneSourceState {
    val code = session?.stateCode
    val title = session?.title?.takeIf { it.isNotBlank() }
    val connected = when {
        title != null && code != STATE_ERROR -> true
        code == STATE_ERROR -> false
        rootItemCount > 0 -> true
        code != null && code != STATE_NONE -> true
        else -> false
    }
    if (!connected) return PhoneSourceState.NotConnected
    return PhoneSourceState.Connected(
        title = title,
        artist = session?.artist?.takeIf { it.isNotBlank() },
        playing = code == STATE_PLAYING || code == STATE_BUFFERING,
    )
}
