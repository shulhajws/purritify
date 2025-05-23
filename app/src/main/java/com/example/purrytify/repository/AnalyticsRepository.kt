package com.example.purrytify.repository

import com.example.purrytify.data.dao.AnalyticsDao
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.dao.TopArtistData
import com.example.purrytify.data.dao.TopSongData
import com.example.purrytify.data.dao.DayStreakSongData
import com.example.purrytify.data.dao.MonthYearData
import com.example.purrytify.data.dao.SongBasicData
import com.example.purrytify.data.entity.ListeningSessionEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsRepository(
    private val analyticsDao: AnalyticsDao,
    private val songDao: SongDao
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Create
    suspend fun startListeningSession(userId: Int, songId: Long): Long {
        val now = Date()
        val calendar = Calendar.getInstance().apply { time = now }

        val session = ListeningSessionEntity(
            userId = userId,
            songId = songId,
            startTime = now,
            endTime = null,
            actualListenDurationMs = 0,
            wasCompleted = false,
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
            dateString = dateFormat.format(now)
        )

        return analyticsDao.insertSession(session)
    }

    // Update (Ends/Paused)
    suspend fun updateListeningSession(
        sessionId: Long,
        actualListenDurationMs: Long,
        wasCompleted: Boolean
    ) {
        val session = analyticsDao.getSessionById(sessionId)
        session?.let {
            val updatedSession = it.copy(
                endTime = Date(),
                actualListenDurationMs = actualListenDurationMs,
                wasCompleted = wasCompleted
            )
            analyticsDao.updateSession(updatedSession)
        }
    }

    // Current Active Session
    suspend fun getActiveSession(userId: Int): ListeningSessionEntity? {
        return analyticsDao.getActiveSession(userId)
    }

    // Monthly Analytics
    suspend fun getMonthlyAnalytics(userId: Int, year: Int, month: Int): MonthlyAnalytics {
        val totalListenTime = analyticsDao.getTotalListenTimeForMonth(userId, year, month)
        val topArtists = analyticsDao.getTopArtistsForMonth(userId, year, month)
        val topSongs = analyticsDao.getTopSongsForMonth(userId, year, month)
        val dayStreakSongs = calculateDayStreakSongs(userId)

        return MonthlyAnalytics(
            year = year,
            month = month,
            totalListenTimeMs = totalListenTime,
            topArtists = topArtists,
            topSongs = topSongs,
            dayStreakSongs = dayStreakSongs
        )
    }

    private suspend fun calculateDayStreakSongs(userId: Int, limit: Int = 10): List<DayStreakSongData> {
        val allListeningData = analyticsDao.getAllUserListeningDates(userId)

        val songDateMap = allListeningData.groupBy { it.songId }
        val streakResults = mutableListOf<Pair<Long, Int>>() // songId to max streak length

        songDateMap.forEach { (songId, dates) ->
            val sortedDates = dates.map { it.dateString }.sorted()
            val maxStreak = findMaxConsecutiveStreak(sortedDates)

            if (maxStreak >= 2) {
                streakResults.add(songId to maxStreak)
            }
        }

        val topStreakSongIds = streakResults
            .sortedByDescending { it.second }
            .take(limit)

        if (topStreakSongIds.isEmpty()) {
            return emptyList()
        }

        val songIds = topStreakSongIds.map { it.first }
        val songDetails = songDao.getSongsByIds(songIds)

        return topStreakSongIds.mapNotNull { (songId, streak) ->
            val song = songDetails.find { it.id == songId }
            song?.let {
                DayStreakSongData(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    consecutiveDays = streak,
                    artworkPath = it.artworkPath
                )
            }
        }
    }

    private fun findMaxConsecutiveStreak(sortedDates: List<String>): Int {
        if (sortedDates.isEmpty()) return 0

        var maxStreak = 1
        var currentStreak = 1

        for (i in 1 until sortedDates.size) {
            val currentDate = parseDate(sortedDates[i])
            val previousDate = parseDate(sortedDates[i - 1])

            val daysDifference = (currentDate.time - previousDate.time) / (24 * 60 * 60 * 1000)
            if (daysDifference == 1L) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }

        return maxStreak
    }

    // Helper: Parse Date
    private fun parseDate(dateString: String): Date {
        return dateFormat.parse(dateString) ?: Date()
    }

    // Get Available Months
    suspend fun getAvailableMonths(userId: Int): List<MonthYear> {
        return analyticsDao.getAvailableMonths(userId).map {
            MonthYear(it.year, it.month)
        }
    }

    // Daily Listen Time for Month
    suspend fun getDailyListenTimeForMonth(userId: Int, year: Int, month: Int): List<DailyListenData> {
        // Generate all days in the month
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1) // month - 1 because Calendar is 0-based
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dailyData = mutableListOf<DailyListenData>()

        // Get listen time for each day
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateString = dateFormat.format(calendar.time)

            val listenTimeMs = analyticsDao.getTotalListenTimeForDay(userId, dateString)
            val listenTimeMinutes = listenTimeMs / (1000 * 60) // Convert to minutes

            dailyData.add(
                DailyListenData(
                    day = day,
                    dateString = dateString,
                    listenTimeMinutes = listenTimeMinutes.toInt()
                )
            )
        }

        return dailyData
    }


    // Get Today's Listen Time
    suspend fun getTodayListenTime(userId: Int): Long {
        val today = dateFormat.format(Date())
        return analyticsDao.getTotalListenTimeForDay(userId, today)
    }

    // Get This Month Listen Time
    suspend fun getCurrentMonthListenTime(userId: Int): Long {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return analyticsDao.getTotalListenTimeForMonth(userId, year, month)
    }

    // Flow for real-time current month updates
    fun getCurrentMonthListenTimeFlow(userId: Int): Flow<Long> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return analyticsDao.getCurrentMonthListenTimeFlow(userId, year, month)
    }
}

data class MonthlyAnalytics(
    val year: Int,
    val month: Int,
    val totalListenTimeMs: Long,
    val topArtists: List<TopArtistData>,
    val topSongs: List<TopSongData>,
    val dayStreakSongs: List<DayStreakSongData>
)

data class MonthYear(
    val year: Int,
    val month: Int
) {
    fun getDisplayName(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        return "$monthName $year"
    }
}

data class DailyListenData(
    val day: Int, // Day of month (1-31)
    val dateString: String, // "yyyy-MM-dd"
    val listenTimeMinutes: Int // Listen time in minutes for this day
)