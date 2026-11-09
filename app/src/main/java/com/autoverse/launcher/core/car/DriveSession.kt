package com.autoverse.launcher.core.car

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/** AutoVerse hidden this long means the car was switched off — the next start is a new drive. */
const val NEW_DRIVE_GAP_MS = 20 * 60_000L

/** Shortly after the unit booted, AutoVerse's first start is a new drive too. */
const val FRESH_BOOT_MS = 10 * 60_000L

/**
 * Pure rule: is this start a new drive? Either AutoVerse was in the
 * background long enough for the car to have been off (many head units sleep
 * instead of rebooting), or this is the first start right after a boot.
 */
fun isNewDrive(gapSinceStopMs: Long?, uptimeMs: Long, firstStartThisProcess: Boolean): Boolean =
    (gapSinceStopMs != null && gapSinceStopMs >= NEW_DRIVE_GAP_MS) ||
        (firstStartThisProcess && uptimeMs <= FRESH_BOOT_MS)

/**
 * Remembers when AutoVerse was last hidden and whether music was playing, so
 * the next drive can pick up where the last one ended.
 */
class DriveSession(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("autoverse_drive", Context.MODE_PRIVATE)

    fun onStop(musicPlaying: Boolean) {
        prefs.edit().putLong(KEY_STOP_MS, System.currentTimeMillis()).putBoolean(KEY_PLAYING, musicPlaying).apply()
    }

    /** Returns true when this start is a new drive. */
    fun onStart(): Boolean {
        val stopped = prefs.getLong(KEY_STOP_MS, 0L).takeIf { it > 0 }
        val gap = stopped?.let { System.currentTimeMillis() - it }
        val first = !startedThisProcess
        startedThisProcess = true
        return isNewDrive(gap, SystemClock.elapsedRealtime(), first)
    }

    /** Music was playing when the car was last switched off. */
    val musicWasPlaying: Boolean get() = prefs.getBoolean(KEY_PLAYING, false)

    /**
     * Sends a "play" media key: Android routes it to the app that played
     * last (Spotify, YouTube Music, AutoVerse's own player…), which resumes —
     * exactly what the car's steering-wheel play button does.
     */
    fun resumeMusic() {
        try {
            val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val KEY_STOP_MS = "stop_ms"
        const val KEY_PLAYING = "playing"

        @Volatile
        var startedThisProcess = false
    }
}
