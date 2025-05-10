package com.example.purrytify.ui.playback

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.data.mapper.SongMapper
import com.example.purrytify.model.Song
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.util.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository
    private var allSongs: List<Song> = emptyList()
    private var currentIndex = 0

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private var currentUserId: Int? = null

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
                        handler.postDelayed(this, 100)
                    } else {
                        handler.removeCallbacks(this)
                    }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Error updating progress: ${e.message}")
                }
            }
        }
    }

    // Queue for pending song ID requests
    private val pendingSongRequests = ArrayDeque<String>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())
        _isLoading.value = true

        // Listen for song deletion events
        viewModelScope.launch {
            EventBus.songDeletedEvents.collect { songId ->
                Log.d("PlayerViewModel", "Song deletion event received for song ID: $songId")
                if (_currentSong.value?.id == songId.toString()) {
                    stopCurrentSong()
                    _currentSong.value = null
                }
            }
        }

        // Listen for song update events
        viewModelScope.launch {
            EventBus.songUpdatedEvents.collect { songId ->
                Log.d("PlayerViewModel", "Song update event received for song ID: $songId")
                if (_currentSong.value?.id == songId.toString()) {
                    // Currently playing song was updated, refresh its details
                    viewModelScope.launch {
                        repository.getSongById(songId)?.let { updatedSongEntity ->
                            val updatedSong = SongMapper.toSong(updatedSongEntity)
                            _currentSong.value = updatedSong
                            _isLiked.value = updatedSong.isLiked
                        }
                    }
                }
            }
        }
    }

    fun updateForUser(userId: Int) {
        if (currentUserId == userId && allSongs.isNotEmpty()) return // Avoid unnecessary updates
        currentUserId = userId
        _isLoading.value = true

        viewModelScope.launch {
            repository.getSongsByUserId(userId).asFlow().collect { songEntities ->
                val newSongsList = SongMapper.toSongList(songEntities)

                // Check if the currently playing song was deleted
                if (_currentSong.value != null && !songWasDeleted(_currentSong.value!!, newSongsList)) {
                    // Current song still exists, update the list
                    allSongs = newSongsList
                    currentIndex = allSongs.indexOfFirst { it.id == _currentSong.value?.id }
                    if (currentIndex < 0) currentIndex = 0
                } else {
                    // Current song was deleted or no song is playing
                    val wasPlaying = _isPlaying.value
                    if (wasPlaying) {
                        stopCurrentSong()
                    }
                    allSongs = newSongsList
                }

                _isLoading.value = false
                processPendingSongRequests()

                Log.d("PlayerViewModel", "Songs updated for user $userId: ${allSongs.size} songs")
            }
        }
    }

    private fun songWasDeleted(song: Song, newSongsList: List<Song>): Boolean {
        return newSongsList.none { it.id == song.id }
    }

    // Process any pending song ID requests that came in before loading completed
    private fun processPendingSongRequests() {
        if (pendingSongRequests.isNotEmpty()) {
            val songId = pendingSongRequests.removeFirst()
            Log.d("PlayerViewModel", "Processing pending request for song ID: $songId")

            val song = allSongs.find { it.id == songId }

            if (song != null) {
                Log.d("PlayerViewModel", "Found pending song: ${song.title}")
                _currentSong.value = song
                playSong(song)
            } else {
                Log.e("PlayerViewModel", "Pending song with ID $songId not found")
            }
        }
    }

    fun getSongById(songId: String): Song? {
        return allSongs.find { it.id == songId }
    }

    fun playSongById(songId: String) {
        Log.d("PlayerViewModel", "playSongById called with ID: $songId")

        if (_isLoading.value) {
            Log.d("PlayerViewModel", "Songs still loading, queueing request for later")
            pendingSongRequests.add(songId)
            return
        }

        val song = allSongs.find { it.id == songId }
        if (song != null) {
            Log.d("PlayerViewModel", "Found song: ${song.title}")

            if (_currentSong.value?.id == songId && _isPlaying.value) {
                Log.d("PlayerViewModel", "Same song is already playing, ignoring request.")
                return
            }

            if (_currentSong.value?.id != songId) {
                stopCurrentSong()
                _currentSong.value = song
            }

            playSong(song)
        } else {
            Log.e("PlayerViewModel", "Song with ID $songId not found")
        }
    }

    fun playSong(song: Song) {
        stopCurrentSong()
        _currentSong.value = song
        _isLiked.value = song.isLiked
        currentIndex = allSongs.indexOfFirst { it.id == song.id }
        Log.d("PlayerViewModel", "Playing song: ${song.title} from ${song.audioUrl}")

        viewModelScope.launch {
            try {
                repository.updateLastPlayed(song.id.toLong())
                Log.d("PlayerViewModel", "Updated lastPlayedAt for song: ${song.title}")
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error updating lastPlayedAt: ${e.message}")
            }
        }

        try {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.apply {
                try {
                    val context = getApplication<Application>().applicationContext
                    val uri = Uri.parse(song.audioUrl)
                    // For content URIs, we need to use a different approach
                    if (song.audioUrl.startsWith("content://")) {
                        Log.d("PlayerViewModel", "Handling content URI")
                        // Check if we have permission first
                        val contentResolver = context.contentResolver
                        val hasPermission = contentResolver.persistedUriPermissions.any {
                            it.uri.toString() == song.audioUrl && it.isReadPermission
                        }
                        if (!hasPermission) {
                            Log.e("PlayerViewModel", "No permission for URI: ${song.audioUrl}")
                            throw SecurityException("No permission for URI: ${song.audioUrl}")
                        }
                        // We have permission, try to open file descriptor
                        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                        parcelFileDescriptor?.use { pfd ->
                            setDataSource(pfd.fileDescriptor)
                        } ?: throw IOException("Could not open file descriptor")
                    }
                    // For file paths (not URIs)
                    else if (song.audioUrl.startsWith("/")) {
                        setDataSource(song.audioUrl)
                    }
                    // For file URIs (file://)
                    else if (song.audioUrl.startsWith("file://")) {
                        setDataSource(song.audioUrl)
                    }
                    // For online URLs
                    else if (song.audioUrl.startsWith("http://") || song.audioUrl.startsWith("https://")) {
                        setDataSource(song.audioUrl)
                    }
                    // Fallback for other cases
                    else {
                        setDataSource(song.audioUrl)
                    }
                    prepareAsync()
                    setOnPreparedListener {
                        _duration.value = it.duration
                        _currentPosition.value = 0
                        _isPlaying.value = true
                        it.start()
                        handler.post(updateProgressRunnable)
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("PlayerViewModel", "MediaPlayer error: what=$what, extra=$extra")
                        _isPlaying.value = false
                        true
                    }
                    setOnCompletionListener {
                        _isPlaying.value = false
                        handler.removeCallbacks(updateProgressRunnable)
                        playNext()
                    }
                } catch (e: SecurityException) {
                    Log.e("PlayerViewModel", "Security exception: ${e.message}")
                    showToast("Permission denied. Please grant storage permission.")
                    _isPlaying.value = false
                } catch (e: IOException) {
                    Log.e("PlayerViewModel", "IO exception: ${e.message}")
                    showToast("Could not play the audio file.")
                    _isPlaying.value = false
                } catch (e: IllegalArgumentException) {
                    Log.e("PlayerViewModel", "Illegal argument: ${e.message}")
                    showToast("Invalid audio file.")
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "MediaPlayer creation failed: ${e.message}")
            showToast("Failed to initialize media player.")
            _isPlaying.value = false
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                handler.removeCallbacks(updateProgressRunnable)
            } else {
                it.start()
                _isPlaying.value = true
                handler.post(updateProgressRunnable)
            }
        }
    }

    fun playNext() {
        if (allSongs.isEmpty()) return
        currentIndex = (currentIndex + 1) % allSongs.size
        val nextSong = allSongs[currentIndex]
        playSong(nextSong)
    }

    fun playPrevious() {
        if (allSongs.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) allSongs.lastIndex else currentIndex - 1
        val prevSong = allSongs[currentIndex]
        playSong(prevSong)
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _currentPosition.value = position
    }

    private fun stopCurrentSong() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        handler.removeCallbacks(updateProgressRunnable)
    }

    fun toggleFavorite() {
        _currentSong.value?.let { song ->
            val newLikedStatus = !_isLiked.value
            _isLiked.value = newLikedStatus

            viewModelScope.launch {
                try {
                    repository.toggleLiked(song.id.toLong(), newLikedStatus)
                    Log.d("PlayerViewModel", "Updated favorite status for song: ${song.title} to $newLikedStatus")

                    val updatedSongs = allSongs.toMutableList()
                    val index = updatedSongs.indexOfFirst { it.id == song.id }
                    if (index != -1) {
                        updatedSongs[index] = updatedSongs[index].copy(isLiked = newLikedStatus)
                        allSongs = updatedSongs
                        _currentSong.value = _currentSong.value?.copy(isLiked = newLikedStatus)
                    }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Error updating favorite status: ${e.message}")
                    _isLiked.value = !newLikedStatus
                }
            }
        } ?: run {
            Log.e("PlayerViewModel", "Cannot toggle favorite: No current song")
            showToast("No song is currently selected")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopCurrentSong()
    }
}