package com.example.purrytify.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object TokenManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val TOKEN_KEY = "auth_token"

    fun saveToken(context: Context, token: String) {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(context: Context): String? {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return sharedPreferences.getString(TOKEN_KEY, null)
    }

    fun clearToken(context: Context) {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        sharedPreferences.edit().remove(TOKEN_KEY).apply()
    }
}

// After successful login, save the token by calling
// TokenManager.saveToken(context, response.token)

// To retrieve the token later, use
// val token = TokenManager.getToken(context)