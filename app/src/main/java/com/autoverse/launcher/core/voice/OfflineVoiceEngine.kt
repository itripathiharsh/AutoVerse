package com.autoverse.launcher.core.voice

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService

/**
 * Tier 2 part 2 — offline wake-word voice built on Vosk (Apache-2.0).
 *
 * Opt-in and self-contained: the ~40 MB small English model downloads once
 * into app-private storage on first enable (no permissions beyond
 * RECORD_AUDIO, no accounts), then recognition runs fully on-device.
 *
 * Two phases, per [OfflineVoiceRules]: a grammar-restricted recognizer waits
 * for "Hey AutoVerse", then a free-form recognizer captures one command for
 * a short window and emits the transcript via [onCommand] — the cockpit
 * routes it exactly like tap-to-talk.
 *
 * The actual microphone recognition is UNVERIFIED until run on a device
 * (documented in docs/tier2-obd-offline.md); download, unzip-flattening and
 * state transitions are covered by unit tests where pure.
 */
class OfflineVoiceEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onState: (OfflineVoiceState) -> Unit,
    private val onCommand: (String) -> Unit,
) {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var preparingJob: Job? = null
    private var captureTimeoutJob: Job? = null
    private var capturing = false

    val hasMicPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    /** Starts (or resumes) wake-word listening; downloads the model first if needed. */
    fun start() {
        if (!hasMicPermission) {
            onState(OfflineVoiceState.Error(MIC_PERMISSION_MESSAGE))
            return
        }
        if (preparingJob?.isActive == true || speechService != null) return
        preparingJob = scope.launch {
            try {
                ensureModel()
                startWakeListening()
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                onState(OfflineVoiceState.Error(failure.message ?: "Offline voice failed to start"))
            }
        }
    }

    /** Fully stops listening and frees the recognizer (the model stays cached). */
    fun stop() {
        captureTimeoutJob?.cancel()
        captureTimeoutJob = null
        speechService?.stop()
        speechService = null
        capturing = false
    }

    /** Releases everything including the loaded model (memory pressure). */
    fun release() {
        stop()
        preparingJob?.cancel()
        preparingJob = null
        model?.close()
        model = null
    }

    private suspend fun ensureModel() {
        val dir = OfflineVoiceRules.modelDir(context.filesDir)
        if (OfflineVoiceRules.isModelComplete(dir) && model == null) {
            onState(OfflineVoiceState.Preparing)
            withContext(Dispatchers.IO) { model = Model(dir.absolutePath) }
            return
        }
        if (model != null) return
        downloadAndUnpack(dir)
        onState(OfflineVoiceState.Preparing)
        withContext(Dispatchers.IO) { model = Model(dir.absolutePath) }
    }

    private suspend fun downloadAndUnpack(dir: File) {
        withContext(Dispatchers.IO) {
            dir.deleteRecursively()
            dir.mkdirs()
            val zip = File(context.filesDir, "voice-model.zip")
            val connection = URL(OfflineVoiceRules.MODEL_URL).openConnection() as HttpURLConnection
            try {
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    throw IOException("Model download failed (HTTP ${connection.responseCode})")
                }
                val total = connection.contentLengthLong
                connection.inputStream.use { input ->
                    zip.outputStream().use { out ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var written = 0L
                        while (input.read(buffer).also { read = it } > 0) {
                            out.write(buffer, 0, read)
                            written += read
                            if (total > 0) {
                                onState(
                                    OfflineVoiceState.Downloading(
                                        ((written * 100) / total).toInt().coerceIn(0, 99),
                                    ),
                                )
                            }
                        }
                    }
                }
                unzipFlattened(zip, dir)
            } finally {
                connection.disconnect()
                zip.delete()
            }
        }
    }

    private fun unzipFlattened(zip: File, targetDir: File) {
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val target = OfflineVoiceRules.safeFlattenedTarget(targetDir, entry.name)
                if (target != null) {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zis.copyTo(out) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        if (!OfflineVoiceRules.isModelComplete(targetDir)) {
            throw IOException("Downloaded voice model is incomplete \u2014 try enabling again")
        }
    }

    private fun startWakeListening() {
        val m = model ?: return
        capturing = false
        val recognizer = Recognizer(m, SAMPLE_RATE, "[\"${OfflineVoiceRules.WAKE_PHRASE}\", \"autoverse\"]")
        val service = SpeechService(recognizer, SAMPLE_RATE)
        service.startListening(wakeListener)
        speechService = service
        onState(OfflineVoiceState.ListeningForWake)
    }

    private fun startCommandCapture() {
        val m = model ?: return
        speechService?.stop()
        speechService = null
        capturing = true
        onState(OfflineVoiceState.CapturingCommand)
        val recognizer = Recognizer(m, SAMPLE_RATE)
        val service = SpeechService(recognizer, SAMPLE_RATE)
        service.startListening(captureListener)
        speechService = service
        captureTimeoutJob?.cancel()
        captureTimeoutJob = scope.launch {
            delay(OfflineVoiceRules.CAPTURE_WINDOW_MS)
            if (capturing) {
                finishCapture(transcript = null)
            }
        }
    }

    private fun finishCapture(transcript: String?) {
        captureTimeoutJob?.cancel()
        captureTimeoutJob = null
        capturing = false
        speechService?.stop()
        speechService = null
        val command = transcript?.let { OfflineVoiceRules.withoutWakePhrase(it) }?.trim().orEmpty()
        if (command.isNotBlank()) {
            onState(OfflineVoiceState.ListeningForWake)
            onCommand(command)
        }
        // Silence in the window simply returns to wake listening.
        startWakeListening()
    }

    private fun textOf(json: String): String? =
        try {
            JSONObject(json).optString("text", "").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }

    private val wakeListener = object : org.vosk.android.RecognitionListener {
        override fun onPartialResult(hypothesis: String) {
            if (OfflineVoiceRules.isWakePhrase(textOf(hypothesis))) {
                startCommandCapture()
            }
        }

        override fun onResult(hypothesis: String) {
            if (OfflineVoiceRules.isWakePhrase(textOf(hypothesis))) {
                startCommandCapture()
            }
        }

        override fun onFinalResult(hypothesis: String) {
            if (OfflineVoiceRules.isWakePhrase(textOf(hypothesis))) {
                startCommandCapture()
            }
        }

        override fun onError(exception: Exception) {
            onState(OfflineVoiceState.Error(exception.message ?: "Recognizer error"))
        }

        override fun onTimeout() = Unit
    }

    private val captureListener = object : org.vosk.android.RecognitionListener {
        override fun onPartialResult(hypothesis: String) = Unit

        override fun onResult(hypothesis: String) {
            if (capturing) finishCapture(textOf(hypothesis))
        }

        override fun onFinalResult(hypothesis: String) {
            if (capturing) finishCapture(textOf(hypothesis))
        }

        override fun onError(exception: Exception) {
            if (capturing) finishCapture(null)
            onState(OfflineVoiceState.Error(exception.message ?: "Recognizer error"))
        }

        override fun onTimeout() {
            if (capturing) finishCapture(null)
        }
    }

    companion object {
        private const val SAMPLE_RATE = 16000f
        const val MIC_PERMISSION_MESSAGE: String =
            "Microphone permission is needed for offline voice"
    }
}
