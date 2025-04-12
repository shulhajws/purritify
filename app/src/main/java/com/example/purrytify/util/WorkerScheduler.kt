package com.example.purrytify.util

import android.content.Context
import android.util.Log
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.purrytify.worker.TokenVerificationWorker
import java.util.concurrent.TimeUnit

fun scheduleTokenVerification(context: Context) {
    // Log for debugging
    Log.d("TokenScheduler", "Scheduling periodic work")

    val workRequest = PeriodicWorkRequestBuilder<TokenVerificationWorker>(
        15, TimeUnit.MINUTES // Use 15 minutes in release (Android production limit is >= 15)
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "TokenVerificationWork",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
