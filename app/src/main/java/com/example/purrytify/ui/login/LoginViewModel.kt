package com.example.purrytify.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.purrytify.network.LoginRequest
import com.example.purrytify.network.LoginResponse
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.util.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginViewModel : ViewModel() {

    fun login(email: String, password: String, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val request = LoginRequest(email, password)
        RetrofitClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val accessToken = response.body()?.accessToken
                    if (accessToken != null) {
                        TokenManager.saveToken(context, accessToken)
                        onSuccess()
                    } else {
                        onError("Token is null")
                    }
                } else {
                    onError("Login failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("LoginViewModel", "Network error: ${t.message}")  // Log the error for debugging
                onError("Network error: ${t.message}")
            }
        })
    }
}