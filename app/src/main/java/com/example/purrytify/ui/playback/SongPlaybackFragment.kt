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
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

class SongPlaybackFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var handler: Handler
    private val viewModel: PlayerViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_playback, container, false)

        checkAndRequestPermissions()

        val songId = arguments?.getString("song_id")
        songId?.let {
            viewModel.playSongById(it)
        }

        val imageAlbum = view.findViewById<ImageView>(R.id.image_album)
        val textTitle = view.findViewById<TextView>(R.id.text_title)
        val textArtist = view.findViewById<TextView>(R.id.text_artist)
        val btnPlay = view.findViewById<ImageButton>(R.id.btn_play)
        val btnNext = view.findViewById<ImageButton>(R.id.btn_next)
        val btnPrev = view.findViewById<ImageButton>(R.id.btn_prev)
        val textCurrentTime = view.findViewById<TextView>(R.id.text_current_time)
        val textTotalTime = view.findViewById<TextView>(R.id.text_total_time)
        seekBar = view.findViewById(R.id.seek_bar)

        handler = Handler(Looper.getMainLooper())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentSong.collectLatest { song ->
                        song?.let {
                            textTitle.text = it.title
                            textArtist.text = it.artist
                            Picasso.get().load(it.albumArt).into(imageAlbum)
                            seekBar.progress = 0
                        }
                    }
                }

                launch {
                    viewModel.isPlaying.collectLatest { isPlaying ->
                        btnPlay.setImageResource(
                            if (isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_filled
                        )
                        if (isPlaying) simulateProgress(textCurrentTime)
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
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnPlay.setOnClickListener {
            viewModel.togglePlayPause()
        }

        btnNext.setOnClickListener {
            viewModel.playNext()
        }

        btnPrev.setOnClickListener {
            viewModel.playPrevious()
        }

        return view
    }

    private fun simulateProgress(textCurrentTime: TextView) {
        handler.post(object : Runnable {
            var progress = seekBar.progress
            override fun run() {
                if (viewModel.isPlaying.value && progress < seekBar.max) {
                    progress++
                    seekBar.progress = progress
                    textCurrentTime.text = formatDuration(progress)
                    handler.postDelayed(this, 1000)
                }
            }
        })
    }

    private fun formatDuration(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun checkAndRequestPermissions() {
        // For Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                    REQUEST_PERMISSION_CODE
                )
            }
        }
        // For Android 6 to Android 12
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry playing the song
                Log.d("Permissions", "Storage permission granted")
                val songId = arguments?.getString("song_id")
                songId?.let {
                    viewModel.playSongById(it)
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
