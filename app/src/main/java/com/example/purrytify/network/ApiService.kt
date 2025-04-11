package com.example.purrytify.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val accessToken: String, val refreshToken: String)

interface ApiService {
    @POST("/api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}
