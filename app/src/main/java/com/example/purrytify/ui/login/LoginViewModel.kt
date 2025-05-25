package com.example.purrytify.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.purrytify.services.LoginRequest
import com.example.purrytify.services.LoginResponse
import com.example.purrytify.services.RetrofitClient
import com.example.purrytify.util.NetworkUtil
import com.example.purrytify.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class LoginViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun login(email: String, password: String, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Check internet connection first
        if (!NetworkUtil.isNetworkAvailable(context)) {
            onError("No internet connection. Please check your network and try again.")
            return
        }

        _isLoading.value = true
        val request = LoginRequest(email, password)

        RetrofitClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                _isLoading.value = false

                if (response.isSuccessful) {
                    val accessToken = response.body()?.accessToken
                    val refreshToken = response.body()?.refreshToken

                    if (accessToken != null) {
                        // Save both tokens
                        TokenManager.saveToken(context, accessToken)
                        refreshToken?.let {
                            TokenManager.saveRefreshToken(context, it)
                        }

                        onSuccess()
                    } else {
                        onError("Login failed: Server didn't return a valid token")
                    }
                } else {
                    when (response.code()) {
                        401 -> onError("Invalid email or password.\nPlease try again.")
                        403 -> onError("Your account is locked.\nPlease contact support.")
                        404 -> onError("Login service unavailable.\nPlease try again later.")
                        500, 502, 503, 504 -> onError("Server error.\nPlease try again later.")
                        else -> onError("Login failed: ${response.message()}")
                    }

                    Log.e("LoginViewModel", "Server error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                _isLoading.value = false

                val errorMessage = when (t) {
                    is IOException -> "Network error. Please check your connection and try again."
                    is HttpException -> "Server error. Please try again later."
                    else -> "Login failed: ${t.message}"
                }

                Log.e("LoginViewModel", "Network error: ${t.message}")
                onError(errorMessage)
            }
        })
    }
}