package com.example.purrytify.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.purrytify.data.dao.TopSongData
import com.example.purrytify.repository.MonthYear
import com.example.purrytify.ui.theme.*

@Composable
fun TopSongsSoundCapsuleScreen(
    monthYear: MonthYear,
    viewModel: SoundCapsuleViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(monthYear) {
        viewModel.selectMonth(monthYear)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlack)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Top songs",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SpotifyGreen,
                    strokeWidth = 3.dp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Month Info and Summary
                    SongsSummaryHeader(
                        monthYear = monthYear,
                        analytics = state.analytics
                    )
                }

                state.analytics?.let { analytics ->
                    if (analytics.topSongs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Your most played songs:",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        itemsIndexed(analytics.topSongs) { index, song ->
                            TopSongItem(
                                rank = index + 1,
                                song = song,
                                viewModel = viewModel
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SongInsightCard(analytics = analytics)
                        }
                    } else {
                        item {
                            NoSongsDataCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongsSummaryHeader(
    monthYear: MonthYear,
    analytics: com.example.purrytify.repository.MonthlyAnalytics?
) {
    Column {
        Text(
            text = monthYear.getDisplayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        analytics?.let {
            Row {
                Text(
                    text = "You played ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.alignBy(FirstBaseline)
                )
                Text(
                    text = "${it.uniqueSongsCount} different songs",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFFFD700), // Golden color like in the image
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignBy(FirstBaseline)
                )
                Text(
                    text = " this month.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.alignBy(FirstBaseline)
                )
            }
        } ?: run {
            Text(
                text = "No song data available for ${monthYear.getDisplayName()}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TopSongItem(
    rank: Int,
    song: TopSongData,
    viewModel: SoundCapsuleViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = String.format("%02d", rank),
                style = MaterialTheme.typography.titleLarge,
                color = if (rank <= 3) Color(0xFFFFD700) else SpotifyGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Song Artwork Placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6B6B),
                                Color(0xFFEE5A24),
                                Color(0xFFFD7F28)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Song Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${song.playCount} plays",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Text(
                        text = viewModel.formatDuration(song.totalListenTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Special indicator for top songs
            if (rank <= 3) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (rank) {
                            1 -> Icons.Default.EmojiEvents
                            2 -> Icons.Default.Star
                            3 -> Icons.Default.Star
                            else -> Icons.Default.MusicNote
                        },
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )

                    if (rank == 1) {
                        Text(
                            text = "#1",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongInsightCard(analytics: com.example.purrytify.repository.MonthlyAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Song Insights",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (analytics.topSongs.isNotEmpty()) {
                val topSong = analytics.topSongs.first()
                val totalPlays = analytics.topSongs.sumOf { it.playCount }
                val topSongPercentage = (topSong.playCount.toFloat() / totalPlays * 100).toInt()

                Text(
                    text = "\"${topSong.title}\" by ${topSong.artist} was your most played song with ${topSong.playCount} plays.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Additional stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InsightStatItem(
                        label = "Total Songs",
                        value = "${analytics.uniqueSongsCount}",
                        modifier = Modifier.weight(1f)
                    )

                    InsightStatItem(
                        label = "Top Song %",
                        value = "${topSongPercentage}%",
                        modifier = Modifier.weight(1f)
                    )

                    InsightStatItem(
                        label = "Total Plays",
                        value = "${totalPlays}",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (analytics.uniqueSongsCount > analytics.topSongs.size) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "You explored ${analytics.uniqueSongsCount - analytics.topSongs.size} other songs beyond your top tracks!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NoSongsDataCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No song data available",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Start listening to music to see your top songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}