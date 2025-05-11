package com.example.purrytify.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    // Event for song deletion
    private val _songDeletedEvents = MutableSharedFlow<Long>(replay = 0)
    val songDeletedEvents: SharedFlow<Long> = _songDeletedEvents.asSharedFlow()

    // Event for song update
    private val _songUpdatedEvents = MutableSharedFlow<Long>(replay = 0)
    val songUpdatedEvents: SharedFlow<Long> = _songUpdatedEvents.asSharedFlow()

    private val _navigateAwayFromDeletedSong = MutableSharedFlow<String>(replay = 0)
    val navigateAwayFromDeletedSong: SharedFlow<String> = _navigateAwayFromDeletedSong.asSharedFlow()

    suspend fun publishNavigateAwayFromDeletedSong(songId: String) {
        _navigateAwayFromDeletedSong.emit(songId)
    }

    suspend fun publishSongDeleted(songId: Long) {
        _songDeletedEvents.emit(songId)
    }

    suspend fun publishSongUpdated(songId: Long) {
        _songUpdatedEvents.emit(songId)
    }
}