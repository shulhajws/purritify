package com.example.purrytify.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import java.util.Date

@Parcelize
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String,
    val audioUrl: String,
    val isLiked: Boolean,
    val isListened: Boolean,
    val uploadedAt: Date?,
    val updatedAt: Date? = null,
    val lastPlayedAt: Date? = null,
    val rank: Int? = null,
    val country: String? = null,
    val isPlaying: Boolean = false,
    val isFromServer: Boolean = false
) : Parcelable