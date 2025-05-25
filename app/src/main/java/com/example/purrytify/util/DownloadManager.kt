package com.example.purrytify.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

data class DownloadProgress(
    val songId: String,
    val progress: Int,
    val isDownloading: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)

class DownloadManager private constructor(private val context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val client = OkHttpClient()
    private val downloadProgresses = ConcurrentHashMap<String, DownloadProgress>()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadProgress>> = _downloadStates.asStateFlow()

    private val activeDownloads = ConcurrentHashMap<String, Call>()

    fun startDownload(
        songId: String,
        title: String,
        artist: String,
        downloadUrl: String,
        artworkUrl: String,
        onComplete: (localFilePath: String, localArtworkPath: String?) -> Unit,
        onError: (error: String) -> Unit
    ) {
        // Check if already downloading
        if (activeDownloads.containsKey(songId)) {
            Log.d("DownloadManager", "Song $songId is already being downloaded")
            return
        }

        // Update progress state
        updateProgress(songId, DownloadProgress(songId, 0, true))

        // Create download directories
        val musicDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Downloaded")
        val artworkDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Artwork")

        if (!musicDir.exists()) musicDir.mkdirs()
        if (!artworkDir.exists()) artworkDir.mkdirs()

        // Sanitize filename
        val sanitizedTitle = sanitizeFileName(title)
        val sanitizedArtist = sanitizeFileName(artist)
        val audioFileName = "${sanitizedArtist} - ${sanitizedTitle}.mp3"
        val artworkFileName = "${sanitizedArtist} - ${sanitizedTitle}.jpg"

        val audioFile = File(musicDir, audioFileName)
        val artworkFile = File(artworkDir, artworkFileName)

        // Start audio download
        downloadFile(
            songId = songId,
            url = downloadUrl,
            outputFile = audioFile,
            onProgress = { progress ->
                updateProgress(songId, DownloadProgress(songId, progress, true))
            },
            onComplete = { audioPath ->
                // Audio downloaded, now download artwork
                if (artworkUrl.isNotEmpty()) {
                    downloadArtwork(
                        songId = songId,
                        artworkUrl = artworkUrl,
                        artworkFile = artworkFile,
                        onArtworkComplete = { artworkPath ->
                            updateProgress(songId, DownloadProgress(songId, 100, false, true))
                            onComplete(audioPath, artworkPath)
                            activeDownloads.remove(songId)
                        },
                        onArtworkError = {
                            // Audio downloaded but artwork failed - still consider success
                            updateProgress(songId, DownloadProgress(songId, 100, false, true))
                            onComplete(audioPath, null)
                            activeDownloads.remove(songId)
                        }
                    )
                } else {
                    updateProgress(songId, DownloadProgress(songId, 100, false, true))
                    onComplete(audioPath, null)
                    activeDownloads.remove(songId)
                }
            },
            onError = { error ->
                updateProgress(songId, DownloadProgress(songId, 0, false, false, error))
                onError(error)
                activeDownloads.remove(songId)
            }
        )
    }

    private fun downloadFile(
        songId: String,
        url: String,
        outputFile: File,
        onProgress: (Int) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)

        activeDownloads[songId] = call

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) {
                    Log.e("DownloadManager", "Download failed for $songId: ${e.message}")
                    onError("Download failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("Download failed: HTTP ${response.code}")
                    return
                }

                try {
                    val responseBody = response.body ?: throw IOException("Empty response body")
                    val contentLength = responseBody.contentLength()

                    responseBody.byteStream().use { inputStream ->
                        FileOutputStream(outputFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var totalBytesRead = 0L
                            var bytesRead: Int

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                if (call.isCanceled()) {
                                    outputFile.delete()
                                    return
                                }

                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead

                                if (contentLength > 0) {
                                    val progress = ((totalBytesRead * 90) / contentLength).toInt()
                                    onProgress(progress)
                                }
                            }
                        }
                    }

                    if (!call.isCanceled()) {
                        onComplete(outputFile.absolutePath)
                    }

                } catch (e: IOException) {
                    if (!call.isCanceled()) {
                        Log.e("DownloadManager", "Error writing file for $songId: ${e.message}")
                        outputFile.delete()
                        onError("Error saving file: ${e.message}")
                    }
                }
            }
        })
    }

    private fun downloadArtwork(
        songId: String,
        artworkUrl: String,
        artworkFile: File,
        onArtworkComplete: (String) -> Unit,
        onArtworkError: () -> Unit
    ) {
        val request = Request.Builder().url(artworkUrl).build()
        val call = client.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w("DownloadManager", "Artwork download failed for $songId: ${e.message}")
                onArtworkError()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.w("DownloadManager", "Artwork download failed for $songId: HTTP ${response.code}")
                    onArtworkError()
                    return
                }

                try {
                    val responseBody = response.body ?: throw IOException("Empty response body")

                    responseBody.byteStream().use { inputStream ->
                        FileOutputStream(artworkFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    updateProgress(songId, DownloadProgress(songId, 95, true))
                    onArtworkComplete(artworkFile.absolutePath)

                } catch (e: IOException) {
                    Log.w("DownloadManager", "Error saving artwork for $songId: ${e.message}")
                    artworkFile.delete()
                    onArtworkError()
                }
            }
        })
    }

    fun cancelDownload(songId: String) {
        activeDownloads[songId]?.cancel()
        activeDownloads.remove(songId)
        downloadProgresses.remove(songId)
        updateProgressStates()

        Log.d("DownloadManager", "Download cancelled for song: $songId")
    }

    fun isDownloading(songId: String): Boolean {
        return activeDownloads.containsKey(songId)
    }

    fun getDownloadProgress(songId: String): DownloadProgress? {
        return downloadProgresses[songId]
    }

    private fun updateProgress(songId: String, progress: DownloadProgress) {
        downloadProgresses[songId] = progress
        updateProgressStates()
    }

    private fun updateProgressStates() {
        _downloadStates.value = downloadProgresses.toMap()
    }

    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(50) // Limit length
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}