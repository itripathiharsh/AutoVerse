package com.autoverse.launcher.core.navigation

private const val HEX_DIGITS = "0123456789ABCDEF"

/**
 * Percent-encodes one URI query component (RFC 3986): unreserved characters
 * A-Z a-z 0-9 - _ . ~ pass through, everything else becomes UTF-8 %XX with
 * uppercase hex digits. Spaces become %20 (never "+"), the encoding both
 * providers' documentation accepts. Pure Kotlin so unit tests do not need
 * android.net.Uri, which returns null under the JVM test stubs.
 */
fun encodeUriComponent(value: String): String {
    val bytes = value.toByteArray(Charsets.UTF_8)
    val encoded = StringBuilder(bytes.size)
    for (byte in bytes) {
        val code = byte.toInt() and 0xFF
        val character = code.toChar()
        if (
            character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' ||
            character == '-' || character == '_' || character == '.' || character == '~'
        ) {
            encoded.append(character)
        } else {
            encoded.append('%')
            encoded.append(HEX_DIGITS[(code shr 4) and 0xF])
            encoded.append(HEX_DIGITS[code and 0xF])
        }
    }
    return encoded.toString()
}
