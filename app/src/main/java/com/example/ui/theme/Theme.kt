package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun MemoryQuestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
