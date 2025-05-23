package com.example.purrytify.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.purrytify.data.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE userId = :userId")
    fun getSongsByUserId(userId: Int): LiveData<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE userId = :userId AND isLiked = 1")
    fun getLikedSongsByUserId(userId: Int): LiveData<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE userId = :userId ORDER BY uploadedAt DESC")
    fun getNewSongsByUserId(userId: Int): LiveData<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE userId = :userId AND lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun getRecentlyPlayedSongsByUserId(userId: Int): LiveData<List<SongEntity>>

    // For Development/Fallback Purposes
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

    // New count queries for profile stats
    @Query("SELECT COUNT(*) FROM songs WHERE userId = :userId")
    fun getSongsCountByUserId(userId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE userId = :userId AND isLiked = 1")
    fun getLikedSongsCountByUserId(userId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE userId = :userId AND isListened = 1")
    fun getListenedSongsCountByUserId(userId: Int): Flow<Int>
}