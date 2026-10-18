package com.autoverse.launcher.core.launcher

/**
 * Encodes the ordered favorite list into a single preference string.
 * Keys are "package/activity" and can never contain a newline, so a newline
 * separator is unambiguous; blank entries and duplicates are dropped on both
 * encode and decode so stored data self-heals.
 */
object FavoritesCodec {

    private const val SEPARATOR = '\n'

    fun encode(favorites: List<String>): String =
        sanitize(favorites).joinToString(SEPARATOR.toString())

    fun decode(stored: String?): List<String> =
        sanitize(stored?.split(SEPARATOR).orEmpty())

    private fun sanitize(values: List<String>): List<String> =
        values.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
}
