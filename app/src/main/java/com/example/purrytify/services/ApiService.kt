package com.example.purrytify.services

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

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
data class SongResponse(
    val id: Int,
    val title: String,
    val artist: String,
    val artwork: String,
    val url: String,
    val duration: String, // format "mm:ss"
    val country: String,  // can be "GLOBAL" or country code ISO 3166-1 alpha-2
    val rank: Int,
    val createdAt: String, // format ISO 8601
    val updatedAt: String  // format ISO 8601
)

interface ApiService {
    // TODO: Define your API endpoints here, add new endpoints as needed

    /**
     * Logs in a user by sending their credentials to the server.
     *
     * @param request The login request containing the user's email and password.
     * @return A `Call` object that can be used to execute the request asynchronously or synchronously.
     *         On success, the server responds with a `LoginResponse` containing the access and refresh tokens.
     *
     * Example usage:
     * ```
     * val loginRequest = LoginRequest(email = "user@example.com", password = "password123")
     * val call = apiService.login(loginRequest)
     * call.enqueue(object : Callback<LoginResponse> {
     *     override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
     *         if (response.isSuccessful) {
     *             val loginResponse = response.body()
     *             // Handle successful login
     *         } else {
     *             // Handle login failure
     *         }
     *     }
     *
     *     override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
     *         // Handle request failure
     *     }
     * })
     * ```
     */
    @POST("/api/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    /**
     * Fetches the user profile from the server.
     *
     * @param token The authorization token in the format "Bearer <token>".
     * @return A `UserProfileResponse` object containing the user's profile details such as ID, username, email,
     *         profile photo URL, location, and timestamps for creation and updates.
     *
     * This function is a suspend function and should be called from a coroutine or another suspend function.
     *
     * Example usage:
     * ```
     * val token = "Bearer your_access_token"
     * val userProfile = apiService.getProfile(token)
     * println("User ID: ${userProfile.id}")
     * ```
     */
    @GET("/api/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): UserProfileResponse

    /**
     * Verifies the validity of the provided authorization token.
     *
     * @param token The authorization token in the format "Bearer <token>".
     * @return A `Call` object that can be used to execute the request asynchronously or synchronously.
     *         On success, the server responds with a 200 status code if the token is valid.
     */
    @GET("/api/verify-token")
    fun verifyToken(
        @Header("Authorization") token: String
    ): Call<Void>

    /**
     * Refreshes the access token using the provided refresh token.
     *
     * @param request A map containing the refresh token, typically with the key "refreshToken".
     * @return A `Call` object that can be used to execute the request asynchronously or synchronously.
     *         On success, the server responds with a `LoginResponse` containing the new access and refresh tokens.
     */
    @POST("/api/refresh-token")
    fun refreshToken(
        @Body request: Map<String, String>
    ): Call<LoginResponse>

    /**
     * Retrieves the top 50 global songs from the server.
     *
     * @return A `Call` object that can be used to execute the request asynchronously or synchronously.
     *         On success, the server responds with a list of `Song` objects.
     */
    @GET("/api/top-songs/global")
    suspend  fun getTopGlobalSongs(): List<SongResponse>

    /**
     * Retrieves the top 10 songs for a specific country from the server.
     *
     * @param countryCode The ISO 3166-1 alpha-2 country code (e.g., "US" for the United States) should be only handle Indonesia (ID), Malaysia (MY), USA (US), UK (GB), Switzerland (CH), Germany (DE), and Brazil (BR).
     * @return A `Call` object that can be used to execute the request asynchronously or synchronously.
     *         On success, the server responds with a list of `Song` objects.
     */
    @GET("/api/top-songs/{country_code}")
    suspend fun getTopCountrySongs(
        @Path("country_code") countryCode: String
    ): List<SongResponse>

    /**
     * Updates user profile information.
     *
     * @param token The authorization token in the format "Bearer <token>".
     * @param location Optional location update as country code (ISO 3166-1 alpha-2).
     * @param profilePhoto Optional profile photo file to upload.
     * @return A `Call` object that can be used to execute the request asynchronously or synchronously.
     *         On success, the server responds with updated UserProfileResponse.
     */
    @Multipart
    @PATCH("/api/profile")
    fun updateProfile(
        @Header("Authorization") token: String,
        @Part("location") location: okhttp3.RequestBody? = null,
        @Part profilePhoto: okhttp3.MultipartBody.Part? = null
    ): Call<UserProfileResponse>

    /**
     * Retrieves a song by its unique ID.
     *
     * @param songId The unique identifier of the song to retrieve.
     * @return A `SongResponse` object containing details about the song, such as title, artist, artwork, URL, duration, country, rank, and timestamps.
     *
     * This function is a suspend function and should be called from a coroutine or another suspend function.
     *
     * Example usage:
     * ```
     * val song = apiService.getSongById("12345")
     * println("Song Title: ${song.title}")
     * ```
     */
    @GET("/api/songs/{song_id}")
    suspend fun getSongById(@Path("song_id") songId: String): SongResponse
}
