package com.autoverse.launcher.core.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.media.MediaBrowserService
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** An installed app that offers its music library to car systems. */
data class MusicApp(
    val packageName: String,
    val serviceClass: String,
    val label: String,
)

/** One entry in a music app's library: a folder/playlist to open, or a song to play. */
data class BrowseItem(
    val mediaId: String,
    val title: String,
    val subtitle: String?,
    val browsable: Boolean,
    val playable: Boolean,
)

/**
 * Talks to music apps the way car systems do: through Android's standard
 * MediaBrowserService. Apps that allow it (most local players, podcast and
 * radio apps; some streaming apps) let AutoVerse list their library and
 * start playback without opening the app. Apps that refuse are reported
 * honestly — the driver then gets "Open app".
 */
class MusicAppBrowser(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var browser: MediaBrowser? = null
    private var connectedApp: MusicApp? = null
    private var controller: MediaController? = null

    /** Called on the main thread when the connected app's playback state or track changes. */
    var onSessionChanged: (() -> Unit)? = null

    private val sessionCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            onSessionChanged?.invoke()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            onSessionChanged?.invoke()
        }

        override fun onSessionDestroyed() {
            onSessionChanged?.invoke()
        }
    }

    /**
     * Installed apps exposing a media browser service (Spotify, JioSaavn,
     * Poweramp…), with the system Bluetooth browser split out as the phone
     * source and known non-music services (Google app, Play services…)
     * dropped. App sources still need [hasMusicContent] before showing.
     */
    fun discoverSources(): MusicSources = try {
        val pm = appContext.packageManager
        val found = pm.queryIntentServices(Intent(MediaBrowserService.SERVICE_INTERFACE), 0)
            .mapNotNull { info ->
                val s = info.serviceInfo ?: return@mapNotNull null
                MusicApp(
                    packageName = s.packageName,
                    serviceClass = s.name,
                    label = launcherLabel(pm, s.packageName)
                        ?: s.applicationInfo?.loadLabel(pm)?.toString()
                        ?: s.packageName,
                )
            }
        selectMusicSources(found, appContext.packageName)
    } catch (_: Exception) {
        MusicSources(bluetooth = null, apps = emptyList())
    }

    /** The name under the app's icon in the launcher (what the driver knows it as). */
    private fun launcherLabel(pm: PackageManager, packageName: String): String? = try {
        pm.getLaunchIntentForPackage(packageName)
            ?.let { pm.resolveActivity(it, 0) }
            ?.loadLabel(pm)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    /** What the connected app's media session reports right now; null when not connected. */
    fun session(): SessionSnapshot? {
        val c = controller ?: return null
        return try {
            val metadata = c.metadata
            SessionSnapshot(
                stateCode = c.playbackState?.state,
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Connects to [app]; null when the app refuses outside players. */
    suspend fun connect(app: MusicApp): String? {
        if (connectedApp == app && browser?.isConnected == true) return browser?.root
        disconnect()
        val root = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine<String?> { cont ->
                var b: MediaBrowser? = null
                val callback = object : MediaBrowser.ConnectionCallback() {
                    override fun onConnected() {
                        val connected = b ?: return cont.resumeOnce(null)
                        controller = try {
                            MediaController(appContext, connected.sessionToken).also {
                                it.registerCallback(sessionCallback, mainHandler)
                            }
                        } catch (_: Exception) {
                            null
                        }
                        cont.resumeOnce(connected.root)
                    }

                    override fun onConnectionFailed() = cont.resumeOnce(null)
                    override fun onConnectionSuspended() = cont.resumeOnce(null)
                }
                b = MediaBrowser(appContext, ComponentName(app.packageName, app.serviceClass), callback, Bundle())
                browser = b
                try {
                    b.connect()
                } catch (_: Exception) {
                    cont.resumeOnce(null)
                }
                cont.invokeOnCancellation { runCatching { b.disconnect() } }
            }
        }
        if (root == null) {
            disconnect()
        } else {
            connectedApp = app
        }
        return root
    }

    /** Lists the children of [parentId] in the connected app. */
    suspend fun children(parentId: String): List<BrowseItem>? {
        val b = browser?.takeIf { it.isConnected } ?: return null
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val callback = object : MediaBrowser.SubscriptionCallback() {
                    override fun onChildrenLoaded(id: String, children: MutableList<MediaBrowser.MediaItem>) {
                        runCatching { b.unsubscribe(parentId) }
                        cont.resumeOnce(children.map { it.toBrowseItem() })
                    }

                    override fun onError(id: String) {
                        runCatching { b.unsubscribe(parentId) }
                        cont.resumeOnce(null)
                    }
                }
                try {
                    b.subscribe(parentId, callback)
                } catch (_: Exception) {
                    cont.resumeOnce(null)
                }
            }
        }
    }

    /** Starts [item] in the connected app; true when the request was delivered. */
    fun play(item: BrowseItem): Boolean = try {
        controller?.transportControls?.playFromMediaId(item.mediaId, Bundle())
        controller != null
    } catch (_: Exception) {
        false
    }

    /** Resumes the connected app's last playback ("Play" on a source tile). */
    fun resume(): Boolean = try {
        controller?.transportControls?.play()
        controller != null
    } catch (_: Exception) {
        false
    }

    fun pause(): Boolean = try {
        controller?.transportControls?.pause()
        controller != null
    } catch (_: Exception) {
        false
    }

    fun skipToNext(): Boolean = try {
        controller?.transportControls?.skipToNext()
        controller != null
    } catch (_: Exception) {
        false
    }

    fun disconnect() {
        runCatching { controller?.unregisterCallback(sessionCallback) }
        runCatching { browser?.disconnect() }
        browser = null
        controller = null
        connectedApp = null
    }

    private fun MediaBrowser.MediaItem.toBrowseItem() = BrowseItem(
        mediaId = mediaId.orEmpty(),
        title = description.title?.toString()?.takeIf { it.isNotBlank() } ?: "Untitled",
        subtitle = description.subtitle?.toString()?.takeIf { it.isNotBlank() },
        browsable = isBrowsable,
        playable = isPlayable,
    )

    private companion object {
        const val CONNECT_TIMEOUT_MS = 6_000L
    }
}

private fun <T> CancellableContinuation<T>.resumeOnce(value: T) {
    if (isActive) resume(value)
}
