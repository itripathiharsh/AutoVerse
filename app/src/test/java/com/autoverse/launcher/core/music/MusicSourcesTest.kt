package com.autoverse.launcher.core.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSourcesTest {

    private fun app(pkg: String, label: String, service: String = "$pkg.MediaService") =
        MusicApp(packageName = pkg, serviceClass = service, label = label)

    private fun item(browsable: Boolean = false, playable: Boolean = false) =
        BrowseItem(mediaId = "id", title = "t", subtitle = null, browsable = browsable, playable = playable)

    private val bluetooth = app("com.android.bluetooth", "Bluetooth", "com.android.bluetooth.avrcpcontroller.BluetoothMediaBrowserService")

    @Test
    fun `Google app, Play services, Android Auto and navigation are not music sources`() {
        val sources = selectMusicSources(
            listOf(
                app("com.google.android.googlequicksearchbox", "Google"),
                app("com.google.android.gms", "Google Play services"),
                app("com.google.android.projection.gearhead", "Android Auto"),
                app("com.google.android.apps.maps", "Maps"),
                app("com.waze", "Waze"),
                app("com.spotify.music", "Spotify"),
            ),
            ownPackage = "com.autoverse.launcher",
        )
        assertEquals(listOf("Spotify"), sources.apps.map { it.label })
    }

    @Test
    fun `Bluetooth browser becomes the phone source, not an app chip`() {
        val sources = selectMusicSources(listOf(bluetooth, app("com.spotify.music", "Spotify")), "com.autoverse.launcher")
        assertEquals("com.android.bluetooth", sources.bluetooth?.packageName)
        assertEquals(PHONE_SOURCE_LABEL, sources.bluetooth?.label)
        assertEquals(listOf("com.spotify.music"), sources.apps.map { it.packageName })
    }

    @Test
    fun `Bluetooth browser is recognised by its service class in another package`() {
        assertTrue(isBluetoothSource("com.oem.bt", "com.android.bluetooth.a2dpsink.mbs.A2dpMediaBrowserService"))
        assertTrue(isBluetoothSource("com.google.android.bluetooth", "x.Service"))
        assertFalse(isBluetoothSource("com.spotify.music", "com.spotify.MediaService"))
        assertNull(selectMusicSources(listOf(app("com.spotify.music", "Spotify")), "me").bluetooth)
    }

    @Test
    fun `own package and duplicate services of one package are dropped`() {
        val sources = selectMusicSources(
            listOf(
                app("com.autoverse.launcher", "AutoVerse"),
                app("com.maxmpz.audioplayer", "Poweramp", "a.One"),
                app("com.maxmpz.audioplayer", "Poweramp", "a.Two"),
            ),
            ownPackage = "com.autoverse.launcher",
        )
        assertEquals(listOf("Poweramp"), sources.apps.map { it.label })
    }

    @Test
    fun `YouTube Music is labelled YT Music so it no longer clashes with YouTube`() {
        val sources = selectMusicSources(
            listOf(
                app("com.google.android.youtube", "YouTube"),
                app("com.google.android.apps.youtube.music", "YouTube"),
            ),
            ownPackage = "me",
        )
        assertEquals(listOf("YouTube", "YT Music"), sources.apps.map { it.label })
    }

    @Test
    fun `remaining label clashes get a hint from the package name`() {
        val labels = disambiguateLabels(
            listOf(
                app("com.google.android.youtube", "YouTube"),
                app("app.revanced.android.youtube", "YouTube"),
                app("com.example.youtube.kids", "YouTube"),
            ),
        ).map { it.label }
        assertEquals(listOf("YouTube", "YouTube (Revanced)", "YouTube (Kids)"), labels)
    }

    @Test
    fun `clashes the package cannot explain are numbered`() {
        val labels = disambiguateLabels(
            listOf(app("com.a.radio", "Radio"), app("com.b.radio", "Radio")),
        ).map { it.label }
        assertEquals(listOf("Radio (A)", "Radio (B)"), labels)
        val numbered = disambiguateLabels(
            listOf(app("com.android.music", "Music"), app("com.google.android.music", "Music")),
        ).map { it.label }
        assertEquals(listOf("Music", "Music 2"), numbered)
    }

    @Test
    fun `label falls back to the package when the system has none`() {
        assertEquals("Spotify", musicSourceLabel("com.spotify.music", " Spotify "))
        assertEquals("Jiosaavn", musicSourceLabel("com.jio.media.jiobeats.jiosaavn", ""))
        assertEquals("YT Music", musicSourceLabel("com.google.android.apps.youtube.music", "YouTube Music"))
    }

    @Test
    fun `a source is shown only when its root lists something to open or play`() {
        assertFalse(hasMusicContent(null))
        assertFalse(hasMusicContent(emptyList()))
        assertFalse(hasMusicContent(listOf(item())))
        assertTrue(hasMusicContent(listOf(item(browsable = true))))
        assertTrue(hasMusicContent(listOf(item(playable = true))))
    }

    @Test
    fun `no phone means the Bluetooth stack reports an error and an empty root`() {
        assertEquals(PhoneSourceState.NotConnected, phoneSourceState(0, SessionSnapshot(stateCode = 7, title = null, artist = null)))
        assertEquals(PhoneSourceState.NotConnected, phoneSourceState(0, null))
        assertEquals(PhoneSourceState.NotConnected, phoneSourceState(0, SessionSnapshot(stateCode = 0, title = null, artist = null)))
        // Stale device folder after the phone left: the error state wins.
        assertEquals(PhoneSourceState.NotConnected, phoneSourceState(1, SessionSnapshot(stateCode = 7, title = null, artist = null)))
        assertTrue(phoneReportsDisconnected(SessionSnapshot(stateCode = 7, title = "", artist = null)))
        assertFalse(phoneReportsDisconnected(SessionSnapshot(stateCode = 0, title = null, artist = null)))
        assertFalse(phoneReportsDisconnected(null))
    }

    @Test
    fun `a connected phone shows its track and whether it plays`() {
        assertEquals(
            PhoneSourceState.Connected(title = "Kesariya", artist = "Arijit Singh", playing = true),
            phoneSourceState(1, SessionSnapshot(stateCode = 3, title = "Kesariya", artist = "Arijit Singh")),
        )
        // Paired and connected, nothing playing yet: device folder in the root.
        assertEquals(
            PhoneSourceState.Connected(title = null, artist = null, playing = false),
            phoneSourceState(1, SessionSnapshot(stateCode = 0, title = null, artist = null)),
        )
        // Paused phone with no browsing support.
        assertEquals(
            PhoneSourceState.Connected(title = null, artist = null, playing = false),
            phoneSourceState(0, SessionSnapshot(stateCode = 2, title = " ", artist = null)),
        )
    }
}
