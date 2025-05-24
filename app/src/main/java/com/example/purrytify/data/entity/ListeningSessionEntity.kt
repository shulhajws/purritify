package com.example.purrytify.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "listening_sessions")
data class ListeningSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int,
    val songId: Long,
    val startTime: Date,
    val endTime: Date? = null,
    val actualListenDurationMs: Long = 0,
    val wasCompleted: Boolean = false,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val dateString: String
)