package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Apple type discipline (WWDC "The Details of UI Typography"): tracking is
// size-specific — large display text wants NEGATIVE tracking (letters drift
// apart as they grow), body sits near 0, and small caps/labels want a little
// POSITIVE tracking for legibility. Leading (lineHeight) tightens as size grows.
val Typography =
  Typography(
    displaySmall = TextStyle( // Big accent number / hero
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).em
    ),
    headlineMedium = TextStyle( // Page title
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.015).em,
        color = Color.White
    ),
    titleMedium = TextStyle( // Section header
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.005).em,
        color = Color.White
    ),
    bodyMedium = TextStyle( // Body
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = Color(0xFF9CA3AF)
    ),
    labelSmall = TextStyle( // Tag chip / small caps label
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
  )

/**
 * Shared tracking values for large inline headings that still use hardcoded
 * `fontSize`. Apply `letterSpacing = HeadingTracking` on big titles so they
 * read tight and intentional (Apple: negative tracking as text grows). The full
 * migration onto MaterialTheme.typography is incremental; this is the cheap win.
 */
val HeadingTracking = (-0.02).em
val TitleTracking = (-0.01).em
