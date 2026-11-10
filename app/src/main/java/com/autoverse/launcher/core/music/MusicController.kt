package com.autoverse.launcher.core.music

import android.app.Application
import android.content.ComponentName
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** How the local library is grouped on the Music page. */
enum class LibraryView { SONGS, ALBUMS, ARTISTS, FOLDERS }

data class LibraryState(
    val permissionGranted: Boolean = false,
    val loading: Boolean = false,
    val tracks: List<LocalTrack> = emptyList(),
)

/** Browsing another music app's library. */
sealed interface AppBrowseState {
    data object None : AppBrowseState
    data class Connecting(val app: MusicApp) : AppBrowseState
    data class Listing(
        val app: MusicApp,
        /** Breadcrumb of opened folders (title, mediaId); the first is the root. */
        val path: List<Pair<String, String>>,
        val items: List<BrowseItem>?,
    ) : AppBrowseState
    data class Refused(val app: MusicApp) : AppBrowseState
}

/**
 * The Music page's brain: the on-unit/USB library and AutoVerse's own
 * player, plus browsing other apps' libraries. Now-playing for whatever
 * app is playing still comes from the shared media-session repository.
 */
class MusicController(app: Application) : AndroidViewModel(app) {
    private val library = LocalMusicLibrary(app)
    private val appBrowser = MusicAppBrowser(app)

    private val _library = MutableStateFlow(LibraryState(permissionGranted = hasAudioReadPermission(app)))
    val libraryState: StateFlow<LibraryState> = _library.asStateFlow()

    /** App sources whose library root actually lists something (never empty or refusing apps). */
    private val _apps = MutableStateFlow<List<MusicApp>>(emptyList())
    val musicApps: StateFlow<List<MusicApp>> = _apps.asStateFlow()

    /** True while newly found apps are being checked for a usable library. */
    private val _checkingApps = MutableStateFlow(false)
    val checkingApps: StateFlow<Boolean> = _checkingApps.asStateFlow()

    /** The system Bluetooth browser (phone music), when this unit has one. */
    private var bluetoothService: MusicApp? = null

    private val _phone = MutableStateFlow<PhoneSourceState>(PhoneSourceState.Checking)
    val phoneState: StateFlow<PhoneSourceState> = _phone.asStateFlow()

    private val _browse = MutableStateFlow<AppBrowseState>(AppBrowseState.None)
    val browseState: StateFlow<AppBrowseState> = _browse.asStateFlow()

    private var player: MediaController? = null
    private var reloadJob: Job? = null
    private var browseJob: Job? = null
    private var probeJob: Job? = null

