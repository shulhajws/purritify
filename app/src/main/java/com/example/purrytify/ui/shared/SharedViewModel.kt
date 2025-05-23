package com.example.purrytify.ui.shared

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.model.UserProfile
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Look at ProfileFragment for example of getting globalUserProfile

class SharedViewModel : ViewModel() {
    // TODO: Add other variable if globally needed
    private val _globalUserProfile = MutableStateFlow<UserProfile?>(null)
    val globalUserProfile: StateFlow<UserProfile?> = _globalUserProfile

    fun fetchUserProfile(context: Context) {
        val token = TokenManager.getToken(context)
        if (token.isNullOrEmpty()) return

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getProfile("Bearer $token")
                _globalUserProfile.value = UserProfile(
                    id = response.id,
                    username = response.username,
                    email = response.email,
                    profilePhoto = "${RetrofitClient.getBaseUrl()}/uploads/profile-picture/${response.profilePhoto}",
                    location = response.location,
                    createdAt = response.createdAt,
                    updatedAt = response.updatedAt
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SharedViewModel", "Error fetching user profile: ${e.message}")
                Toast.makeText(context, "Failed to fetch user profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearGlobalUserProfile() {
        _globalUserProfile.value = null
    }
}