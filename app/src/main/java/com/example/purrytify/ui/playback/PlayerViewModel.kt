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
import com.example.purrytify.repository.AnalyticsRepository
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.util.AudioRouteManager
import com.example.purrytify.util.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

enum class RepeatMode {
    NO_REPEAT,
    REPEAT_ALL,
    REPEAT_ONE
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository
    private var allSongs: List<Song> = emptyList()
    private var currentIndex = 0
    private var currentUserId: Int? = null

    private val analyticsRepository: AnalyticsRepository
    private var currentSessionId: Long? = null
    private var sessionStartTime: Long = 0
    private var totalPausedDuration: Long = 0
    private var lastPauseTime: Long = 0
    private var wasPlayingBeforePause = false

    private val songQueue = mutableListOf<Song>()
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue
    private val pendingSongRequests = ArrayDeque<String>()

    private val _isShuffleOn = MutableStateFlow(false)
    val isShuffleOn: StateFlow<Boolean> = _isShuffleOn
    private var originalIndexes = mutableListOf<Int>()

    private val _repeatMode = MutableStateFlow(RepeatMode.NO_REPEAT)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

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


    private var audioRouteManager: AudioRouteManager? = null

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

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())
        analyticsRepository = AnalyticsRepository(database.analyticsDao(), database.songDao())
        _isLoading.value = true
        audioRouteManager = AudioRouteManager(application.applicationContext)

        // Song Deletion
        viewModelScope.launch {
            EventBus.songDeletedEvents.collect { songId ->
                Log.d("PlayerViewModel", "Song deletion event received for song ID: $songId")
                val currentSongId = _currentSong.value?.id
                Log.d("PlayerViewModel", "Current song ID: $currentSongId")

                if (currentSongId == songId.toString()) {
                    Log.d("PlayerViewModel", "Current song matches deleted song, stopping playback")
                    stopCurrentSong()
                    _currentSong.value = null
                }

                songQueue.removeAll { it.id == songId.toString() }
                _queue.value = songQueue.toList()
            }
        }

        // Song Update
        viewModelScope.launch {
            EventBus.songUpdatedEvents.collect { songId ->
                Log.d("PlayerViewModel", "Song update event received for song ID: $songId")

                val updatedSongEntity = repository.getSongById(songId)
                val updatedSong = updatedSongEntity?.let { SongMapper.toSong(it) } ?: return@collect
                val songIdString = songId.toString()

                if (_currentSong.value?.id == songIdString) {
                    _currentSong.value = updatedSong
                    _isLiked.value = updatedSong.isLiked
                }

                val queueIndex = songQueue.indexOfFirst { it.id == songIdString }
                if (queueIndex != -1) {
                    songQueue[queueIndex] = updatedSong
                    _queue.value = songQueue.toList()
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
            currentIndex = allSongs.indexOfFirst { it.id == song.id }
            Log.d("PlayerViewModel", "Found song: ${song.title}")

            if (_currentSong.value?.id == songId && _isPlaying.value) {
                Log.d("PlayerViewModel", "Same song is already playing, ignoring request.")
                return
            }

            if (_currentSong.value?.id != songId) {
                stopCurrentSong()
                _currentSong.value = song
            }

            clearQueue()

            playSong(song)
        } else {
            Log.e("PlayerViewModel", "Song with ID $songId not found")
        }
    }

    fun playSong(song: Song) {
        Log.d("PlayerViewModel", "playSong called for: ${song.title}")

        val currentSongId = _currentSong.value?.id
        if (currentSongId != null && currentSongId != song.id) {
            Log.d("PlayerViewModel", "Switching songs, ending previous session")
            endCurrentSession()
        }

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

                    setOnPreparedListener {
                        _duration.value = it.duration
                        _currentPosition.value = 0
                        _isPlaying.value = true
                        it.start()
                        handler.post(updateProgressRunnable)
                        startAnalyticsSession(song)
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("PlayerViewModel", "MediaPlayer error: what=$what, extra=$extra")
                        _isPlaying.value = false
                        true
                    }
                    setOnCompletionListener {
                        _isPlaying.value = false
                        handler.removeCallbacks(updateProgressRunnable)
                        handleSongCompletion()
                    }
                    prepareAsync()

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

    private fun handleSongCompletion() {
        Log.d("PlayerViewModel", "Song completed")

        currentSessionId?.let { sessionId ->
            viewModelScope.launch {
                try {
                    val totalSessionDuration = System.currentTimeMillis() - sessionStartTime
                    val actualListenTime = maxOf(0, totalSessionDuration - totalPausedDuration)

                    analyticsRepository.updateListeningSession(
                        sessionId = sessionId,
                        actualListenDurationMs = actualListenTime,
                        wasCompleted = true
                    )

                    Log.d("PlayerViewModel", "Song completed - ended analytics session: $sessionId, duration: ${actualListenTime}ms")

                    currentSessionId = null
                    sessionStartTime = 0
                    totalPausedDuration = 0
                    lastPauseTime = 0
                    wasPlayingBeforePause = false

                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Error ending completed session: ${e.message}")
                }
            }
        }

        when (_repeatMode.value) {
            RepeatMode.REPEAT_ONE -> {
                _currentSong.value?.let { playSong(it) }
                return
            }
            RepeatMode.REPEAT_ALL -> {
                if (songQueue.isEmpty() && currentIndex == allSongs.lastIndex) {
                    currentIndex = 0
                    if (allSongs.isNotEmpty()) {
                        playSong(allSongs[currentIndex])
                    }
                    return
                }
            }
            RepeatMode.NO_REPEAT -> {
                if (songQueue.isEmpty() && currentIndex == allSongs.lastIndex) {
                    stopCurrentSong()
                    return
                }
            }
        }

        if (songQueue.isNotEmpty()) {
            val nextSong = songQueue.removeAt(0)
            _queue.value = songQueue.toList()
            playSong(nextSong)
        } else {
            playNext()
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

                lastPauseTime = System.currentTimeMillis()
                wasPlayingBeforePause = true
                Log.d("PlayerViewModel", "Song paused, recording pause time")
            } else {
                it.start()
                _isPlaying.value = true
                handler.post(updateProgressRunnable)

                if (wasPlayingBeforePause && lastPauseTime > 0) {
                    totalPausedDuration += System.currentTimeMillis() - lastPauseTime
                    wasPlayingBeforePause = false
                }
                Log.d("PlayerViewModel", "Song resumed, pause duration: ${totalPausedDuration}ms")
            }
        }
    }

    fun playNext() {
        // Queue Exists
        if (songQueue.isNotEmpty()) {
            val nextSong = songQueue.removeAt(0)
            _queue.value = songQueue.toList()
            playSong(nextSong)
            return
        }

        // No Songs
        if (allSongs.isEmpty()) return

        // Shuffle On
        if (_isShuffleOn.value) {
            val originalIndex = allSongs.indexOfFirst { it.id == _currentSong.value?.id }
            val shuffledIndex = originalIndexes.indexOf(originalIndex)

            if (shuffledIndex < originalIndexes.size - 1) {
                // Not end of shuffled list
                val nextShuffledIndex = shuffledIndex + 1
                val nextOriginalIndex = originalIndexes[nextShuffledIndex]
                playSong(allSongs[nextOriginalIndex])
            } else {
                // End of shuffled list: reshuffle/stop
                if (_repeatMode.value == RepeatMode.REPEAT_ALL) {
                    shuffleIndexes()
                    if (originalIndexes.isNotEmpty()) {
                        val nextOriginalIndex = originalIndexes[0]
                        playSong(allSongs[nextOriginalIndex])
                    }
                } else {
                    stopCurrentSong()
                }
            }
            return
        }

        // Normal
        if (currentIndex < allSongs.size - 1) {
            currentIndex++
            playSong(allSongs[currentIndex])
        // Repeat All
        } else if (_repeatMode.value == RepeatMode.REPEAT_ALL) {
            currentIndex = 0
            playSong(allSongs[currentIndex])
        } else {
            stopCurrentSong()
        }
    }

    fun playPrevious() {
        if (allSongs.isEmpty()) return

        if (mediaPlayer != null && mediaPlayer!!.currentPosition > 3000) {
            mediaPlayer?.seekTo(0)
            _currentPosition.value = 0
            return
        }

        // Shuffle On
        if (_isShuffleOn.value) {
            val originalIndex = allSongs.indexOfFirst { it.id == _currentSong.value?.id }
            val shuffledIndex = originalIndexes.indexOf(originalIndex)

            if (shuffledIndex > 0) {
                // Not beginning of shuffle list
                val prevShuffledIndex = shuffledIndex - 1
                val prevOriginalIndex = originalIndexes[prevShuffledIndex]
                playSong(allSongs[prevOriginalIndex])
            } else {
                // Beginning of shuffled list: repeat/restart current song
                if (_repeatMode.value == RepeatMode.REPEAT_ALL) {
                    val lastShuffledIndex = originalIndexes.size - 1
                    val lastOriginalIndex = originalIndexes[lastShuffledIndex]
                    playSong(allSongs[lastOriginalIndex])
                } else {
                    mediaPlayer?.seekTo(0)
                    _currentPosition.value = 0
                }
            }
            return
        }

        // Normal
        if (currentIndex > 0) {
            currentIndex--
            playSong(allSongs[currentIndex])
        // Repeat All
        } else if (_repeatMode.value == RepeatMode.REPEAT_ALL) {
            currentIndex = allSongs.lastIndex
            playSong(allSongs[currentIndex])
        } else {
            mediaPlayer?.seekTo(0)
            _currentPosition.value = 0
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _currentPosition.value = position
    }

    private fun stopCurrentSong() {
        if (mediaPlayer != null && currentSessionId != null) {
            Log.d("PlayerViewModel", "Stopping current song, ending analytics session")
            endCurrentSession()
        }

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

    fun addToQueue(song: Song) {
        songQueue.add(song)
        _queue.value = songQueue.toList()
        showToast("Added to queue: ${song.title}")
    }

    fun addToQueueById(songId: String) {
        if (_isLoading.value) {
            Log.d("PlayerViewModel", "Cannot add to queue while songs are loading")
            showToast("Cannot add to queue right now")
            return
        }

        val song = allSongs.find { it.id == songId }
        if (song != null) {
            addToQueue(song)
        } else {
            Log.e("PlayerViewModel", "Song with ID $songId not found")
            showToast("Song not found")
        }
    }

    fun removeFromQueue(position: Int) {
        if (position in 0 until songQueue.size) {
            val removedSong = songQueue.removeAt(position)
            _queue.value = songQueue.toList()
            showToast("Removed from queue: ${removedSong.title}")
        }
    }

    fun clearQueue() {
        songQueue.clear()
        _queue.value = emptyList()
    }

    fun toggleShuffle() {
        val newShuffleState = !_isShuffleOn.value
        _isShuffleOn.value = newShuffleState

        if (newShuffleState) {
            shuffleIndexes()
        } else {
            originalIndexes.clear()
        }
    }

    private fun shuffleIndexes() {
        originalIndexes = List(allSongs.size) { it }.shuffled().toMutableList()
        Log.d("PlayerViewModel", "Shuffled indexes: $originalIndexes")
    }

    fun cycleRepeatMode() {
        val currentMode = _repeatMode.value
        val nextMode = when (currentMode) {
            RepeatMode.NO_REPEAT -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.NO_REPEAT
        }
        _repeatMode.value = nextMode
    }

    private fun startAnalyticsSession(song: Song) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                sessionStartTime = System.currentTimeMillis()
                totalPausedDuration = 0
                lastPauseTime = 0
                wasPlayingBeforePause = false

                currentSessionId = analyticsRepository.startListeningSession(
                    userId = userId,
                    songId = song.id.toLong()
                )

                Log.d("PlayerViewModel", "Started analytics session: $currentSessionId for song: ${song.title}")
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error starting analytics session: ${e.message}")
            }
        }
    }

    private fun endCurrentSession() {
        val sessionId = currentSessionId ?: return

        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val totalSessionDuration = currentTime - sessionStartTime

                // If currently paused, add the current pause duration
                val finalPausedDuration = if (lastPauseTime > 0 && wasPlayingBeforePause) {
                    totalPausedDuration + (currentTime - lastPauseTime)
                } else {
                    totalPausedDuration
                }

                // Calculate actual listen time (total session time minus paused time)
                val actualListenTime = maxOf(0, totalSessionDuration - finalPausedDuration)

                // Consider session completed if they listened to at least 30 seconds or 50% of song
                val songDuration = _duration.value.toLong()
                val completionThreshold = minOf(30000L, songDuration / 2) // 30 seconds or 50% of song
                val wasCompleted = actualListenTime >= completionThreshold

                analyticsRepository.updateListeningSession(
                    sessionId = sessionId,
                    actualListenDurationMs = actualListenTime,
                    wasCompleted = wasCompleted
                )

                Log.d("PlayerViewModel", "Ended analytics session: $sessionId, duration: ${actualListenTime}ms, completed: $wasCompleted")

                currentSessionId = null
                sessionStartTime = 0
                totalPausedDuration = 0
                lastPauseTime = 0
                wasPlayingBeforePause = false

            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error ending analytics session: ${e.message}")
            }
        }
    }

    fun getAnalyticsRepository(): AnalyticsRepository {
        return analyticsRepository
    }

    fun getAudioRouteManager(): AudioRouteManager? {
        return audioRouteManager
    }

    override fun onCleared() {
        super.onCleared()

        if (currentSessionId != null) {
            Log.d("PlayerViewModel", "ViewModel cleared, ending active session")
            endCurrentSession()
        }

        stopCurrentSong()
        audioRouteManager?.cleanup()
        audioRouteManager = null
    }

    fun forceEndCurrentSession() {
        if (currentSessionId != null) {
            Log.d("PlayerViewModel", "Force ending current session")
            endCurrentSession()
        }
    }
}