package com.example.purrytify.ui.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.ui.shared.SharedViewModel
import com.example.purrytify.R
import com.example.purrytify.databinding.FragmentProfileBinding
import com.example.purrytify.ui.login.LoginActivity
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.ui.theme.White
import com.example.purrytify.util.NetworkUtil
import com.example.purrytify.util.TokenManager
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
                        val context = LocalContext.current

                        // Logout button
                        Button(
                            onClick = {
                                // Clear the token
                                TokenManager.clearToken(context)

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