package com.example.purrytify.ui.library

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.purrytify.ui.theme.PurrytifyTheme
import androidx.navigation.fragment.navArgs
import com.example.purrytify.ui.library.AddSongViewModel
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.purrytify.ui.shared.SharedViewModel
import kotlinx.coroutines.launch

class AddSongFragment : Fragment() {
    private val viewModel: AddSongViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val songId = arguments?.getLong("songId", -1L) ?: -1L

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.globalUserProfile.collect { userProfile ->
                    userProfile?.let { profile ->
                        try {
                            val userId = profile.id.toInt()
                            viewModel.updateCurrentUserId(userId)
                        } catch (e: NumberFormatException) {
                            Log.e("AddSongFragment", "Invalid user ID format: ${profile.id}")
                        }
                    }
                }
            }
        }

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