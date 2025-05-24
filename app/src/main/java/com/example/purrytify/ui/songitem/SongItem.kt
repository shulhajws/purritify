package com.example.purrytify.ui.songitem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.purrytify.R
import com.example.purrytify.model.Song
import com.example.purrytify.ui.playback.PlayerViewModel

@Composable
fun SongItem(
    song: Song,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayback: (Song) -> Unit,
    onAddToQueueClick: () -> Unit,
//    onLikeClick: () -> Unit,
    onShareClick: () -> Unit
) {
    var isOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                playerViewModel.playSong(song)
                onNavigateToPlayback(song)
            }
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.placeholder_album_art),
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = { isOpen = true },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options"
            )
        }

        // More options menu overflow
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text("Add to queue") },
                onClick = {
                    onAddToQueueClick()
                    isOpen = false
                }
            )
            // TODO: if you have time, handle and add like songs options here
//            if (!song.isFromServer) {
//                DropdownMenuItem(
//                    text = { Text("Like this song") },
//                    onClick = {
//                        onLikeClick()
//                        isOpen = false
//                    }
//                )
//            }
            DropdownMenuItem(
                text = { Text("Share song") },
                onClick = {
                    onShareClick()
                    isOpen = false
                }
            )
        }
    }
}
