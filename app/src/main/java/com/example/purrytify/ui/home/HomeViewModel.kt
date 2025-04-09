package com.example.purrytify.ui.home

import androidx.lifecycle.ViewModel
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {

    // For new songs section
    private val _newSongs = MutableStateFlow<List<Song>>(emptyList())
    val newSongs: StateFlow<List<Song>> = _newSongs.asStateFlow()

    // For recently played songs section
    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = _recentlyPlayedSongs.asStateFlow()

    init {
        // Load sample data when ViewModel is created
        loadNewSongs()
        loadRecentlyPlayedSongs()
    }

    private fun loadNewSongs() {
        // In a real app, this would come from a repository or API
        _newSongs.value = listOf(
            Song("1", "Starboy", "The Weeknd1, Daft Punk", "https://picsum.photos/200/200?random=1", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            Song("2", "Here Comes The Sun", "The Beatles", "https://picsum.photos/200/200?random=2", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
            Song("3", "Midnight Pretenders", "Tomoko Aran", "https://picsum.photos/200/200?random=3", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            Song("4", "Blinding Lights", "The Weeknd", "https://picsum.photos/200/200?random=4", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3")
        )
    }

    private fun loadRecentlyPlayedSongs() {
        // In a real app, this would come from a repository or API
        _recentlyPlayedSongs.value = listOf(
            Song("5", "Eleanor Rigby", "The Beatles", "https://picsum.photos/200/200?random=5", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"),
            Song("6", "Plastic Love", "Mariya Takeuchi", "https://picsum.photos/200/200?random=6", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3"),
            Song("7", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", "https://picsum.photos/200/200?random=7", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3"),
            Song("8", "DENIAL IS A RIVER", "Doechii", "https://picsum.photos/200/200?random=8", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
            Song("9", "Violent Crimes", "Kanye West", "https://picsum.photos/200/200?random=9", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3"),
            Song("10", "Some", "BOL4", "https://picsum.photos/200/200?random=10", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3")
        )
    }

    fun playSong(songId: String, callback: (String) -> Unit) {
        // In a real app, this would trigger actual playback logic
        // For now, we'll just prepare for navigation to a placeholder page
        // Navigation itself is handled in the Fragment
        callback(songId)
    }
}