package com.creator.audioseeker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.creator.audioseeker.ThemePreference

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    background = Color(0xFFF0FDFA),
    onBackground = Color(0xFF134E4A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF134E4A),
    surfaceVariant = Color(0xFFE8F0F3),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFF99F6E4),
    error = Color(0xFFDC2626),
    onError = Color.White,
)

private val TealDarkColors = darkColorScheme(
    primary = Color(0xFF58D6C2),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF8FF8E4),
    secondary = Color(0xFFF6C35B),
    onSecondary = Color(0xFF3A2A00),
    background = Color(0xFF081C1A),
    onBackground = Color(0xFFE7F7F3),
    surface = Color(0xFF102B28),
    onSurface = Color(0xFFE7F7F3),
    surfaceVariant = Color(0xFF193B36),
    onSurfaceVariant = Color(0xFFA4BBB5),
    outline = Color(0xFF31534D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val StandardDarkColors = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF172033),
    primaryContainer = Color(0xFF2A3452),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFF6C35B),
    onSecondary = Color(0xFF3A2A00),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    error = Color(0xFFE7AAA5),
    onError = Color(0xFF3A0807),
)

@Composable
fun AudioSeekerTheme(themePreference: ThemePreference, content: @Composable () -> Unit) {
    val useDarkSystemBars = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.DARK, ThemePreference.STANDARD_DARK -> true
        ThemePreference.LIGHT -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkSystemBars
                isAppearanceLightNavigationBars = !useDarkSystemBars
            }
        }
    }
    MaterialTheme(
        colorScheme = when (themePreference) {
            ThemePreference.SYSTEM -> if (isSystemInDarkTheme()) TealDarkColors else LightColors
            ThemePreference.DARK -> TealDarkColors
            ThemePreference.STANDARD_DARK -> StandardDarkColors
            ThemePreference.LIGHT -> LightColors
        },
        content = content,
    )
}
