package com.example.purrytify.ui.library

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.AppDatabase
import com.example.purrytify.data.entity.SongEntity
import com.example.purrytify.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class AddSongState(
    val songUri: Uri? = null,
    val songFileName: String = "",
    val songDuration: Long = 0,
    val artworkUri: Uri? = null,
    val title: String = "",
    val artist: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val editMode: Boolean = false,
    val songId: Long = -1
)

class AddSongViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository
    private val _state = MutableStateFlow(AddSongState())
    val state: StateFlow<AddSongState> = _state.asStateFlow()

    init {
        val songDao = AppDatabase.getDatabase(application).songDao()
        repository = SongRepository(songDao)
    }

    fun loadSongData(songId: Long) {
        if (songId <= 0) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val song = repository.getSongById(songId)
                song?.let {
                    _state.update { state ->
                        state.copy(
                            songId = song.id,
                            title = song.title,
                            artist = song.artist,
                            songDuration = song.duration,
                            songUri = Uri.parse(song.filePath),
                            artworkUri = if (song.artworkPath != null) Uri.parse(song.artworkPath) else null,
                            songFileName = getFileNameFromUri(getApplication(), Uri.parse(song.filePath)),
                            editMode = true,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Error loading song: ${e.message}", isLoading = false) }
                Log.e("AddSongViewModel", "Error loading song", e)
            }
        }
    }

    fun updateTitle(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun updateArtist(artist: String) {
        _state.update { it.copy(artist = artist) }
    }

    fun setSongUri(uri: Uri?) {
        uri?.let {
            _state.update { it.copy(songUri = uri) }
            extractMetadata(uri)
        }
    }

    fun setArtworkUri(uri: Uri?) {
        _state.update { it.copy(artworkUri = uri) }
    }

    private fun extractMetadata(uri: Uri) {
        try {
            val fileName = getFileNameFromUri(getApplication(), uri)
            _state.update { it.copy(songFileName = fileName) }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(getApplication<Application>(), uri)

            // Extract duration
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLong() ?: 0

            // Extract title if available
            val extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val title = if (!extractedTitle.isNullOrEmpty() && _state.value.title.isEmpty())
                extractedTitle else _state.value.title

            // Extract artist if available
            val extractedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val artist = if (!extractedArtist.isNullOrEmpty() && _state.value.artist.isEmpty())
                extractedArtist else _state.value.artist

            retriever.release()

            _state.update {
                it.copy(
                    songDuration = duration,
                    title = title,
                    artist = artist
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = "Error extracting metadata: ${e.message}") }
            Log.e("AddSongViewModel", "Error extracting metadata", e)
        }
    }

    fun saveSong() {
        val currentState = _state.value

        if (currentState.title.isEmpty() || currentState.artist.isEmpty() || currentState.songUri == null) {
            _state.update { it.copy(error = "Please fill in all required fields") }
            return
        }

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val song = SongEntity(
                    id = if (currentState.editMode) currentState.songId else 0,
                    title = currentState.title,
                    artist = currentState.artist,
                    duration = currentState.songDuration,
                    filePath = currentState.songUri.toString(),
                    artworkPath = currentState.artworkUri?.toString()
                )

                if (currentState.editMode) {
                    repository.update(song)
                } else {
                    repository.insert(song)
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error saving song: ${e.message}"
                    )
                }
                Log.e("AddSongViewModel", "Error saving song", e)
            }
        }
    }

    fun deleteSong() {
        val currentState = _state.value

        if (!currentState.editMode || currentState.songId <= 0) {
            return
        }

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val song = repository.getSongById(currentState.songId)
                song?.let {
                    repository.delete(it)
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            isSaved = true,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error deleting song: ${e.message}"
                    )
                }
                Log.e("AddSongViewModel", "Error deleting song", e)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun resetSaveState() {
        _state.update { it.copy(isSaved = false) }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var fileName = "Unknown file"

        val cursor = context.contentResolver?.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = it.getString(displayNameIndex)
                }
            }
        }

        return fileName
    }

    fun formatDuration(duration: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }
}