package com.autoverse.launcher.core.voice

import java.io.File

/**
 * Tier 2 part 2 — offline wake-word voice, pure logic layer (no Android
 * types) so every rule is JVM-testable. The Vosk engine that consumes these
 * rules lives in [OfflineVoiceEngine].
 *
 * Design: a two-phase recognizer. Phase 1 listens continuously for the wake
 * phrase with a grammar-restricted recognizer. On wake, phase 2 captures a
 * free-form command for a short window and hands the transcript to the same
 * cockpit router the tap-to-talk path uses ("open spotify", "navigate to…").
 */
sealed interface OfflineVoiceState {
    data object Disabled : OfflineVoiceState
    data class Downloading(val percent: Int) : OfflineVoiceState
    data object Preparing : OfflineVoiceState
    data object ListeningForWake : OfflineVoiceState
    data object CapturingCommand : OfflineVoiceState
    data class Error(val message: String) : OfflineVoiceState
}

object OfflineVoiceRules {

    /** Vosk small English model: Apache-2.0, ~40 MB, hosted by the Vosk project. */
    const val MODEL_URL: String = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    const val MODEL_DIR_NAME: String = "voice-model"
    const val WAKE_PHRASE: String = "hey autoverse"

    /** Free-form capture window after a wake, milliseconds. */
    const val CAPTURE_WINDOW_MS: Long = 6_000L

    fun modelDir(filesDir: File): File = File(filesDir, MODEL_DIR_NAME)

    /**
     * The model is considered present when the acoustic-model folder and a
     * config file exist at the target directory root (Vosk layout flattened
     * out of the zip).
     */
    fun isModelComplete(dir: File): Boolean =
        File(dir, "am").exists() && (File(dir, "conf/model.conf").exists() || File(dir, "conf").isDirectory)

    /**
     * Wake detection: case-insensitive containment of the wake phrase. A
     * bare "autoverse" also wakes, because small models frequently drop the
     * leading "hey".
     */
    fun isWakePhrase(text: String?): Boolean {
        val t = text?.trim()?.lowercase() ?: return false
        return t.contains(WAKE_PHRASE) || t == "autoverse"
    }

    /** Strips the wake phrase from a transcript that arrived with it. */
    fun withoutWakePhrase(text: String): String =
        text.trim()
            .replace(Regex("(?i)hey\\s+autoverse[,!.]?\\s*"), "")
            .replace(Regex("(?i)^autoverse[,!.]?\\s*"), "")
            .trim()

    /**
     * Target file for a zip entry, flattened: the archive's top-level
     * directory (vosk-model-small-en-us-0.15/…) is stripped so the model
     * files land directly in the target dir. Path traversal ("..", absolute
     * names) is rejected — returns null for anything unsafe.
     */
    fun safeFlattenedTarget(targetDir: File, entryName: String): File? {
        val normalized = entryName.replace('\\', '/')
        if (normalized.isEmpty() || normalized.endsWith("/")) return null
        val parts = normalized.split('/').filter { it.isNotBlank() }
        if (parts.size < 2) return null // only the root folder entry
        if (parts.any { it == ".." || it == "." }) return null
        val relative = parts.drop(1).joinToString("/")
        return File(targetDir, relative).canonicalFile
            .takeIf { it.path.startsWith(targetDir.canonicalFile.path + File.separator) }
    }

    /** Human-readable size for the cockpit's download state. */
    fun bytesLabel(bytes: Long): String = when {
        bytes >= 1_000_000L -> "${"%.1f".format(bytes / 1_000_000.0)} MB"
        bytes >= 1_000L -> "${bytes / 1_000} KB"
        else -> "$bytes B"
    }
}
