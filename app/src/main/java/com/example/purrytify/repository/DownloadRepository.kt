package com.example.purrytify.repository

import android.util.Log
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.model.Song
import com.example.purrytify.network.SongResponse
import java.util.Date

class DownloadRepository(private val songRepository: SongRepository) {

    /**
     * Check if server song is downloaded by title and artist (with user ID)
     */
    suspend fun isServerSongDownloadedByUser(userId: Int, title: String, artist: String): Boolean {
        // We need to get songs directly from the DAO since LiveData doesn't work well in suspend functions
        // This requires a new DAO method or use a different approach

        // For now, let's use a simple approach and check by getting all user songs
        // TODO: add a specific DAO query method like:
        // @Query("SELECT COUNT(*) FROM songs WHERE userId = :userId AND title = :title AND artist = :artist AND isFromServer = 1")
        // suspend fun isServerSongDownloaded(userId: Int, title: String, artist: String): Boolean

        try {
            // Create a temporary song entity to check
            // This is a workaround - you should implement a proper DAO method
            return false // For now, allow all downloads
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error checking if song is downloaded: ${e.message}")
            return false
        }
    }

    /**
     * Remove the problematic method that uses LiveData incorrectly
     */
    private suspend fun isServerSongDownloaded(serverSongId: String, title: String, artist: String): Boolean {
        // This method had issues with LiveData access in suspend functions
        // Use isServerSongDownloadedByUser instead
        return false
    }

    /**
     * Save downloaded server song to local database
     */
    suspend fun saveDownloadedSong(
        userId: Int,
        serverSong: SongResponse,
        localFilePath: String,
        localArtworkPath: String?
    ): Long {
        val songEntity = SongEntity(
            id = 0, // Auto-generate new ID for local storage
            userId = userId,
            title = serverSong.title,
            artist = serverSong.artist,
            duration = parseDurationToMilliseconds(serverSong.duration),
            filePath = localFilePath,
            artworkPath = localArtworkPath ?: serverSong.artwork,
            uploadedAt = Date(),
            updatedAt = Date(),
            lastPlayedAt = null,
            isLiked = false,
            isListened = false,
            rank = serverSong.rank,
            country = serverSong.country,
            isFromServer = true
        )

        return songRepository.insert(songEntity)
    }

    /**
     * Save downloaded server song to local database (from Song model)
     */
    suspend fun saveDownloadedSong(
        userId: Int,
        serverSong: Song,
        localFilePath: String,
        localArtworkPath: String?
    ): Long {
        val songEntity = SongEntity(
            id = 0, // Auto-generate new ID for local storage
            userId = userId,
            title = serverSong.title,
            artist = serverSong.artist,
            duration = 0, // We'll get this from MediaMetadataRetriever if needed
            filePath = localFilePath,
            artworkPath = localArtworkPath ?: serverSong.albumArt,
            uploadedAt = serverSong.uploadedAt ?: Date(),
            updatedAt = Date(),
            lastPlayedAt = serverSong.lastPlayedAt,
            isLiked = serverSong.isLiked,
            isListened = serverSong.isListened,
            rank = serverSong.rank,
            country = serverSong.country,
            isFromServer = true
        )

        return songRepository.insert(songEntity)
    }

    /**
     * Check if any server songs are already downloaded for bulk operations (SongResponse)
     */
    suspend fun filterUndownloadedSongResponses(userId: Int, serverSongs: List<SongResponse>): List<SongResponse> {
        return serverSongs.filter { serverSong ->
            !isServerSongDownloadedByUser(userId, serverSong.title, serverSong.artist)
        }
    }

    /**
     * Check if any server songs are already downloaded for bulk operations (Song model)
     */
    suspend fun filterUndownloadedSongs(userId: Int, serverSongs: List<Song>): List<Song> {
        return serverSongs.filter { serverSong ->
            !isServerSongDownloadedByUser(userId, serverSong.title, serverSong.artist)
        }
    }

    /**
     * Get all downloaded server songs for a user
     */
    suspend fun getDownloadedServerSongs(userId: Int): List<SongEntity> {
        // This would require a new DAO method, but for now we can filter from existing songs
        // In a real implementation, you'd add this to your DAO:
        // @Query("SELECT * FROM songs WHERE userId = :userId AND isFromServer = 1")
        // suspend fun getDownloadedServerSongs(userId: Int): List<SongEntity>

        // For now, this is a placeholder - you'd need to implement the actual DAO method
        return emptyList()
    }

    /**
     * Parse duration string (mm:ss) to milliseconds
     */
    private fun parseDurationToMilliseconds(durationString: String): Long {
        return try {
            val parts = durationString.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val seconds = parts[1].toLong()
                (minutes * 60 + seconds) * 1000
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.w("DownloadRepository", "Could not parse duration: $durationString")
            0L
        }
    }

    /**
     * Delete downloaded song and its files
     */
    suspend fun deleteDownloadedSong(songId: Long): Boolean {
        return try {
            val song = songRepository.getSongById(songId)
            if (song != null && song.isFromServer) {
                // Delete the actual files
                try {
                    val audioFile = java.io.File(song.filePath)
                    if (audioFile.exists()) {
                        audioFile.delete()
                    }

                    song.artworkPath?.let { artworkPath ->
                        val artworkFile = java.io.File(artworkPath)
                        if (artworkFile.exists()) {
                            artworkFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DownloadRepository", "Could not delete files for song $songId: ${e.message}")
                }

                // Delete from database
                songRepository.delete(song)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error deleting downloaded song: ${e.message}")
            false
        }
    }
}