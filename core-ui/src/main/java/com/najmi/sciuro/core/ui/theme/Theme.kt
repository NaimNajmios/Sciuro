package com.najmi.sciuro.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun SciuroTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeManager = androidx.compose.runtime.remember { ThemeManager.getInstance(context) }
    val themePref = themeManager.themePreference.collectAsState().value
    val palettePref = themeManager.palettePreference.collectAsState().value

    val darkTheme = when (themePref) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = remember(palettePref, darkTheme) {
        buildColorScheme(palettePref, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun buildColorScheme(palette: PalettePreference, dark: Boolean): ColorScheme {
    val pc = paletteColors(palette, dark)
    return if (dark) {
        darkColorScheme(
            primary = pc.primary,
            onPrimary = pc.onPrimary,
            secondary = pc.secondary,
            onSecondary = pc.onSecondary,
            tertiary = pc.tertiary,
            onTertiary = pc.onTertiary,
            background = pc.background,
            surface = pc.surface,
            surfaceVariant = pc.surfaceVariant,
            onBackground = pc.onBackground,
            onSurface = pc.onSurface,
            onSurfaceVariant = pc.onSurfaceVariant,
            secondaryContainer = pc.surfaceVariant,
            onSecondaryContainer = pc.onSurfaceVariant,
            error = SignalDanger,
        )
    } else {
        lightColorScheme(
            primary = pc.primary,
            onPrimary = pc.onPrimary,
            secondary = pc.secondary,
            onSecondary = pc.onSecondary,
            tertiary = pc.tertiary,
            onTertiary = pc.onTertiary,
            background = pc.background,
            surface = pc.surface,
            surfaceVariant = pc.surfaceVariant,
            onBackground = pc.onBackground,
            onSurface = pc.onSurface,
            onSurfaceVariant = pc.onSurfaceVariant,
            secondaryContainer = pc.surfaceVariant,
            onSecondaryContainer = pc.onSurfaceVariant,
            error = SignalDanger,
        )
    }
}
