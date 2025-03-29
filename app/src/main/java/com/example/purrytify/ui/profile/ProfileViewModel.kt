package com.example.purrytify.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.purrytify.model.UserProfile

class ProfileViewModel : ViewModel() {
    // Mock user profile data
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Stats
    private val _songsCount = MutableStateFlow(0)
    val songsCount: StateFlow<Int> = _songsCount.asStateFlow()

    private val _likedCount = MutableStateFlow(0)
    val likedCount: StateFlow<Int> = _likedCount.asStateFlow()

    private val _listenedCount = MutableStateFlow(0)
    val listenedCount: StateFlow<Int> = _listenedCount.asStateFlow()

    init {
        // Load mock data
        loadMockProfile()
        loadMockStats()
    }

    private fun loadMockProfile() {
        // In a real app, this would fetch from API
        _userProfile.value = UserProfile(
            id = "1",
            username = "13522xxx",
            email = "user@example.com",
            profilePhoto = "https://picsum.photos/200/200?random=10",
            location = "Indonesia",
            createdAt = "2023-01-01",
            updatedAt = "2023-02-01"
        )
    }

    private fun loadMockStats() {
        _songsCount.value = 135
        _likedCount.value = 32
        _listenedCount.value = 50
    }
}

