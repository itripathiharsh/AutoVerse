package com.autoverse.launcher.core.routing

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale
import kotlin.math.roundToInt

/** Language of turn-by-turn directions, voice prompts and driving alerts. */
enum class NavLanguage(val label: String, val locale: Locale) {
    ENGLISH("English", Locale("en", "IN")),
    HINDI("हिंदी", Locale("hi", "IN")),
}

val LocalNavLanguage = staticCompositionLocalOf { NavLanguage.ENGLISH }

/**
 * Every phrase a driver sees or hears while navigating, in English and
 * Hindi. Pure (no Android), so wording is unit-tested.
 */
object NavText {

    fun maneuver(m: Maneuver, lang: NavLanguage): String = when (lang) {
        NavLanguage.ENGLISH -> maneuverText(m)
        NavLanguage.HINDI -> when (m.type) {
            ManeuverType.CONTINUE -> "सीधे चलते रहें" // सीधे चलते रहें
            ManeuverType.TURN_LEFT -> "बाएँ मुड़ें" // बाएँ मुड़ें
            ManeuverType.TURN_SLIGHT_LEFT -> "हल्का बाएँ मुड़ें" // हल्का बाएँ मुड़ें
            ManeuverType.TURN_SHARP_LEFT -> "तेज़ बाएँ मुड़ें" // तेज़ बाएँ मुड़ें
            ManeuverType.TURN_RIGHT -> "दाएँ मुड़ें" // दाएँ मुड़ें
            ManeuverType.TURN_SLIGHT_RIGHT -> "हल्का दाएँ मुड़ें"
            ManeuverType.TURN_SHARP_RIGHT -> "तेज़ दाएँ मुड़ें"
            ManeuverType.KEEP_LEFT -> "बाएँ रहें" // बाएँ रहें
            ManeuverType.KEEP_RIGHT -> "दाएँ रहें" // दाएँ रहें
            ManeuverType.U_TURN -> "यू-टर्न लें" // यू-टर्न लें
            ManeuverType.ROUNDABOUT ->
                if (m.roundaboutExit > 0) "गोलचक्कर से ${ordinalHi(m.roundaboutExit)} निकास लें" // गोलचक्कर से N निकास लें
                else "गोलचक्कर में जाएँ" // गोलचक्कर में जाएँ
            ManeuverType.EXIT_LEFT -> "बाएँ वाला एग्ज़िट लें" // बाएँ वाला एग्ज़िट लें
            ManeuverType.EXIT_RIGHT -> "दाएँ वाला एग्ज़िट लें"
            ManeuverType.ARRIVE -> "आप पहुँच गए हैं" // आप पहुँच गए हैं
        }
    }

    /** "500 metres" / "500 मीटर", "1.5 kilometres" / "1.5 किलोमीटर". */
    fun spokenDistance(meters: Double, lang: NavLanguage): String = when (lang) {
        NavLanguage.ENGLISH -> com.autoverse.launcher.core.routing.spokenDistance(meters)
        NavLanguage.HINDI -> if (meters < 1000) {
            "${(meters / 50).roundToInt().coerceAtLeast(1) * 50} मीटर"
        } else {
            val km = (meters / 100).roundToInt() / 10.0
            (if (km == km.toInt().toDouble()) km.toInt().toString() else km.toString()) + " किलोमीटर"
        }
    }

    /** Heads-up prompt: "In 500 metres, turn left" / "500 मीटर बाद, बाएँ मुड़ें". */
    fun farPrompt(meters: Double, m: Maneuver, lang: NavLanguage): String = when (lang) {
        NavLanguage.ENGLISH -> "In ${spokenDistance(meters, lang)}, ${maneuver(m, lang).replaceFirstChar(Char::lowercase)}"
        NavLanguage.HINDI -> "${spokenDistance(meters, lang)} बाद, ${maneuver(m, lang)}"
    }

    /** "Turn left, then turn right" / "बाएँ मुड़ें, फिर दाएँ मुड़ें". */
    fun nearPrompt(m: Maneuver, then: Maneuver?, lang: NavLanguage): String {
        val now = maneuver(m, lang)
        if (then == null) return now
        return when (lang) {
            NavLanguage.ENGLISH -> "$now, then ${maneuver(then, lang).replaceFirstChar(Char::lowercase)}"
            NavLanguage.HINDI -> "$now, फिर ${maneuver(then, lang)}"
        }
    }

    fun then(m: Maneuver, lang: NavLanguage): String = when (lang) {
        NavLanguage.ENGLISH -> "Then ${maneuver(m, lang).replaceFirstChar(Char::lowercase)}"
        NavLanguage.HINDI -> "फिर ${maneuver(m, lang)}"
    }

    fun starting(name: String, lang: NavLanguage) = when (lang) {
        NavLanguage.ENGLISH -> "Starting route to $name"
        NavLanguage.HINDI -> "$name का रास्ता शुरू" // ... का रास्ता शुरू
    }

    fun arrived(name: String, lang: NavLanguage) = when (lang) {
        NavLanguage.ENGLISH -> "You have arrived at $name"
        NavLanguage.HINDI -> "आप $name पहुँच गए हैं"
    }

    fun recalculating(lang: NavLanguage) = when (lang) {
        NavLanguage.ENGLISH -> "Recalculating"
        NavLanguage.HINDI -> "नया रास्ता खोजा जा रहा है" // नया रास्ता खोजा जा रहा है
    }

    fun routeUpdated(lang: NavLanguage) = when (lang) {
        NavLanguage.ENGLISH -> "Route updated"
        NavLanguage.HINDI -> "रास्ता बदला गया" // रास्ता बदला गया
    }

    fun speedLimit(limit: Int, lang: NavLanguage) = when (lang) {
        NavLanguage.ENGLISH -> "Speed limit $limit"
        NavLanguage.HINDI -> "गति सीमा $limit" // गति सीमा
    }

    fun camera(limit: Int?, lang: NavLanguage) = when (lang) {
        NavLanguage.ENGLISH -> "Speed camera ahead" + (limit?.let { ", limit $it" } ?: "")
        NavLanguage.HINDI -> "आगे स्पीड कैमरा है" + (limit?.let { ", सीमा $it" } ?: "") // आगे स्पीड कैमरा है
    }

    // On-screen labels of the navigation cards.
    fun start(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "शुरू करें" else "Start" // शुरू करें
    fun cancel(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "रद्द करें" else "Cancel" // रद्द करें
    fun end(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "ख़त्म" else "End" // ख़त्म
    fun done(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "ठीक है" else "Done" // ठीक है
    fun retry(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "फिर से" else "Retry" // फिर से
    fun whereTo(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "कहाँ जाना है?" else "Where to?" // कहाँ जाना है?
    fun arrivedTitle(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "आप पहुँच गए" else "You have arrived"
    fun findingRoute(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "सबसे अच्छा रास्ता खोज रहे हैं…" else "Finding the best route…"
    fun recalculatingTitle(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "नया रास्ता…" else "Recalculating…"
    fun arrive(lang: NavLanguage) = if (lang == NavLanguage.HINDI) "पहुँचेंगे" else "arrive" // पहुँचेंगे

    private fun ordinalHi(n: Int) = "${n}वाँ" // 2वाँ
}
