package com.example.purrytify.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.purrytify.model.Song
import com.example.purrytify.ui.playback.PlayerViewModel
import com.example.purrytify.ui.songitem.SongItem
import com.example.purrytify.ui.theme.White

@Composable
fun PlaylistScreen(
    playlistName: String,
    songs: ArrayList<Song>,
    onBackClick: () -> Unit,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayback: (String) -> Unit,
    onAddToQueueClick: () -> Unit,
    onShareClick: () -> Unit
) {
    // Copied from profile screen colors
    val textColor = White
//    val secondaryTextColor = SoftGray
//    val topColor = Color(0xFF00667B)
//    val bottomColor = DarkBlack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Back button
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back Button",
                        tint = textColor
                    )
                }

                // Playlist name
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Below are the main content of the screen
            PlaylistContent(
                songs = songs,
                playerViewModel = playerViewModel,
                onNavigateToPlayback = onNavigateToPlayback,
                onAddToQueueClick = onAddToQueueClick,
                onShareClick = onShareClick
            )
        }
    }
}

@Composable
fun PlaylistContent(
    songs: ArrayList<Song>,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayback: (String) -> Unit,
    onAddToQueueClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row layout for display number of songs and play all button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Display the number of songs in the playlist
            Text(
                text = "${songs.size} songs",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Button to play all of the songs in the playlist (put all of the songs in the queue)
            IconButton(onClick = {
                /* TODO: Here, Put your code to Handle play all of the songs */
            }) {
                Icon(
                    // TODO: decide whether handle play and pause or not (for now only play)
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play All",
                    tint = Color.White,
                )
            }
        }

        // Display list of songs
        songs.forEach { song ->
            SongItem(
                song = song,
                playerViewModel = playerViewModel,
                onNavigateToPlayback = onNavigateToPlayback,
                onAddToQueueClick = onAddToQueueClick,
                onShareClick = onShareClick,
            )
        }
    }
}