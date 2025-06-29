package com.example.purrytify.ui.library

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.purrytify.R
import com.example.purrytify.databinding.FragmentLibraryBinding
import com.example.purrytify.ui.playback.PlayerViewModel
import com.example.purrytify.ui.playback.QueueDialog
import com.example.purrytify.ui.shared.SharedViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var navController: NavController
    private lateinit var songAdapter: LibrarySongAdapter
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var emptyStateTextView: TextView
    private lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        libraryViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[LibraryViewModel::class.java]
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        binding.fragmentHeaderTitle.text = getString(R.string.title_library)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.globalUserProfile.collect { userProfile ->
                    userProfile?.let { profile ->
                        try {
                            val userId = profile.id.toInt()
                            libraryViewModel.updateForUser(userId)
                        } catch (e: NumberFormatException) {
                            Log.e("LibraryFragment", "Invalid user ID format: ${profile.id}")
                        }
                    }
                }
            }
        }

        return binding.root
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = NavHostFragment.findNavController(this)

        // Replace the ComposeView with regular layout
        val contentView = LayoutInflater.from(requireContext())
            .inflate(R.layout.library_content, binding.libraryComposeView, false)
        (binding.libraryComposeView.parent as ViewGroup).removeView(binding.libraryComposeView)

        // Add the new layout where the ComposeView was
        val layoutParams = binding.libraryComposeView.layoutParams
        contentView.layoutParams = layoutParams
        (binding.root as ViewGroup).addView(contentView,
            binding.root.indexOfChild(binding.fragmentHeaderTitle) + 1)


        // Setup the RecyclerView
        recyclerView = contentView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_songs)
        emptyStateTextView = contentView.findViewById(R.id.text_empty_state)

        songAdapter = LibrarySongAdapter(
            onSongClick = { song ->
                playerViewModel.playSong(song)
                navigateToPlayback(song.id)
            },
            playerViewModel = playerViewModel
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = songAdapter
        }

        // Tab buttons
        val btnTabAll = contentView.findViewById<MaterialButton>(R.id.btn_tab_all)
        val btnTabLiked = contentView.findViewById<MaterialButton>(R.id.btn_tab_liked)
        updateTabAppearance(btnTabAll, btnTabLiked, 0)
        btnTabAll.setOnClickListener {
            libraryViewModel.selectTab(0)
            updateTabAppearance(btnTabAll, btnTabLiked, 0)
        }
        btnTabLiked.setOnClickListener {
            libraryViewModel.selectTab(1)
            updateTabAppearance(btnTabAll, btnTabLiked, 1)
        }

        // Add button
        val btnAddSong = contentView.findViewById<View>(R.id.btn_add_song)
        btnAddSong.setOnClickListener {
            navigateToAddSong()
        }

        // Queue button
        val btnQueue = contentView.findViewById<View>(R.id.btn_queue)
        btnQueue.setOnClickListener {
            showQueueDialog()
        }

        // Observe the songs list
        viewLifecycleOwner.lifecycleScope.launch {
            libraryViewModel.selectedTab
                .flatMapLatest { tabIndex ->
                    updateTabAppearance(btnTabAll, btnTabLiked, tabIndex)
                    if (tabIndex == 0) libraryViewModel.allSongs else libraryViewModel.likedSongs
                }
                .collect { songs ->
                    songAdapter.submitList(songs)

                    if (songs.isEmpty()) {
                        val emptyMessage = if (libraryViewModel.selectedTab.value == 0) {
                            "No songs in library"
                        } else {
                            "No liked songs"
                        }
                        emptyStateTextView.text = emptyMessage
                        emptyStateTextView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyStateTextView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
        }

    }

    private fun updateTabAppearance(btnAll: MaterialButton, btnLiked: MaterialButton, selectedTab: Int) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val surfaceVariantColor = ContextCompat.getColor(requireContext(), R.color.surface)
        val onPrimaryColor = ContextCompat.getColor(requireContext(), R.color.on_primary)
        val onSurfaceVariantColor = ContextCompat.getColor(requireContext(), R.color.on_surface)

        if (selectedTab == 0) {
            btnAll.setBackgroundColor(primaryColor)
            btnAll.setTextColor(onPrimaryColor)
            btnLiked.setBackgroundColor(surfaceVariantColor)
            btnLiked.setTextColor(onSurfaceVariantColor)
        } else {
            btnAll.setBackgroundColor(surfaceVariantColor)
            btnAll.setTextColor(onSurfaceVariantColor)
            btnLiked.setBackgroundColor(primaryColor)
            btnLiked.setTextColor(onPrimaryColor)
        }
    }

    private fun navigateToPlayback(songId: String) {
        val bundle = Bundle().apply {
            putString("songId", songId)
        }
        navController.navigate(R.id.navigation_song_playback, bundle)
    }

    private fun navigateToAddSong() {
        val bundle = Bundle().apply {
            putLong("songId", -1L)
        }
        navController.navigate(R.id.action_library_to_add_song, bundle)
    }

    private fun showQueueDialog() {
        Log.d("LibraryFragment", "Showing queue dialog")
        val dialog = QueueDialog(
            viewModel = playerViewModel,
            onDismiss = {
                (childFragmentManager.findFragmentByTag("QueueDialog") as? DialogFragment)?.dismiss()
            }
        )

        dialog.show(childFragmentManager, "QueueDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}