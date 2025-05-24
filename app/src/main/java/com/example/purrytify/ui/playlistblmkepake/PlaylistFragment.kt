//package com.example.purrytify.ui.playlist
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.NavController
//import androidx.navigation.fragment.NavHostFragment
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.purrytify.R
//import com.example.purrytify.databinding.FragmentPlaylistBinding
//import com.example.purrytify.ui.playback.PlayerViewModel
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.launch
//
//class PlaylistFragment : Fragment() {
//    private var _binding: FragmentPlaylistBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var navController: NavController
//    private val playlistViewModel: PlaylistViewModel by activityViewModels()
//    private val playerViewModel: PlayerViewModel by activityViewModels()
//    private lateinit var playlistAdapter: PlaylistAdapter
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        navController = NavHostFragment.findNavController(this)
//
//        setupRecyclerView()
//        observePlaylists()
//
//        binding.btnAddPlaylist.setOnClickListener {
//            navigateToAddPlaylist()
//        }
//    }
//
//    private fun setupRecyclerView() {
//        playlistAdapter = PlaylistAdapter(
//            onPlaylistClick = { playlist ->
//                navigateToPlaylistDetails(playlist.id)
//            },
//            onDeleteClick = { playlist ->
//                playlistViewModel.deletePlaylist(playlist.id)
//                Toast.makeText(requireContext(), "Playlist deleted", Toast.LENGTH_SHORT).show()
//            }
//        )
//
//        binding.recyclerPlaylists.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = playlistAdapter
//        }
//    }
//
//    private fun observePlaylists() {
//        viewLifecycleOwner.lifecycleScope.launch {
//            playlistViewModel.playlists.collectLatest { playlists ->
//                playlistAdapter.submitList(playlists)
//                binding.textEmptyState.visibility =
//                    if (playlists.isEmpty()) View.VISIBLE else View.GONE
//            }
//        }
//    }
//
//    private fun navigateToAddPlaylist() {
//        navController.navigate(R.id.action_playlistFragment_to_addPlaylistFragment)
//    }
//
//    private fun navigateToPlaylistDetails(playlistId: Long) {
//        val bundle = Bundle().apply {
//            putLong("playlistId", playlistId)
//        }
//        navController.navigate(R.id.action_playlistFragment_to_playlistDetailsFragment, bundle)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}