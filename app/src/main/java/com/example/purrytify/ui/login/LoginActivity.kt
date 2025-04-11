package com.example.purrytify.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.purrytify.MainActivity

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = LoginViewModel()

            LoginScreen(
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
                            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        }
    }
}
