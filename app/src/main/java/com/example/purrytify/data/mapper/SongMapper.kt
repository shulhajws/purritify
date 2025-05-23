package com.example.purrytify.data.mapper

import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.model.Song
import java.util.Date

object SongMapper {
    fun toSong(entity: SongEntity): Song {
        return Song(
            id = entity.id.toString(),
            title = entity.title,
            artist = entity.artist,
            albumArt = entity.artworkPath ?: "https://picsum.photos/200/200?random=${entity.id/3}",
            audioUrl = entity.filePath,
            isLiked = entity.isLiked,
            isListened = entity.isListened,
            uploadedAt = entity.uploadedAt,
            updatedAt = entity.updatedAt,
            lastPlayedAt = entity.lastPlayedAt,
            rank = entity.rank,
            country = entity.country,
            isPlaying = false,
            isFromServer = entity.isFromServer
        )
    }

    fun toSongEntity(song: Song): SongEntity {
        return SongEntity(
            id = song.id.toLongOrNull() ?: 0,
            userId = 0, // Set proper userId IF NEEDED
            title = song.title,
            artist = song.artist,
            duration = 0, // Set proper duration IF NEEDED
            filePath = song.audioUrl,
            artworkPath = song.albumArt,
            uploadedAt = Date(System.currentTimeMillis()),
            updatedAt = song.updatedAt,
            lastPlayedAt = null,
            isLiked = false,
            isListened = false,
            rank = song.rank,
            country = song.country,
            isFromServer = song.isFromServer
        )
    }

    fun toSongList(entities: List<SongEntity>): List<Song> {
        return entities.map { toSong(it) }
    }
}