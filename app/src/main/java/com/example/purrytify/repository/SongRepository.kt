package com.example.purrytify.repository

import androidx.lifecycle.LiveData
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.SongEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

class SongRepository(private val songDao: SongDao) {
    fun getSongsByUserId(userId: Int): LiveData<List<SongEntity>> {
        return songDao.getSongsByUserId(userId)
    }

    fun getLikedSongsByUserId(userId: Int): LiveData<List<SongEntity>> {
        return songDao.getLikedSongsByUserId(userId)
    }

    fun getNewSongsByUserId(userId: Int): LiveData<List<SongEntity>> {
        return songDao.getNewSongsByUserId(userId)
    }

    fun getRecentlyPlayedSongsByUserId(userId: Int): LiveData<List<SongEntity>> {
        return songDao.getRecentlyPlayedSongsByUserId(userId)
    }

    fun getSongsCountByUserId(userId: Int): Flow<Int> {
        return songDao.getSongsCountByUserId(userId)
    }

    fun getLikedSongsCountByUserId(userId: Int): Flow<Int> {
        return songDao.getLikedSongsCountByUserId(userId)
    }

    fun getListenedSongsCountByUserId(userId: Int): Flow<Int> {
        return songDao.getListenedSongsCountByUserId(userId)
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
}