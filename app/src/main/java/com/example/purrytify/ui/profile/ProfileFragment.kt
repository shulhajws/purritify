package com.example.purrytify.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.example.purrytify.R
import com.example.purrytify.databinding.FragmentProfileBinding
import com.example.purrytify.ui.login.LoginActivity
import com.example.purrytify.ui.shared.SharedViewModel
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.ui.theme.White
import com.example.purrytify.util.NetworkUtil
import com.example.purrytify.util.TokenManager
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var navController: NavController
    private val soundCapsuleViewModel: SoundCapsuleViewModel by lazy {
        ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[SoundCapsuleViewModel::class.java]
    }


    private val sharedViewModel: SharedViewModel by lazy {
        ViewModelProvider(requireActivity())[SharedViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.fragmentHeaderTitle.text = getString(R.string.title_profile)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.globalUserProfile.collect { userProfile ->
                    userProfile?.let { profile ->
                        try {
                            val userId = profile.id.toInt()
                            Log.d("ProfileFragment", "Updating profileViewModel with userId: $userId")
                            profileViewModel.updateForUser(userId)
                        } catch (e: NumberFormatException) {
                            Log.e("ProfileFragment", "Invalid user ID format: ${profile.id}")
                        }
                    }
                }
            }
        }

        binding.profileComposeView.setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Note: If you see warning down here, just ignore it, Android Studio have wrong logic this time
                    if (!NetworkUtil.isNetworkAvailable(requireContext()) && (sharedViewModel.globalUserProfile == null)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No internet connection 😕",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "You can still use the app, but some features may not work properly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Logout Button
                            val context = LocalContext.current
                            Button(
                                onClick = {
                                    // Clear the token
                                    TokenManager.clearToken(context)

                                    // Clear globalUserProfile variable
                                    sharedViewModel.clearGlobalUserProfile()

                                    // Navigate to LoginActivity
                                    val intent = Intent(context, LoginActivity::class.java)
                                    context.startActivity(intent)

                                    // Toast message
                                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

                                    // Finish current activity "if needed"
                                    (context as? Activity)?.finish()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                ),
                                shape = RoundedCornerShape(percent = 50),
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Logout",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = White
                                )
                            }
                        }
                    } else {
                        Log.d("ProfileFragment", "Debug currentUserProfile: internet connection available")
                        // Get global user profile from SharedViewModel
                        val userProfile = sharedViewModel.globalUserProfile.collectAsState().value
                        Log.d("ProfileFragment",  "Debug currentUserProfile: $userProfile")

                        val songsCount = profileViewModel.songsCount.collectAsState().value
                        val likedCount = profileViewModel.likedCount.collectAsState().value
                        val listenedCount = profileViewModel.listenedCount.collectAsState().value
                        Log.d("ProfileFragment", "Stats from DB - Songs: $songsCount, Liked: $likedCount, Listened: $listenedCount")

                        // If userProfile is null, force logout
                        if (userProfile == null) {
                            // Clear the token
                            TokenManager.clearToken(requireContext())

                            // Clear globalUserProfile variable
                            sharedViewModel.clearGlobalUserProfile()

                            // Navigate to LoginActivity
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            startActivity(intent)

                            // Toast message
                            Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()

                            // Finish current activity "if needed"
                            (requireContext() as? Activity)?.finish()
                        }
                        userProfile?.let {
                            Log.d("ProfileFragment", "it.username: ${it.username}")
                            ProfileScreen(
                                userProfile = it,
                                songsCount = songsCount,
                                likedCount = likedCount,
                                listenedCount = listenedCount,
                                soundCapsuleViewModel = soundCapsuleViewModel,
                                onSoundCapsuleNavigation = { screen, monthYear ->
                                    navigateToSoundCapsuleScreen(screen, monthYear)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun navigateToSoundCapsuleScreen(screen: String, monthYear: com.example.purrytify.repository.MonthYear) {
        val bundle = Bundle().apply {
            putInt("year", monthYear.year)
            putInt("month", monthYear.month)
        }

        try {
            when (screen) {
                "time_listened" -> navController.navigate(R.id.navigation_sound_capsule_time_listened, bundle)
                "top_artists" -> navController.navigate(R.id.navigation_sound_capsule_top_artists, bundle)
                "top_songs" -> navController.navigate(R.id.navigation_sound_capsule_top_songs, bundle)
                else -> Log.w("ProfileFragment", "Unknown sound capsule screen: $screen")
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error navigating to sound capsule screen: ${e.message}")
            Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}