    /** Probe verdicts per service: has content, and when it was checked (elapsed realtime). */
    private val probeVerdicts = mutableMapOf<String, Pair<Boolean, Long>>()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleReload()
    }

    init {
        appBrowser.onSessionChanged = ::onBrowserSessionChanged
        refresh()
        try {
            app.contentResolver.registerContentObserver(library.contentUri, true, observer)
        } catch (_: Exception) {
        }
    }

    /** Re-reads permission, sources and the library (also called when the page opens). */
    fun refresh() {
        val sources = appBrowser.discoverSources()
        bluetoothService = sources.bluetooth
        probeSources(sources.apps)
        val granted = hasAudioReadPermission(getApplication())
        _library.value = _library.value.copy(permissionGranted = granted)
        if (granted) scheduleReload(immediate = true)
    }

    /**
     * Shows only app sources whose library root lists something. Apps already
     * known to have content show straight away; new ones (and ones that were
     * empty a while ago, e.g. not signed in yet) are connected to and checked
     * in parallel with their own browser, so the open source is not disturbed.
     */
    private fun probeSources(candidates: List<MusicApp>) {
        probeJob?.cancel()
        _apps.value = candidates.filter { probeVerdicts[probeKey(it)]?.first == true || isKnownMusicApp(it.packageName) }
        val now = SystemClock.elapsedRealtime()
        val toProbe = candidates.filter { app ->
            val verdict = probeVerdicts[probeKey(app)]
            verdict == null || (!verdict.first && now - verdict.second > REPROBE_EMPTY_MS)
        }
        if (toProbe.isEmpty()) {
            _checkingApps.value = false
            return
        }
        _checkingApps.value = true
        probeJob = viewModelScope.launch {
            val results = coroutineScope {
                toProbe.map { app -> async { app to probeHasContent(app) } }.awaitAll()
            }
            val checkedAt = SystemClock.elapsedRealtime()
            results.forEach { (app, ok) -> probeVerdicts[probeKey(app)] = ok to checkedAt }
            _apps.value = candidates.filter { probeVerdicts[probeKey(it)]?.first == true || isKnownMusicApp(it.packageName) }
            _checkingApps.value = false
        }
    }

    /** Connects to [app] with a throwaway browser; true when its root lists something. */
    private suspend fun probeHasContent(app: MusicApp): Boolean {
        val probe = MusicAppBrowser(getApplication())
        return try {
            val root = probe.connect(app)
            root != null && hasMusicContent(probe.children(root))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        } finally {
            probe.disconnect()
        }
    }

    private fun probeKey(app: MusicApp) = "${app.packageName}/${app.serviceClass}"

    /** A USB stick being indexed fires many change events; reload once it settles. */
    private fun scheduleReload(immediate: Boolean = false) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            if (!immediate) delay(1_500)
            _library.value = _library.value.copy(loading = true)
            val tracks = library.load()
            _library.value = LibraryState(permissionGranted = hasAudioReadPermission(getApplication()), loading = false, tracks = tracks)
        }
    }

    // ---- AutoVerse player ---------------------------------------------------

    /** Plays [tracks] from [startIndex] in AutoVerse's own player. */
    fun playLocal(tracks: List<LocalTrack>, startIndex: Int) {
        if (tracks.isEmpty()) return
        AutoVersePlaybackService.saveQueue(getApplication(), tracks, startIndex)
        viewModelScope.launch {
            val controller = connectPlayer() ?: return@launch
            val items = tracks.map { t ->
                MediaItem.Builder()
                    .setMediaId(t.id.toString())
                    .setUri(t.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(t.title)
                            .setArtist(t.artist)
                            .setAlbumTitle(t.album)
                            .build(),
                    )
                    .build()
            }
            controller.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            controller.shuffleModeEnabled = false
            controller.prepare()
            controller.play()
        }
    }

    fun shuffleLocal(tracks: List<LocalTrack>) {
        if (tracks.isEmpty()) return
        playLocal(tracks.shuffled(), 0)
    }

    private suspend fun connectPlayer(): MediaController? {
        player?.takeIf { it.isConnected }?.let { return it }
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, AutoVersePlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        return suspendCancellableCoroutine { cont ->
            future.addListener(
                {
                    val c = try {
                        future.get()
                    } catch (_: Exception) {
                        null
                    }
                    player = c
                    if (cont.isActive) cont.resume(c)
                },
                ContextCompat.getMainExecutor(app),
            )
            cont.invokeOnCancellation { future.cancel(true) }
        }
    }

    // ---- other apps' libraries ---------------------------------------------

    fun openApp(app: MusicApp) {
        browseJob?.cancel()
        _browse.value = AppBrowseState.Connecting(app)
        browseJob = viewModelScope.launch {
            val root = appBrowser.connect(app)
            if (root == null) {
                _browse.value = AppBrowseState.Refused(app)
                return@launch
            }
            _browse.value = AppBrowseState.Listing(app, listOf(app.label to root), null)
            _browse.value = AppBrowseState.Listing(app, listOf(app.label to root), appBrowser.children(root) ?: emptyList())
        }
    }

    fun openFolder(item: BrowseItem) {
        val s = _browse.value as? AppBrowseState.Listing ?: return
        browseJob?.cancel()
        val path = s.path + (item.title to item.mediaId)
        _browse.value = s.copy(path = path, items = null)
        browseJob = viewModelScope.launch {
            _browse.value = s.copy(path = path, items = appBrowser.children(item.mediaId) ?: emptyList())
        }
    }

    /** Back one level; returns false at the app's root (the UI then closes browsing). */
    fun browseBack(): Boolean {
        val s = _browse.value as? AppBrowseState.Listing ?: return false
        if (s.path.size <= 1) return false
        browseJob?.cancel()
        val path = s.path.dropLast(1)
        _browse.value = s.copy(path = path, items = null)
        browseJob = viewModelScope.launch {
            _browse.value = s.copy(path = path, items = appBrowser.children(path.last().second) ?: emptyList())
        }
        return true
    }

    fun playFromApp(item: BrowseItem): Boolean {
        player?.takeIf { it.isConnected && it.isPlaying }?.pause()
        return appBrowser.play(item)
    }

    fun resumeApp(): Boolean {
        player?.takeIf { it.isConnected && it.isPlaying }?.pause()
        return appBrowser.resume()
    }

    fun closeApp() {
        browseJob?.cancel()
        appBrowser.disconnect()
        _browse.value = AppBrowseState.None
    }

    // ---- phone over Bluetooth ----------------------------------------------

    /**
     * Opens the phone source: connects to the system Bluetooth browser and
     * decides whether a phone is really there (device folder in the root, or
     * a live playback state) — not just whether the service answered.
     */
    fun openPhone() {
        browseJob?.cancel()
        val bt = bluetoothService
        if (bt == null) {
            // This unit's Bluetooth stack doesn't share phone music with apps.
            appBrowser.disconnect()
            _browse.value = AppBrowseState.None
            _phone.value = PhoneSourceState.NotConnected
            return
        }
        _phone.value = PhoneSourceState.Checking
        _browse.value = AppBrowseState.Connecting(bt)
        browseJob = viewModelScope.launch {
            val root = appBrowser.connect(bt)
            if (root == null) {
                _browse.value = AppBrowseState.Refused(bt)
                _phone.value = PhoneSourceState.NotConnected
                return@launch
            }
            val path = listOf(bt.label to root)
            // With no phone the stack reports "disconnected" at once, while its
            // root may never answer; show the empty state now, keep listening.
            if (phoneReportsDisconnected(appBrowser.session())) _phone.value = PhoneSourceState.NotConnected
            _browse.value = AppBrowseState.Listing(bt, path, null)
            val items = appBrowser.children(root) ?: emptyList()
            _browse.value = AppBrowseState.Listing(bt, path, items)
            _phone.value = phoneSourceState(items.size, appBrowser.session())
        }
    }

    /** Starts the phone's player (AVRCP play); false when the request couldn't be sent. */
    fun playPhone(): Boolean {
        if (!isPhoneOpen()) return false
        player?.takeIf { it.isConnected && it.isPlaying }?.pause()
        return appBrowser.resume()
    }

    fun pausePhone(): Boolean = isPhoneOpen() && appBrowser.pause()

    fun nextPhone(): Boolean = isPhoneOpen() && appBrowser.skipToNext()

    private fun isPhoneOpen(): Boolean {
        val bt = bluetoothService ?: return false
        return (_browse.value as? AppBrowseState.Listing)?.app?.packageName == bt.packageName
    }

    /** The Bluetooth session changed: a phone connected, disconnected, or changed track. */
    private fun onBrowserSessionChanged() {
        if (!isPhoneOpen()) return
        val listing = _browse.value as? AppBrowseState.Listing ?: return
        val atRoot = listing.path.size <= 1
        val rootCount = if (atRoot) listing.items?.size ?: 0 else 1
        val wasConnected = _phone.value is PhoneSourceState.Connected
        val now = phoneSourceState(rootCount, appBrowser.session())
        _phone.value = now
        // A phone that just connected shows up as a device folder: list it.
        if (!wasConnected && now is PhoneSourceState.Connected && atRoot && listing.items?.isEmpty() == true) {
            browseJob?.cancel()
            browseJob = viewModelScope.launch {
                val rootId = listing.path.first().second
                val items = appBrowser.children(rootId) ?: emptyList()
                _browse.value = listing.copy(items = items)
            }
        }
    }

    override fun onCleared() {
        appBrowser.onSessionChanged = null
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(observer)
        } catch (_: Exception) {
        }
        appBrowser.disconnect()
        player?.release()
        super.onCleared()
    }

    private companion object {
        /** An app that was empty (not signed in, never opened) is checked again after this long. */
        const val REPROBE_EMPTY_MS = 2 * 60_000L
    }
}

/** Groups for the Albums / Artists / Folders views, biggest first within the same name order. */
fun groupTracks(tracks: List<LocalTrack>, view: LibraryView): List<Pair<String, List<LocalTrack>>> = when (view) {
    LibraryView.SONGS -> listOf("All songs" to tracks)
    LibraryView.ALBUMS -> tracks.groupBy { it.album }.toSortedMap(String.CASE_INSENSITIVE_ORDER).toList()
    LibraryView.ARTISTS -> tracks.groupBy { it.artist }.toSortedMap(String.CASE_INSENSITIVE_ORDER).toList()
    LibraryView.FOLDERS -> tracks.groupBy { "${it.source} › ${it.folder}" }.toSortedMap(String.CASE_INSENSITIVE_ORDER).toList()
}

/** Search across title, artist and album. */
fun filterTracks(tracks: List<LocalTrack>, query: String): List<LocalTrack> {
    val q = query.trim()
    if (q.isEmpty()) return tracks
    return tracks.filter {
        it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
    }
}
