package com.example.purrytify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var title: String,
    var artist: String,
    var duration: Long, // in milliseconds
    var filePath: String,
    var artworkPath: String? = null // Store path to artwork image
)