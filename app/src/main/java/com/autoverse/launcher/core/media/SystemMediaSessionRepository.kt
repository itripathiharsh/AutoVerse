package com.autoverse.launcher.core.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

private const val READ_SESSIONS_FAILED_MESSAGE = "Couldn't read media sessions. Try again."

/**
 * [MediaSessionRepository] over the platform's MediaSessionManager. Requires
 * notification-listener access, provided by [AutoVerseMediaListenerService];
 * without it every emission is [MediaUiState.AccessRequired]. No polling:
 * updates arrive through the active-sessions listener and controller
 * callbacks registered only while the flow is collected.
 */
class SystemMediaSessionRepository(context: Context) : MediaSessionRepository {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessionManager =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val listenerComponent =
        ComponentName(appContext, AutoVerseMediaListenerService::class.java)
    private val artworkCache = MediaArtworkCache(appContext)
    private val controllerLock = Any()

    @Volatile
    private var activeController: MediaController? = null

    // Embedded-artwork memoization, touched only from the observer (main thread).
    private var memoizedMetadata: MediaMetadata? = null
    private var memoizedArtwork: ImageBitmap? = null

    override fun observe(): Flow<MediaUiState> = callbackFlow {
        if (!isNotificationAccessEnabled(appContext)) {
            trySend(MediaUiState.AccessRequired)
            awaitClose { }
            return@callbackFlow
        }
        trySend(MediaUiState.Connecting)

        var boundController: MediaController? = null
        var controllerCallback: MediaController.Callback? = null

        fun emitFromBound() {
            val controller = boundController ?: return
            trySend(activeStateOf(controller))
        }

        fun unbindController() {
            val controller = boundController ?: return
            controllerCallback?.let(controller::unregisterCallback)
            boundController = null
            synchronized(controllerLock) {
                if (activeController === controller) activeController = null
            }
        }

        fun bindController(controller: MediaController) {
            unbindController()
            val callback = controllerCallback ?: return
            controller.registerCallback(callback, mainHandler)
            boundController = controller
            synchronized(controllerLock) { activeController = controller }
        }

        fun refreshSessions() {
            val controllers = try {
                sessionManager.getActiveSessions(listenerComponent)
            } catch (_: SecurityException) {
                unbindController()
                trySend(MediaUiState.AccessRequired)
                return
            } catch (_: Exception) {
                unbindController()
                trySend(MediaUiState.Error(READ_SESSIONS_FAILED_MESSAGE))
                return
            }

            val candidates = controllers.map { controller ->
                MediaSessionCandidate(
                    key = candidateKeyOf(controller),
                    playbackStateCode = controller.playbackState?.state
                        ?: PlaybackState.STATE_NONE,
                    hasMetadata = controller.metadata != null,
                )
            }
            val selected = selectMediaSession(candidates)
            val controller = selected?.let { candidate ->
                controllers.firstOrNull { candidateKeyOf(it) == candidate.key }
            }

            if (controller == null) {
                unbindController()
                trySend(MediaUiState.NoSession)
                return
            }

            bindController(controller)
            trySend(activeStateOf(controller))
        }

        controllerCallback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) = emitFromBound()

            override fun onMetadataChanged(metadata: MediaMetadata?) = emitFromBound()

            override fun onSessionDestroyed() {
                unbindController()
                refreshSessions()
            }
        }

        val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener {
            refreshSessions()
        }

        try {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsListener,
                listenerComponent,
                mainHandler,
            )
        } catch (_: SecurityException) {
            trySend(MediaUiState.AccessRequired)
            awaitClose { }
            return@callbackFlow
        } catch (_: Exception) {
            trySend(MediaUiState.Error(READ_SESSIONS_FAILED_MESSAGE))
            awaitClose { }
            return@callbackFlow
        }

        refreshSessions()

        awaitClose {
            unbindController()
            sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
        }
    }

    override suspend fun send(command: MediaCommand): MediaCommandOutcome =
        withContext(Dispatchers.IO) {
            if (!isNotificationAccessEnabled(appContext)) {
                return@withContext MediaCommandOutcome.ACCESS_LOST
            }
            val controller = synchronized(controllerLock) { activeController }
                ?: return@withContext MediaCommandOutcome.FAILED
            try {
                when (command) {
                    MediaCommand.PLAY -> controller.transportControls.play()
                    MediaCommand.PAUSE -> controller.transportControls.pause()
                    MediaCommand.NEXT -> controller.transportControls.skipToNext()
                    MediaCommand.PREVIOUS -> controller.transportControls.skipToPrevious()
                }
                MediaCommandOutcome.SENT
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                classifyMediaCommandFailure(failure)
            }
        }

    override suspend fun loadArtwork(uri: String, targetMaxPx: Int): ImageBitmap? =
        artworkCache.load(uri, targetMaxPx)

    override fun trimMemory() {
        artworkCache.trimMemory()
    }

    private fun candidateKeyOf(controller: MediaController): String =
        "${controller.packageName}#${System.identityHashCode(controller.sessionToken)}"

    private fun activeStateOf(controller: MediaController): MediaUiState.Active {
        val metadata = controller.metadata
        val playback = controller.playbackState
        val status = playbackStatusOf(playback?.state ?: PlaybackState.STATE_NONE)
        return MediaUiState.Active(
            packageName = controller.packageName,
            appLabel = resolveAppLabel(controller.packageName),
            title = metadataString(metadata, MediaMetadata.METADATA_KEY_TITLE),
            artist = metadataString(metadata, MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadataString(metadata, MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            album = metadataString(metadata, MediaMetadata.METADATA_KEY_ALBUM),
            artworkUri = metadataString(metadata, MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadataString(metadata, MediaMetadata.METADATA_KEY_ART_URI)
                ?: metadataString(metadata, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI),
            embeddedArtwork = embeddedArtworkOf(metadata),
            status = status,
            controls = mediaControlsOf(mediaActionSet(playback?.actions ?: 0L), status),
            progress = playback?.let {
                mediaProgressOf(
                    positionMs = it.position,
                    durationMs = metadataDurationMs(metadata),
                    playbackSpeed = it.playbackSpeed,
                    playing = status.isPlaying,
                    lastPositionUpdateTimeMs = it.lastPositionUpdateTime,
                    nowElapsedRealtimeMs = SystemClock.elapsedRealtime(),
                )
            },
        )
    }

    private fun resolveAppLabel(packageName: String): String? = try {
        val info = appContext.packageManager.getApplicationInfo(packageName, 0)
        appContext.packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        null
    }

    private fun metadataString(metadata: MediaMetadata?, key: String): String? = try {
        metadata?.getString(key)
    } catch (_: Exception) {
        null
    }

    private fun metadataDurationMs(metadata: MediaMetadata?): Long = try {
        metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    } catch (_: Exception) {
        0L
    }

    private fun embeddedArtworkOf(metadata: MediaMetadata?): ImageBitmap? {
        if (metadata == null) return null
        if (metadata === memoizedMetadata) return memoizedArtwork
        val artwork = artworkBitmapOf(metadata)
        memoizedMetadata = metadata
        memoizedArtwork = artwork
        return artwork
    }

    private fun artworkBitmapOf(metadata: MediaMetadata): ImageBitmap? = try {
        val bitmap: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}
