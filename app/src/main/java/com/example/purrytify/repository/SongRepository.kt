package com.example.purrytify.repository

import androidx.lifecycle.LiveData
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.SongEntity

class SongRepository(private val songDao: SongDao) {
    val allSongs: LiveData<List<SongEntity>> = songDao.getAllSongs()

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