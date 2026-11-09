package com.autoverse.launcher.core.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.os.Bundle
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
    private var browser: MediaBrowser? = null
    private var connectedApp: MusicApp? = null
    private var controller: MediaController? = null

    /** Every installed app exposing a media browser service (Spotify, JioSaavn, Poweramp…). */
    fun installedMusicApps(): List<MusicApp> = try {
        val pm = appContext.packageManager
        pm.queryIntentServices(Intent(MediaBrowserService.SERVICE_INTERFACE), 0)
            .mapNotNull { info ->
                val s = info.serviceInfo ?: return@mapNotNull null
                if (s.packageName == appContext.packageName) return@mapNotNull null
                MusicApp(
                    packageName = s.packageName,
                    serviceClass = s.name,
                    label = s.applicationInfo.loadLabel(pm).toString(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    } catch (_: Exception) {
        emptyList()
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
                            MediaController(appContext, connected.sessionToken)
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

    fun disconnect() {
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
