package com.example.purrytify.repository

import android.os.Parcelable
import android.util.Log
import com.example.purrytify.data.dao.SongDao
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.data.mapper.SongMapper
import com.example.purrytify.model.Song
import com.example.purrytify.services.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class RecommendationPlaylist(
    val id: String,
    val title: String,
    val description: String,
    val songs: List<Song>,
    val type: PlaylistType
) : Parcelable

@Parcelize
enum class PlaylistType : Parcelable {
    DAILY_MIX,
    LIKED_BASED,
    ARTIST_BASED,
    RECENTLY_PLAYED,
}

class RecommendationRepository(
    private val songDao: SongDao
) {

    suspend fun generateRecommendations(userId: Int, userLocation: String): List<RecommendationPlaylist> = withContext(Dispatchers.IO) {
        val recommendations = mutableListOf<RecommendationPlaylist>()

        try {
            coroutineScope {
                val userSongsDeferred = async { songDao.getSongsByUserId(userId) }
                val likedSongsDeferred = async { songDao.getLikedSongsByUserId(userId) }
                val recentSongsDeferred = async { songDao.getRecentlyPlayedSongsByUserId(userId) }
                val globalSongsDeferred = async { fetchGlobalSongs() }
                val countrySongsDeferred = async { fetchCountrySongs(userLocation) }

                val userSongs = userSongsDeferred.await().value ?: emptyList()
                val likedSongs = likedSongsDeferred.await().value ?: emptyList()
                val recentSongs = recentSongsDeferred.await().value ?: emptyList()
                val globalSongs = globalSongsDeferred.await()
                val countrySongs = countrySongsDeferred.await()

                recommendations.addAll(generateDailyMix(userSongs, globalSongs, countrySongs))
                recommendations.addAll(generateLikedBasedRecommendations(likedSongs, globalSongs, countrySongs))
                recommendations.addAll(generateArtistBasedRecommendations(userSongs, globalSongs, countrySongs))
                recommendations.addAll(generateRecentlyPlayedMix(recentSongs, globalSongs))
            }
            Log.d("RecommendationRepository", "Generated ${recommendations.size} recommendations")
        } catch (e: Exception) {
            Log.e("RecommendationRepository", "Error generating recommendations: ${e.message}")
        }

        return@withContext recommendations.take(4)
    }

    private suspend fun fetchGlobalSongs(): List<Song> {
        return try {
            val response = RetrofitClient.instance.getTopGlobalSongs()
            response.map { songResponse ->
                Song(
                    id = songResponse.id.toString(),
                    title = songResponse.title,
                    artist = songResponse.artist,
                    albumArt = songResponse.artwork,
                    audioUrl = songResponse.url,
                    isLiked = false,
                    isListened = false,
                    uploadedAt = null,
                    rank = songResponse.rank,
                    country = songResponse.country,
                    isFromServer = true
                )
            }
        } catch (e: Exception) {
            Log.e("RecommendationRepository", "Error fetching global songs: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchCountrySongs(countryCode: String): List<Song> {
        return try {
            val response = RetrofitClient.instance.getTopCountrySongs(countryCode)
            response.map { songResponse ->
                Song(
                    id = songResponse.id.toString(),
                    title = songResponse.title,
                    artist = songResponse.artist,
                    albumArt = songResponse.artwork,
                    audioUrl = songResponse.url,
                    isLiked = false,
                    isListened = false,
                    uploadedAt = null,
                    rank = songResponse.rank,
                    country = songResponse.country,
                    isFromServer = true
                )
            }
        } catch (e: Exception) {
            Log.e("RecommendationRepository", "Error fetching country songs: ${e.message}")
            emptyList()
        }
    }

    private fun generateDailyMix(
        userSongs: List<SongEntity>,
        globalSongs: List<Song>,
        countrySongs: List<Song>
    ): List<RecommendationPlaylist> {
        val recommendations = mutableListOf<RecommendationPlaylist>()

        val dailyMixSongs = mutableListOf<Song>()

        dailyMixSongs.addAll(globalSongs.take(8))

        val userSongsList = SongMapper.toSongList(userSongs)
        dailyMixSongs.addAll(userSongsList.shuffled().take(4))

        dailyMixSongs.addAll(countrySongs.take(3))

        if (dailyMixSongs.isNotEmpty()) {
            recommendations.add(
                RecommendationPlaylist(
                    id = "daily_mix_${Date().time}",
                    title = "Daily Mix",
                    description = "Your daily dose of great music",
                    songs = dailyMixSongs.shuffled().take(7),
                    type = PlaylistType.DAILY_MIX
                )
            )
        }

        return recommendations
    }

    private fun generateLikedBasedRecommendations(
        likedSongs: List<SongEntity>,
        globalSongs: List<Song>,
        countrySongs: List<Song>
    ): List<RecommendationPlaylist> {
        if (likedSongs.isEmpty()) return emptyList()

        val recommendations = mutableListOf<RecommendationPlaylist>()
        val likedSongsList = SongMapper.toSongList(likedSongs)

        val likedArtists = likedSongsList.map { it.artist.lowercase() }.distinct()

        val similarArtistSongs = mutableListOf<Song>()
        (globalSongs + countrySongs).forEach { serverSong ->
            if (likedArtists.any { artist ->
                    serverSong.artist.lowercase().contains(artist) ||
                            artist.contains(serverSong.artist.lowercase())
                }) {
                similarArtistSongs.add(serverSong)
            }
        }

        val recommendedSongs = mutableListOf<Song>()
        recommendedSongs.addAll(likedSongsList.shuffled().take(5))
        recommendedSongs.addAll(similarArtistSongs.shuffled().take(10))

        if (recommendedSongs.isNotEmpty()) {
            recommendations.add(
                RecommendationPlaylist(
                    id = "liked_based_${Date().time}",
                    title = "More Like What You Love",
                    description = "Based on your liked songs",
                    songs = recommendedSongs.shuffled().take(7),
                    type = PlaylistType.LIKED_BASED
                )
            )
        }

        return recommendations
    }

    private fun generateArtistBasedRecommendations(
        userSongs: List<SongEntity>,
        globalSongs: List<Song>,
        countrySongs: List<Song>
    ): List<RecommendationPlaylist> {
        if (userSongs.isEmpty()) return emptyList()

        val recommendations = mutableListOf<RecommendationPlaylist>()
        val userSongsList = SongMapper.toSongList(userSongs)

        val artistCount = mutableMapOf<String, Int>()
        userSongsList.forEach { song ->
            val artist = song.artist.lowercase()
            artistCount[artist] = artistCount.getOrDefault(artist, 0) + 1
        }

        val topArtist = artistCount.maxByOrNull { it.value }?.key

        if (topArtist != null) {
            val artistSongs = mutableListOf<Song>()
            (globalSongs + countrySongs).forEach { serverSong ->
                if (serverSong.artist.lowercase().contains(topArtist) ||
                    topArtist.contains(serverSong.artist.lowercase())) {
                    artistSongs.add(serverSong)
                }
            }

            val userArtistSongs = userSongsList.filter {
                it.artist.lowercase().contains(topArtist) ||
                        topArtist.contains(it.artist.lowercase())
            }

            val combinedSongs = mutableListOf<Song>()
            combinedSongs.addAll(userArtistSongs.take(3))
            combinedSongs.addAll(artistSongs.take(12))

            if (combinedSongs.isNotEmpty()) {
                recommendations.add(
                    RecommendationPlaylist(
                        id = "artist_based_${Date().time}",
                        title = "More from ${topArtist.replaceFirstChar { it.uppercase() }}",
                        description = "Songs by your favorite artist",
                        songs = combinedSongs.shuffled().take(7),
                        type = PlaylistType.ARTIST_BASED
                    )
                )
            }
        }

        return recommendations
    }

    private fun generateRecentlyPlayedMix(
        recentSongs: List<SongEntity>,
        globalSongs: List<Song>
    ): List<RecommendationPlaylist> {
        if (recentSongs.isEmpty()) return emptyList()

        val recommendations = mutableListOf<RecommendationPlaylist>()
        val recentSongsList = SongMapper.toSongList(recentSongs)

        val mixSongs = mutableListOf<Song>()
        mixSongs.addAll(recentSongsList.take(5))
        mixSongs.addAll(globalSongs.shuffled().take(7))

        recommendations.add(
            RecommendationPlaylist(
                id = "recent_mix_${Date().time}",
                title = "Pick Up Where You Left Off",
                description = "Recent songs + trending hits",
                songs = mixSongs.shuffled().take(7),
                type = PlaylistType.RECENTLY_PLAYED
            )
        )

        return recommendations
    }
}