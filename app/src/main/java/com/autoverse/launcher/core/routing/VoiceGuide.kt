package com.autoverse.launcher.core.routing

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Spoken turn instructions through Android's built-in text-to-speech (free,
 * works offline once the unit's voice data is installed). Hindi uses the
 * hi-IN voice; English prefers Indian English. If the Hindi voice isn't
 * installed, [hindiAvailable] is false and the controller speaks English so
 * the driver never hears Hindi words read by an English voice. Silent —
 * never crashing — on units without a TTS engine.
 */
class VoiceGuide(context: Context) {
    private var ready = false
    private var wanted: NavLanguage = NavLanguage.ENGLISH

    @Volatile
    var hindiAvailable = false
        private set

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            hindiAvailable = isUsable(tts.isLanguageAvailable(NavLanguage.HINDI.locale))
            apply(wanted)
        }
    }

    fun setLanguage(lang: NavLanguage) {
        wanted = lang
        if (ready) apply(lang)
    }

    private fun apply(lang: NavLanguage) {
        val candidates = if (lang == NavLanguage.HINDI && hindiAvailable) {
            listOf(NavLanguage.HINDI.locale)
        } else {
            listOf(Locale("en", "IN"), Locale.UK, Locale.US, Locale.ENGLISH)
        }
        for (locale in candidates) {
            val result = try {
                tts.setLanguage(locale)
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            if (isUsable(result)) return
        }
    }

    private fun isUsable(result: Int) =
        result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED

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
