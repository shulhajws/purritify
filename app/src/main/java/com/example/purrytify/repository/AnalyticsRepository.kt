package com.example.purrytify.repository

import com.example.purrytify.data.dao.AnalyticsDao
import com.example.purrytify.data.dao.DayStreakSongData
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.dao.TopArtistData
import com.example.purrytify.data.dao.TopSongData
import com.example.purrytify.data.entity.ListeningSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalyticsRepository(
    private val analyticsDao: AnalyticsDao,
    private val songDao: SongDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Session Management
    // Create New Listening Session
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

    // Update Existing Session When Ended Or Paused
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

    // Get Currently Active Session If Any
    suspend fun getActiveSession(userId: Int): ListeningSessionEntity? {
        return analyticsDao.getActiveSession(userId)
    }

    // Monthly Analytics
    // Get Complete Monthly Analytics Data
    suspend fun getMonthlyAnalytics(userId: Int, year: Int, month: Int): MonthlyAnalytics {
        val totalListenTimeMs = analyticsDao.getTotalListenTimeForMonth(userId, year, month)
        val topArtists = analyticsDao.getTopArtistsForMonth(userId, year, month)
        val topSongs = analyticsDao.getTopSongsForMonth(userId, year, month)
        val dayStreakSongs = calculateDayStreakSongs(userId)

        // new metrics
        val uniqueArtistsCount = analyticsDao.getUniqueArtistsCountForMonth(userId, year, month)
        val uniqueSongsCount = analyticsDao.getUniqueSongsCountForMonth(userId, year, month)
        val dailyAverageMinutes = analyticsDao.getDailyAverageListenTimeForMonth(userId, year, month) ?: 0.0

        return MonthlyAnalytics(
            year = year,
            month = month,
            totalListenTimeMinutes = totalListenTimeMs / (1000 * 60), // convert to minutes
            topArtists = topArtists,
            topSongs = topSongs,
            dayStreakSongs = dayStreakSongs,
            uniqueArtistsCount = uniqueArtistsCount,
            uniqueSongsCount = uniqueSongsCount,
            dailyAverageMinutes = dailyAverageMinutes
        )
    }

    // Streak Calculation
    // Calculate Day Streak Songs With Date Ranges
    private suspend fun calculateDayStreakSongs(userId: Int, limit: Int = 5): List<DayStreakSongData> {
        val allListeningData = analyticsDao.getAllUserListeningDates(userId)

        val songDateMap = allListeningData.groupBy { it.songId }
        val streakResults = mutableListOf<StreakResult>()

        songDateMap.forEach { (songId, dates) ->
            val sortedDates = dates.map { it.dateString }.distinct().sorted()
            val streaks = findAllConsecutiveStreaks(sortedDates)

            // get the longest streak that's 2+ days
            val longestStreak = streaks.filter { it.length >= 2 }.maxByOrNull { it.length }

            longestStreak?.let {
                streakResults.add(StreakResult(songId, it))
            }
        }

        val topStreakSongIds = streakResults
            .sortedByDescending { it.streak.length }
            .take(limit)

        if (topStreakSongIds.isEmpty()) {
            return emptyList()
        }

        val songIds = topStreakSongIds.map { it.songId }
        val songDetails = songDao.getSongsByIds(songIds)

        return topStreakSongIds.mapNotNull { streakResult ->
            val song = songDetails.find { it.id == streakResult.songId }
            song?.let {
                DayStreakSongData(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    consecutiveDays = streakResult.streak.length,
                    startDate = streakResult.streak.startDate,
                    endDate = streakResult.streak.endDate,
                    artworkPath = it.artworkPath
                )
            }
        }
    }

    // Find All Consecutive Streaks In Date List
    private fun findAllConsecutiveStreaks(sortedDates: List<String>): List<StreakInfo> {
        if (sortedDates.isEmpty()) return emptyList()

        val streaks = mutableListOf<StreakInfo>()
        var currentStreakStart = sortedDates[0]
        var currentStreakEnd = sortedDates[0]
        var currentLength = 1

        for (i in 1 until sortedDates.size) {
            val currentDate = parseDate(sortedDates[i])
            val previousDate = parseDate(sortedDates[i - 1])

            val daysDifference = (currentDate.time - previousDate.time) / (24 * 60 * 60 * 1000)

            if (daysDifference == 1L) {
                // consecutive day, extend current streak
                currentStreakEnd = sortedDates[i]
                currentLength++
            } else {
                // non-consecutive day, save current streak if valid and start new one
                if (currentLength >= 2) {
                    streaks.add(StreakInfo(currentLength, currentStreakStart, currentStreakEnd))
                }
                currentStreakStart = sortedDates[i]
                currentStreakEnd = sortedDates[i]
                currentLength = 1
            }
        }

        // don't forget the last streak
        if (currentLength >= 2) {
            streaks.add(StreakInfo(currentLength, currentStreakStart, currentStreakEnd))
        }

        return streaks
    }

    // Helper Functions

    // Parse Date String To Date Object
    private fun parseDate(dateString: String): Date {
        return dateFormat.parse(dateString) ?: Date()
    }

    // Data Retrieval
    // Get Available Months For User
    suspend fun getAvailableMonths(userId: Int): List<MonthYear> {
        return analyticsDao.getAvailableMonths(userId).map {
            MonthYear(it.year, it.month)
        }
    }

    // Get Daily Listen Time For Specific Month
    suspend fun getDailyListenTimeForMonth(userId: Int, year: Int, month: Int): List<DailyListenData> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1) // month - 1 because Calendar is 0-based
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dailyData = mutableListOf<DailyListenData>()

        // get listen time for each day
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateString = dateFormat.format(calendar.time)

            val listenTimeMs = analyticsDao.getTotalListenTimeForDay(userId, dateString)
            val listenTimeMinutes = listenTimeMs / (1000 * 60) // convert to minutes

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
        val todayMs = analyticsDao.getTotalListenTimeForDay(userId, today)
        return todayMs / (1000 * 60) // convert to minutes
    }

    // Get Current Month Listen Time
    suspend fun getCurrentMonthListenTime(userId: Int): Long {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val currentMonthMs = analyticsDao.getTotalListenTimeForMonth(userId, year, month)
        return currentMonthMs / (1000 * 60) // convert to minutes
    }

    // Flow For Real Time Current Month Updates
    fun getCurrentMonthListenTimeFlow(userId: Int): Flow<Long> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return analyticsDao.getCurrentMonthListenTimeFlow(userId, year, month)
            .map { it / (1000 * 60) } // convert to minutes
    }

    // Export And Formatting
    // Get Analytics Data For Export
    suspend fun getAnalyticsForExport(userId: Int, year: Int, month: Int): AnalyticsExportData {
        val analytics = getMonthlyAnalytics(userId, year, month)
        val dailyData = getDailyListenTimeForMonth(userId, year, month)

        return AnalyticsExportData(
            year = year,
            month = month,
            monthName = MonthYear(year, month).getDisplayName(),
            totalListenTimeMinutes = analytics.totalListenTimeMinutes,
            totalListenTimeFormatted = formatMinutesToReadable(analytics.totalListenTimeMinutes),
            topArtists = analytics.topArtists,
            topSongs = analytics.topSongs,
            dayStreakSongs = analytics.dayStreakSongs,
            dailyListenData = dailyData,
            uniqueArtistsCount = analytics.uniqueArtistsCount,
            uniqueSongsCount = analytics.uniqueSongsCount,
            dailyAverageMinutes = analytics.dailyAverageMinutes
        )
    }

    // Format Time Duration For Display
    // Format Time Duration For Display
    fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format("%dh %dm %ds", hours, minutes, seconds)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }

    // Format Minutes To Readable Format For Display
    fun formatMinutesToReadable(minutes: Long): String {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        return when {
            hours > 0 -> String.format("%dh %dm", hours, remainingMinutes)
            minutes > 0 -> String.format("%dm", minutes)
            else -> "0m"
        }
    }

    // Format Time Duration For Csv Export More Detailed
    fun formatDurationForExport(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

// Data Classes

data class MonthlyAnalytics(
    val year: Int,
    val month: Int,
    val totalListenTimeMinutes: Long,
    val topArtists: List<TopArtistData>,
    val topSongs: List<TopSongData>,
    val dayStreakSongs: List<DayStreakSongData>,
    val uniqueArtistsCount: Int,
    val uniqueSongsCount: Int,
    val dailyAverageMinutes: Double
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
    val day: Int, // day of month (1-31)
    val dateString: String, // "yyyy-MM-dd"
    val listenTimeMinutes: Int // listen time in minutes for this day
)

data class AnalyticsExportData(
    val year: Int,
    val month: Int,
    val monthName: String,
    val totalListenTimeMinutes: Long, // changed to minutes for display
    val totalListenTimeFormatted: String,
    val topArtists: List<TopArtistData>,
    val topSongs: List<TopSongData>,
    val dayStreakSongs: List<DayStreakSongData>,
    val dailyListenData: List<DailyListenData>,
    val uniqueArtistsCount: Int,
    val uniqueSongsCount: Int,
    val dailyAverageMinutes: Double
)

// Private Data Classes For Internal Use
private data class StreakInfo(
    val length: Int,
    val startDate: String,
    val endDate: String
)

private data class StreakResult(
    val songId: Long,
    val streak: StreakInfo
)