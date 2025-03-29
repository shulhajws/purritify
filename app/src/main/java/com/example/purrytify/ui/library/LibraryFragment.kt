package com.example.purrytify.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.purrytify.R
import com.example.purrytify.databinding.FragmentLibraryBinding
import com.example.purrytify.ui.theme.PurrytifyTheme

class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        libraryViewModel = ViewModelProvider(this)[LibraryViewModel::class.java]
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        binding.fragmentHeaderTitle.text = getString(R.string.title_library)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = NavHostFragment.findNavController(this)
        binding.libraryComposeView.setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        onSongClick = { songId -> navigateToPlayback(songId) }
                    )
                }
            }
        }
    }

    private fun navigateToPlayback(songId: String) {
        val bundle = Bundle().apply {
            putString("song_id", songId)
        }
        navController.navigate(R.id.navigation_song_playback, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}