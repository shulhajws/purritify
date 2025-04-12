package com.example.purrytify.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.ui.shared.SharedViewModel
import com.example.purrytify.R
import com.example.purrytify.databinding.FragmentProfileBinding
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.util.NetworkUtil
import kotlinx.coroutines.flow.collectLatest

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileViewModel: ProfileViewModel // TODO: Potentially unused

    private val sharedViewModel: SharedViewModel by lazy {
        ViewModelProvider(requireActivity())[SharedViewModel::class.java]
    }

    override fun onCreateView(  // TODO: Potentially unused
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

        binding.profileComposeView.setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!NetworkUtil.isNetworkAvailable(requireContext())) {
                        Log.d("ProfileFragment", "No internet connection")
                        NetworkUtil.NoInternetScreen()
                    } else {
                        // Get global user profile from SharedViewModel
                        val userProfile = sharedViewModel.globalUserProfile.collectAsState().value
                        userProfile?.let {
                            ProfileScreen(userProfile = it)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}