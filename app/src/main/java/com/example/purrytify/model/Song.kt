package com.example.purrytify.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String,
    val audioUrl: String,
    val isPlaying: Boolean = false
) : Parcelable