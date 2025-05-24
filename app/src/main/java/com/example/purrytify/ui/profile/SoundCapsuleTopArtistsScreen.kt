package com.example.purrytify.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.example.purrytify.data.dao.TopArtistData
import com.example.purrytify.repository.MonthYear
import com.example.purrytify.ui.theme.*

@Composable
fun TopArtistsSoundCapsuleScreen(
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
                text = "Top artists",
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
                    MonthSummaryHeader(
                        monthYear = monthYear,
                        analytics = state.analytics
                    )
                }

                state.analytics?.let { analytics ->
                    if (analytics.topArtists.isNotEmpty()) {
                        item {
                            Text(
                                text = "Your top artists this month:",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        itemsIndexed(analytics.topArtists) { index, artist ->
                            TopArtistItem(
                                rank = index + 1,
                                artist = artist,
                                viewModel = viewModel
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            ArtistInsightCard(analytics = analytics)
                        }
                    } else {
                        item {
                            NoArtistsDataCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSummaryHeader(
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
                    text = "You listened to ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.alignBy(FirstBaseline)
                )
                Text(
                    text = "${it.uniqueArtistsCount} artists",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF4FC3F7), // Light blue color like in the image
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
                text = "No artist data available for ${monthYear.getDisplayName()}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TopArtistItem(
    rank: Int,
    artist: TopArtistData,
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
                color = if (rank <= 3) Color(0xFF4FC3F7) else SpotifyGreen,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Artist Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4FC3F7),
                                Color(0xFF29B6F6),
                                Color(0xFF0277BD)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = artist.artist.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Artist Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = artist.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${artist.playCount} plays",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Text(
                        text = viewModel.formatDuration(artist.totalListenTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Play count indicator (visual element)
            if (rank <= 3) {
                Icon(
                    imageVector = when (rank) {
                        1 -> Icons.Default.Star
                        2 -> Icons.Default.Star
                        3 -> Icons.Default.Star
                        else -> Icons.Default.MusicNote
                    },
                    contentDescription = null,
                    tint = Color(0xFFFFD700), // Gold color for top 3
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ArtistInsightCard(analytics: com.example.purrytify.repository.MonthlyAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4FC3F7).copy(alpha = 0.1f)
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
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = Color(0xFF4FC3F7),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Artist Insights",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (analytics.topArtists.isNotEmpty()) {
                val topArtist = analytics.topArtists.first()
                val totalPlays = analytics.topArtists.sumOf { it.playCount }
                val topArtistPercentage = (topArtist.playCount.toFloat() / totalPlays * 100).toInt()

                Text(
                    text = "${topArtist.artist} was your most played artist with ${topArtist.playCount} plays (${topArtistPercentage}% of your total plays).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (analytics.uniqueArtistsCount > analytics.topArtists.size) {
                    Text(
                        text = "You also discovered ${analytics.uniqueArtistsCount - analytics.topArtists.size} other artists this month!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun NoArtistsDataCard() {
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
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No artist data available",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Start listening to music to see your top artists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}