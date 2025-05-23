package com.example.purrytify.data.dao

import androidx.room.*
import com.example.purrytify.data.entity.ListeningSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {

    // CRUD
    @Insert
    suspend fun insertSession(session: ListeningSessionEntity): Long

    @Update
    suspend fun updateSession(session: ListeningSessionEntity)

    @Query("SELECT * FROM listening_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ListeningSessionEntity?

    // Month Listen Time
    @Query("""
        SELECT COALESCE(SUM(actualListenDurationMs), 0) 
        FROM listening_sessions 
        WHERE userId = :userId AND year = :year AND month = :month
    """)
    suspend fun getTotalListenTimeForMonth(userId: Int, year: Int, month: Int): Long

    // Day Listen Time
    @Query("""
        SELECT COALESCE(SUM(actualListenDurationMs), 0)
        FROM listening_sessions
        WHERE userId = :userId AND dateString = :dateString
    """)
    suspend fun getTotalListenTimeForDay(userId: Int, dateString: String): Long

    // Flow (Real-time) Month Listen Time
    @Query("""
        SELECT COALESCE(SUM(actualListenDurationMs), 0)
        FROM listening_sessions
        WHERE userId = :userId AND year = :year AND month = :month
    """)
    fun getCurrentMonthListenTimeFlow(userId: Int, year: Int, month: Int): Flow<Long>

    // Top Artists
    @Query("""
        SELECT s.artist, 
               COUNT(*) as playCount, 
               SUM(ls.actualListenDurationMs) as totalListenTime
        FROM listening_sessions ls
        JOIN songs s ON ls.songId = s.id
        WHERE ls.userId = :userId AND ls.year = :year AND ls.month = :month
        GROUP BY s.artist
        ORDER BY playCount DESC, totalListenTime DESC
        LIMIT :limit
    """)
    suspend fun getTopArtistsForMonth(userId: Int, year: Int, month: Int, limit: Int = 5): List<TopArtistData>

    // Top Songs
    @Query("""
        SELECT s.id, s.title, s.artist, 
               COUNT(*) as playCount, 
               SUM(ls.actualListenDurationMs) as totalListenTime
        FROM listening_sessions ls
        JOIN songs s ON ls.songId = s.id
        WHERE ls.userId = :userId AND ls.year = :year AND ls.month = :month
        GROUP BY s.id, s.title, s.artist
        ORDER BY playCount DESC, totalListenTime DESC
        LIMIT :limit
    """)
    suspend fun getTopSongsForMonth(userId: Int, year: Int, month: Int, limit: Int = 5): List<TopSongData>

    // For Streak Calculation (Handled by Repository)
    @Query("""
        SELECT DISTINCT ls.songId, ls.dateString
        FROM listening_sessions ls
        WHERE ls.userId = :userId
        ORDER BY ls.songId, ls.dateString
    """)
    suspend fun getAllUserListeningDates(userId: Int): List<SongDateData>

    // Available Months
    @Query("""
        SELECT DISTINCT year, month
        FROM listening_sessions
        WHERE userId = :userId
        ORDER BY year DESC, month DESC
    """)
    suspend fun getAvailableMonths(userId: Int): List<MonthYearData>

    // Active Session if Any
    @Query("""
        SELECT * FROM listening_sessions 
        WHERE userId = :userId AND endTime IS NULL 
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    suspend fun getActiveSession(userId: Int): ListeningSessionEntity?
}

data class TopArtistData(
    val artist: String,
    val playCount: Int,
    val totalListenTime: Long
)

data class TopSongData(
    val id: Long,
    val title: String,
    val artist: String,
    val playCount: Int,
    val totalListenTime: Long
)

data class MonthYearData(
    val year: Int,
    val month: Int
)

data class SongDateData(
    val songId: Long,
    val dateString: String
)

data class DayStreakSongData(
    val id: Long,
    val title: String,
    val artist: String,
    val consecutiveDays: Int,
    val artworkPath: String?
)