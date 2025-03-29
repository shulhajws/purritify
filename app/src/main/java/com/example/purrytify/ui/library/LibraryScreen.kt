package com.example.purrytify.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.model.Song
import com.example.purrytify.ui.theme.White

@Composable
fun LibraryScreen(viewModel: LibraryViewModel,
                  onSongClick: (String) -> Unit,
                  onAddClick: () -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                TabButton(
                    text = "All",
                    isSelected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                TabButton(
                    text = "Liked",
                    isSelected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) }
                )
            }

            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add song",
                    tint = White
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )

        // Song list
        val songsToShow = if (selectedTab == 0) allSongs else likedSongs

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(songsToShow) { song ->
                LibrarySongItem(
                    song = song,
                    onSongClick = { onSongClick(song.id) }
                )
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isSelected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun LibrarySongItem(song: Song, onSongClick: () -> Unit) {
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
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}