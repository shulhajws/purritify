package com.example.purrytify.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.purrytify.network.RetrofitClient
import com.example.purrytify.util.TokenManager
import retrofit2.HttpException

class TokenVerificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Log the current time for debugging
        Log.e("TokenWorker", "Running token verification at ${System.currentTimeMillis()}")

        val context = applicationContext
        val token = TokenManager.getToken(context)


        if (token.isNullOrEmpty()) {
            // No token available, consider logging out the user
            return Result.success()
        }

        try {
            // Call the verify-token endpoint
            val response = RetrofitClient.instance.verifyToken("Bearer $token").execute()

            if (!response.isSuccessful && response.code() == 403) {
                // Token expired, handle re-login or refresh token
                val refreshToken = TokenManager.getRefreshToken(context)
                if (!refreshToken.isNullOrEmpty()) {
                    refreshJwtToken(refreshToken, context)
                } else {
                    // Logout the user if no refresh token is available
                    TokenManager.clearToken(context)
                }
            }
        } catch (e: HttpException) {
            e.printStackTrace()
        }

        return Result.success()
    }

    private fun refreshJwtToken(refreshToken: String, context: Context) {
        try {
            val response = RetrofitClient.instance.refreshToken(mapOf("refreshToken" to refreshToken)).execute()
            if (response.isSuccessful) {
                val newAccessToken = response.body()?.accessToken
                if (!newAccessToken.isNullOrEmpty()) {
                    TokenManager.saveToken(context, newAccessToken)
                } else {
                    // Logout the user if refreshing the token fails
                    TokenManager.clearToken(context)
                }
            } else {
                // Logout the user if refreshing the token fails
                TokenManager.clearToken(context)
            }
        } catch (e: HttpException) {
            e.printStackTrace()
        }
    }
}