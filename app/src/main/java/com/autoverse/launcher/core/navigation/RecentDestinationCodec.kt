package com.autoverse.launcher.core.navigation

/**
 * Recent destinations persist as one newline-separated string with one
 * escaped destination text per line. Malformed or unparsable lines are
 * skipped so one bad entry never hides the rest.
 */
object RecentDestinationCodec {

    fun encode(recents: List<RecentDestination>): String =
        recents.joinToString("\n") {
            escapeCodecField(navigationDestinationText(it.destination))
        }

    fun decode(raw: String?): List<RecentDestination> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split('\n').mapNotNull { rawLine ->
            val text = unescapeCodecField(rawLine.trimEnd('\r'))
            if (text.isBlank()) return@mapNotNull null
            val parsed = parseNavigationDestination(text)
            val destination = (parsed as? DestinationParseResult.Valid)?.destination
                ?: return@mapNotNull null
            RecentDestination(destination)
        }
    }
}
