package com.autoverse.launcher.core.music

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONArray
import org.json.JSONObject

/**
 * AutoVerse's own music player for songs on the unit, an SD card or a USB
 * drive. It runs as a media service so music keeps playing while the driver
 * uses Maps or any other app, takes audio focus like a real car player
 * (ducks for navigation prompts, pauses when unplugged/for calls), and
 * publishes a normal media session — so the Home music card, steering-wheel
 * buttons and the system media controls all work with it.
 *
 * Playback resumption: the last queue is remembered, so a "play" key after
 * the car starts (or a steering-wheel press) picks up where the driver left
 * off even after the unit rebooted.
 */
class AutoVersePlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                saveIndex(applicationContext, player.currentMediaItemIndex)
            }
        })
        session = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    val (items, index) = loadLastQueue(applicationContext)
                    return if (items.isEmpty()) {
                        Futures.immediateFailedFuture(UnsupportedOperationException("nothing to resume"))
                    } else {
                        Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(items, index, 0L))
                    }
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // A launcher is rarely swiped away; if it is and nothing plays, stop.
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        private const val PREFS = "autoverse_music"
        private const val KEY_QUEUE = "last_queue"
        private const val KEY_INDEX = "last_index"
        private const val MAX_SAVED = 500

        /** Remembers the queue AutoVerse's player was given (for resumption). */
        fun saveQueue(context: Context, tracks: List<LocalTrack>, index: Int) {
            val arr = JSONArray()
            tracks.take(MAX_SAVED).forEach {
                arr.put(JSONObject().put("id", it.id).put("u", it.uri).put("t", it.title).put("a", it.artist).put("al", it.album))
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_QUEUE, arr.toString()).putInt(KEY_INDEX, index).apply()
        }

        private fun saveIndex(context: Context, index: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_INDEX, index).apply()
        }

        fun loadLastQueue(context: Context): Pair<List<MediaItem>, Int> = try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString(KEY_QUEUE, "[]"))
            val items = List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                MediaItem.Builder()
                    .setMediaId(o.optLong("id").toString())
                    .setUri(o.getString("u"))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(o.optString("t"))
                            .setArtist(o.optString("a"))
                            .setAlbumTitle(o.optString("al"))
                            .build(),
                    )
                    .build()
            }
            items to prefs.getInt(KEY_INDEX, 0).coerceIn(0, (items.size - 1).coerceAtLeast(0))
        } catch (_: Exception) {
            emptyList<MediaItem>() to 0
        }
    }
}
