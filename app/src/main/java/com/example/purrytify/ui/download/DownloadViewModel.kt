package com.example.purrytify.ui.download

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.Song
import com.example.purrytify.services.SongResponse
import com.example.purrytify.repository.DownloadRepository
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.util.DownloadManager
import com.example.purrytify.util.DownloadProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class DownloadState(
    val downloadingCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val downloadProgress: Map<String, DownloadProgress> = emptyMap(),
    val isBulkDownloading: Boolean = false
)

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = DownloadManager.getInstance(application)
    private val downloadRepository: DownloadRepository

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private var currentUserId: Int? = null

    init {
        val database = AppDatabase.getDatabase(application)
        val songRepository = SongRepository(database.songDao())
        downloadRepository = DownloadRepository(songRepository, database.songDao())

        viewModelScope.launch {
            downloadManager.downloadStates.collect { progressMap ->
                updateDownloadState(progressMap)
            }
        }
    }

    fun updateForUser(userId: Int) {
        currentUserId = userId
    }

    fun downloadSong(song: Song) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                // Check if already downloaded
                if (downloadRepository.isServerSongDownloadedByUser(userId, song.title, song.artist)) {
                    downloadManager.showToast("Song already downloaded: ${song.title}")
                    return@launch
                }

                // Check if already downloading
                if (downloadManager.isDownloading(song.id)) {
                    downloadManager.showToast("Song is already downloading: ${song.title}")
                    return@launch
                }

                downloadManager.showToast("Starting download: ${song.title}")

                downloadManager.startDownload(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    downloadUrl = song.audioUrl,
                    artworkUrl = song.albumArt,
                    onComplete = { localFilePath, localArtworkPath ->
                        viewModelScope.launch {
                            try {
                                downloadRepository.saveDownloadedSong(
                                    userId = userId,
                                    serverSong = song,
                                    localFilePath = localFilePath,
                                    localArtworkPath = localArtworkPath
                                )
                                downloadManager.showToast("Downloaded: ${song.title}")
                                Log.d("DownloadViewModel", "Successfully downloaded and saved: ${song.title}")
                            } catch (e: Exception) {
                                Log.e("DownloadViewModel", "Error saving downloaded song: ${e.message}")
                                downloadManager.showToast("Download completed but failed to save: ${song.title}")
                            }
                        }
                    },
                    onError = { error ->
                        Log.e("DownloadViewModel", "Download failed for ${song.title}: $error")
                        downloadManager.showToast("Download failed: ${song.title}")
                    }
                )

            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error starting download: ${e.message}")
                downloadManager.showToast("Failed to start download: ${song.title}")
            }
        }
    }

    fun downloadSongs(songs: List<Song>) {
        val userId = currentUserId ?: return

        if (_downloadState.value.isBulkDownloading) {
            downloadManager.showToast("Bulk download already in progress")
            return
        }

        viewModelScope.launch {
            try {
                val undownloadedSongs = downloadRepository.filterUndownloadedSongs(userId, songs)

                if (undownloadedSongs.isEmpty()) {
                    downloadManager.showToast("All songs are already downloaded")
                    return@launch
                }

                _downloadState.value = _downloadState.value.copy(isBulkDownloading = true)
                downloadManager.showToast("Starting bulk download: ${undownloadedSongs.size} songs")

                coroutineScope {
                    val jobs = undownloadedSongs.map { song ->
                        async {
                            kotlinx.coroutines.delay(500)

                            if (!downloadManager.isDownloading(song.id)) {
                                suspendCancellableCoroutine<Unit> { cont ->
                                    downloadManager.startDownload(
                                        songId = song.id,
                                        title = song.title,
                                        artist = song.artist,
                                        downloadUrl = song.audioUrl,
                                        artworkUrl = song.albumArt,
                                        onComplete = { localFilePath, localArtworkPath ->
                                            viewModelScope.launch {
                                                try {
                                                    downloadRepository.saveDownloadedSong(
                                                        userId = userId,
                                                        serverSong = song,
                                                        localFilePath = localFilePath,
                                                        localArtworkPath = localArtworkPath
                                                    )
                                                    Log.d("DownloadViewModel", "Bulk download completed: ${song.title}")
                                                } catch (e: Exception) {
                                                    Log.e("DownloadViewModel", "Error saving song: ${e.message}")
                                                } finally {
                                                    cont.resume(Unit)
                                                }
                                            }
                                        },
                                        onError = {
                                            Log.e("DownloadViewModel", "Bulk download failed for ${song.title}: $it")
                                            cont.resume(Unit)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    jobs.awaitAll()
                }
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error in bulk download: ${e.message}")
                downloadManager.showToast("Bulk download failed")
            } finally {
                _downloadState.value = _downloadState.value.copy(isBulkDownloading = false)
            }
        }
    }

    fun cancelDownload(songId: String) {
        downloadManager.cancelDownload(songId)
        downloadManager.showToast("Download cancelled")
    }

    /**
     * Check if a song is already downloaded
     */
    suspend fun isSongDownloaded(title: String, artist: String): Boolean {
        val userId = currentUserId ?: return false
        return downloadRepository.isServerSongDownloadedByUser(userId, title, artist)
    }

    fun isSongDownloading(songId: String): Boolean {
        return downloadManager.isDownloading(songId)
    }

    fun getDownloadProgress(songId: String): DownloadProgress? {
        return downloadManager.getDownloadProgress(songId)
    }

    private fun updateDownloadState(progressMap: Map<String, DownloadProgress>) {
        val downloadingCount = progressMap.values.count { it.isDownloading }
        val completedCount = progressMap.values.count { it.isCompleted }
        val failedCount = progressMap.values.count { it.error != null }

        _downloadState.value = _downloadState.value.copy(
            downloadingCount = downloadingCount,
            completedCount = completedCount,
            failedCount = failedCount,
            downloadProgress = progressMap
        )
    }
}