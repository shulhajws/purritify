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

    // Time Based Analytics
    // Total Listen Time For Month
    @Query("""
        SELECT COALESCE(SUM(actualListenDurationMs), 0) 
        FROM listening_sessions 
        WHERE userId = :userId AND year = :year AND month = :month
    """)
    suspend fun getTotalListenTimeForMonth(userId: Int, year: Int, month: Int): Long

    // Total Listen Time For Day
    @Query("""
        SELECT COALESCE(SUM(actualListenDurationMs), 0)
        FROM listening_sessions
        WHERE userId = :userId AND dateString = :dateString
    """)
    suspend fun getTotalListenTimeForDay(userId: Int, dateString: String): Long

    // Real Time Listen Time Flow For Current Month
    @Query("""
        SELECT COALESCE(SUM(actualListenDurationMs), 0)
        FROM listening_sessions
        WHERE userId = :userId AND year = :year AND month = :month
    """)
    fun getCurrentMonthListenTimeFlow(userId: Int, year: Int, month: Int): Flow<Long>

    // Daily Average Listen Time For Month In Minutes
    @Query("""
        SELECT AVG(daily_total) as avgMinutes
        FROM (
            SELECT SUM(actualListenDurationMs) / (1000 * 60) as daily_total
            FROM listening_sessions
            WHERE userId = :userId AND year = :year AND month = :month
            GROUP BY dateString
        )
    """)
    suspend fun getDailyAverageListenTimeForMonth(userId: Int, year: Int, month: Int): Double?

    // Content Diversity Analytics
    // Count Unique Artists Listened To In Month
    @Query("""
        SELECT COUNT(DISTINCT s.artist)
        FROM listening_sessions ls
        JOIN songs s ON ls.songId = s.id
        WHERE ls.userId = :userId AND ls.year = :year AND ls.month = :month
    """)
    suspend fun getUniqueArtistsCountForMonth(userId: Int, year: Int, month: Int): Int

    // Count Unique Songs Listened To In Month
    @Query("""
        SELECT COUNT(DISTINCT ls.songId)
        FROM listening_sessions ls
        WHERE ls.userId = :userId AND ls.year = :year AND ls.month = :month
    """)
    suspend fun getUniqueSongsCountForMonth(userId: Int, year: Int, month: Int): Int

    // Top Content Analytics
    // Top Artists For Month
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

    // Top Songs For Month
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

    // Streak Calculation Support
    // Get All User Listening Dates For Streak Calculation
    @Query("""
        SELECT DISTINCT ls.songId, ls.dateString
        FROM listening_sessions ls
        WHERE ls.userId = :userId
        ORDER BY ls.songId, ls.dateString
    """)
    suspend fun getAllUserListeningDates(userId: Int): List<SongDateData>

    // Session Analytics
    // Total Session Count For Month
    @Query("""
        SELECT COUNT(*) 
        FROM listening_sessions 
        WHERE userId = :userId AND year = :year AND month = :month
    """)
    suspend fun getSessionCountForMonth(userId: Int, year: Int, month: Int): Int

    // Average Session Duration For Month
    @Query("""
        SELECT AVG(actualListenDurationMs) 
        FROM listening_sessions 
        WHERE userId = :userId AND year = :year AND month = :month 
        AND actualListenDurationMs > 0
    """)
    suspend fun getAverageSessionDurationForMonth(userId: Int, year: Int, month: Int): Double?

    // Most Active Day Of Week For Month
    @Query("""
        SELECT 
            CASE CAST(strftime('%w', startTime/1000, 'unixepoch') AS INTEGER)
                WHEN 0 THEN 'Sunday'
                WHEN 1 THEN 'Monday'
                WHEN 2 THEN 'Tuesday'
                WHEN 3 THEN 'Wednesday'
                WHEN 4 THEN 'Thursday'
                WHEN 5 THEN 'Friday'
                WHEN 6 THEN 'Saturday'
            END as dayOfWeek,
            COUNT(*) as sessionCount,
            SUM(actualListenDurationMs) as totalTime
        FROM listening_sessions
        WHERE userId = :userId AND year = :year AND month = :month
        GROUP BY CAST(strftime('%w', startTime/1000, 'unixepoch') AS INTEGER)
        ORDER BY sessionCount DESC, totalTime DESC
        LIMIT 1
    """)
    suspend fun getMostActiveDayOfWeekForMonth(userId: Int, year: Int, month: Int): DayOfWeekData?

    // Data Management
    // Get Available Months That Have Data
    @Query("""
        SELECT DISTINCT year, month
        FROM listening_sessions
        WHERE userId = :userId
        ORDER BY year DESC, month DESC
    """)
    suspend fun getAvailableMonths(userId: Int): List<MonthYearData>

    // Get Current Active Session Without End Time
    @Query("""
        SELECT * FROM listening_sessions 
        WHERE userId = :userId AND endTime IS NULL 
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    suspend fun getActiveSession(userId: Int): ListeningSessionEntity?

    // Additional Cleanup Operations
    // Delete Old Sessions For Maintenance
    @Query("""
        DELETE FROM listening_sessions 
        WHERE userId = :userId AND startTime < :beforeDate
    """)
    suspend fun deleteOldSessions(userId: Int, beforeDate: Long)

    // Get Total Number Of Sessions For User
    @Query("""
        SELECT COUNT(*) 
        FROM listening_sessions 
        WHERE userId = :userId
    """)
    suspend fun getTotalSessionsForUser(userId: Int): Int
}

// Data Classes For Query Results----------------------------
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

data class DayOfWeekData(
    val dayOfWeek: String,
    val sessionCount: Int,
    val totalTime: Long
)

data class DayStreakSongData(
    val id: Long,
    val title: String,
    val artist: String,
    val consecutiveDays: Int,
    val startDate: String, // "yyyy-MM-dd" format
    val endDate: String,   // "yyyy-MM-dd" format
    val artworkPath: String?
) {
    fun getStreakDescription(): String {
        return if (startDate == endDate) {
            // single day (shouldn't happen since we filter for 2+ days)
            "1 day"
        } else {
            val formatter = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            try {
                val start = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(startDate)
                val end = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(endDate)

                if (start != null && end != null) {
                    "$consecutiveDays days (${formatter.format(start)} - ${formatter.format(end)})"
                } else {
                    "$consecutiveDays days"
                }
            } catch (e: Exception) {
                "$consecutiveDays days"
            }
        }
    }
}