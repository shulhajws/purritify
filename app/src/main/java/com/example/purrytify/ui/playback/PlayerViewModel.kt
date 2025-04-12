//PlayerViewModel.kt
package com.example.purrytify.ui.playback

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.data.mapper.SongMapper
import com.example.purrytify.model.Song
import com.example.purrytify.repository.SongRepository
import kotlinx.coroutines.launch
import java.io.IOException

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository
    private var allSongs: List<Song> = emptyList()
    private var currentIndex = 0

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

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
                        handler.removeCallbacks(this) // stop updating if not playing
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

        viewModelScope.launch {
            repository.allSongs.asFlow().collect { songEntities ->
                allSongs = SongMapper.toSongList(songEntities)
                Log.d("PlayerViewModel", "Loaded ${allSongs.size} songs from database")
            }
        }
    }

    // Add this method to your PlayerViewModel class
    fun getSongById(songId: String): Song? {
        return allSongs.find { it.id == songId }
    }

    fun playSong(song: Song) {
        stopCurrentSong()
        _currentSong.value = song
        currentIndex = allSongs.indexOfFirst { it.id == song.id }
        Log.d("PlayerViewModel", "Playing song: ${song.title} from ${song.audioUrl}")

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
                        playNext() // Auto-play next song when current finishes
                    }
                } catch (e: SecurityException) {
                    Log.e("PlayerViewModel", "Security exception: ${e.message}")
                    showToast("Permission denied. Please grant storage permission.")
                    _isPlaying.value = false
                } catch (e: IOException) {
                    Log.e("PlayerViewModel", "IO exception: ${e.message}")
                    showToast("Could not play the audio file. Please try again.")
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

    fun playSongById(songId: String) {
        val song = allSongs.find { it.id == songId }
        if (song != null) {
            playSong(song)
        } else {
            Log.e("PlayerViewModel", "Song with ID $songId not found")
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

    fun stopCurrentSong() {
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

    override fun onCleared() {
        super.onCleared()
        stopCurrentSong()
    }
}