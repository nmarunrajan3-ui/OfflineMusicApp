package com.auramusic.offline.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auramusic.offline.data.AudioScanner
import com.auramusic.offline.data.CustomAlbumRepository
import com.auramusic.offline.data.DuplicateFinder
import com.auramusic.offline.data.FavoritesRepository
import com.auramusic.offline.data.RecentlyPlayedRepository
import com.auramusic.offline.data.RecycleBinManager
import com.auramusic.offline.data.SettingsRepository
import com.auramusic.offline.model.AudioCategory
import com.auramusic.offline.model.AudioTrack
import com.auramusic.offline.model.CustomAlbum
import com.auramusic.offline.model.DuplicateGroup
import com.auramusic.offline.model.PlaybackState
import com.auramusic.offline.service.MusicPlaybackService
import com.auramusic.offline.util.ShareHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val audioScanner = AudioScanner(application)
    private val recycleBinManager = RecycleBinManager(application)
    private val customAlbumRepo = CustomAlbumRepository(application)
    private val favoritesRepo = FavoritesRepository(application)
    private val recentlyPlayedRepo = RecentlyPlayedRepository(application)
    private val settingsRepo = SettingsRepository(application)

    private var playbackService: MusicPlaybackService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? MusicPlaybackService.LocalBinder
            playbackService = localBinder?.getService()
            isServiceBound = true

            // Restore last played track if service has no track loaded yet
            if (playbackState.value.currentTrack == null && _rawAllTracks.value.isNotEmpty()) {
                val musicOnly = _rawAllTracks.value.filter { it.category == AudioCategory.MUSIC }
                val lastId = recentlyPlayedRepo.getLastPlayedTrackId()
                val trackToRestore = if (lastId != null) {
                    musicOnly.find { it.id == lastId } ?: musicOnly.firstOrNull()
                } else {
                    musicOnly.firstOrNull()
                }
                if (trackToRestore != null) {
                    playbackService?.restoreTrackWithoutPlaying(trackToRestore, musicOnly)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    // Playback state exposed directly from service companion
    val playbackState: StateFlow<PlaybackState> = MusicPlaybackService.playbackState

    // Theme state
    private val _isDarkMode = MutableStateFlow(settingsRepo.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Library states
    private val _rawAllTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val isLoading = MutableStateFlow(true)

    // Search query for auto-search everywhere
    val searchQuery = MutableStateFlow("")

    // Multi-select state
    val isSelectionMode = MutableStateFlow(false)
    val selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())

    // Custom albums
    private val _customAlbums = MutableStateFlow<List<CustomAlbum>>(emptyList())
    val customAlbums: StateFlow<List<CustomAlbum>> = _customAlbums.asStateFlow()

    // Favorites
    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())

    // Recently played
    private val _recentIds = MutableStateFlow<List<Long>>(emptyList())

    // Filtered separate streams
    val musicTracks: StateFlow<List<AudioTrack>> = combine(_rawAllTracks, _favoriteIds, searchQuery) { tracks, favs, query ->
        tracks.filter { it.category == AudioCategory.MUSIC }
            .map { it.copy(isFavorite = favs.contains(it.id)) }
            .filter {
                if (query.isBlank()) true else {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyPlayedTracks: StateFlow<List<AudioTrack>> = combine(musicTracks, _recentIds) { allMusic, recentIds ->
        val map = allMusic.associateBy { it.id }
        recentIds.mapNotNull { map[it] }.take(5)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteTracks: StateFlow<List<AudioTrack>> = musicTracks.combine(_favoriteIds) { tracks, favs ->
        tracks.filter { favs.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dedicated isolated Voice Recordings
    val voiceRecordings: StateFlow<List<AudioTrack>> = combine(_rawAllTracks, searchQuery) { tracks, query ->
        tracks.filter { it.category == AudioCategory.VOICE_RECORDING }
            .filter {
                if (query.isBlank()) true else {
                    it.title.contains(query, ignoreCase = true) || it.dataPath.contains(query, ignoreCase = true)
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dedicated isolated Call Records
    val callRecords: StateFlow<List<AudioTrack>> = combine(_rawAllTracks, searchQuery) { tracks, query ->
        tracks.filter { it.category == AudioCategory.CALL_RECORD }
            .filter {
                if (query.isBlank()) true else {
                    it.title.contains(query, ignoreCase = true) || it.dataPath.contains(query, ignoreCase = true)
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Recycle bin items
    val recycleBinTracks: StateFlow<List<AudioTrack>> = combine(_rawAllTracks, searchQuery) { tracks, query ->
        tracks.filter { it.category == AudioCategory.RECYCLE_BIN }
            .filter {
                if (query.isBlank()) true else {
                    it.title.contains(query, ignoreCase = true)
                }
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Duplicate groups
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = musicTracks.combine(searchQuery) { tracks, _ ->
        DuplicateFinder.findDuplicates(tracks)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        bindPlaybackService()
        loadData()
    }

    private fun bindPlaybackService() {
        val intent = Intent(getApplication(), MusicPlaybackService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun loadData() {
        viewModelScope.launch {
            isLoading.value = true
            _favoriteIds.value = favoritesRepo.getFavoriteIds()
            _recentIds.value = recentlyPlayedRepo.getRecentTrackIds()
            _customAlbums.value = customAlbumRepo.getAlbums()

            val trashedPaths = recycleBinManager.getTrashedPaths()
            var scanned = audioScanner.scanDeviceAudio(trashedPaths)

            // If device has no offline audio files yet, create demo tracks so the user can test all features immediately
            if (scanned.isEmpty()) {
                scanned = createSampleDemoLibrary()
            }

            _rawAllTracks.value = scanned
            isLoading.value = false

            // Restore last played song for bottom bar
            if (playbackState.value.currentTrack == null) {
                val musicOnly = scanned.filter { it.category == AudioCategory.MUSIC }
                val lastId = recentlyPlayedRepo.getLastPlayedTrackId()
                val trackToRestore = if (lastId != null) {
                    musicOnly.find { it.id == lastId } ?: musicOnly.firstOrNull()
                } else {
                    musicOnly.firstOrNull()
                }

                if (trackToRestore != null) {
                    playbackService?.restoreTrackWithoutPlaying(trackToRestore, musicOnly)
                }
            }
        }
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        settingsRepo.setDarkMode(next)
    }

    // Playback Actions
    fun playTrack(track: AudioTrack, playlist: List<AudioTrack> = musicTracks.value) {
        val intent = Intent(getApplication(), MusicPlaybackService::class.java)
        getApplication<Application>().startService(intent)

        val index = playlist.indexOfFirst { it.id == track.id }
        playbackService?.playTrackList(playlist, if (index != -1) index else 0)
        recentlyPlayedRepo.recordTrackPlayed(track.id)
        _recentIds.value = recentlyPlayedRepo.getRecentTrackIds()
    }

    fun playNext(track: AudioTrack) {
        val intent = Intent(getApplication(), MusicPlaybackService::class.java)
        getApplication<Application>().startService(intent)
        playbackService?.playNext(track)
    }

    fun addToQueue(track: AudioTrack) {
        val intent = Intent(getApplication(), MusicPlaybackService::class.java)
        getApplication<Application>().startService(intent)
        playbackService?.addToQueue(track)
    }

    fun removeFromQueue(index: Int) {
        playbackService?.removeFromQueue(index)
    }

    fun togglePlayPause() {
        val service = playbackService
        if (service != null) {
            service.togglePlayPause()
        } else {
            val intent = Intent(getApplication(), MusicPlaybackService::class.java).apply {
                action = com.auramusic.offline.service.MediaNotificationHelper.ACTION_TOGGLE
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun next() {
        val intent = Intent(getApplication(), MusicPlaybackService::class.java)
        getApplication<Application>().startService(intent)
        playbackService?.next()
    }

    fun previous() {
        val intent = Intent(getApplication(), MusicPlaybackService::class.java)
        getApplication<Application>().startService(intent)
        playbackService?.previous()
    }

    fun seekTo(positionMs: Long) {
        playbackService?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playbackService?.toggleShuffle()
    }

    fun toggleRepeat() {
        playbackService?.toggleRepeat()
    }

    fun setSleepTimer(minutes: Int?) {
        playbackService?.setSleepTimer(minutes)
    }

    fun toggleFavorite(trackId: Long) {
        val isFav = favoritesRepo.toggleFavorite(trackId)
        val current = _favoriteIds.value.toMutableSet()
        if (isFav) current.add(trackId) else current.remove(trackId)
        _favoriteIds.value = current
    }

    // Custom Albums
    fun createCustomAlbum(name: String, description: String, customPhotoUri: String?) {
        val album = CustomAlbum(
            name = name,
            description = description,
            customPhotoUri = customPhotoUri
        )
        customAlbumRepo.saveAlbum(album)
        _customAlbums.value = customAlbumRepo.getAlbums()
    }

    fun deleteCustomAlbum(albumId: String) {
        customAlbumRepo.deleteAlbum(albumId)
        _customAlbums.value = customAlbumRepo.getAlbums()
    }

    fun addTracksToAlbum(albumId: String, trackIds: List<Long>) {
        trackIds.forEach { customAlbumRepo.addTrackToAlbum(albumId, it) }
        _customAlbums.value = customAlbumRepo.getAlbums()
    }

    fun removeTrackFromAlbum(albumId: String, trackId: Long) {
        customAlbumRepo.removeTrackFromAlbum(albumId, trackId)
        _customAlbums.value = customAlbumRepo.getAlbums()
    }

    // Recycle Bin
    fun moveToRecycleBin(tracks: List<AudioTrack>) {
        viewModelScope.launch {
            tracks.forEach { recycleBinManager.moveToTrash(it) }
            loadData()
            clearSelection()
        }
    }

    fun restoreFromRecycleBin(track: AudioTrack) {
        viewModelScope.launch {
            recycleBinManager.restore(track)
            loadData()
        }
    }

    fun deletePermanently(track: AudioTrack) {
        viewModelScope.launch {
            recycleBinManager.deletePermanently(track)
            loadData()
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            val trashed = recycleBinTracks.value
            recycleBinManager.emptyBin(trashed)
            loadData()
        }
    }

    // Duplicate Cleaner
    fun cleanDuplicates(groups: List<DuplicateGroup>) {
        viewModelScope.launch {
            val duplicatesToTrash = mutableListOf<AudioTrack>()
            for (group in groups) {
                if (group.tracks.size > 1) {
                    duplicatesToTrash.addAll(group.tracks.drop(1))
                }
            }
            moveToRecycleBin(duplicatesToTrash)
        }
    }

    // Multi-Selection
    fun toggleSelectionMode() {
        val next = !isSelectionMode.value
        isSelectionMode.value = next
        if (!next) {
            selectedTrackIds.value = emptySet()
        }
    }

    fun startSelectionWith(trackId: Long) {
        isSelectionMode.value = true
        selectedTrackIds.value = setOf(trackId)
    }

    fun toggleFavoritesForSelected() {
        val selected = selectedTrackIds.value
        if (selected.isEmpty()) return
        val currentFavs = _favoriteIds.value.toMutableSet()
        val allAreFav = selected.all { currentFavs.contains(it) }
        selected.forEach { id ->
            if (allAreFav) {
                favoritesRepo.toggleFavorite(id)
                currentFavs.remove(id)
            } else {
                if (!currentFavs.contains(id)) {
                    favoritesRepo.toggleFavorite(id)
                    currentFavs.add(id)
                }
            }
        }
        _favoriteIds.value = currentFavs
    }

    fun toggleSelectTrack(trackId: Long) {
        val current = selectedTrackIds.value.toMutableSet()
        if (current.contains(trackId)) {
            current.remove(trackId)
        } else {
            current.add(trackId)
        }
        selectedTrackIds.value = current
    }

    fun selectAll(tracks: List<AudioTrack>) {
        selectedTrackIds.value = tracks.map { it.id }.toSet()
    }

    fun deselectAll() {
        selectedTrackIds.value = emptySet()
    }

    fun clearSelection() {
        selectedTrackIds.value = emptySet()
        isSelectionMode.value = false
    }

    fun shareSelectedTracks(context: Context) {
        val selected = musicTracks.value.filter { selectedTrackIds.value.contains(it.id) }
        if (selected.isNotEmpty()) {
            ShareHelper.shareTracks(context, selected)
        }
    }

    private fun createSampleDemoLibrary(): List<AudioTrack> {
        val now = System.currentTimeMillis()
        return listOf(
            AudioTrack(
                id = 1001L,
                title = "Midnight City Lights",
                artist = "Aura Synthetics",
                album = "Neon Horizons",
                durationMs = 214000L,
                dataPath = "/storage/emulated/0/Music/Midnight City Lights.mp3",
                contentUriString = "",
                albumId = 1L,
                size = 8500000L,
                dateAdded = now - 10000,
                category = AudioCategory.MUSIC
            ),
            AudioTrack(
                id = 1002L,
                title = "Starlight Sonata",
                artist = "Eleanor Vance",
                album = "Cosmic Nocturnes",
                durationMs = 185000L,
                dataPath = "/storage/emulated/0/Music/Starlight Sonata.mp3",
                contentUriString = "",
                albumId = 2L,
                size = 6200000L,
                dateAdded = now - 20000,
                category = AudioCategory.MUSIC
            ),
            AudioTrack(
                id = 1003L,
                title = "Ocean Breeze Acoustic",
                artist = "Julian Croft",
                album = "Summer Memories",
                durationMs = 242000L,
                dataPath = "/storage/emulated/0/Music/Ocean Breeze Acoustic.mp3",
                contentUriString = "",
                albumId = 3L,
                size = 9400000L,
                dateAdded = now - 30000,
                category = AudioCategory.MUSIC
            ),
            AudioTrack(
                id = 1004L,
                title = "Quantum Echoes",
                artist = "Nebula Theory",
                album = "Deep Space",
                durationMs = 276000L,
                dataPath = "/storage/emulated/0/Music/Quantum Echoes.mp3",
                contentUriString = "",
                albumId = 4L,
                size = 11000000L,
                dateAdded = now - 40000,
                category = AudioCategory.MUSIC
            ),
            AudioTrack(
                id = 1005L,
                title = "Velvet Sunset",
                artist = "Elena Rostova",
                album = "Golden Hour Sessions",
                durationMs = 198000L,
                dataPath = "/storage/emulated/0/Music/Velvet Sunset.mp3",
                contentUriString = "",
                albumId = 5L,
                size = 7800000L,
                dateAdded = now - 50000,
                category = AudioCategory.MUSIC
            ),
            AudioTrack(
                id = 1006L,
                title = "Aurora Borealis Dream",
                artist = "Nordic Chill",
                album = "Arctic Sounds",
                durationMs = 230000L,
                dataPath = "/storage/emulated/0/Music/Aurora Borealis Dream.mp3",
                contentUriString = "",
                albumId = 6L,
                size = 8900000L,
                dateAdded = now - 60000,
                category = AudioCategory.MUSIC
            ),
            // Sample Voice Recording (Cleanly isolated)
            AudioTrack(
                id = 2001L,
                title = "Voice Memo - Studio Melody Idea",
                artist = "Voice Recorder",
                album = "Voice Notes",
                durationMs = 64000L,
                dataPath = "/storage/emulated/0/Recordings/Voice Memo - Studio Melody Idea.m4a",
                contentUriString = "",
                albumId = 20L,
                size = 1200000L,
                dateAdded = now - 80000,
                category = AudioCategory.VOICE_RECORDING
            ),
            // Sample Call Recording (Cleanly isolated)
            AudioTrack(
                id = 3001L,
                title = "Call_Recording_+18005550199_20261101",
                artist = "Phone Call Record",
                album = "Call Records",
                durationMs = 142000L,
                dataPath = "/storage/emulated/0/Call/Call_Recording_+18005550199_20261101.amr",
                contentUriString = "",
                albumId = 30L,
                size = 900000L,
                dateAdded = now - 90000,
                category = AudioCategory.CALL_RECORD
            )
        )
    }

    override fun onCleared() {
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onCleared()
    }
}
