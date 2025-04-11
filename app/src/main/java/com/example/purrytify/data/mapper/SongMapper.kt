package com.example.purrytify.data.mapper

import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.model.Song

object SongMapper {
    fun toSong(entity: SongEntity): Song {
        return Song(
            id = entity.id.toString(),
            title = entity.title,
            artist = entity.artist,
            albumArt = entity.artworkPath ?: "https://picsum.photos/200/200?random=${entity.id}", // Fallback if no artwork
            audioUrl = entity.filePath,
            isPlaying = false
        )
    }

    fun toSongEntity(song: Song): SongEntity {
        return SongEntity(
            id = song.id.toLongOrNull() ?: 0,
            title = song.title,
            artist = song.artist,
            duration = 0, // You'll need to set this appropriately
            filePath = song.audioUrl,
            artworkPath = song.albumArt
        )
    }

    fun toSongList(entities: List<SongEntity>): List<Song> {
        return entities.map { toSong(it) }
    }
}