package com.autoverse.launcher.core.navigation

/**
 * Saved places persist as one newline-separated string; each line is
 * `<escaped label>TAB<escaped destination text>`. Labels may contain any
 * character — TAB, newline and backslash are escaped, so the raw tab after a
 * line's label is unambiguous. Malformed or unparsable lines are skipped
 * rather than failing the whole list, so one bad entry never hides the rest.
 */
object SavedPlaceCodec {

    fun encode(places: List<SavedPlace>): String =
        places.joinToString("\n") { place ->
            escapeCodecField(place.label) +
                "\t" +
                escapeCodecField(navigationDestinationText(place.destination))
        }

    fun decode(raw: String?): List<SavedPlace> {
        if (raw.isNullOrEmpty()) return emptyList()
        return raw.split('\n').mapNotNull(::decodeLine)
    }

    private fun decodeLine(rawLine: String): SavedPlace? {
        val line = rawLine.trimEnd('\r')
        val separator = line.indexOf('\t')
        if (separator < 0) return null
        val label = unescapeCodecField(line.substring(0, separator)).trim()
        val destinationText = unescapeCodecField(line.substring(separator + 1))
        if (destinationText.isBlank()) return null
        val parsed = parseNavigationDestination(destinationText)
        val destination = (parsed as? DestinationParseResult.Valid)?.destination ?: return null
        return SavedPlace(
            label = label.take(MAX_SAVED_PLACE_LABEL_LENGTH)
                .ifBlank { navigationDestinationText(destination) },
            destination = destination,
        )
    }
}

/** Escapes TAB, newline, carriage return and backslash for line-based storage. */
internal fun escapeCodecField(value: String): String {
    val builder = StringBuilder(value.length)
    for (character in value) {
        when (character) {
            '\\' -> builder.append("\\\\")
            '\t' -> builder.append("\\t")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            else -> builder.append(character)
        }
    }
    return builder.toString()
}

/** Reverses [escapeCodecField]; unknown escapes keep the backslash literally. */
internal fun unescapeCodecField(value: String): String {
    val builder = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character == '\\' && index + 1 < value.length) {
            when (value[index + 1]) {
                '\\' -> {
                    builder.append('\\')
                    index += 2
                }
                't' -> {
                    builder.append('\t')
                    index += 2
                }
                'n' -> {
                    builder.append('\n')
                    index += 2
                }
                'r' -> {
                    builder.append('\r')
                    index += 2
                }
                else -> {
                    builder.append(character)
                    index += 1
                }
            }
        } else {
            builder.append(character)
            index += 1
        }
    }
    return builder.toString()
}
