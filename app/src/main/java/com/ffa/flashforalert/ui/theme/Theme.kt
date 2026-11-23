package com.ffa.flashforalert.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val FfaBg = Color(0xFF111318)
val FfaSurface = Color(0xFF181B22)
val FfaSurface2 = Color(0xFF1F2430)
val FfaAccent = Color(0xFFFFB020)
val FfaAccentSoft = Color(0xFFFFC247)
val FfaOnBg = Color(0xFFF5F5F7)
val FfaMuted = Color(0xFF9AA3B2)
val FfaDivider = Color(0xFF2A2F3A)
val FfaDanger = Color(0xFFFF6B6B)

private val DarkColors = darkColorScheme(
    primary = FfaAccent,
    onPrimary = Color(0xFF1A1200),
    secondary = FfaAccentSoft,
    background = FfaBg,
    onBackground = FfaOnBg,
    surface = FfaSurface,
    onSurface = FfaOnBg,
    surfaceVariant = FfaSurface2,
    onSurfaceVariant = FfaMuted,
    outline = FfaDivider,
    error = FfaDanger
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB86E00),
    onPrimary = Color.White,
    secondary = FfaAccent,
    background = Color(0xFFF7F7FA),
    onBackground = Color(0xFF111318),
    surface = Color.White,
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFF0F1F5),
    onSurfaceVariant = Color(0xFF5C6470),
    outline = Color(0xFFDDE1EA),
    error = FfaDanger
)

@Composable
fun FfaTheme(
    darkTheme: Boolean = true, // primary visual identity is dark
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}