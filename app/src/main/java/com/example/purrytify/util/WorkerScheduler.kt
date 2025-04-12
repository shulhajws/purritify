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
    Log.e("TokenScheduler", "Scheduling periodic work")

    val workRequest = PeriodicWorkRequestBuilder<TokenVerificationWorker>(
        5, TimeUnit.MINUTES // TODO: gunakan 15 menit di release (batas android prod 15), 5 menit hanya untuk debug
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "TokenVerificationWork",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
