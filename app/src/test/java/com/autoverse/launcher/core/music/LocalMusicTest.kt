package com.autoverse.launcher.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalMusicTest {

    private fun track(title: String, artist: String, album: String, folder: String, source: String = "SanDisk USB drive") =
        LocalTrack(
            id = title.hashCode().toLong(),
            uri = "content://media/x/audio/media/${title.hashCode()}",
            title = title,
            artist = artist,
            album = album,
            durationMs = 200_000,
            folder = folder,
            source = source,
            albumId = album.hashCode().toLong(),
        )

    private val songs = listOf(
        track("Tum Hi Ho", "Arijit Singh", "Aashiqui 2", "Music/Bollywood"),
        track("Kesariya", "Arijit Singh", "Brahmastra", "Music/Bollywood"),
        track("Lag Ja Gale", "Lata Mangeshkar", "Woh Kaun Thi", "Music/Old Hindi", source = "This unit"),
    )

    @Test
    fun `folder comes from Android 10 relative path or old full path`() {
        assertEquals("Music/Old Hindi", folderOf("Music/Old Hindi/"))
        assertEquals("Music", folderOf("/storage/1A2B-3C4D/Music/song.mp3"))
        assertEquals("Download", folderOf("/storage/emulated/0/Download/song.mp3"))
        assertEquals("Top folder", folderOf("/storage/1A2B-3C4D/song.mp3"))
        assertEquals("Top folder", folderOf(""))
    }

    @Test
    fun `source names the USB drive when Android describes it`() {
        val names = mapOf("1a2b-3c4d" to "SanDisk USB drive", "external_primary" to "This unit")
        assertEquals("SanDisk USB drive", sourceLabel("1a2b-3c4d", names))
        assertEquals("This unit", sourceLabel("external_primary", emptyMap()))
        assertEquals("USB / SD card", sourceLabel("9f9f-0000", emptyMap()))
        assertEquals("This unit", sourceLabel(null, emptyMap()))
    }

    @Test
    fun `unknown tags are treated as missing`() {
        assertNull(cleanTag("<unknown>"))
        assertNull(cleanTag("  "))
        assertEquals("Arijit Singh", cleanTag(" Arijit Singh "))
    }

    @Test
    fun `search matches title, artist or album`() {
        assertEquals(2, filterTracks(songs, "arijit").size)
        assertEquals(listOf("Lag Ja Gale"), filterTracks(songs, "woh kaun").map { it.title })
        assertEquals(3, filterTracks(songs, " ").size)
    }

    @Test
    fun `library groups by album, artist and folder with source`() {
        assertEquals(listOf("Arijit Singh", "Lata Mangeshkar"), groupTracks(songs, LibraryView.ARTISTS).map { it.first })
        assertEquals(2, groupTracks(songs, LibraryView.ARTISTS).first().second.size)
        assertEquals(
            listOf("SanDisk USB drive › Music/Bollywood", "This unit › Music/Old Hindi"),
            groupTracks(songs, LibraryView.FOLDERS).map { it.first },
        )
        assertEquals(3, groupTracks(songs, LibraryView.ALBUMS).size)
        assertEquals(1, groupTracks(songs, LibraryView.SONGS).size)
    }
}
