package com.example.purrytify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    secondary = SpotifyGreen,
    tertiary = SpotifyGreen,
    background = DarkBlack,
    surface = SoftBlack,
    onSurface = White,
    onBackground = White,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = Black
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