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
            Song("1", "Starboy", "The Weeknd2, Daft Punk", "https://picsum.photos/200/200?random=1", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
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
    }

    private fun loadLikedSongs() {
        // In a real app, this would come from a repository or API
        // For now, just use a subset of all songs
        _likedSongs.value = listOf(
            Song("1", "Starboy", "The Weeknd3, Daft Punk", "https://picsum.photos/200/200?random=1", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            Song("3", "Midnight Pretenders", "Tomoko Aran", "https://picsum.photos/200/200?random=3", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            Song("6", "Plastic Love", "Mariya Takeuchi", "https://picsum.photos/200/200?random=6", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3")
        )
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }
}