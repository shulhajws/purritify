package com.example.purrytify.repository

import androidx.lifecycle.LiveData
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.SongEntity
import java.util.Date
class SongRepository(private val songDao: SongDao) {
    val allSongs: LiveData<List<SongEntity>> = songDao.getAllSongs()
    val likedSongs: LiveData<List<SongEntity>> = songDao.getLikedSongs()
    val newSongs: LiveData<List<SongEntity>> = songDao.getNewSongs()
    val recentlyPlayedSongs: LiveData<List<SongEntity>> = songDao.getRecentlyPlayedSongs()

    suspend fun insert(song: SongEntity): Long {
        return songDao.insert(song)
    }

    suspend fun update(song: SongEntity) {
        songDao.update(song)
    }

    suspend fun delete(song: SongEntity) {
        songDao.delete(song)
    }

    suspend fun getSongById(id: Long): SongEntity? {
        return songDao.getSongById(id)
    }

    suspend fun toggleLiked(songId: Long, liked: Boolean) {
        val song = songDao.getSongById(songId)
        song?.let {
            it.isLiked = liked
            songDao.update(it)
        }
    }

    suspend fun updateLastPlayed(songId: Long) {
        val song = songDao.getSongById(songId)
        song?.let {
            it.lastPlayedAt = Date()
            it.isListened = true
            songDao.update(it)
        }
    }
}