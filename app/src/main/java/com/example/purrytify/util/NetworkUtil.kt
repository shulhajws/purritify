package com.example.purrytify.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Code below are Example of call NetworkUtil to handle internet issue in ProfileFragment.kt
// 1. Check if the network is available in onViewCreated
// 2. If not available, show NoInternetScreen
// 3. Add internet issue handling also inside fetch data method

//override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//    super.onViewCreated(view, savedInstanceState)
//
//    binding.profileComposeView.setContent {
//        PurrytifyTheme {
//            Surface(
//                modifier = Modifier.fillMaxSize(),
//                color = MaterialTheme.colorScheme.background
//            ) {
//                if (!NetworkUtil.isNetworkAvailable(requireContext())) {
//                    NetworkUtil.NoInternetScreen()
//                } else {
//                    // Fetch user profile data if network is available
//                    profileViewModel.fetchUserProfile(requireContext())
//                    ProfileScreen(viewModel = profileViewModel)
//                }
//            }
//        }
//    }
//}

object NetworkUtil {
    private var isConnected = true

    fun registerNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (!isConnected) {
                    isConnected = true
                    Toast.makeText(context, "Connected to the internet", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                isConnected = false
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @Composable
    fun NoInternetScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No internet connection. Please check your connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }
    }
}