package com.example.purrytify.ui.profile

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.UserProfile
import com.example.purrytify.repository.MonthYear
import com.example.purrytify.ui.login.LoginActivity
import com.example.purrytify.ui.theme.DarkBlack
import com.example.purrytify.ui.theme.SoftGray
import com.example.purrytify.ui.theme.White
import com.example.purrytify.util.TokenManager

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    songsCount: Int = 0,
    likedCount: Int = 0,
    listenedCount: Int = 0,
    soundCapsuleViewModel: SoundCapsuleViewModel? = null,
    onSoundCapsuleNavigation: (String, MonthYear) -> Unit = { _, _ -> }
) {
    val textColor = White
    val secondaryTextColor = SoftGray
    val topColor = Color(0xFF00667B)
    val bottomColor = DarkBlack

    LazyColumn(
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
        item {
            // Profile Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Profile Picture
                Image(
                    painter = rememberAsyncImagePainter(model = userProfile.profilePhoto),
                    contentDescription = "Profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // User Information
                Text(
                    text = userProfile.username,
                    style = MaterialTheme.typography.headlineMedium,
                    color = textColor
                )

                Text(
                    text = userProfile.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )

                Text(
                    text = userProfile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )

                Spacer(modifier = Modifier.height(8.dp))


                Spacer(modifier = Modifier.height(24.dp))

                // Logout Button
                val context = LocalContext.current
                Button(
                    onClick = {
                        TokenManager.clearToken(context)
                        val intent = Intent(context, LoginActivity::class.java)
                        context.startActivity(intent)
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Logout",
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
                    StatItem(
                        count = songsCount,
                        label = "SONGS"
                    )

                    StatItem(
                        count = likedCount,
                        label = "LIKED"
                    )

                    StatItem(
                        count = listenedCount,
                        label = "LISTENED"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Sound Capsule Section
        item {
            soundCapsuleViewModel?.let { viewModel ->
                OverviewSoundCapsule(
                    viewModel = viewModel,
                    onTimeListenedClick = { monthYear ->
                        onSoundCapsuleNavigation("time_listened", monthYear)
                    },
                    onTopArtistsClick = { monthYear ->
                        onSoundCapsuleNavigation("top_artists", monthYear)
                    },
                    onTopSongsClick = { monthYear ->
                        onSoundCapsuleNavigation("top_songs", monthYear)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
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