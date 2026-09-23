package com.workout.tracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// App accent palette — near-black surfaces + one accent color.
// BRAND: change AppGreen to your accent (and AppGreenDim to a darker shade of it).
// The setup flow sets these from your chosen color.
val AppGreen = Color(0xFF2BD576)
val AppGreenDim = Color(0xFF1E9E58)
val AppBackground = Color(0xFF0A0A0A)
val AppSurface = Color(0xFF161616)
val AppSurfaceHigh = Color(0xFF1F1F1F)
val AppOnSurface = Color(0xFFF2F2F2)
val AppMuted = Color(0xFF8A8A8A)
val AppDivider = Color(0xFF2A2A2A)
val AppField = Color(0xFF232323)

private val AppColorScheme = darkColorScheme(
    primary = AppGreen,
    onPrimary = Color(0xFF06110B),
    primaryContainer = AppGreenDim,
    onPrimaryContainer = Color.White,
    secondary = AppGreen,
    onSecondary = Color.Black,
    background = AppBackground,
    onBackground = AppOnSurface,
    surface = AppSurface,
    onSurface = AppOnSurface,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = AppMuted,
    outline = AppDivider,
    error = Color(0xFFFF6B6B),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content
    )
}
