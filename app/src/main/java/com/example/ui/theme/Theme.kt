package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    primaryContainer = ImmersivePrimaryContainer,
    secondary = ImmersiveSecondary,
    tertiary = ImmersiveGold,
    background = ImmersiveBg,
    surface = ImmersiveSurface,
    surfaceVariant = ImmersiveSurfaceVariant,
    onPrimary = ImmersiveBg,
    onSecondary = ImmersiveBg,
    onTertiary = ImmersiveBg,
    onBackground = ImmersiveTextPrimary,
    onSurface = ImmersiveTextPrimary,
    onSurfaceVariant = ImmersiveTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFF6F3FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7E0EC),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun MemoryQuestTheme(
    darkMode: String = "AUTO",
    content: @Composable () -> Unit
) {
    val isDark = when (darkMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


