package com.najmi.sciuro.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    secondary = Color.Gray,
    tertiary = SignalTransfer,
    background = DarkSurfaceBase,
    surface = DarkSurfaceSheet,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFAAAAAA),
    error = SignalDanger
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    secondary = Color.Gray,
    tertiary = SignalTransfer,
    background = LightSurfaceBase,
    surface = LightSurfaceSheet,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF666666),
    error = SignalDanger
)

@Composable
fun SciuroTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeManager = androidx.compose.runtime.remember { ThemeManager.getInstance(context) }
    val themePref = themeManager.themePreference.collectAsState().value
    
    val darkTheme = when (themePref) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
