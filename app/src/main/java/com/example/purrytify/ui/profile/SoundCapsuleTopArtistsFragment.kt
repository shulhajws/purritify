package com.example.purrytify.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.purrytify.repository.MonthYear
import com.example.purrytify.ui.theme.PurrytifyTheme

class SoundCapsuleTopArtistsFragment : Fragment() {
    private val soundCapsuleViewModel: SoundCapsuleViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val year = arguments?.getInt("year") ?: return ComposeView(requireContext())
        val month = arguments?.getInt("month") ?: return ComposeView(requireContext())
        val monthYear = MonthYear(year, month)

        return ComposeView(requireContext()).apply {
            setContent {
                PurrytifyTheme {
                    TopArtistsSoundCapsuleScreen(
                        monthYear = monthYear,
                        viewModel = soundCapsuleViewModel,
                        onBackClick = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}