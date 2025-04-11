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
    val uploadedAt: Date,
    val lastPlayedAt: Date? = null,
    val isPlaying: Boolean = false
) : Parcelable