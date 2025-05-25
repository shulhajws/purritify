package com.example.purrytify.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.Song
import com.example.purrytify.repository.PlaylistType
import com.example.purrytify.repository.RecommendationPlaylist
import com.example.purrytify.ui.theme.SoftGray
import com.example.purrytify.ui.theme.SpotifyGreen
import com.example.purrytify.ui.theme.White

@Composable
fun RecommendationSection(
    recommendations: List<RecommendationPlaylist>,
    isLoading: Boolean,
    error: String?,
    onPlaylistClick: (RecommendationPlaylist) -> Unit,
    onSongClick: (Song) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Made for You",
                color = White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Recommendations",
                    tint = White
                )
            }
        }

        when {
            isLoading -> {
                LoadingRecommendations()
            }
            error != null -> {
                ErrorRecommendations(
                    error = error,
                    onRetry = onRefresh
                )
            }
            recommendations.isEmpty() -> {
                EmptyRecommendations(onRefresh = onRefresh)
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(recommendations) { playlist ->
                        RecommendationPlaylistCard(
                            playlist = playlist,
                            onPlaylistClick = { onPlaylistClick(playlist) },
                            onSongClick = onSongClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationPlaylistCard(
    playlist: RecommendationPlaylist,
    onPlaylistClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .height(200.dp)
            .clickable { onPlaylistClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            PlaylistArtwork(
                songs = playlist.songs.take(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getPlaylistIcon(playlist.type),
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${playlist.songs.size} songs",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PlaylistArtwork(
    songs: List<Song>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SpotifyGreen.copy(alpha = 0.3f),
                        Color.Black.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        when (songs.size) {
            0 -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                )
            }
            1 -> {
                Image(
                    painter = rememberAsyncImagePainter(model = songs[0].albumArt),
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
                        if (songs.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = songs[0].albumArt),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(end = 1.dp, bottom = 1.dp)
                            )
                        }
                        if (songs.size > 1) {
                            Image(
                                painter = rememberAsyncImagePainter(model = songs[1].albumArt),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(start = 1.dp, bottom = 1.dp)
                            )
                        }
                    }
                    if (songs.size > 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = songs[2].albumArt),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(end = 1.dp, top = 1.dp)
                            )
                            if (songs.size > 3) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = songs[3].albumArt),
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
}

@Composable
fun LoadingRecommendations() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.height(220.dp)
    ) {
        items(3) {
            Card(
                modifier = Modifier
                    .width(180.dp)
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SpotifyGreen,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorRecommendations(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Failed to load recommendations",
                style = MaterialTheme.typography.titleMedium,
                color = White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
fun EmptyRecommendations(onRefresh: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = SoftGray,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No recommendations yet",
                style = MaterialTheme.typography.titleMedium,
                color = White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Listen to more songs to get personalized recommendations",
                style = MaterialTheme.typography.bodySmall,
                color = SoftGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen
                )
            ) {
                Text("Refresh")
            }
        }
    }
}

private fun getPlaylistIcon(type: PlaylistType): ImageVector {
    return when (type) {
        PlaylistType.DAILY_MIX -> Icons.Default.Today
        PlaylistType.LIKED_BASED -> Icons.Default.Favorite
        PlaylistType.ARTIST_BASED -> Icons.Default.Person
        PlaylistType.RECENTLY_PLAYED -> Icons.Default.History
    }
}