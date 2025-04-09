package com.example.purrytify.ui.playback

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ViewModel
import com.example.purrytify.model.Song

class PlayerViewModel : ViewModel() {

    // Dummy Data
    private val dummySongs = listOf(
        Song("1", "Starboy", "The Weeknd9, Daft Punk", "https://picsum.photos/200/200?random=1", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
        Song("2", "Here Comes The Sun", "The Beatles", "https://picsum.photos/200/200?random=2", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
        Song("3", "Midnight Pretenders", "Tomoko Aran", "https://picsum.photos/200/200?random=3", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
        Song("4", "Blinding Lights", "The Weeknd", "https://picsum.photos/200/200?random=4", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
        Song("5", "Eleanor Rigby", "The Beatles", "https://picsum.photos/200/200?random=5", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
        Song("6", "Plastic Love", "Mariya Takeuchi", "https://picsum.photos/200/200?random=6", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"),
        Song("7", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", "https://picsum.photos/200/200?random=7", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"),
        Song("8", "DENIAL IS A RIVER", "Doechii", "https://picsum.photos/200/200?random=8", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
        Song("9", "Violent Crimes", "Kanye West", "https://picsum.photos/200/200?random=9", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"),
        Song("10", "Some", "BOL4", "https://picsum.photos/200/200?random=10", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3")
    )
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
                _currentPosition.value=it.currentPosition
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun playSong(song: Song) {
        stopCurrentSong()

        _currentSong.value = song
        currentIndex = dummySongs.indexOfFirst { it.id == song.id }

        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.audioUrl)
            prepareAsync()
            setOnPreparedListener {
                _duration.value = it.duration
                _currentPosition.value = 0
                _isPlaying.value = true
                it.start()
                handler.post(updateProgressRunnable)
            }
        }
    }

    fun playSongById(songId: String) {
        val song = dummySongs.find { it.id == songId } ?: return
        playSong(song)
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
        currentIndex = (currentIndex + 1) % dummySongs.size
        val nextSong = dummySongs[currentIndex]
        playSong(nextSong)
    }

    fun playPrevious() {
        currentIndex = if (currentIndex - 1 < 0) dummySongs.lastIndex else currentIndex - 1
        val prevSong = dummySongs[currentIndex]
        playSong(prevSong)
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _currentPosition.value = position
    }

    fun stopCurrentSong() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        handler.removeCallbacks(updateProgressRunnable)
    }

    override fun onCleared() {
        super.onCleared()
        stopCurrentSong()
    }
}
