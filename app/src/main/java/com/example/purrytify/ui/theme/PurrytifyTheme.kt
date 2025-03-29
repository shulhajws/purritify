package com.example.purrytify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,          // Primary green color for buttons, highlights
    secondary = SpotifyGreen,         // Could use a variation if needed
    tertiary = SpotifyGreen,          // Could use a variation if needed
    background = DarkBlack,           // Main background (121212)
    surface = SoftBlack,              // Cards, surfaces (212121)
    onSurface = White,                // Text on surfaces
    onBackground = White,             // Text on background
    onPrimary = Black,                // Text on primary (green) elements
    onSecondary = Black,              // Text on secondary elements
    onTertiary = Black                // Text on tertiary elements
)

@Composable
fun PurrytifyTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}