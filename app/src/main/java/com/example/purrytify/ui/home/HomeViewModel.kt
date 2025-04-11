package com.example.purrytify.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SongRepository
    private val _newSongs = MutableStateFlow<List<Song>>(emptyList())
    val newSongs: StateFlow<List<Song>> = _newSongs.asStateFlow()

    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = _recentlyPlayedSongs.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())

        viewModelScope.launch {
            repository.allSongs.asFlow().collect { songEntities ->
                val songs = SongMapper.toSongList(songEntities)

                // TODO: Logic for new and recently played songs
                val numSongSize = minOf(4, songs.size)
                _newSongs.value = songs.take(numSongSize)
                _recentlyPlayedSongs.value = songs.drop(numSongSize)
            }
        }
    }

    fun playSong(songId: String, callback: (String) -> Unit) {
        callback(songId)
    }
}