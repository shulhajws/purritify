package com.example.purrytify.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.Song
import com.example.purrytify.ui.theme.SoftGray
import com.example.purrytify.ui.theme.SpotifyGreen
import com.example.purrytify.ui.theme.White

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    downloadViewModel: com.example.purrytify.ui.download.DownloadViewModel,
    onSongClick: (Song) -> Unit
) {
    val newSongs by viewModel.newSongs.collectAsState()
    val recentlyPlayedSongs by viewModel.recentlyPlayedSongs.collectAsState()
    val globalSongs by viewModel.globalSongs.collectAsState()
    val countrySongs by viewModel.countrySongs.collectAsState()
    val downloadState by downloadViewModel.downloadState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Global Section with Download All button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top 50 Global",
                    color = White,
                    style = MaterialTheme.typography.headlineMedium,
                )

                // Download All Global button
                IconButton(
                    onClick = {
                        if (!downloadState.isBulkDownloading) {
                            downloadViewModel.downloadSongs(globalSongs)
                        }
                    },
                    enabled = globalSongs.isNotEmpty() && !downloadState.isBulkDownloading
                ) {
                    if (downloadState.isBulkDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SpotifyGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download All Global Songs",
                            tint = White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (globalSongs.isEmpty()) {
                EmptyStateMessage(message = "No global songs")
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    items(globalSongs) { song ->
                        NewSongItem(
                            song = song,
                            downloadViewModel = downloadViewModel,
                            downloadState = downloadState,
                            onSongClick = { onSongClick(song) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Top Country Section with Download All button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top 10 Country",
                    color = White,
                    style = MaterialTheme.typography.headlineMedium,
                )

                // Download All Country button
                IconButton(
                    onClick = {
                        if (!downloadState.isBulkDownloading) {
                            downloadViewModel.downloadSongs(countrySongs)
                        }
                    },
                    enabled = countrySongs.isNotEmpty() && !downloadState.isBulkDownloading
                ) {
                    if (downloadState.isBulkDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = SpotifyGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download All Country Songs",
                            tint = White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (countrySongs.isEmpty()) {
                EmptyStateMessage(message = "No country songs")
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    items(countrySongs) { song ->
                        NewSongItem(
                            song = song,
                            downloadViewModel = downloadViewModel,
                            downloadState = downloadState,
                            onSongClick = { onSongClick(song) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // New Songs Section (local songs, no download needed)
            Text(
                text = "New songs",
                color = White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (newSongs.isEmpty()) {
                EmptyStateMessage(message = "No new songs")
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    items(newSongs) { song ->
                        NewSongItem(
                            song = song,
                            downloadViewModel = null, // No download for local songs
                            downloadState = downloadState,
                            onSongClick = { onSongClick(song) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recently Played Section (local songs, no download needed)
            Text(
                text = "Recently played",
                color = White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (recentlyPlayedSongs.isEmpty()) {
                EmptyStateMessage(message = "No recently played songs")
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    recentlyPlayedSongs.forEach { song ->
                        RecentlyPlayedItem(
                            song = song,
                            onSongClick = { onSongClick(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = SoftGray,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun NewSongItem(
    song: Song,
    downloadViewModel: com.example.purrytify.ui.download.DownloadViewModel?,
    downloadState: com.example.purrytify.ui.download.DownloadState,
    onSongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onSongClick)
    ) {
        Box(
            modifier = Modifier.size(120.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = song.albumArt),
                contentDescription = "${song.title} album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Download button overlay for server songs
            if (downloadViewModel != null && song.isFromServer) {
                val isDownloading = downloadState.downloadProgress[song.id]?.isDownloading == true
                val isCompleted = downloadState.downloadProgress[song.id]?.isCompleted == true

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (!isDownloading && !isCompleted) {
                                downloadViewModel.downloadSong(song)
                            }
                        },
                        enabled = !isDownloading && !isCompleted,
                        modifier = Modifier.size(32.dp)
                    ) {
                        when {
                            isDownloading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = SpotifyGreen,
                                    strokeWidth = 2.dp
                                )
                            }
                            isCompleted -> {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Downloaded",
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Song",
                                    tint = White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            color = White,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.artist,
            style = MaterialTheme.typography.labelMedium,
            color = SoftGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RecentlyPlayedItem(song: Song, onSongClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSongClick)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = song.albumArt),
            contentDescription = "${song.title} album art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = White,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = SoftGray,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}