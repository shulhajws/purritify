package com.example.purrytify.ui.profile

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
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
import com.example.purrytify.ui.login.LoginActivity
import com.example.purrytify.ui.theme.DarkBlack
import com.example.purrytify.ui.theme.SoftGray
import com.example.purrytify.ui.theme.White
import com.example.purrytify.util.TokenManager

@Composable
fun ProfileScreen(userProfile: UserProfile) {
//    val userProfile by viewModel.userProfile.collectAsState()
//    val songsCount by viewModel.songsCount.collectAsState()
//    val likedCount by viewModel.likedCount.collectAsState()
//    val listenedCount by viewModel.listenedCount.collectAsState()

//    val buttonColor = DarkGray    // Uncomment this line to use DarkGray
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
            Box(
                modifier = Modifier
                    .padding(top = 48.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = userProfile.profilePhoto),
                    contentDescription = "Profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display all of the information that UserProfile had

            // Username
            Text(
                text = userProfile.username,
                style = MaterialTheme.typography.headlineMedium,
                color = textColor
            )

            // Location
            Text(
                text = userProfile.location,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )

            // Email
            Text(
                text = userProfile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Created At
            Text(
                text = "Account Created at: ${userProfile.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )

            // Updated At
            Text(
                text = "Account Updated at: ${userProfile.updatedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            val context = LocalContext.current
            Button(
                onClick = {
                    // Clear the token
                    TokenManager.clearToken(context)

                    // Navigate to LoginActivity
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)

                    // Toast message
                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

                    // Finish current activity "if needed"
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
                // Songs Count
                StatItem(
                    count = 135, // TODO: Replace with songsCount by access database
                    label = "SONGS"
                )

                // Liked Count
                StatItem(
                    count = 135, // TODO: Replace with likedCount by access database
                    label = "LIKED"
                )

                // Listened Count
                StatItem(
                    count = 135, // TODO: Replace with listenedCount by access database
                    label = "LISTENED"
                )
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