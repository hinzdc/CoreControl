package com.sargaverse.corecontrol.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyberPrimary,
    onPrimary = CyberBackgroundDark,
    primaryContainer = CyberPrimaryVariant,
    onPrimaryContainer = TextPrimaryDark,
    secondary = CyberSecondary,
    onSecondary = TextPrimaryDark,
    background = CyberBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CyberSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberCardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = CyberPrimaryVariant,
    onPrimary = CyberSurfaceLight,
    primaryContainer = CyberPrimary,
    onPrimaryContainer = CyberBackgroundDark,
    secondary = CyberSecondary,
    onSecondary = CyberSurfaceLight,
    background = CyberBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = CyberSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CyberSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = CyberCardBorderLight
)

@Composable
fun CoreControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
