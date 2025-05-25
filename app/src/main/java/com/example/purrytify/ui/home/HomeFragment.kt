package com.example.purrytify.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.purrytify.MainActivity
import com.example.purrytify.R
import com.example.purrytify.databinding.FragmentHomeBinding
import com.example.purrytify.model.Song
import com.example.purrytify.repository.RecommendationPlaylist
import com.example.purrytify.ui.download.DownloadViewModel
import com.example.purrytify.ui.playback.PlayerViewModel
import com.example.purrytify.ui.shared.SharedViewModel
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var downloadViewModel: DownloadViewModel
    private lateinit var recommendationViewModel: RecommendationViewModel
    private lateinit var navController: NavController
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var btnScan: ImageButton

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            // Handle scan succeed
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(result.contents)
            }
            startActivity(intent)
        } else {
            // Scan cancelled
            Toast.makeText(requireContext(), "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListener() {
        btnScan.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                launchScanner()
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchScanner()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedViewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]

        val homeViewModelFactory = HomeViewModelFactory(requireActivity().application, sharedViewModel)
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

        downloadViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[DownloadViewModel::class.java]

        recommendationViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[RecommendationViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.fragmentHeaderTitle.text = getString(R.string.title_home)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.globalUserProfile.collect { userProfile ->
                    userProfile?.let { profile ->
                        try {
                            val userId = profile.id.toInt()
                            homeViewModel.updateForUser(userId)
                            downloadViewModel.updateForUser(userId)
                            recommendationViewModel.updateForUser(userId, profile.location)
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
        btnScan = view.findViewById(R.id.btn_scan_qr)

        this.setupClickListener()

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
                        downloadViewModel = downloadViewModel,
                        recommendationViewModel = recommendationViewModel,
                        onSongClick = { song ->
                            playerViewModel.playSongBySongModel(song)
                            navigateToPlayback(song)
                        },
                        onPlaylistClick = { playlist ->
                            navigateToPlaylistDetail(playlist)
                        }
                    )
                }
            }
        }
    }

    private fun navigateToPlayback(song: Song) {
        val bundle = Bundle().apply {
            putString("songId", song.id)
            putParcelable("song", song)
        }
        navController.navigate(R.id.navigation_song_playback, bundle)
    }

    private fun navigateToPlaylistDetail(playlist: RecommendationPlaylist) {
        val bundle = Bundle().apply {
            putParcelable("playlist", playlist)
        }
        navController.navigate(R.id.navigation_playlist_detail, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // TODO: if you have time, style the UI when camera opened (cupas has write it how in Google DocsTerpusat
    private fun launchScanner() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan a QR Code")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(false)
        }
        scanLauncher.launch(options)
    }
}