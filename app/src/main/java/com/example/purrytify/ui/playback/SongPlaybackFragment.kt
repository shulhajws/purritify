package com.example.purrytify.ui.playback

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.purrytify.R
import com.squareup.picasso.Picasso
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SongPlaybackFragment : Fragment() {

    private lateinit var seekBar: SeekBar
    private lateinit var handler: Handler
    private val viewModel: PlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_playback, container, false)

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
}
