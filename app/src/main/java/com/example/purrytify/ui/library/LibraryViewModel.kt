package com.example.purrytify.ui.library

import androidx.lifecycle.ViewModel
import com.example.purrytify.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryViewModel : ViewModel() {
    // For all songs section
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // For liked songs section
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()

    // For tracking which tab is selected (0 for All, 1 for Liked)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadAllSongs()
        loadLikedSongs()
    }

    private fun loadAllSongs() {
        // In a real app, this would come from a repository or API
        _allSongs.value = listOf(
            Song("1", "Starboy", "The Weeknd, Daft Punk", "https://picsum.photos/200/200?random=1"),
            Song("2", "Here Comes The Sun - Remastered", "The Beatles", "https://picsum.photos/200/200?random=2"),
            Song("3", "Midnight Pretenders", "Tomoko Aran", "https://picsum.photos/200/200?random=3"),
            Song("4", "Violent Crimes", "Kanye West", "https://picsum.photos/200/200?random=4"),
            Song("5", "DENIAL IS A RIVER", "Doechii", "https://picsum.photos/200/200?random=5"),
            Song("6", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", "https://picsum.photos/200/200?random=6"),
            Song("7", "Violent Crimes", "Kanye West", "https://picsum.photos/200/200?random=4"),
            Song("8", "DENIAL IS A RIVER", "Doechii", "https://picsum.photos/200/200?random=5"),
            Song("10", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", "https://picsum.photos/200/200?random=6"),
            Song("9", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", "https://picsum.photos/200/200?random=1")
        )
    }

    private fun loadLikedSongs() {
        // In a real app, this would come from a repository or API
        // For now, just use a subset of all songs
        _likedSongs.value = listOf(
            Song("1", "Starboy", "The Weeknd, Daft Punk", "https://picsum.photos/200/200?random=1"),
            Song("3", "Midnight Pretenders", "Tomoko Aran", "https://picsum.photos/200/200?random=3"),
            Song("6", "Doomsday", "MF DOOM, Pebbles The Invisible Girl", "https://picsum.photos/200/200?random=6")
        )
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }
}