package dev.predrag.newsfeed.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E9C),
    onPrimary = Color.White,
    secondary = Color(0xFF4A6572),
    background = Color(0xFFFBFCFE),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BCBFF),
    onPrimary = Color(0xFF00325A),
    secondary = Color(0xFFB4C9D6),
    background = Color(0xFF101416),
    surface = Color(0xFF171B1E),
    error = Color(0xFFF2B8B5),
)

@Composable
fun NewsFeedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
