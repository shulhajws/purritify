package com.example.purrytify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
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

        // Check if user is logged in
        val token = TokenManager.getToken(this)
        if (token.isNullOrEmpty()) {
            // User is not logged in, redirect to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Close MainActivity
        } else {
            // User is logged in, proceed with MainActivity

            // Fetch user profile data
            sharedViewModel.fetchUserProfile(this)

            // Call schedulerTokenVerification()
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

        // Menyembunyikan mini player saat currentSong null
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