package com.example.purrytify.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.purrytify.ui.theme.PurrytifyTheme
import androidx.navigation.fragment.navArgs
import com.example.purrytify.ui.library.AddSongViewModel
import androidx.fragment.app.viewModels

class AddSongFragment : Fragment() {
    private val viewModel: AddSongViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Get songId from arguments
        val songId = arguments?.getLong("songId", -1L) ?: -1L

        return ComposeView(requireContext()).apply {
            setContent {
                PurrytifyTheme {
                    AddSongScreen(
                        songId = songId,
                        onBackClick = { findNavController().navigateUp() },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}