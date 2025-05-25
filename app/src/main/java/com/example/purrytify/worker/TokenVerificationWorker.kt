package com.example.purrytify.worker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.purrytify.services.RetrofitClient
import com.example.purrytify.util.TokenManager
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TokenVerificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Log the current time for debugging (Format waktu menjadi string yang lebih mudah dibaca
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(Date(System.currentTimeMillis()))
        Log.d("TokenWorker", "Running token verification at $currentTime")

        val context = applicationContext
        val token = TokenManager.getToken(context)

        // Log the token for debugging
        Log.d("TokenWorker", "Debug current token: $token")

        if (token.isNullOrEmpty()) {
            // No token available, consider logging out the user
            TokenManager.clearToken(context)
            // Show toast message
            Toast.makeText(context, "Your session has ended. Please log in again.", Toast.LENGTH_SHORT).show()
            return Result.success()
        }

        try {
            // Call the verify-token endpoint
            val response = RetrofitClient.instance.verifyToken("Bearer $token").execute()

            if (!response.isSuccessful && response.code() == 403) {
                // Token expired, handle re-login or refresh token
//                val refreshToken = TokenManager.getRefreshToken(context)
//                if (!refreshToken.isNullOrEmpty()) {
//                    refreshJwtToken(refreshToken, context)
//                    Log.d("TokenWorker", "Token refreshed successfully")
//
//                    // TODO: Re-fetch the user profile or any other necessary data (?)
//                } else {
                    // Logout the user if no refresh token is available
                    TokenManager.clearToken(context)

                    // Show toast message
                    Toast.makeText(context, "Session is no longer valid. Please log in again.", Toast.LENGTH_SHORT).show()
//                }
            }
        } catch (e: HttpException) {
            e.printStackTrace()
            Toast.makeText(context, "Oops! Something went wrong while checking your session. Please try again later.", Toast.LENGTH_SHORT).show()
            Log.e("TokenWorker", "Error verifying token: ${e.message()}")
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
            Toast.makeText(context, "Couldn’t refresh your session. Please sign in again.", Toast.LENGTH_SHORT).show()
            Log.e("TokenWorker", "Error refreshing token: ${e.message()}")
        }
    }
}