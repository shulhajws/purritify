package com.example.purrytify.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.ui.theme.DarkBlack
import com.example.purrytify.ui.theme.DarkGray
import com.example.purrytify.ui.theme.White
import com.example.purrytify.ui.theme.SoftGray
import com.example.purrytify.ui.theme.SpotifyGreen


@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val songsCount by viewModel.songsCount.collectAsState()
    val likedCount by viewModel.likedCount.collectAsState()
    val listenedCount by viewModel.listenedCount.collectAsState()

    val buttonColor = DarkGray
    val textColor = White
    val secondaryTextColor = SoftGray
    val topColor = Color(0xFF00667B)
    val bottomColor = DarkBlack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor),
                    startY = 0f,
                    endY = 1000f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            userProfile?.let { profile ->
                Box(
                    modifier = Modifier
                        .padding(top = 48.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = profile.profilePhoto),
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "✏️",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Username
                Text(
                    text = profile.username,
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )

                // Location
                Text(
                    text = profile.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Edit Profile Button
                Button(
                    onClick = { /* Handle edit profile click */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor
                    ),
                    shape = RoundedCornerShape(percent=50),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = White
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Songs Count
                    StatItem(
                        count = songsCount,
                        label = "SONGS"
                    )

                    // Liked Count
                    StatItem(
                        count = likedCount,
                        label = "LIKED"
                    )

                    // Listened Count
                    StatItem(
                        count = listenedCount,
                        label = "LISTENED"
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(count: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
    }
}