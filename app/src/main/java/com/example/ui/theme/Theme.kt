package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CarromDarkColorScheme =
  darkColorScheme(
    primary = RoyalGold,
    onPrimary = ObsidianBg,
    secondary = CyanAccent,
    onSecondary = ObsidianBg,
    tertiary = CrimsonQueen,
    onTertiary = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextMuted,
  )

@Composable
fun CarromTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = CarromDarkColorScheme, typography = Typography, content = content)
}
