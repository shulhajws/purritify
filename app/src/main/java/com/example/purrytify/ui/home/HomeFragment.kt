package com.example.purrytify.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.purrytify.R
import com.example.purrytify.databinding.FragmentHomeBinding
import com.example.purrytify.ui.playback.PlayerViewModel
import com.example.purrytify.ui.shared.SharedViewModel
import com.example.purrytify.ui.theme.PurrytifyTheme
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var navController: NavController
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        val homeViewModelFactory = HomeViewModelFactory(requireActivity().application, sharedViewModel)

        val homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.fragmentHeaderTitle.text = getString(R.string.title_home)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.globalUserProfile.collect { userProfile ->
                    userProfile?.let { profile ->
                        try {
                            val userId = profile.id.toInt()
                            homeViewModel.updateForUser(userId)
                        } catch (e: NumberFormatException) {
                            Log.e("HomeFragment", "Invalid user ID format: ${profile.id}")
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = NavHostFragment.findNavController(this)

        val sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        val homeViewModelFactory = HomeViewModelFactory(requireActivity().application, sharedViewModel)

        val homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

        binding.homeComposeView.setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onSongClick = { song ->
                            playerViewModel.playSong(song)
                            navigateToPlayback(song.id)
                        },
                    )
                }
            }
        }
    }

    private fun navigateToPlayback(songId: String) {
        val bundle = Bundle().apply {
            putString("songId", songId)
        }
        navController.navigate(R.id.navigation_song_playback, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}