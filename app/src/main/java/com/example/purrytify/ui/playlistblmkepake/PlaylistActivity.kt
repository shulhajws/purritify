//package com.example.purrytify.ui.playlist
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.viewModels
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.ui.Modifier
//import com.example.purrytify.model.Song
//import com.example.purrytify.ui.playback.PlayerViewModel
//import com.example.purrytify.ui.theme.PurrytifyTheme
//
//class PlaylistActivity : ComponentActivity() {
//    // Add variables initialization here as needed
//    private val playerViewModel: PlayerViewModel by viewModels()
//
//    // TODO: Harusnya pake navigateToPlayback yang di HomeFragment aja
////    private fun navigateToPlayback(songId: String) {
////        // Debugging log
////        Log.d("HomeViewModel", "Navigating to playback with songId: $songId")
////
//////        if (!::navController.isInitialized) {
//////            Log.e("HomeViewModel", "navController is not initialized!")
//////            return
//////        }
////
////        val bundle = Bundle().apply {
////            putString("songId", songId)
////        }
////        Log.d("HomeViewModel", "Success to init bundle")
////        try {
//////            navController = NavHostFragment.findNavController(this)
////            navController.navigate(R.id.navigation_song_playback, bundle)
////        } catch (e: Exception) {
////            Log.e("HomeViewModel", "Playback Navigation failed: ${e.message}", e)
////        }
////        Log.d("HomeViewModel", "Finally navigated to playback with songId: $songId")
////
////    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Retrieve playlistName and playlistSongs from intent extras
//        val playlistName = intent.getStringExtra("playlistName") ?: "Unknown Playlist"
//        val playlistSongs = intent.getParcelableArrayListExtra<Song>("playlistSongs") ?: arrayListOf()
//        // Retrieve method from HomeFragment for navigating to playback
//        val navigateToPlayback =
//
//        setContent {
//            PurrytifyTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    PlaylistScreen(
//                        playlistName = playlistName,
//                        songs = playlistSongs,
//                        onBackClick = { finish() },
//                        playerViewModel = playerViewModel,
//                        onNavigateToPlayback = { song ->
//                            playerViewModel.playSong(song)
//                            navigateToPlayback(song.id)
//                        },
//                        onAddToQueueClick = { /* TODO: Handle add to queue */ },
//                        onShareClick = { /* TODO: Handle share */ }
//                    )
//                }
//            }
//        }
//    }
//}