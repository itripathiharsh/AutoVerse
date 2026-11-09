package com.autoverse.launcher.core.routing

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/** Download progress across all tiles a route needs. */
data class TileDownloadProgress(
    val tile: String,
    val tileIndex: Int,
    val tileCount: Int,
    val bytesDone: Long,
    val bytesTotal: Long,
) {
    val fraction: Float get() = if (bytesTotal > 0) (bytesDone.toFloat() / bytesTotal).coerceIn(0f, 1f) else 0f
}

/**
 * Owns AutoVerse's on-device routing data in the app's private storage:
 * the car profile (copied from assets) and the BRouter road-data tiles,
 * downloaded once per region over the car's internet and reused for every
 * route after that. Nothing here talks to a routing server.
 */
class RoutingDataStore(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "brouter")
    val segmentDir = File(root, "segments4")
    private val profileDir = File(root, "profiles2")

    /** Absolute path of the car profile, installed (or refreshed) from assets. */
    fun carProfilePath(): String {
        profileDir.mkdirs()
        // Assets are compressed in the APK (no openFd), so a version marker
        // decides when the bundled profile must be re-copied.
        val marker = File(profileDir, ".version")
        val current = marker.takeIf { it.exists() }?.readText()?.trim()
        if (current != PROFILE_VERSION) {
            for (name in listOf("lookups.dat", "car-vario.brf")) {
                appContext.assets.open("brouter/$name").use { input ->
                    File(profileDir, name).outputStream().use { input.copyTo(it) }
                }
            }
            marker.writeText(PROFILE_VERSION)
        }
        return File(profileDir, "car-vario.brf").absolutePath
    }

    fun hasTile(name: String): Boolean = File(segmentDir, "$name.rd5").let { it.exists() && it.length() > 0 }

    fun missingTiles(names: List<String>): List<String> = names.filterNot(::hasTile)

    /** Megabytes of road data already on the unit. */
    fun storedMegabytes(): Long =
        (segmentDir.listFiles()?.sumOf { it.length() } ?: 0L) / (1024 * 1024)

    /**
     * Downloads [tiles] one by one into a .part file, renamed only when
     * complete, so a dropped connection never leaves a corrupt tile behind.
     * A tile that does not exist upstream (sea, outside coverage) is skipped.
     */
    suspend fun download(tiles: List<String>, onProgress: (TileDownloadProgress) -> Unit) =
        withContext(Dispatchers.IO) {
            segmentDir.mkdirs()
            tiles.forEachIndexed { index, tile ->
                coroutineContext.ensureActive()
                val target = File(segmentDir, "$tile.rd5")
                if (target.exists() && target.length() > 0) return@forEachIndexed
                val part = File(segmentDir, "$tile.rd5.part")
                val connection = (URL(routingTileUrl(tile)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "AutoVerse-CarLauncher")
                }
                try {
                    val code = connection.responseCode
                    if (code == HttpURLConnection.HTTP_NOT_FOUND) return@forEachIndexed
                    if (code != HttpURLConnection.HTTP_OK) throw IOException("Road data server returned $code")
                    val total = connection.contentLengthLong
                    var done = 0L
                    var lastReport = 0L
                    connection.inputStream.use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                coroutineContext.ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                done += n
                                if (done - lastReport > 256 * 1024) {
                                    lastReport = done
                                    onProgress(TileDownloadProgress(tile, index, tiles.size, done, total))
                                }
                            }
                        }
                    }
                    if (total > 0 && done != total) throw IOException("Download of $tile was incomplete")
                    if (!part.renameTo(target)) throw IOException("Could not save $tile")
                    onProgress(TileDownloadProgress(tile, index, tiles.size, done, done))
                } finally {
                    connection.disconnect()
                    if (part.exists()) part.delete()
                }
            }
        }

    /** Removes all downloaded road data (Settings → storage). */
    fun clear() {
        segmentDir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        /** Bump when assets/brouter changes (BRouter 1.7.10 car profile, mode 9 hints). */
        const val PROFILE_VERSION = "1.7.10-car-1"
    }
}
