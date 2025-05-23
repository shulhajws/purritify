package com.example.purrytify.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val accessToken: String, val refreshToken: String)
data class UserProfileResponse(
    val id: String,
    val username: String,
    val email: String,
    val profilePhoto: String,
    val location: String,
    val createdAt: String,
    val updatedAt: String
)

interface ApiService {
    // TODO: Define your API endpoints here, add new endpoints as needed

    // Login API
    @POST("/api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // User Profile API
    @GET("/api/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): UserProfileResponse

    // Verify Token API
    @GET("/api/verify-token")
    fun verifyToken(
        @Header("Authorization") token: String
    ): Call<Void>

    // Refresh Token API
    @POST("/api/refresh-token")
    fun refreshToken(
        @Body request: Map<String, String>
    ): Call<LoginResponse>

}
