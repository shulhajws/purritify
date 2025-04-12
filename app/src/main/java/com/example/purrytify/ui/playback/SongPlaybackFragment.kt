//SongPlaybackFragment.kt
package com.example.purrytify.ui.playback

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.purrytify.R
import com.squareup.picasso.Picasso
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class SongPlaybackFragment : Fragment() {
    private lateinit var navController: NavController
    private lateinit var seekBar: SeekBar
    private lateinit var handler: Handler
    private lateinit var viewModel: PlayerViewModel

    // UI Components
    private lateinit var imageAlbum: ImageView
    private lateinit var textTitle: TextView
    private lateinit var textArtist: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnEditSong: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[PlayerViewModel::class.java]
        return inflater.inflate(R.layout.fragment_song_playback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize NavController
        navController = NavHostFragment.findNavController(this)

        // Initialize UI components
        initializeViews(view)

        // Setup loading state observer first
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    // Maybe show a loading indicator here
                    Log.d("SongPlayback", "Waiting for songs to load...")
                } else {
                    Log.d("SongPlayback", "Songs loaded")
                }
            }
        }

        // First check permissions
        if (checkAndRequestPermissions()) {
            // Only try to play if permissions are granted and we have the song ID
            val songId = arguments?.getString("songId")
            songId?.let {
                Log.d("SongPlayback", "Attempting to play song with ID: $songId")
                viewModel.playSongById(it)
            }
        }

        setupClickListeners()

        setupSeekBarListener()

        setupObservers()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSong.collect { song ->
                Log.d("SongPlayback", "Current song state changed: ${song?.title ?: "null"}")

                song?.let {
                    textTitle.text = it.title
                    textArtist.text = it.artist
                    Picasso.get()
                        .load(it.albumArt)
                        .placeholder(R.drawable.placeholder_album_art)
                        .error(R.drawable.placeholder_album_art)
                        .into(imageAlbum)
                    seekBar.progress = 0
                } ?: run {
                    // Handle null song state
                    Log.d("SongPlayback", "Current song is null, waiting for data...")
                    textTitle.text = "Loading..."
                    textArtist.text = ""
                    // Set Image Default (If Needed)
                }
            }
        }
    }

    private fun ensureUriPermission(uriString: String): Boolean {
        try {
            val uri = Uri.parse(uriString)
            val contentResolver = requireContext().contentResolver
            // Check if we already have the permission
            val list = contentResolver.persistedUriPermissions
            val alreadyHasPermission = list.any {
                it.uri.toString() == uriString && it.isReadPermission
            }
            if (alreadyHasPermission) {
                Log.d("SongPlayback", "Already have persistent URI permission for: $uri")
                return true
            }
            // If we don't have it already, try to take it
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            Log.d("SongPlayback", "Successfully took persistent URI permission for: $uri")
            return true
        } catch (e: Exception) {
            Log.e("SongPlayback", "Failed to take persistent URI permission: ${e.message}")
            return false
        }
    }

    private fun initializeViews(view: View) {
        imageAlbum = view.findViewById(R.id.image_album)
        textTitle = view.findViewById(R.id.text_title)
        textArtist = view.findViewById(R.id.text_artist)
        btnPlay = view.findViewById(R.id.btn_play)
        btnNext = view.findViewById(R.id.btn_next)
        btnPrev = view.findViewById(R.id.btn_prev)
        btnEditSong = view.findViewById(R.id.btn_edit_delete_song)
        btnFavorite = view.findViewById(R.id.btn_favorite)
        textCurrentTime = view.findViewById(R.id.text_current_time)
        textTotalTime = view.findViewById(R.id.text_total_time)
        seekBar = view.findViewById(R.id.seek_bar)
        handler = Handler(Looper.getMainLooper())
    }

    private fun setupClickListeners() {
        btnPlay.setOnClickListener {
            viewModel.togglePlayPause()
        }

        btnNext.setOnClickListener {
            viewModel.playNext()
        }

        btnPrev.setOnClickListener {
            viewModel.playPrevious()
        }

        btnFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        btnEditSong.setOnClickListener {
            // Debug to check if currentSong is null at click time
            Log.d("SongPlayback", "Edit button clicked, currentSong: ${viewModel.currentSong.value}")

            // Get songId from arguments as fallback if currentSong is null
            val songId = viewModel.currentSong.value?.id ?: arguments?.getString("songId")
            if (songId != null) {
                try {
                    val longId = songId.toLong()
                    val bundle = Bundle().apply {
                        putLong("songId", longId)
                    }
                    navController.navigate(R.id.action_song_playback_to_edit_delete_song, bundle)
                } catch (e: Exception) {
                    Log.e("SongPlayback", "Error converting song ID: ${e.message}")
                    Toast.makeText(requireContext(), "Invalid song ID format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "No song selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentSong.collectLatest { song ->
                        song?.let {
                            textTitle.text = it.title
                            textArtist.text = it.artist
                            Picasso.get()
                                .load(it.albumArt)
                                .placeholder(R.drawable.placeholder_album_art)
                                .error(R.drawable.placeholder_album_art)
                                .into(imageAlbum)
                            seekBar.progress = 0
                        }
                    }
                }

                launch {
                    viewModel.isPlaying.collectLatest { isPlaying ->
                        btnPlay.setImageResource(
                            if (isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_filled
                        )
                    }
                }

                launch {
                    viewModel.currentPosition.collectLatest { position ->
                        seekBar.progress = position
                        textCurrentTime.text = formatDuration(position)
                    }
                }

                launch {
                    viewModel.duration.collectLatest { duration ->
                        seekBar.max = duration
                        textTotalTime.text = formatDuration(duration)
                    }
                }

                launch {
                    viewModel.isLiked.collectLatest { isLiked ->
                        updateFavoriteButton(isLiked)
                    }
                }
            }
        }
    }

    private fun updateFavoriteButton(isLiked: Boolean) {
        btnFavorite.setImageResource(
            if (isLiked) R.drawable.ic_fav else R.drawable.ic_fav_border
        )
    }

    private fun navigateToEditDeleteSong(songId: String?) {
        if (songId == null) {
            Toast.makeText(requireContext(), "Song ID not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val longId = songId.toLong()
            val bundle = Bundle().apply {
                putLong("songId", longId)
            }
            navController.navigate(R.id.action_song_playback_to_edit_delete_song, bundle)
        } catch (e: Exception) {
            Log.e("SongPlayback", "Error converting song ID: ${e.message}")
            Toast.makeText(requireContext(), "Invalid song ID format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDuration(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        // Check storage permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // If we need to request permissions, do so and return false
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSION_CODE)
            return false
        }

        // Check URI permissions for the current song
        val songId = arguments?.getString("songId")
        if (songId != null) {
            val currentSong = viewModel.getSongById(songId)
            if (currentSong?.audioUrl?.startsWith("content://") == true) {
                return ensureUriPermission(currentSong.audioUrl)
            }
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permission granted, retry playing the song
                Log.d("Permissions", "Storage permission granted")

                val songId = arguments?.getString("songId")
                songId?.let {
                    val currentSong = viewModel.getSongById(it)
                    currentSong?.let { song ->
                        if (song.audioUrl.startsWith("content://")) {
                            // Return if we couldn't get URI permission
                            if (!ensureUriPermission(song.audioUrl)) {
                                Toast.makeText(
                                    requireContext(),
                                    "Could not access the song file",
                                    Toast.LENGTH_LONG
                                ).show()
                                return
                            }
                        }
                        viewModel.playSongById(it)
                    }
                }
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(
                    requireContext(),
                    "Storage permission is needed to play songs",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        const val REQUEST_PERMISSION_CODE = 100
    }
}