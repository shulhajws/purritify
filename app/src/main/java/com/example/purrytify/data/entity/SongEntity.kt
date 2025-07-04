package com.example.purrytify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var userId: Int,
    var title: String,
    var artist: String,
    var duration: Long,
    var filePath: String,
    var artworkPath: String? = null,
    var uploadedAt: Date,
    var updatedAt: Date? = null,
    var lastPlayedAt: Date? = null,
    var isLiked: Boolean = false,
    var isListened: Boolean = false,
    var rank: Int? = null,
    var country: String? = null,
    var isFromServer: Boolean = false,
)