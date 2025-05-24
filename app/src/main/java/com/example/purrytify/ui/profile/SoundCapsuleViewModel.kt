package com.example.purrytify.ui.profile

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.AnalyticsRepository
import com.example.purrytify.repository.MonthYear
import com.example.purrytify.repository.MonthlyAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.util.Calendar

data class SoundCapsuleState(
    val isLoading: Boolean = false,
    val availableMonths: List<MonthYear> = emptyList(),
    val selectedMonth: MonthYear? = null,
    val analytics: MonthlyAnalytics? = null,
    val currentMonthListenTime: Long = 0,
    val error: String? = null,
    val isExporting: Boolean = false
)

class SoundCapsuleViewModel(application: Application) : AndroidViewModel(application) {
    private val analyticsRepository: AnalyticsRepository
    private val _state = MutableStateFlow(SoundCapsuleState())
    val state: StateFlow<SoundCapsuleState> = _state.asStateFlow()

    private var currentUserId: Int? = null
    private val monthlyAnalyticsCache = mutableMapOf<MonthYear, MonthlyAnalytics>()
    private var realTimeUpdateJob: kotlinx.coroutines.Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        analyticsRepository = AnalyticsRepository(database.analyticsDao(), database.songDao())
    }

    fun updateForUser(userId: Int) {
        if (currentUserId == userId) return
        currentUserId = userId

        realTimeUpdateJob?.cancel()

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            Log.d("SoundCapsuleViewModel", "Starting to load available months...")

            try {
                val availableMonths = analyticsRepository.getAvailableMonths(userId)
                Log.d("SoundCapsuleViewModel", "Found ${availableMonths.size} available months: $availableMonths")

                // Select current month by default
                val calendar = Calendar.getInstance()
                val currentMonth = MonthYear(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1
                )

                val selectedMonth = if (availableMonths.contains(currentMonth)) {
                    currentMonth
                } else {
                    availableMonths.firstOrNull()
                }

                Log.d("SoundCapsuleViewModel", "Selected month: $selectedMonth")

                _state.update {
                    it.copy(
                        availableMonths = availableMonths,
                        selectedMonth = selectedMonth,
                        isLoading = false,
                        error = null
                    )
                }

                selectedMonth?.let {
                    Log.d("SoundCapsuleViewModel", "Loading analytics for selected month: $it")
                    loadAnalyticsForMonth(it)
                }

                startRealTimeUpdates(userId)

            } catch (e: Exception) {
                Log.e("SoundCapsuleViewModel", "Error loading data: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load analytics data: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startRealTimeUpdates(userId: Int) {
        realTimeUpdateJob?.cancel()

        realTimeUpdateJob = viewModelScope.launch {
            while (true) {
                try {
                    val currentListenTime = analyticsRepository.getCurrentMonthListenTime(userId)

                    // Update if Changes Exist
                    if (_state.value.currentMonthListenTime != currentListenTime) {
                        _state.update { it.copy(currentMonthListenTime = currentListenTime) }
                        Log.d("SoundCapsuleViewModel", "Updated current month listen time: $currentListenTime minutes")
                    }

                    // Only Check Current Month
                    val currentCalendar = Calendar.getInstance()
                    val currentMonth = MonthYear(
                        currentCalendar.get(Calendar.YEAR),
                        currentCalendar.get(Calendar.MONTH) + 1
                    )

                    if (_state.value.selectedMonth == currentMonth) {
                        // Fresh Analytics
                        val freshAnalytics = analyticsRepository.getMonthlyAnalytics(
                            userId,
                            currentMonth.year,
                            currentMonth.month
                        )

                        // Current Analytics
                        val currentAnalytics = _state.value.analytics
                        if (currentAnalytics == null || hasAnalyticsChanged(currentAnalytics, freshAnalytics)) {
                            monthlyAnalyticsCache[currentMonth] = freshAnalytics
                            _state.update {
                                it.copy(analytics = freshAnalytics)
                            }
                            Log.d("SoundCapsuleViewModel", "Updated analytics for current month")
                        }
                    }

                } catch (e: Exception) {
                    Log.e("SoundCapsuleViewModel", "Error in real-time update: ${e.message}")
                }

                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    private fun hasAnalyticsChanged(old: MonthlyAnalytics, new: MonthlyAnalytics): Boolean {
        return old.totalListenTimeMinutes != new.totalListenTimeMinutes ||
                old.uniqueArtistsCount != new.uniqueArtistsCount ||
                old.uniqueSongsCount != new.uniqueSongsCount ||
                old.topArtists.size != new.topArtists.size ||
                old.topSongs.size != new.topSongs.size ||
                old.dayStreakSongs.size != new.dayStreakSongs.size
    }

    fun selectMonth(monthYear: MonthYear) {
        if (_state.value.selectedMonth != monthYear) {
            _state.update { it.copy(selectedMonth = monthYear) }
            loadAnalyticsForMonth(monthYear)
        }
    }

    private fun loadAnalyticsForMonth(monthYear: MonthYear) {
        val userId = currentUserId ?: return

        monthlyAnalyticsCache[monthYear]?.let { cachedAnalytics ->
            _state.update {
                it.copy(
                    analytics = cachedAnalytics,
                    selectedMonth = monthYear,
                    isLoading = false,
                    error = null
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val analytics = analyticsRepository.getMonthlyAnalytics(
                    userId,
                    monthYear.year,
                    monthYear.month
                )

                monthlyAnalyticsCache[monthYear] = analytics

                _state.update {
                    it.copy(
                        analytics = analytics,
                        selectedMonth = monthYear,
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                Log.e("SoundCapsuleViewModel", "Error loading analytics: ${e.message}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load analytics for ${monthYear.getDisplayName()}"
                    )
                }
            }
        }
    }

    fun refreshCurrentMonth() {
        val currentCalendar = Calendar.getInstance()
        val currentMonth = MonthYear(
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH) + 1
        )

        monthlyAnalyticsCache.remove(currentMonth)
        if (_state.value.selectedMonth == currentMonth) {
            loadAnalyticsForMonth(currentMonth)
        }
    }

    fun getAnalyticsForMonth(monthYear: MonthYear): MonthlyAnalytics? {
        return monthlyAnalyticsCache[monthYear]
    }

    fun exportAnalytics(context: Context, format: ExportFormat) {
        val userId = currentUserId ?: return
        val selectedMonth = _state.value.selectedMonth ?: return

        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }

            try {
                val exportData = analyticsRepository.getAnalyticsForExport(
                    userId,
                    selectedMonth.year,
                    selectedMonth.month
                )

                when (format) {
                    ExportFormat.CSV -> exportToCsv(context, exportData)
                    ExportFormat.PDF -> exportToPdf(context, exportData)
                }

            } catch (e: Exception) {
                Log.e("SoundCapsuleViewModel", "Error exporting: ${e.message}")
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun exportToCsv(context: Context, exportData: com.example.purrytify.repository.AnalyticsExportData) {
        try {
            val fileName = "sound_capsule_${exportData.year}_${exportData.month}.csv"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            FileWriter(file).use { writer ->
                // Write header
                writer.append("Sound Capsule Analytics Report\n")
                writer.append("Month: ${exportData.monthName}\n")
                writer.append("Total Listen Time: ${exportData.totalListenTimeFormatted}\n")
                writer.append("Daily Average: ${String.format("%.1f", exportData.dailyAverageMinutes)} minutes\n")
                writer.append("Unique Artists: ${exportData.uniqueArtistsCount}\n")
                writer.append("Unique Songs: ${exportData.uniqueSongsCount}\n\n")

                // Top Artists
                writer.append("Top Artists\n")
                writer.append("Rank,Artist,Play Count,Total Listen Time\n")
                exportData.topArtists.forEachIndexed { index, artist ->
                    writer.append("${index + 1},\"${artist.artist}\",${artist.playCount},${analyticsRepository.formatDuration(artist.totalListenTime)}\n")
                }
                writer.append("\n")

                // Top Songs
                writer.append("Top Songs\n")
                writer.append("Rank,Title,Artist,Play Count,Total Listen Time\n")
                exportData.topSongs.forEachIndexed { index, song ->
                    writer.append("${index + 1},\"${song.title}\",\"${song.artist}\",${song.playCount},${analyticsRepository.formatDuration(song.totalListenTime)}\n")
                }
                writer.append("\n")

                // Day Streak Songs
                if (exportData.dayStreakSongs.isNotEmpty()) {
                    writer.append("Day Streak Songs\n")
                    writer.append("Song,Artist,Consecutive Days,Start Date,End Date\n")
                    exportData.dayStreakSongs.forEach { streak ->
                        writer.append("\"${streak.title}\",\"${streak.artist}\",${streak.consecutiveDays},${streak.startDate},${streak.endDate}\n")
                    }
                    writer.append("\n")
                }

                // Daily Listen Data
                writer.append("Daily Listen Data\n")
                writer.append("Day,Date,Listen Time (minutes)\n")
                exportData.dailyListenData.forEach { daily ->
                    writer.append("${daily.day},${daily.dateString},${daily.listenTimeMinutes}\n")
                }
            }

            shareFile(context, file, "text/csv")
            Toast.makeText(context, "CSV exported successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("SoundCapsuleViewModel", "CSV export error: ${e.message}")
            throw e
        }
    }

    private fun exportToPdf(context: Context, exportData: com.example.purrytify.repository.AnalyticsExportData) {
        // For simplicity, we'll create a text-based "PDF" file
        // In a real implementation, you'd use a PDF library like iTextPDF
        try {
            val fileName = "sound_capsule_${exportData.year}_${exportData.month}.txt"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            FileWriter(file).use { writer ->
                writer.append("SOUND CAPSULE ANALYTICS REPORT\n")
                writer.append("=" + "=".repeat(40) + "\n\n")
                writer.append("Month: ${exportData.monthName}\n")
                writer.append("Total Listen Time: ${exportData.totalListenTimeFormatted}\n")
                writer.append("Daily Average: ${String.format("%.1f", exportData.dailyAverageMinutes)} minutes\n")
                writer.append("Unique Artists: ${exportData.uniqueArtistsCount}\n")
                writer.append("Unique Songs: ${exportData.uniqueSongsCount}\n\n")

                writer.append("TOP ARTISTS\n")
                writer.append("-".repeat(30) + "\n")
                exportData.topArtists.forEachIndexed { index, artist ->
                    writer.append("${index + 1}. ${artist.artist}\n")
                    writer.append("   Plays: ${artist.playCount} | Time: ${analyticsRepository.formatDuration(artist.totalListenTime)}\n\n")
                }

                writer.append("TOP SONGS\n")
                writer.append("-".repeat(30) + "\n")
                exportData.topSongs.forEachIndexed { index, song ->
                    writer.append("${index + 1}. ${song.title}\n")
                    writer.append("   by ${song.artist}\n")
                    writer.append("   Plays: ${song.playCount} | Time: ${analyticsRepository.formatDuration(song.totalListenTime)}\n\n")
                }

                if (exportData.dayStreakSongs.isNotEmpty()) {
                    writer.append("DAY STREAK SONGS\n")
                    writer.append("-".repeat(30) + "\n")
                    exportData.dayStreakSongs.forEach { streak ->
                        writer.append("${streak.title} by ${streak.artist}\n")
                        writer.append("${streak.getStreakDescription()}\n\n")
                    }
                }
            }

            shareFile(context, file, "text/plain")
            Toast.makeText(context, "Report exported successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("SoundCapsuleViewModel", "PDF export error: ${e.message}")
            throw e
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Sound Capsule"))
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun formatDuration(milliseconds: Long): String {
        return analyticsRepository.formatDuration(milliseconds)
    }

    fun formatMinutesToReadable(minutes: Long): String {
        return analyticsRepository.formatMinutesToReadable(minutes)
    }

    override fun onCleared() {
        super.onCleared()
        realTimeUpdateJob?.cancel()
    }
}

enum class ExportFormat {
    CSV, PDF
}