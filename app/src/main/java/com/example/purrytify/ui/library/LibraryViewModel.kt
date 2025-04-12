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
import com.example.purrytify.ui.shared.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private var currentUserId: Int? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())
    }

    fun updateForUser(userId: Int) {
        if (currentUserId == userId) return // Avoid unnecessary updates
        currentUserId = userId

        viewModelScope.launch {
            repository.getSongsByUserId(userId).asFlow().collect { songEntities ->
                _allSongs.value = SongMapper.toSongList(songEntities)
            }
        }

        viewModelScope.launch {
            repository.getLikedSongsByUserId(userId).asFlow().collect { songEntities ->
                _likedSongs.value = SongMapper.toSongList(songEntities)
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }
}
