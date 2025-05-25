package com.example.purrytify.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.Song
import com.example.purrytify.repository.RecommendationPlaylist
import com.example.purrytify.ui.theme.SoftGray
import com.example.purrytify.ui.theme.SpotifyGreen
import com.example.purrytify.ui.theme.White

@Composable
fun PlaylistDetailScreen(
    playlist: RecommendationPlaylist,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SpotifyGreen.copy(alpha = 0.3f),
                        Color.Black
                    ),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        item {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            // Playlist Header
            PlaylistHeader(
                playlist = playlist,
                onPlayAllClick = onPlayAllClick,
                onShuffleClick = onShuffleClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Songs Header
            Text(
                text = "Songs",
                style = MaterialTheme.typography.titleMedium,
                color = White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Songs
        itemsIndexed(playlist.songs) { index, song ->
            PlaylistSongItem(
                song = song,
                index = index + 1,
                onSongClick = { onSongClick(song) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun PlaylistHeader(
    playlist: RecommendationPlaylist,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Playlist Artwork
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            when {
                playlist.songs.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        SpotifyGreen.copy(alpha = 0.5f),
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = White.copy(alpha = 0.5f),
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
                playlist.songs.size == 1 -> {
                    Image(
                        painter = rememberAsyncImagePainter(model = playlist.songs[0].albumArt),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = playlist.songs[0].albumArt),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(end = 1.dp, bottom = 1.dp)
                            )
                            if (playlist.songs.size > 1) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = playlist.songs[1].albumArt),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(start = 1.dp, bottom = 1.dp)
                                )
                            }
                        }
                        if (playlist.songs.size > 2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = playlist.songs[2].albumArt),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(end = 1.dp, top = 1.dp)
                                )
                                if (playlist.songs.size > 3) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = playlist.songs[3].albumArt),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(start = 1.dp, top = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Playlist Info
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.headlineMedium,
            color = White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = playlist.description,
            style = MaterialTheme.typography.bodyLarge,
            color = SoftGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${playlist.songs.size} songs",
            style = MaterialTheme.typography.bodyMedium,
            color = SoftGray.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = onShuffleClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Play All
            Button(
                onClick = onPlayAllClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Play All",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PlaylistSongItem(
    song: Song,
    index: Int,
    onSongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSongClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Number
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = SoftGray,
            modifier = Modifier.width(32.dp)
        )

        // Album Art
        Image(
            painter = rememberAsyncImagePainter(model = song.albumArt),
            contentDescription = "${song.title} album art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = SoftGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Server Indicator
        if (song.isFromServer) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = "From server",
                tint = SpotifyGreen.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}