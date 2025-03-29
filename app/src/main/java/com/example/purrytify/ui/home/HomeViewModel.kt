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
            Song("1", "Starboy", "The Weeknd, Daft Punk", "https://placekitten.com/200/200"),
            Song("2", "Here Comes The Sun", "The Beatles", "https://placekitten.com/201/201"),
            Song("3", "Midnight Pretenders", "Tomoko Aran", "https://placekitten.com/202/202"),
            Song("4", "Violet", "Kaho", "https://placekitten.com/203/203")
        )
    }

    private fun loadRecentlyPlayedSongs() {
        // In a real app, this would come from a repository or API
        _recentlyPlayedSongs.value = listOf(
            Song("5", "Jazz is for ordinary people", "berlioz", "https://placekitten.com/204/204"),
            Song("6", "Loose", "Daniel Caesar", "https://placekitten.com/205/205"),
            Song("7", "Nights", "Frank Ocean", "https://placekitten.com/206/206"),
            Song("8", "Kiss of Life", "Sade", "https://placekitten.com/207/207"),
            Song("9", "BEST INTEREST", "Tyler, The Creator", "https://placekitten.com/208/208")
        )
    }

    fun playSong(songId: String, callback: (String) -> Unit) {
        // In a real app, this would trigger actual playback logic
        // For now, we'll just prepare for navigation to a placeholder page
        // Navigation itself is handled in the Fragment
        callback(songId)
    }
}