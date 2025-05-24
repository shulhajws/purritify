package com.example.purrytify.ui.home

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.data.mapper.SongMapper
import com.example.purrytify.model.Song
import com.example.purrytify.model.UserProfile
import com.example.purrytify.services.RetrofitClient
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.ui.shared.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(application: Application, private val sharedViewModel: SharedViewModel) : AndroidViewModel(application) {
    private val repository: SongRepository
    private val _newSongs = MutableStateFlow<List<Song>>(emptyList())
    val newSongs: StateFlow<List<Song>> = _newSongs.asStateFlow()
    private val _recentlyPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayedSongs: StateFlow<List<Song>> = _recentlyPlayedSongs.asStateFlow()

    private val _globalSongs = MutableStateFlow<List<Song>>(emptyList())
    val globalSongs: StateFlow<List<Song>> = _globalSongs.asStateFlow()

    private val _countrySongs = MutableStateFlow<List<Song>>(emptyList())
    val countrySongs: StateFlow<List<Song>> = _countrySongs.asStateFlow()

    private var currentUserId: Int? = null
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    private val userProfile: StateFlow<UserProfile?> = _userProfile

    init {
        Log.e("HomeViewModel", "Success to get here")

        val database = AppDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())

        // Observe globalUserProfile from SharedViewModel
        viewModelScope.launch {
            sharedViewModel.fetchUserProfile(application.applicationContext)
            sharedViewModel.globalUserProfile.collectLatest { profile ->
                _userProfile.value = profile
                if (profile != null) {
                    // Only fetch if userProfile already available
                    fetchCountrySongs(application.applicationContext, profile.location)
                }
                else {
                    Log.e("HomeViewModel", "User profile is null, cannot fetch country songs.")
                }
            }
        }

        Log.e("HomeViewModel", "Success to get the user profile")

        // Fetch global songs automatically
        fetchGlobalSongs(application.applicationContext)
        Log.e("HomeViewModel", "Success to fetch global songs")


        // Fetch country songs automatically
        Log.e("HomeViewModel", "Fetching country songs with user: ${userProfile.value}")
        userProfile.value?.let { fetchCountrySongs(application.applicationContext, it.location) }
        Log.e("HomeViewModel", "Success to fetch country songs")
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

    private fun fetchGlobalSongs(context: Context) {
        viewModelScope.launch {
            try {
                val globalSongsResponse = RetrofitClient.instance.getTopGlobalSongs()

                Log.e("HomeViewModel", "Here the globalSongsResponse: $globalSongsResponse")

                if (globalSongsResponse.isEmpty()) throw IllegalStateException("Global songs response is empty")
                _globalSongs.value = globalSongsResponse.map { response ->
                    Song(
                        id = response.id.toString(),
                        title = response.title,
                        artist = response.artist,
                        albumArt = response.artwork,
                        audioUrl = response.url,
                        isLiked = false, // Default value, update if needed
                        isListened = false, // Default value, update if needed
                        uploadedAt = null, // Update if needed
                        updatedAt = null, // Update if needed
                        lastPlayedAt = null, // Update if needed
                        rank = response.rank,
                        country = response.country,
                        isPlaying = false,
                        isFromServer = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HomeViewModel", "Failed to fetch global songs: ${e.message}", e)
                Toast.makeText(context, "Failed to fetch global songs", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchCountrySongs(context: Context, countryCode: String) {
        viewModelScope.launch {
            try {
                val countrySongsResponse = RetrofitClient.instance.getTopCountrySongs(countryCode)

                Log.e("HomeViewModel", "Here the countrySongsResponse: $countrySongsResponse")


                if (countrySongsResponse.isEmpty()) throw IllegalStateException("Country songs response is empty")
                _countrySongs.value = countrySongsResponse.map { response ->
                    Song(
                        id = response.id.toString(),
                        title = response.title,
                        artist = response.artist,
                        albumArt = response.artwork,
                        audioUrl = response.url,
                        isLiked = false, // Default value, update if needed
                        isListened = false, // Default value, update if needed
                        uploadedAt = null, // Update if needed
                        updatedAt = null, // Update if needed
                        lastPlayedAt = null, // Update if needed
                        rank = response.rank,
                        country = response.country,
                        isPlaying = false,
                        isFromServer = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HomeViewModel", "Failed to fetch country songs: ${e.message}")
                Toast.makeText(context, "Failed to fetch country songs", Toast.LENGTH_LONG).show()
            }
        }
    }
}