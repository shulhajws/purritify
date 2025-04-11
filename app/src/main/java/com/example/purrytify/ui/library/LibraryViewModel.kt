package com.example.purrytify.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.data.mapper.SongMapper
import com.example.purrytify.model.Song
import com.example.purrytify.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository

    // For all songs section
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // For liked songs section (in a real app, you might have a separate table for this)
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()

    // For tracking which tab is selected (0 for All, 1 for Liked)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())

        // Load songs from repository
        viewModelScope.launch {
            repository.allSongs.asFlow().collect { songEntities ->
                val songs = SongMapper.toSongList(songEntities)
                _allSongs.value = songs

                // TODO: Logic for liked songs
                _likedSongs.value = songs.filter { it.id.toLong() % 3 == 0L }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }
}