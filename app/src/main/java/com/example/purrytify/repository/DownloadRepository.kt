package com.example.purrytify.repository

import android.util.Log
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.model.Song
import com.example.purrytify.services.SongResponse
import java.util.Date

class DownloadRepository(
    private val songRepository: SongRepository,
    private val songDao: SongDao
) {

    suspend fun isServerSongDownloadedByUser(userId: Int, title: String, artist: String): Boolean {
        return try {
            songDao.isServerSongDownloaded(userId, title, artist)
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error checking if song is downloaded: ${e.message}")
            false
        }
    }

    suspend fun getServerSongByTitleAndArtist(userId: Int, title: String, artist: String): SongEntity? {
        return try {
            songDao.getServerSongByTitleAndArtist(userId, title, artist)
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error getting server song: ${e.message}")
            null
        }
    }

    suspend fun saveDownloadedSong(
        userId: Int,
        serverSong: SongResponse,
        localFilePath: String,
        localArtworkPath: String?
    ): Long {
        if (isServerSongDownloadedByUser(userId, serverSong.title, serverSong.artist)) {
            Log.w("DownloadRepository", "Song already exists: ${serverSong.title} by ${serverSong.artist}")
            // Return the existing song's ID instead of creating duplicate
            val existingSong = getServerSongByTitleAndArtist(userId, serverSong.title, serverSong.artist)
            return existingSong?.id ?: -1L
        }

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

    suspend fun saveDownloadedSong(
        userId: Int,
        serverSong: Song,
        localFilePath: String,
        localArtworkPath: String?
    ): Long {
        // Double-check before saving to prevent race conditions
        if (isServerSongDownloadedByUser(userId, serverSong.title, serverSong.artist)) {
            Log.w("DownloadRepository", "Song already exists: ${serverSong.title} by ${serverSong.artist}")
            // Return the existing song's ID instead of creating duplicate
            val existingSong = getServerSongByTitleAndArtist(userId, serverSong.title, serverSong.artist)
            return existingSong?.id ?: -1L
        }

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

    suspend fun filterUndownloadedSongs(userId: Int, serverSongs: List<Song>): List<Song> {
        val undownloadedSongs = mutableListOf<Song>()

        for (serverSong in serverSongs) {
            if (!isServerSongDownloadedByUser(userId, serverSong.title, serverSong.artist)) {
                undownloadedSongs.add(serverSong)
            } else {
                Log.d("DownloadRepository", "Skipping already downloaded song: ${serverSong.title} by ${serverSong.artist}")
            }
        }

        Log.d("DownloadRepository", "Filtered ${serverSongs.size} songs to ${undownloadedSongs.size} undownloaded songs")
        return undownloadedSongs
    }

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
}