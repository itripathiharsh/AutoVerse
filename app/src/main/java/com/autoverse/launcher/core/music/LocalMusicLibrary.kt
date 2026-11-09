package com.autoverse.launcher.core.music

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One song file found on the unit, an SD card or a USB drive. */
data class LocalTrack(
    val id: Long,
    /** content:// address of the file (kept as text so the model is JVM-testable). */
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    /** Folder the file sits in, e.g. "Music/Old Hindi". */
    val folder: String,
    /** Where the file is: "This unit", "SanDisk USB drive", "SD card"… */
    val source: String,
    val albumId: Long,
)

/** Permission needed to read music files on this Android version. */
fun audioReadPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

fun hasAudioReadPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioReadPermission()) == PackageManager.PERMISSION_GRANTED

/**
 * Reads the system media index for music on every mounted volume — internal
 * storage, SD cards and USB drives (Android indexes a USB stick as soon as it
 * is plugged in). Nothing is copied or uploaded.
 */
class LocalMusicLibrary(context: Context) {
    private val appContext = context.applicationContext

    /** The URI to watch for changes (USB plugged in, files added). */
    val contentUri: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    suspend fun load(): List<LocalTrack> = withContext(Dispatchers.IO) {
        if (!hasAudioReadPermission(appContext)) return@withContext emptyList()
        val volumeNames = volumeDescriptions()
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.VOLUME_NAME)
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()
        val tracks = mutableListOf<LocalTrack>()
        try {
            appContext.contentResolver.query(
                contentUri,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 20000",
                null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val volCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) c.getColumnIndex(MediaStore.Audio.Media.VOLUME_NAME) else -1
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    @Suppress("DEPRECATION")
                    c.getColumnIndex(MediaStore.Audio.Media.DATA)
                }
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val volume = if (volCol >= 0) c.getString(volCol) else null
                    val base = if (volume != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Audio.Media.getContentUri(volume)
                    } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val rawPath = if (pathCol >= 0) c.getString(pathCol).orEmpty() else ""
                    tracks += LocalTrack(
                        id = id,
                        uri = ContentUris.withAppendedId(base, id).toString(),
                        title = cleanTag(c.getString(titleCol)) ?: c.getString(nameCol).orEmpty().substringBeforeLast('.'),
                        artist = cleanTag(c.getString(artistCol)) ?: "Unknown artist",
                        album = cleanTag(c.getString(albumCol)) ?: "Unknown album",
                        durationMs = c.getLong(durCol),
                        folder = folderOf(rawPath),
                        source = sourceLabel(volume, volumeNames),
                        albumId = c.getLong(albumIdCol),
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        } catch (_: Exception) {
            // A half-mounted USB drive can make the query fail; show what we have.
        }
        tracks
    }

    /** Media-store volume name → what the driver calls it ("SanDisk USB drive"). */
    private fun volumeDescriptions(): Map<String, String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyMap()
        val sm = appContext.getSystemService(StorageManager::class.java) ?: return emptyMap()
        return try {
            sm.storageVolumes.mapNotNull { v ->
                val name = v.mediaStoreVolumeName ?: return@mapNotNull null
                name to (if (v.isPrimary) THIS_UNIT else v.getDescription(appContext))
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    companion object {
        const val THIS_UNIT = "This unit"
    }
}

/** MediaStore tags often hold "<unknown>"; treat that and blanks as missing. */
fun cleanTag(raw: String?): String? =
    raw?.trim()?.takeIf { it.isNotEmpty() && !it.equals("<unknown>", ignoreCase = true) }

/** "Music/Old Hindi/" or "/storage/1234-ABCD/Music/x.mp3" → "Music/Old Hindi" / "Music". */
fun folderOf(rawPath: String): String {
    var p = rawPath.trim().replace('\\', '/')
    // RELATIVE_PATH (Android 10+) is a folder ending in "/"; old DATA paths end in the file name.
    if (!p.endsWith('/')) p = p.substringBeforeLast('/', "")
    p = p.trim('/')
    // Strip "storage/<volume>" or "storage/emulated/0" prefixes from old-style paths.
    val parts = p.split('/').filter { it.isNotEmpty() }
    val trimmed = when {
        parts.size >= 3 && parts[0] == "storage" && parts[1] == "emulated" -> parts.drop(3)
        parts.size >= 2 && parts[0] == "storage" -> parts.drop(2)
        parts.size >= 2 && parts[0] == "mnt" -> parts.drop(2)
        else -> parts
    }
    return trimmed.joinToString("/").ifEmpty { "Top folder" }
}

fun sourceLabel(volumeName: String?, descriptions: Map<String, String>): String = when {
    volumeName == null -> LocalMusicLibrary.THIS_UNIT
    descriptions[volumeName] != null -> descriptions.getValue(volumeName)
    volumeName == "external_primary" || volumeName == "external" -> LocalMusicLibrary.THIS_UNIT
    else -> "USB / SD card"
}
