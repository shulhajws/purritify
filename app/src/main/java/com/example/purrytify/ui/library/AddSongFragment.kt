package com.example.purrytify.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.purrytify.ui.theme.PurrytifyTheme

class AddSongFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PurrytifyTheme {
                    AddSongScreen(
                        onBackClick = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}