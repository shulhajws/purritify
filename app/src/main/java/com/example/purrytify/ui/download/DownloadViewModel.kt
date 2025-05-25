package com.example.purrytify.ui.download

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.model.Song
import com.example.purrytify.network.SongResponse
import com.example.purrytify.repository.DownloadRepository
import com.example.purrytify.repository.SongRepository
import com.example.purrytify.util.DownloadManager
import com.example.purrytify.util.DownloadProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        downloadRepository = DownloadRepository(songRepository)

        // Observe download manager states
        viewModelScope.launch {
            downloadManager.downloadStates.collect { progressMap ->
                updateDownloadState(progressMap)
            }
        }
    }

    fun updateForUser(userId: Int) {
        currentUserId = userId
    }

    /**
     * Download a single song from server
     */
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

    /**
     * Download a single song from SongResponse
     */
    fun downloadSongResponse(serverSong: SongResponse) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            try {
                // Check if already downloaded
                if (downloadRepository.isServerSongDownloadedByUser(userId, serverSong.title, serverSong.artist)) {
                    downloadManager.showToast("Song already downloaded: ${serverSong.title}")
                    return@launch
                }

                // Check if already downloading
                if (downloadManager.isDownloading(serverSong.id.toString())) {
                    downloadManager.showToast("Song is already downloading: ${serverSong.title}")
                    return@launch
                }

                downloadManager.showToast("Starting download: ${serverSong.title}")

                downloadManager.startDownload(
                    songId = serverSong.id.toString(),
                    title = serverSong.title,
                    artist = serverSong.artist,
                    downloadUrl = serverSong.url,
                    artworkUrl = serverSong.artwork,
                    onComplete = { localFilePath, localArtworkPath ->
                        viewModelScope.launch {
                            try {
                                downloadRepository.saveDownloadedSong(
                                    userId = userId,
                                    serverSong = serverSong,
                                    localFilePath = localFilePath,
                                    localArtworkPath = localArtworkPath
                                )
                                downloadManager.showToast("Downloaded: ${serverSong.title}")
                                Log.d("DownloadViewModel", "Successfully downloaded and saved: ${serverSong.title}")
                            } catch (e: Exception) {
                                Log.e("DownloadViewModel", "Error saving downloaded song: ${e.message}")
                                downloadManager.showToast("Download completed but failed to save: ${serverSong.title}")
                            }
                        }
                    },
                    onError = { error ->
                        Log.e("DownloadViewModel", "Download failed for ${serverSong.title}: $error")
                        downloadManager.showToast("Download failed: ${serverSong.title}")
                    }
                )

            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error starting download: ${e.message}")
                downloadManager.showToast("Failed to start download: ${serverSong.title}")
            }
        }
    }

    /**
     * Download multiple songs (bulk download)
     */
    fun downloadSongs(songs: List<Song>) {
        val userId = currentUserId ?: return

        if (_downloadState.value.isBulkDownloading) {
            downloadManager.showToast("Bulk download already in progress")
            return
        }

        viewModelScope.launch {
            try {
                // Filter out already downloaded songs
                val undownloadedSongs = downloadRepository.filterUndownloadedSongs(userId, songs)

                if (undownloadedSongs.isEmpty()) {
                    downloadManager.showToast("All songs are already downloaded")
                    return@launch
                }

                _downloadState.value = _downloadState.value.copy(isBulkDownloading = true)
                downloadManager.showToast("Starting bulk download: ${undownloadedSongs.size} songs")

                undownloadedSongs.forEach { song ->
                    // Add small delay between downloads to avoid overwhelming the server
                    kotlinx.coroutines.delay(200)

                    if (!downloadManager.isDownloading(song.id)) {
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
                                        Log.e("DownloadViewModel", "Error saving bulk downloaded song: ${e.message}")
                                    }
                                }
                            },
                            onError = { error ->
                                Log.e("DownloadViewModel", "Bulk download failed for ${song.title}: $error")
                            }
                        )
                    }
                }

                // Reset bulk downloading state after a delay
                kotlinx.coroutines.delay(2000)
                _downloadState.value = _downloadState.value.copy(isBulkDownloading = false)

            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error in bulk download: ${e.message}")
                _downloadState.value = _downloadState.value.copy(isBulkDownloading = false)
                downloadManager.showToast("Bulk download failed")
            }
        }
    }

    /**
     * Cancel a specific download
     */
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

    /**
     * Check if a song is currently downloading
     */
    fun isSongDownloading(songId: String): Boolean {
        return downloadManager.isDownloading(songId)
    }

    /**
     * Get download progress for a specific song
     */
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