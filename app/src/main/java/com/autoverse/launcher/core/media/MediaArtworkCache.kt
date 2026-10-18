package com.autoverse.launcher.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_CACHE_BYTES = 8 * 1024 * 1024

/**
 * Decodes session artwork content URIs off the main thread into the smallest
 * power-of-two-sampled bitmap that covers [targetMaxPx], the longest-edge
 * budget chosen by the active performance profile (256 px Balanced/Enhanced,
 * 128 px Constrained). Entries are keyed by uri plus target so profile
 * changes cannot serve an oversized bitmap. Failed and oversized decodes are
 * dropped rather than cached. Bitmaps are never recycled: evicted entries
 * become garbage-collectable instead.
 */
class MediaArtworkCache(context: Context) {

    private val appContext = context.applicationContext

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    suspend fun load(uri: String, targetMaxPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$targetMaxPx"
        cache.get(key)?.let { return@withContext it.asImageBitmap() }
        val bitmap = decodeSampled(uri, targetMaxPx) ?: return@withContext null
        if (bitmap.byteCount <= MAX_CACHE_BYTES) {
            cache.put(key, bitmap)
        }
        bitmap.asImageBitmap()
    }

    /** Drops all decoded artwork; safe to call under memory pressure. */
    fun trimMemory() {
        cache.evictAll()
    }

    private fun decodeSampled(uri: String, targetMaxPx: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = appContext.contentResolver.openInputStream(Uri.parse(uri))
        if (boundsStream == null) {
            null
        } else {
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, targetMaxPx)
            }
            appContext.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Smallest power-of-two sample size whose result keeps both dimensions
 * within [targetMaxPx]. Non-positive inputs yield 1 (no sampling).
 */
fun sampleSizeFor(width: Int, height: Int, targetMaxPx: Int): Int {
    if (width <= 0 || height <= 0 || targetMaxPx <= 0) return 1
    var sampleSize = 1
    var longestEdge = maxOf(width, height)
    while (longestEdge / 2 >= targetMaxPx) {
        sampleSize *= 2
        longestEdge /= 2
    }
    return sampleSize
}
