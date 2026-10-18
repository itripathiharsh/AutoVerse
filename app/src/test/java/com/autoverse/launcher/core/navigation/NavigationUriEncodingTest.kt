package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 6 — URI component encoding. Unreserved characters pass through,
 * everything else becomes UTF-8 %XX with uppercase hex; spaces are %20
 * (never "+"). Pure Kotlin, so android.net.Uri is never involved.
 */
class NavigationUriEncodingTest {

    @Test
    fun `unreserved characters pass through unchanged`() {
        assertEquals(
            "AZaz09-_.~",
            encodeUriComponent("AZaz09-_.~"),
        )
    }

    @Test
    fun `spaces become percent twenty`() {
        assertEquals("Wembley%20Stadium", encodeUriComponent("Wembley Stadium"))
        assertEquals("a%20%20b", encodeUriComponent("a  b"))
    }

    @Test
    fun `uri delimiters are escaped`() {
        assertEquals(
            "a%26b%3Dc%3Fd%2Fe%23f%2Bg",
            encodeUriComponent("a&b=c?d/e#f+g"),
        )
    }

    @Test
    fun `accented latin encodes as two byte sequences`() {
        assertEquals("caf%C3%A9", encodeUriComponent("café"))
        assertEquals("M%C3%BCnchen", encodeUriComponent("München"))
        assertEquals("Stra%C3%9Fe", encodeUriComponent("Straße"))
    }

    @Test
    fun `unicode encodes as utf8 bytes with uppercase hex`() {
        assertEquals("%E6%9D%B1%E4%BA%AC", encodeUriComponent("東京"))
        assertEquals("%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0", encodeUriComponent("Москва"))
    }

    @Test
    fun `mixed address encodes every component`() {
        assertEquals(
            "K%C3%B6nigsallee%201%2C%20D%C3%BCsseldorf",
            encodeUriComponent("Königsallee 1, Düsseldorf"),
        )
    }

    @Test
    fun `percent sign itself is escaped`() {
        assertEquals("50%25%20off", encodeUriComponent("50% off"))
    }
}
