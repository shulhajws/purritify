package com.example.purrytify.ui.playback

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
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
                _currentPosition.value = it.currentPosition
                handler.postDelayed(this, 1000)
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

    fun playSong(song: Song) {
        stopCurrentSong()
        _currentSong.value = song
        currentIndex = allSongs.indexOfFirst { it.id == song.id }

        Log.d("PlayerViewModel", "Playing song: ${song.title} from ${song.audioUrl}")

        try {
            mediaPlayer = MediaPlayer().apply {
                try {
                    // For local files stored on the device
                    if (song.audioUrl.startsWith("content://") || song.audioUrl.startsWith("file://")) {
                        Log.d("PlayerViewModel", "Trying to play song from URL: ${song.audioUrl}")
                        setDataSource(getApplication<Application>().applicationContext, Uri.parse(song.audioUrl))
                    } else {
                        // Use Dummy Music for Failed Cases
                        Log.d("PlayerViewModel", "Failed to get song URI: ${song.audioUrl}")
                        setDataSource("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                    }

                    prepareAsync()
                    setOnPreparedListener {
                        _duration.value = it.duration
                        _currentPosition.value = 0
                        _isPlaying.value = true
                        it.start()
                        handler.post(updateProgressRunnable)
                    }
                } catch (securityException: SecurityException) {
                    Log.e("PlayerViewModel", "Permission denied: ${securityException.message}")
                    // Here you could notify the UI that permissions are needed
                } catch (e: IOException) {
                    Log.e("PlayerViewModel", "Error setting data source: ${e.message}")
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("PlayerViewModel", "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error playing song", e)
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
