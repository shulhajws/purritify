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
            Song("1", "Starboy", "The Weeknd, Daft Punk", "https://picsum.photos/200/200?random=1"),
            Song("2", "Here Comes The Sun", "The Beatles", "https://picsum.photos/200/200?random=2"),
            Song("3", "Midnight Pretenders", "Tomoko Aran", "https://picsum.photos/200/200?random=3"),
            Song("4", "Violet", "Kaho", "https://picsum.photos/200/200?random=4")
        )
    }

    private fun loadRecentlyPlayedSongs() {
        // In a real app, this would come from a repository or API
        _recentlyPlayedSongs.value = listOf(
            Song("5", "Jazz is for ordinary people", "berlioz", "https://picsum.photos/200/200?random=5"),
            Song("6", "Loose", "Daniel Caesar", "https://picsum.photos/200/200?random=6"),
            Song("7", "Nights", "Frank Ocean", "https://picsum.photos/200/200?random=7"),
            Song("8", "Kiss of Life", "Sade", "https://picsum.photos/200/200?random=8"),
            Song("9", "BEST INTEREST", "Tyler, The Creator", "https://picsum.photos/200/200?random=9")
        )
    }

    fun playSong(songId: String, callback: (String) -> Unit) {
        // In a real app, this would trigger actual playback logic
        // For now, we'll just prepare for navigation to a placeholder page
        // Navigation itself is handled in the Fragment
        callback(songId)
    }
}