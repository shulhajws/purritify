package com.example.purrytify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.purrytify.databinding.ActivityMainBinding
import com.example.purrytify.ui.login.LoginActivity
import com.example.purrytify.ui.playback.MiniPlayer
import com.example.purrytify.ui.playback.PlayerViewModel
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
            // TODO: fetch user data here if needed

            // Call schedulerTokenVerification()
            scheduleTokenVerification(this)
        }

        // Register network callback
        NetworkUtil.registerNetworkCallback(this)

        // Check if the network is available
        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
        }

        // Setup navigation
        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_song_playback) {
                binding.miniPlayerContainer.visibility = View.GONE
            } else {
                if (playerViewModel.currentSong.value != null) {
                    binding.miniPlayerContainer.visibility = View.VISIBLE
                }
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

        // ComposeView setup
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