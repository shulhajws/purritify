package com.example.purrytify.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.RecommendationPlaylist
import com.example.purrytify.repository.RecommendationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecommendationState(
    val playlists: List<RecommendationPlaylist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class RecommendationViewModel(application: Application) : AndroidViewModel(application) {

    private val recommendationRepository: RecommendationRepository

    private val _state = MutableStateFlow(RecommendationState())
    val state: StateFlow<RecommendationState> = _state.asStateFlow()

    private var currentUserId: Int? = null
    private var userLocation: String? = null

    init {
        val database = AppDatabase.getDatabase(application)
        recommendationRepository = RecommendationRepository(database.songDao())
    }

    fun updateForUser(userId: Int, location: String) {
        if (currentUserId == userId && userLocation == location) return

        currentUserId = userId
        userLocation = location
        loadRecommendations()
    }

    fun refreshRecommendations() {
        if (currentUserId != null && userLocation != null) {
            loadRecommendations()
        }
    }

    private fun loadRecommendations() {
        val userId = currentUserId ?: return
        val location = userLocation ?: "ID"

        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val recommendations = recommendationRepository.generateRecommendations(userId, location)
                _state.value = _state.value.copy(
                    playlists = recommendations,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load recommendations: ${e.message}"
                )
            }
        }
    }
}