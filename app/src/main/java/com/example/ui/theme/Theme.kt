package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldGreen,
    secondary = CyanAccent,
    tertiary = RoseCrimson,
    background = DeepObsidian,
    surface = SurfaceCard,
    onPrimary = DeepObsidian,
    onSecondary = DeepObsidian,
    onTertiary = DeepObsidian,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted
  )

private val LightColorScheme = DarkColorScheme // Force dark theme for this app

@Composable
fun AegisFireTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Force custom theme colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
