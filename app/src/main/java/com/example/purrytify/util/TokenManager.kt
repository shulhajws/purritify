package com.example.purrytify.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.purrytify.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TokenManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val TOKEN_KEY = "auth_token"
    private const val REFRESH_TOKEN_KEY = "refresh_token"
    private const val TOKEN_TIMESTAMP_KEY = "token_timestamp"

    // JWT token expiration time in milliseconds (5 minutes = 300,000 ms)
    private const val TOKEN_EXPIRATION_DURATION = 300_000L

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(context: Context, token: String) {
        val sharedPreferences = getEncryptedPrefs(context)
        sharedPreferences.edit()
            .putString(TOKEN_KEY, token)
            .putLong(TOKEN_TIMESTAMP_KEY, System.currentTimeMillis())
            .apply()
        Log.d("Token_Manager", "Token saved: $token")
    }

    fun getToken(context: Context): String? {
        return getEncryptedPrefs(context).getString(TOKEN_KEY, null)
    }

    fun saveRefreshToken(context: Context, refreshToken: String) {
        val sharedPreferences = getEncryptedPrefs(context)
        sharedPreferences.edit()
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .apply()
    }

    fun getRefreshToken(context: Context): String? {
        return getEncryptedPrefs(context).getString(REFRESH_TOKEN_KEY, null)
    }

    fun clearToken(context: Context) {
        val sharedPreferences = getEncryptedPrefs(context)
        sharedPreferences.edit()
            .remove(TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(TOKEN_TIMESTAMP_KEY)
            .apply()
    }

    fun isTokenExpired(context: Context): Boolean {
        val tokenExists = getToken(context) != null
        if (!tokenExists) return true

        val timestamp = getEncryptedPrefs(context).getLong(TOKEN_TIMESTAMP_KEY, 0)
        val currentTime = System.currentTimeMillis()

        return currentTime - timestamp > TOKEN_EXPIRATION_DURATION
    }

    /**
     * Verifies token with server and refreshes if necessary
     * Returns true if token is valid (or was refreshed successfully)
     * Returns false if token is invalid and couldn't be refreshed
     */
    suspend fun verifyAndRefreshTokenIfNeeded(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            // If token is expired based on timestamp, try to refresh
            if (isTokenExpired(context)) {
                Log.d("Token_Manager", "Token is expired, trying to refresh")
                val refreshToken = getRefreshToken(context)
                if (refreshToken != null) {
                    try {
                        val response = RetrofitClient.instance
                            .refreshToken(mapOf("refreshToken" to refreshToken))
                            .execute()

                        if (response.isSuccessful) {
                            val newAccessToken = response.body()?.accessToken
                            if (newAccessToken != null) {
                                saveToken(context, newAccessToken)
                                return@withContext true
                            }
                        } else {
                            // Refresh token request failed
                            clearToken(context)
                            return@withContext false
                        }
                    } catch (e: Exception) {
                        // Network error or exception
                        return@withContext false
                    }
                }
                // No refresh token, token is truly expired
                return@withContext false
            }

            // If not expired by timestamp, verify with server
            try {
                val token = getToken(context) ?: return@withContext false
                val response = RetrofitClient.instance.verifyToken("Bearer $token").execute()
                Log.d("Token_Manager", "response.message: ${response.message()}")

                return@withContext if (response.isSuccessful) {
                    true
                } else if (response.code() == 403) {
                    // Token is invalid according to server, try to refresh
                    val refreshToken = getRefreshToken(context)
                    if (refreshToken != null) {
                        try {
                            val refreshResponse = RetrofitClient.instance
                                .refreshToken(mapOf("refreshToken" to refreshToken))
                                .execute()

                            if (refreshResponse.isSuccessful) {
                                val newAccessToken = refreshResponse.body()?.accessToken
                                if (newAccessToken != null) {
                                    saveToken(context, newAccessToken)
                                    return@withContext true
                                }
                            }
                        } catch (e: Exception) {
                            // Refresh token request failed
                            clearToken(context)
                        }
                    }
                    // No refresh token or refresh failed
                    clearToken(context)
                    false
                } else {
                    // Other server error
                    false
                }
            } catch (e: Exception) {
                // Network error or exception
                return@withContext if (NetworkUtil.isNetworkAvailable(context)) {
                    // If network is available but error occurred, consider token invalid
                    false
                } else {
                    // If network is not available, keep token status unchanged
                    !isTokenExpired(context)
                }
            }
        }
    }
}