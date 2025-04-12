package com.example.purrytify.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.purrytify.data.entity.SongEntity

@Dao
interface SongDao {
    @Query("SELECT * FROM songs")
    fun getAllSongs(): LiveData<List<SongEntity>>

    @Insert
    suspend fun insert(song: SongEntity): Long

    @Update
    suspend fun update(song: SongEntity)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE isLiked = 1")
    fun getLikedSongs(): LiveData<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY uploadedAt DESC")
    fun getNewSongs(): LiveData<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun getRecentlyPlayedSongs(): LiveData<List<SongEntity>>
}