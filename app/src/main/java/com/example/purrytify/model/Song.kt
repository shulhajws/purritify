package com.example.purrytify.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumArt: String,
    val isPlaying: Boolean = false
)