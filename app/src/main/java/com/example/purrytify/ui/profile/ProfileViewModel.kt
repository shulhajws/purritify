package com.example.purrytify.ui.profile

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.UserProfile
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.util.NetworkUtil
import com.example.purrytify.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    // Repository
    private val repository: SongRepository

    // Stats
    private val _songsCount = MutableStateFlow(0)
    val songsCount: StateFlow<Int> = _songsCount.asStateFlow()

    private val _likedCount = MutableStateFlow(0)
    val likedCount: StateFlow<Int> = _likedCount.asStateFlow()

    private val _listenedCount = MutableStateFlow(0)
    val listenedCount: StateFlow<Int> = _listenedCount.asStateFlow()

    // Current user ID being viewed
    private var currentUserId: Int? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SongRepository(database.songDao())

        // Initialize with default values
        _songsCount.value = 0
        _likedCount.value = 0
        _listenedCount.value = 0
    }

    fun updateForUser(userId: Int) {
        if (currentUserId == userId) return
        currentUserId = userId

        // Start collecting song stats
        collectSongStats(userId)
    }

    private fun collectSongStats(userId: Int) {
        // Collect songs count
        viewModelScope.launch {
            repository.getSongsCountByUserId(userId).collect { count ->
                _songsCount.value = count
            }
        }

        // Collect liked songs count
        viewModelScope.launch {
            repository.getLikedSongsCountByUserId(userId).collect { count ->
                _likedCount.value = count
            }
        }

        // Collect listened songs count
        viewModelScope.launch {
            repository.getListenedSongsCountByUserId(userId).collect { count ->
                _listenedCount.value = count
            }
        }
    }
}

//    fun fetchUserProfile(context: Context) {
//        // Prevent server request if no internet
//        if (!NetworkUtil.isNetworkAvailable(context)) {
//            // Handle no internet case
//            Log.d("ProfileViewModel", "No internet connection")
//            return
//        }
//
//        val token = TokenManager.getToken(context)
//        Log.d("ProfileViewModel", "Fetching user profile with token: $token")
//        if (token.isNullOrEmpty()) return
//
//        viewModelScope.launch {
//            try {
//                val response = RetrofitClient.instance.getProfile("Bearer $token")
//                _userProfile.value = UserProfile(
//                    id = response.id,
//                    username = response.username,
//                    email = response.email,
//                    profilePhoto = "${RetrofitClient.getBaseUrl()}/uploads/profile-picture/${response.profilePhoto}",
//                    location = response.location,
//                    createdAt = response.createdAt,
//                    updatedAt = response.updatedAt
//                )
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }


