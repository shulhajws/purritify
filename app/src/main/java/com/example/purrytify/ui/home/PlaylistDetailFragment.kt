package com.example.purrytify.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.purrytify.model.Song
import com.example.purrytify.repository.RecommendationPlaylist
import com.example.purrytify.ui.playback.PlayerViewModel
import com.example.purrytify.ui.theme.PurrytifyTheme

class PlaylistDetailFragment : Fragment() {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PurrytifyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val playlist = arguments?.getParcelable<RecommendationPlaylist>("playlist")

                        if (playlist != null) {
                            PlaylistDetailScreen(
                                playlist = playlist,
                                onBackClick = { findNavController().navigateUp() },
                                onSongClick = { song ->
                                    playSong(song)
                                },
                                onPlayAllClick = {
                                    playAllSongs(playlist.songs)
                                },
                                onShuffleClick = {
                                    playAllSongsShuffled(playlist.songs)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun playSong(song: Song) {
        playerViewModel.playSongBySongModel(song)
        navigateToPlayback(song)
    }

    private fun playAllSongs(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            playerViewModel.clearQueue()
            songs.drop(1).forEach { song ->
                playerViewModel.addToQueue(song)
            }

            playerViewModel.playSongBySongModel(songs.first())
            navigateToPlayback(songs.first())
        }
    }

    private fun playAllSongsShuffled(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            val shuffledSongs = songs.shuffled()

            playerViewModel.clearQueue()
            shuffledSongs.drop(1).forEach { song ->
                playerViewModel.addToQueue(song)
            }

            playerViewModel.playSongBySongModel(shuffledSongs.first())
            navigateToPlayback(shuffledSongs.first())
        }
    }

    private fun navigateToPlayback(song: Song) {
        val bundle = Bundle().apply {
            putString("songId", song.id)
            putParcelable("song", song)
        }
        findNavController().navigate(
            com.example.purrytify.R.id.navigation_song_playback,
            bundle
        )
    }
}