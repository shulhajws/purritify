package com.example.purrytify.ui.playback

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.purrytify.R
import com.example.purrytify.model.Song
import com.example.purrytify.ui.download.DownloadViewModel
import com.example.purrytify.ui.shared.SharedViewModel
import com.example.purrytify.util.AudioDevice
import com.example.purrytify.util.AudioRouteManager
import com.example.purrytify.util.QRCodeUtils
import com.squareup.picasso.Picasso
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SongPlaybackFragment : Fragment() {
    private lateinit var navController: NavController
    private lateinit var seekBar: SeekBar
    private lateinit var handler: Handler
    private lateinit var viewModel: PlayerViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var audioRouteManager: AudioRouteManager
    private lateinit var btnAudioOutput: ImageButton
    private var currentAudioDevice: AudioDevice? = null

    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var btnDownload: ImageButton

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
    private lateinit var textNextUp: TextView

    private lateinit var btnShuffle: ImageButton
    private lateinit var btnRepeat: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var btnQRCode: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[PlayerViewModel::class.java]

        audioRouteManager = viewModel.getAudioRouteManager() ?: AudioRouteManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.globalUserProfile.collect { userProfile ->
                    userProfile?.let { profile ->
                        try {
                            val userId = profile.id.toInt()
                            viewModel.updateForUser(userId)
                        } catch (e: NumberFormatException) {
                            Log.e("SongPlaybackFragment", "Invalid user ID format: ${profile.id}")
                        }
                    }
                }
            }
        }

        downloadViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[DownloadViewModel::class.java]

        return inflater.inflate(R.layout.fragment_song_playback, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            navController.navigateUp()
        }
        navController = NavHostFragment.findNavController(this)

        initializeViews(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    Log.d("SongPlayback", "Waiting for songs to load...")
                } else {
                    Log.d("SongPlayback", "Songs loaded")
                }
            }
        }

        // Playing Song
        if (checkAndRequestPermissions()) {
            val songId = arguments?.getString("songId")
            val song = arguments?.getParcelable<Song>("song")
            song?.let {
                Log.d("SongPlayback", "Attempting to play song with ID: ${it.id}, title: ${it.title}")
                viewModel.playSongBySongModel(it)
            }
        }

        setupClickListeners()

        setupSeekBarListener()

        setupObservers()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSong.collect { song ->
                Log.d("SongPlayback", "Current song state changed: ${song?.title ?: "null"}")

                if (song == null) {
                    val songId = arguments?.getString("songId")
                    if (songId != null && viewModel.getSongById(songId) == null) {
                        Log.d("SongPlayback", "Song no longer exists, navigating away")

                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isAdded() && !isDetached()) {
                                navController.navigateUp()
                            }
                        }, 100)
                    }
                }

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

    override fun onResume() {
        super.onResume()
        Log.d("SongPlayback", "onResume called")

        val song = arguments?.getParcelable<Song>("song")
        song?.let {
            // Check the song if exist in local db (only check if not from server)
            val songToResume:Song? = if (it.isFromServer) {
                song
            } else {
                viewModel.getSongById(it.id)
            }
            // Check if the song exists
            if (songToResume == null) {
                Log.d("SongPlayback", "Song no longer exists in onResume, navigating away")
                navController.navigateUp()
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
        textNextUp = view.findViewById(R.id.text_next_up)
        btnPlay = view.findViewById(R.id.btn_play)
        btnNext = view.findViewById(R.id.btn_next)
        btnPrev = view.findViewById(R.id.btn_prev)
        btnEditSong = view.findViewById(R.id.btn_edit_delete_song)
        btnDownload = view.findViewById(R.id.btn_download)
        btnShare = view.findViewById(R.id.btn_share)
        btnQRCode = view.findViewById(R.id.btn_qr_code)
        btnFavorite = view.findViewById(R.id.btn_favorite)
        textCurrentTime = view.findViewById(R.id.text_current_time)
        textTotalTime = view.findViewById(R.id.text_total_time)
        seekBar = view.findViewById(R.id.seek_bar)
        handler = Handler(Looper.getMainLooper())
        btnAudioOutput = view.findViewById(R.id.btn_audio_output)
        btnShuffle = view.findViewById(R.id.btn_shuffle)
        btnRepeat = view.findViewById(R.id.btn_repeat)
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

        btnAudioOutput.setOnClickListener {
            showAudioOutputDialog()
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

        btnDownload.setOnClickListener {
            viewModel.currentSong.value?.let { song ->
                if (song.isFromServer) {
                    downloadViewModel.downloadSong(song)
                }
            }
        }

        btnShuffle.setOnClickListener {
            Log.d("SongPlayback", "Shuffle button clicked")
            viewModel.toggleShuffle()
        }

        btnRepeat.setOnClickListener {
            Log.d("SongPlayback", "Repeat button clicked")
            viewModel.cycleRepeatMode()
        }

        btnShare.setOnClickListener {
            val currentSong = viewModel.currentSong.value
            if (currentSong != null && currentSong.isFromServer) {
                // Can be https or http. In this case, https used
                val shareUrl = "https://purrytify/song/${currentSong.id}"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out this song on Purrytify: $shareUrl")
                }
                Log.d("SongPlayback", "Sharing song with ID: ${currentSong.id}, URL: $shareUrl")
                try {
                    startActivity(Intent.createChooser(shareIntent, "Share Song"))
                } catch (e: Exception) {
                    Log.e("SongPlayback", "Error while sharing song: ${e.message}", e)
                }
            } else {
                Log.d("SongPlayback", "Share action failed: currentSong is null or not from server")
                Toast.makeText(requireContext(), "Only songs from the server can be shared.", Toast.LENGTH_SHORT).show()
            }
        }

        btnQRCode.setOnClickListener {
            val currentSong = viewModel.currentSong.value
            if (currentSong != null && currentSong.isFromServer) {
                // Can be https or http. In this case, https used
                val deepLink = "https://purrytify://song/${currentSong.id}"
                val qrBitmap = QRCodeUtils.generateQRCode(deepLink)
                if (qrBitmap != null) {
                    QRCodeUtils.showQrPreviewDialog(requireContext(), qrBitmap, currentSong.title, currentSong.artist)
                } else {
                    Toast.makeText(requireContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Only songs from the server can be shared via QR", Toast.LENGTH_SHORT).show()
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

                // Observe shuffle state
                launch {
                    viewModel.isShuffleOn.collectLatest { isShuffleOn ->
                        updateShuffleButton(isShuffleOn)
                    }
                }

                // Observe repeat mode
                launch {
                    viewModel.repeatMode.collectLatest { repeatMode ->
                        updateRepeatButton(repeatMode)
                    }
                }

                // Observe queue to show next up song
                launch {
                    viewModel.queue.collectLatest { queue ->
                        updateNextUpText(queue)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                audioRouteManager.currentDevice.collect { device ->
                    currentAudioDevice = device
                    updateAudioOutputButton()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    downloadViewModel.downloadState.collect { downloadState ->
                        updateDownloadButton(downloadState)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.globalUserProfile.collect { userProfile ->
                    userProfile?.let { profile ->
                        try {
                            val userId = profile.id.toInt()
                            downloadViewModel.updateForUser(userId)
                        } catch (e: NumberFormatException) {
                            Log.e("SongPlaybackFragment", "Invalid user ID format: ${profile.id}")
                        }
                    }
                }
            }
        }

    }

    private fun updateShuffleButton(isShuffleOn: Boolean) {
        btnShuffle.setImageResource(
            if (isShuffleOn) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle
        )

        // Update tint color to show active state
        btnShuffle.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (isShuffleOn) R.color.spotify_green else R.color.white
            )
        )
    }

    private fun updateRepeatButton(repeatMode: RepeatMode) {
        val iconRes = when (repeatMode) {
            RepeatMode.NO_REPEAT -> R.drawable.ic_repeat
            RepeatMode.REPEAT_ALL -> R.drawable.ic_repeat_all
            RepeatMode.REPEAT_ONE -> R.drawable.ic_repeat_one
        }

        btnRepeat.setImageResource(iconRes)

        // Update tint color to show active state
        btnRepeat.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (repeatMode != RepeatMode.NO_REPEAT) R.color.spotify_green else R.color.white
            )
        )
    }

    private fun updateNextUpText(queue: List<Song>) {
        if (queue.isNotEmpty()) {
            val nextSong = queue.first()
            textNextUp.text = "Next Up: ${nextSong.title} by ${nextSong.artist}"
            textNextUp.visibility = View.VISIBLE
        } else {
            textNextUp.visibility = View.GONE
        }
    }

    private fun updateFavoriteButton(isLiked: Boolean) {
        btnFavorite.setImageResource(
            if (isLiked) R.drawable.ic_fav else R.drawable.ic_fav_border
        )
    }

    private fun showAudioOutputDialog() {
        val dialog = AudioOutputDialog(
            audioRouteManager = audioRouteManager,
            onDismiss = {
                (childFragmentManager.findFragmentByTag("AudioOutputDialog") as? DialogFragment)?.dismiss()
            }
        )

        dialog.show(childFragmentManager, "AudioOutputDialog")
    }

    private fun updateAudioOutputButton() {
        val iconResId = when (currentAudioDevice?.deviceType) {
            AudioDevice.DeviceType.BLUETOOTH -> R.drawable.ic_bluetooth_audio
            AudioDevice.DeviceType.WIRED_HEADSET -> R.drawable.ic_headset
            AudioDevice.DeviceType.USB_AUDIO -> R.drawable.ic_usb
            else -> R.drawable.ic_audio_output
        }

        btnAudioOutput.setImageResource(iconResId)
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

    private fun updateDownloadButton(downloadState: com.example.purrytify.ui.download.DownloadState) {
        val currentSong = viewModel.currentSong.value

        if (currentSong?.isFromServer == true) {
            btnDownload.visibility = View.VISIBLE

            val isDownloading = downloadState.downloadProgress[currentSong.id]?.isDownloading == true
            val isCompleted = downloadState.downloadProgress[currentSong.id]?.isCompleted == true

            when {
                isDownloading -> {
                    btnDownload.setImageResource(R.drawable.ic_downloading) // You'll need this drawable
                    btnDownload.isEnabled = false
                }
                isCompleted -> {
                    btnDownload.setImageResource(R.drawable.ic_download_done) // You'll need this drawable
                    btnDownload.isEnabled = false
                }
                else -> {
                    btnDownload.setImageResource(R.drawable.ic_download)
                    btnDownload.isEnabled = true
                }
            }
        } else {
            btnDownload.visibility = View.GONE
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SongPlayback", "onDestroy called")
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
                Log.d("SongPlayback", "Storage permission granted")

                val song = arguments?.getParcelable<Song>("song")
                song?.let {
                    val currentSong = viewModel.getSongById(it.id)
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
                        viewModel.playSongBySongModel(it)
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