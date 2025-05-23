package com.example.purrytify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.purrytify.databinding.ActivityMainBinding
import com.example.purrytify.ui.login.LoginActivity
import com.example.purrytify.ui.playback.MiniPlayer
import com.example.purrytify.ui.playback.PlayerViewModel
import com.example.purrytify.ui.shared.SharedViewModel
import com.example.purrytify.util.NetworkUtil
import com.example.purrytify.util.TokenManager
import com.example.purrytify.util.scheduleTokenVerification
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val playerViewModel: PlayerViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Starting token validation check")
                val isValid = checkTokenValidity()
                Log.d("MainActivity", "Token validation result: $isValid")

                if (!isValid) {
                    // User is not logged in or token is invalid, redirect to LoginActivity
                    Log.e("MainActivity", "User is not logged in or token is invalid")
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                    return@launch
                }

                Log.d("MainActivity", "Token valid, initializing app")
                initializeApp()
            } catch (e: Exception) {
                Log.e("MainActivity", "Exception during token validation: ${e.message}", e)
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun checkTokenValidity(): Boolean {
        val token = TokenManager.getToken(this)
        Log.d("MainActivity", "Token: $token")
        if (token.isNullOrEmpty()) {
            Log.e("MainActivity", "Token is null or empty")
            return false
        }

        // Then verify/refresh token with server
        return TokenManager.verifyAndRefreshTokenIfNeeded(this)
    }

    companion object {
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 101
    }

    private fun checkBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            val hasPermissions = permissions.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }

            if (!hasPermissions) {
                ActivityCompat.requestPermissions(this, permissions, BLUETOOTH_PERMISSION_REQUEST_CODE)
                return false
            }
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                playerViewModel.getAudioRouteManager()?.updateDeviceList()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permissions are required to connect to Bluetooth audio devices",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun initializeApp() {
        // Fetch user profile data
        sharedViewModel.fetchUserProfile(this)

        // Call scheduleTokenVerification() for periodic checks
        scheduleTokenVerification(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.globalUserProfile.collect { userProfile ->
                    userProfile?.let { profile ->
                        try {
                            val userId = profile.id.toInt()
                            playerViewModel.updateForUser(userId)
                        } catch (e: NumberFormatException) {
                            Log.e("MainActivity", "Invalid user ID format: ${profile.id}")
                        }
                    }
                }
            }
        }

        // Register network callback
        NetworkUtil.registerNetworkCallback(this)

        // Check if the network is available
        if (!NetworkUtil.isNetworkAvailable(this)) {
            Log.d("MainActivity", "No internet connection")
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
        }

        // Setup navigation
        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showMiniPlayer = destination.id == R.id.navigation_home ||
                    destination.id == R.id.navigation_library ||
                    destination.id == R.id.navigation_profile

            // Update mini player visibility based on navigation and song state
            binding.miniPlayerContainer.visibility = if (showMiniPlayer &&
                playerViewModel.currentSong.value != null) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_library,
                R.id.navigation_profile
            )
        )
        navView.setupWithNavController(navController)

        checkBluetoothPermissions()

        val composeView = ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        binding.miniPlayerContainer.addView(composeView)

        composeView.setContent {
            MiniPlayerComposable(playerViewModel, navController)
        }

        // Hide mini player when currentSong is null
        lifecycleScope.launch {
            playerViewModel.currentSong.collectLatest { song ->
                val isInPlayerScreen = navController.currentDestination?.id == R.id.navigation_song_playback
                binding.miniPlayerContainer.visibility =
                    if (song != null && !isInPlayerScreen) View.VISIBLE else View.GONE
            }
        }
    }
}

@Composable
fun MiniPlayerComposable(playerViewModel: PlayerViewModel, navController: NavController) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    MaterialTheme {
        Surface(color = Color.Transparent) {
            currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    onMiniPlayerClick = {
                        if (navController.currentDestination?.id != R.id.navigation_song_playback) {
                            navController.navigate(
                                R.id.navigation_song_playback,
                                bundleOf("songId" to song.id)
                            )
                        }
                    },
                    onPlayPauseClick = {
                        playerViewModel.togglePlayPause()
                    }
                )
            }
        }
    }
}