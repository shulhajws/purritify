package com.example.purrytify.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.purrytify.R
import com.example.purrytify.model.Song

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onMiniPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable { onMiniPlayerClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(text = song.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }

        IconButton(onClick = { onPlayPauseClick() }) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black
            )
        }
    }
}