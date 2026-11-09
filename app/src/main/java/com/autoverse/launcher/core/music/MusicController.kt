package com.autoverse.launcher.core.music

import android.app.Application
import android.content.ComponentName
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Job
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

    private val _apps = MutableStateFlow<List<MusicApp>>(emptyList())
    val musicApps: StateFlow<List<MusicApp>> = _apps.asStateFlow()

    private val _browse = MutableStateFlow<AppBrowseState>(AppBrowseState.None)
    val browseState: StateFlow<AppBrowseState> = _browse.asStateFlow()

    private var player: MediaController? = null
    private var reloadJob: Job? = null
    private var browseJob: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleReload()
    }

    init {
        refresh()
        try {
            app.contentResolver.registerContentObserver(library.contentUri, true, observer)
        } catch (_: Exception) {
        }
    }

    /** Re-reads permission, apps and the library (also called when the page opens). */
    fun refresh() {
        _apps.value = appBrowser.installedMusicApps()
        val granted = hasAudioReadPermission(getApplication())
        _library.value = _library.value.copy(permissionGranted = granted)
        if (granted) scheduleReload(immediate = true)
    }

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

    override fun onCleared() {
        try {
            getApplication<Application>().contentResolver.unregisterContentObserver(observer)
        } catch (_: Exception) {
        }
        appBrowser.disconnect()
        player?.release()
        super.onCleared()
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
