package com.example.purrytify.ui.profile

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.repository.AnalyticsRepository
import com.example.purrytify.repository.DailyListenData
import com.example.purrytify.repository.MonthYear
import com.example.purrytify.repository.MonthlyAnalytics
import com.example.purrytify.ui.theme.DarkBlack
import com.example.purrytify.ui.theme.DarkBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

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

    fun loadDailyDataForMonth(monthYear: MonthYear, onDataLoaded: (List<DailyListenData>) -> Unit) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                val dailyData = analyticsRepository.getDailyListenTimeForMonth(
                    userId = userId,
                    year = monthYear.year,
                    month = monthYear.month
                )
                onDataLoaded(dailyData)
                Log.d("SoundCapsuleViewModel", "Loaded daily data for ${monthYear.getDisplayName()}: ${dailyData.size} days")
            } catch (e: Exception) {
                Log.e("SoundCapsuleViewModel", "Error loading daily data: ${e.message}")
                onDataLoaded(emptyList())
            }
        }
    }

    fun exportCompleteAnalytics(context: Context) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }

            try {
                val completeExportData = analyticsRepository.getAllAnalyticsForExport(userId)
                exportCompleteToCsv(context, completeExportData)

            } catch (e: Exception) {
                Log.e("SoundCapsuleViewModel", "Error exporting complete analytics: ${e.message}")
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun exportCompleteToCsv(context: Context, exportData: com.example.purrytify.repository.CompleteAnalyticsExportData) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fileName = "purrytify_complete_sound_capsule_${dateFormat.format(exportData.exportDate)}.csv"

            // Save to both internal app directory and Downloads folder
            val internalFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            val downloadsFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

            val csvContent = generateCsvContent(exportData)

            // Write to internal storage
            FileWriter(internalFile).use { writer ->
                writer.append(csvContent)
            }

            // Write to Downloads folder (if possible)
            try {
                FileWriter(downloadsFile).use { writer ->
                    writer.append(csvContent)
                }
                Toast.makeText(context, "CSV exported to Downloads and shared successfully", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.w("SoundCapsuleViewModel", "Could not write to Downloads folder: ${e.message}")
                Toast.makeText(context, "CSV exported and shared successfully", Toast.LENGTH_SHORT).show()
            }

            shareFile(context, internalFile, "text/csv")

        } catch (e: Exception) {
            Log.e("SoundCapsuleViewModel", "Complete CSV export error: ${e.message}")
            throw e
        }
    }

    private fun generateCsvContent(exportData: com.example.purrytify.repository.CompleteAnalyticsExportData): String {
        val content = StringBuilder()

        // Write header
        content.append("PURRYTIFY COMPLETE SOUND CAPSULE ANALYTICS\n")
        content.append("Export Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(exportData.exportDate)}\n")
        content.append("User ID: ${exportData.userId}\n")
        content.append("=" + "=".repeat(50) + "\n\n")

        // Overall Statistics
        content.append("OVERALL STATISTICS\n")
        content.append("Total Listen Time: ${exportData.totalListenTimeFormatted}\n")
        content.append("Average Monthly Listen Time: ${analyticsRepository.formatMinutesToReadable(exportData.averageMonthlyListenTime)}\n")
        content.append("Total Unique Artists: ${exportData.totalUniqueArtists}\n")
        content.append("Total Unique Songs: ${exportData.totalUniqueSongs}\n")
        content.append("Total Months with Data: ${exportData.monthlyBreakdown.size}\n\n")

        // Overall Top Artists
        content.append("OVERALL TOP ARTISTS\n")
        content.append("Rank,Artist,Total Play Count,Total Listen Time\n")
        exportData.overallTopArtists.forEachIndexed { index, artist ->
            content.append("${index + 1},\"${artist.artist}\",${artist.playCount},${analyticsRepository.formatDurationForExport(artist.totalListenTime)}\n")
        }
        content.append("\n")

        // Overall Top Songs
        content.append("OVERALL TOP SONGS\n")
        content.append("Rank,Title,Artist,Total Play Count,Total Listen Time\n")
        exportData.overallTopSongs.forEachIndexed { index, song ->
            content.append("${index + 1},\"${song.title}\",\"${song.artist}\",${song.playCount},${analyticsRepository.formatDurationForExport(song.totalListenTime)}\n")
        }
        content.append("\n")

        // Overall Day Streak Songs
        if (exportData.overallDayStreakSongs.isNotEmpty()) {
            content.append("OVERALL DAY STREAK SONGS\n")
            content.append("Song,Artist,Consecutive Days,Start Date,End Date\n")
            exportData.overallDayStreakSongs.forEach { streak ->
                content.append("\"${streak.title}\",\"${streak.artist}\",${streak.consecutiveDays},${streak.startDate},${streak.endDate}\n")
            }
            content.append("\n")
        }

        // Monthly Breakdown
        content.append("MONTHLY BREAKDOWN\n")
        content.append("=" + "=".repeat(30) + "\n\n")

        exportData.monthlyBreakdown.forEach { monthData ->
            val monthName = monthData.monthYear.getDisplayName()
            content.append("$monthName\n")
            content.append("-".repeat(monthName.length) + "\n")

            content.append("Total Listen Time: ${analyticsRepository.formatMinutesToReadable(monthData.analytics.totalListenTimeMinutes)}\n")
            content.append("Daily Average: ${String.format("%.1f", monthData.analytics.dailyAverageMinutes)} minutes\n")
            content.append("Unique Artists: ${monthData.analytics.uniqueArtistsCount}\n")
            content.append("Unique Songs: ${monthData.analytics.uniqueSongsCount}\n\n")

            // Monthly Top Artists
            if (monthData.analytics.topArtists.isNotEmpty()) {
                content.append("Top Artists:\n")
                monthData.analytics.topArtists.take(5).forEachIndexed { index, artist ->
                    content.append("${index + 1}. ${artist.artist} (${artist.playCount} plays)\n")
                }
                content.append("\n")
            }

            // Monthly Top Songs
            if (monthData.analytics.topSongs.isNotEmpty()) {
                content.append("Top Songs:\n")
                monthData.analytics.topSongs.take(5).forEachIndexed { index, song ->
                    content.append("${index + 1}. ${song.title} by ${song.artist} (${song.playCount} plays)\n")
                }
                content.append("\n")
            }

            // Daily Listen Data
            content.append("Daily Listen Data:\n")
            content.append("Day,Date,Listen Time (minutes)\n")
            monthData.dailyData.forEach { daily ->
                content.append("${daily.day},${daily.dateString},${daily.listenTimeMinutes}\n")
            }
            content.append("\n" + "=".repeat(50) + "\n\n")
        }

        return content.toString()
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

    fun shareMonthAsImage(context: Context, monthYear: MonthYear) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }

            try {
                val analytics = analyticsRepository.getMonthlyAnalytics(userId, monthYear.year, monthYear.month)
                val imageFile = generateSoundCapsuleImage(context, monthYear, analytics)
                shareImageFile(context, imageFile)
            } catch (e: Exception) {
                Log.e("SoundCapsuleViewModel", "Error sharing image: ${e.message}")
                Toast.makeText(context, "Failed to create share image", Toast.LENGTH_SHORT).show()
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    private suspend fun generateSoundCapsuleImage(
        context: Context,
        monthYear: MonthYear,
        analytics: MonthlyAnalytics
    ): File = withContext(Dispatchers.IO) {
        val width = 1080
        val height = 1920

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val backgroundPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    DarkBlack.toArgb(),
                    DarkBlue.toArgb(),
                    DarkBlack.toArgb()
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // App branding
        val brandPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("🎵 Purrytify", 80f, 120f, brandPaint)

        // Month and year
        val monthPaint = Paint().apply {
            color = Color.WHITE
            textSize = 72f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("My ${monthYear.getDisplayName()}", 80f, 220f, monthPaint)

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#1DB954") // SpotifyGreen
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("Sound Capsule", 80f, 300f, subtitlePaint)

        // Main listening time - large and prominent
        val mainTimePaint = Paint().apply {
            color = Color.parseColor("#1DB954")
            textSize = 120f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val timeText = formatMinutesToReadable(analytics.totalListenTimeMinutes)
        canvas.drawText(timeText, 80f, 480f, mainTimePaint)

        val timeLabelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            isAntiAlias = true
        }
        canvas.drawText("Time listened", 80f, 540f, timeLabelPaint)

        // Stats section
        var yPosition = 680f

        // Top Artists section
        if (analytics.topArtists.isNotEmpty()) {
            val sectionPaint = Paint().apply {
                color = Color.WHITE
                textSize = 56f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText("Top artists", 80f, yPosition, sectionPaint)
            yPosition += 80f

            val artistPaint = Paint().apply {
                color = Color.parseColor("#B3B3B3")
                textSize = 42f
                isAntiAlias = true
            }

            analytics.topArtists.take(5).forEachIndexed { index, artist ->
                val rankText = "${index + 1} ${artist.artist}"
                canvas.drawText(rankText, 120f, yPosition, artistPaint)
                yPosition += 60f
            }
            yPosition += 40f
        }

        // Top Songs section
        if (analytics.topSongs.isNotEmpty()) {
            val sectionPaint = Paint().apply {
                color = Color.WHITE
                textSize = 56f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            canvas.drawText("Top songs", 80f, yPosition, sectionPaint)
            yPosition += 80f

            val songPaint = Paint().apply {
                color = Color.parseColor("#B3B3B3")
                textSize = 42f
                isAntiAlias = true
            }

            analytics.topSongs.take(5).forEachIndexed { index, song ->
                val songText = "${index + 1} ${song.title}"
                canvas.drawText(songText, 120f, yPosition, songPaint)

                val artistText = "   ${song.artist}"
                canvas.drawText(artistText, 120f, yPosition + 45f, Paint().apply {
                    color = Color.parseColor("#808080")
                    textSize = 36f
                    isAntiAlias = true
                })
                yPosition += 100f
            }
            yPosition += 40f
        }

        // Additional stats
        val statsPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            isAntiAlias = true
        }

        canvas.drawText("${analytics.uniqueArtistsCount} artists discovered", 80f, yPosition, statsPaint)
        yPosition += 60f
        canvas.drawText("${analytics.uniqueSongsCount} songs played", 80f, yPosition, statsPaint)
        yPosition += 60f

        val avgText = "Daily average: ${String.format("%.0f", analytics.dailyAverageMinutes)} min"
        canvas.drawText(avgText, 80f, yPosition, statsPaint)

        // Footer
        val datePaint = Paint().apply {
            color = Color.parseColor("#808080")
            textSize = 32f
            isAntiAlias = true
        }
        val dateText = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(dateText, width - 400f, height - 80f, datePaint)

        // Save to file
        val fileName = "purrytify_${monthYear.year}_${monthYear.month}_sound_capsule.png"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        bitmap.recycle()
        return@withContext file
    }

    private fun shareImageFile(context: Context, imageFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Check out my music listening stats from Purrytify! 🎵")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Your Sound Capsule"))
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