package com.autoverse.launcher.core.navigation

import java.math.BigDecimal

/** Longest accepted destination input; longer text is rejected, not truncated. */
const val MAX_DESTINATION_LENGTH = 512

/**
 * A validated place to hand off to a navigation app: either a free-text
 * query (address, place name) or explicit coordinates. AutoVerse never
 * invents coordinates and never resolves a query itself.
 */
sealed interface NavigationDestination {
    data class Query(val text: String) : NavigationDestination
    data class Coordinates(val latitude: Double, val longitude: Double) : NavigationDestination
}

/** Why an entered destination was rejected. */
enum class DestinationParseFailure { OUT_OF_RANGE, TOO_LONG }

/** Short, factual copy explaining why a destination was rejected. */
fun destinationFailureMessage(reason: DestinationParseFailure): String = when (reason) {
    DestinationParseFailure.OUT_OF_RANGE -> "Latitude must be -90 to 90 and longitude -180 to 180"
    DestinationParseFailure.TOO_LONG -> "That destination is too long"
}

/** Result of parsing raw user input into a handoff-safe destination. */
sealed interface DestinationParseResult {

    /** Nothing was entered; the UI treats this as "no destination yet". */
    data object Empty : DestinationParseResult

    data class Valid(val destination: NavigationDestination) : DestinationParseResult

    data class Invalid(val reason: DestinationParseFailure) : DestinationParseResult
}

private val PLAIN_DECIMAL = Regex("^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$")

/**
 * Parses one line of user input. Exactly two plain-decimal numbers separated
 * by a comma (both inside valid latitude/longitude ranges) become
 * [NavigationDestination.Coordinates]; anything else non-blank becomes a
 * [NavigationDestination.Query] verbatim, because addresses legitimately
 * contain commas, digits and words.
 */
fun parseNavigationDestination(input: String): DestinationParseResult {
    val text = input.trim()
    if (text.isEmpty()) return DestinationParseResult.Empty
    if (text.length > MAX_DESTINATION_LENGTH) {
        return DestinationParseResult.Invalid(DestinationParseFailure.TOO_LONG)
    }

    val parts = text.split(',')
    if (parts.size == 2) {
        val latitudeText = parts[0].trim()
        val longitudeText = parts[1].trim()
        if (latitudeText.matches(PLAIN_DECIMAL) && longitudeText.matches(PLAIN_DECIMAL)) {
            val latitude = latitudeText.toDoubleOrNull()
            val longitude = longitudeText.toDoubleOrNull()
            if (latitude != null && longitude != null) {
                return if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                    DestinationParseResult.Valid(
                        NavigationDestination.Coordinates(latitude, longitude),
                    )
                } else {
                    DestinationParseResult.Invalid(DestinationParseFailure.OUT_OF_RANGE)
                }
            }
        }
    }

    return DestinationParseResult.Valid(NavigationDestination.Query(text))
}

/** Plain decimal rendering: no scientific notation, no trailing zeros, no "-0". */
fun formatCoordinate(value: Double): String {
    if (value == 0.0) return "0"
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

/**
 * Phase 18 — canonical one-line text for [destination]: the query verbatim,
 * or "lat, lng" plain decimals for coordinates. Round-trips through
 * [parseNavigationDestination] back to the same destination, so saved places
 * and recent destinations can persist as plain text without inventing data.
 */
fun navigationDestinationText(destination: NavigationDestination): String = when (destination) {
    is NavigationDestination.Query -> destination.text
    is NavigationDestination.Coordinates ->
        "${formatCoordinate(destination.latitude)}, ${formatCoordinate(destination.longitude)}"
}
