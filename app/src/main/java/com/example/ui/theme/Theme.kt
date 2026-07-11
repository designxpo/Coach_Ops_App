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
    primary = CyberAccent,
    onPrimary = CyberAccentDark,
    background = CyberBgPrimary,
    onBackground = CyberTextPrimary,
    surface = CyberBgPrimary,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberBgCard,
    onSurfaceVariant = CyberTextSecondary,
    surfaceContainer = CyberBgCardElevated,
    error = CyberDanger,
    outline = CyberTextMuted
  )

private val LightColorScheme = DarkColorScheme // Default to dark always


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  // Clamp the system font scale. Layouts are designed around ~1.0; OEM "Large
  // text" presets (1.3–2.0) wrapped headers/chips into vertical strips on
  // small screens. 1.15 still honours larger-text users without breaking UI.
  val density = androidx.compose.ui.platform.LocalDensity.current
  val clampedDensity = androidx.compose.ui.unit.Density(
    density = density.density,
    fontScale = density.fontScale.coerceAtMost(1.15f)
  )
  androidx.compose.runtime.CompositionLocalProvider(
    androidx.compose.ui.platform.LocalDensity provides clampedDensity
  ) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
