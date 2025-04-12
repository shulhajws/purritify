package com.example.purrytify.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository
    private val _newSongs = MutableStateFlow<List<Song>>(emptyList())
    val newSongs: StateFlow<List<Song>> = _newSongs.asStateFlow()
    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = _recentlyPlayedSongs.asStateFlow()

    private var currentUserId: Int? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())
    }

    fun updateForUser(userId: Int) {
        if (currentUserId == userId) return
        currentUserId = userId

        viewModelScope.launch {
            repository.getNewSongsByUserId(userId).asFlow().collect { songEntities ->
                val songsList = SongMapper.toSongList(songEntities)
                _newSongs.value = songsList.take(5)
            }
        }

        viewModelScope.launch {
            repository.getRecentlyPlayedSongsByUserId(userId).asFlow().collect { songEntities ->
                val songsList = SongMapper.toSongList(songEntities)
                _recentlyPlayedSongs.value = songsList.take(5)
            }
        }
    }
}