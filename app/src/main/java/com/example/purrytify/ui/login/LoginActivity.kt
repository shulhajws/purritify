package com.example.purrytify.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.purrytify.MainActivity
import com.example.purrytify.ui.theme.PurrytifyTheme

class LoginActivity : ComponentActivity() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLoading by viewModel.isLoading.collectAsState()

                    LoginScreen(
                        isLoading = isLoading,
                        onLogin = { email, password ->
                            viewModel.login(
                                email,
                                password,
                                this,
                                onSuccess = {
                                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                                    // Navigate to the main/home screen
                                    val intent = Intent(this, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                },
                                onError = { error ->
                                    Log.e("LoginActivity", "Login error -> $error")
                                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}