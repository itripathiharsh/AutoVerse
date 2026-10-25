package com.autoverse.launcher.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OfflineVoiceRulesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `wake phrase detected case-insensitively`() {
        assertTrue(OfflineVoiceRules.isWakePhrase("hey autoverse"))
        assertTrue(OfflineVoiceRules.isWakePhrase("Hey AutoVerse open spotify"))
        assertTrue(OfflineVoiceRules.isWakePhrase("  autoverse "))
    }

    @Test
    fun `other speech does not wake`() {
        assertFalse(OfflineVoiceRules.isWakePhrase(null))
        assertFalse(OfflineVoiceRules.isWakePhrase(""))
        assertFalse(OfflineVoiceRules.isWakePhrase("open spotify"))
        assertFalse(OfflineVoiceRules.isWakePhrase("hey google"))
    }

    @Test
    fun `wake phrase stripped from transcripts`() {
        assertEquals("open spotify", OfflineVoiceRules.withoutWakePhrase("hey autoverse open spotify"))
        assertEquals("navigate to work", OfflineVoiceRules.withoutWakePhrase("Hey AutoVerse, navigate to work"))
        assertEquals("", OfflineVoiceRules.withoutWakePhrase("autoverse")) // bare wake, nothing left
    }

    @Test
    fun `model completeness needs am and conf`() {
        val dir = tmp.newFolder("model")
        assertFalse(OfflineVoiceRules.isModelComplete(dir))
        dir.resolve("am").mkdirs()
        dir.resolve("conf").mkdirs()
        assertTrue(OfflineVoiceRules.isModelComplete(dir))
    }

    @Test
    fun `zip entries flatten under the target dir`() {
        val target = tmp.newFolder("voice-model")
        val file = OfflineVoiceRules.safeFlattenedTarget(target, "vosk-model-small-en-us-0.15/conf/model.conf")
        assertEquals(target.resolve("conf/model.conf").canonicalPath, file?.canonicalPath)
    }

    @Test
    fun `unsafe zip entries rejected`() {
        val target = tmp.newFolder("voice-model")
        assertNull(OfflineVoiceRules.safeFlattenedTarget(target, "vosk-model/../../evil.so"))
        assertNull(OfflineVoiceRules.safeFlattenedTarget(target, "vosk-model/"))
        assertNull(OfflineVoiceRules.safeFlattenedTarget(target, "single-root-file"))
        assertNull(OfflineVoiceRules.safeFlattenedTarget(target, ""))
    }

    @Test
    fun `byte labels are human readable`() {
        assertEquals("40.0 MB", OfflineVoiceRules.bytesLabel(40_000_000L))
        assertEquals("512 KB", OfflineVoiceRules.bytesLabel(512_000L))
        assertEquals("900 B", OfflineVoiceRules.bytesLabel(900L))
    }
}
