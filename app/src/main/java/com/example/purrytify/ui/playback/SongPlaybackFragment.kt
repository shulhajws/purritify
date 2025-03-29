package com.example.purrytify.ui.playback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.purrytify.R

class SongPlaybackFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_song_playback, container, false)

        // Get song ID from arguments
        val songId = arguments?.getString("song_id")

        view.findViewById<TextView>(R.id.playback_placeholder_text).text =
            "Song Playback Placeholder for song ID: $songId"

        return view
    }
}