package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    primaryContainer = ElegantSecondary,
    onPrimaryContainer = ElegantOnPrimary,
    secondary = ElegantSecondary,
    onSecondary = ElegantOnPrimary,
    secondaryContainer = ElegantButtonMuted,
    onSecondaryContainer = ElegantSecondary,
    background = ElegantObsidian,
    surface = ElegantObsidian,
    surfaceVariant = ElegantSurface,
    onSurfaceVariant = ElegantMuted,
    outlineVariant = ElegantBorder,
    error = ErrorRed,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    primaryContainer = ElegantSecondary,
    onPrimaryContainer = ElegantOnPrimary,
    secondary = ElegantSecondary,
    onSecondary = ElegantOnPrimary,
    secondaryContainer = ElegantButtonMuted,
    onSecondaryContainer = ElegantSecondary,
    background = ElegantObsidian,
    surface = ElegantObsidian,
    surfaceVariant = ElegantSurface,
    onSurfaceVariant = ElegantMuted,
    outlineVariant = ElegantBorder,
    error = ErrorRed,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force Elegant Dark custom branding as requested
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
