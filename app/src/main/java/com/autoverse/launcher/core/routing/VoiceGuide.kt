package com.autoverse.launcher.core.routing

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Spoken turn instructions through Android's built-in text-to-speech (free,
 * works offline once the unit's voice data is installed). Indian English is
 * preferred; any English voice is the fallback. Silent — never crashing — on
 * units without a TTS engine.
 */
class VoiceGuide(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) chooseVoice()
    }

    private fun chooseVoice() {
        val preferred = listOf(Locale("en", "IN"), Locale.UK, Locale.US, Locale.ENGLISH)
        for (locale in preferred) {
            val result = try {
                tts.setLanguage(locale)
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) return
        }
    }

    fun speak(text: String) {
        if (!ready) return
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "autoverse-nav")
        } catch (_: Exception) {
        }
    }

    fun stop() {
        try {
            tts.stop()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        try {
            tts.shutdown()
        } catch (_: Exception) {
        }
    }
}
