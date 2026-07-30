package com.najmi.sciuro.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay


@Composable
fun SciuroTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager.getInstance(context) }

    val themePref by themeManager.themePreference.collectAsState()
    val palettePref by themeManager.palettePreference.collectAsState()
    themeManager.scheduleChanged.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
        }
    }

    val darkTheme = when (themePref) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = remember(palettePref, darkTheme) {
        if (palettePref == PalettePreference.DYNAMIC && Build.VERSION.SDK_INT >= 31) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            buildColorScheme(palettePref, darkTheme)
        }
    }

    SciuroSemanticTokens(dark = darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun buildColorScheme(palette: PalettePreference, dark: Boolean): ColorScheme {
    val pc = paletteColors(palette, dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = pc.primary,
            onPrimary = pc.onPrimary,
            primaryContainer = pc.primaryContainer,
            onPrimaryContainer = pc.onPrimaryContainer,
            secondary = pc.secondary,
            onSecondary = pc.onSecondary,
            secondaryContainer = pc.surfaceVariant,
            onSecondaryContainer = pc.onSurfaceVariant,
            tertiary = pc.tertiary,
            onTertiary = pc.onTertiary,
            tertiaryContainer = pc.tertiaryContainer,
            onTertiaryContainer = pc.onTertiaryContainer,
            background = pc.background,
            onBackground = pc.onBackground,
            surface = pc.surface,
            onSurface = pc.onSurface,
            surfaceVariant = pc.surfaceVariant,
            onSurfaceVariant = pc.onSurfaceVariant,
            error = SignalDanger,
            errorContainer = pc.errorContainer,
            onErrorContainer = pc.onErrorContainer,
        )
    } else {
        lightColorScheme(
            primary = pc.primary,
            onPrimary = pc.onPrimary,
            primaryContainer = pc.primaryContainer,
            onPrimaryContainer = pc.onPrimaryContainer,
            secondary = pc.secondary,
            onSecondary = pc.onSecondary,
            secondaryContainer = pc.surfaceVariant,
            onSecondaryContainer = pc.onSurfaceVariant,
            tertiary = pc.tertiary,
            onTertiary = pc.onTertiary,
            tertiaryContainer = pc.tertiaryContainer,
            onTertiaryContainer = pc.onTertiaryContainer,
            background = pc.background,
            onBackground = pc.onBackground,
            surface = pc.surface,
            onSurface = pc.onSurface,
            surfaceVariant = pc.surfaceVariant,
            onSurfaceVariant = pc.onSurfaceVariant,
            error = SignalDanger,
            errorContainer = pc.errorContainer,
            onErrorContainer = pc.onErrorContainer,
        )
    }
    assert(validatePaletteContrast(pc)) {
        buildPaletteAssertionMessage(pc, palette, dark)
    }
    return scheme
}

private fun validatePaletteContrast(pc: PaletteColors): Boolean {
    val pairs = listOf(
        pc.primary to pc.onPrimary,
        pc.secondary to pc.onSecondary,
        pc.tertiary to pc.onTertiary,
        pc.background to pc.onBackground,
        pc.surface to pc.onSurface,
        pc.surfaceVariant to pc.onSurfaceVariant,
        pc.primaryContainer to pc.onPrimaryContainer,
        pc.tertiaryContainer to pc.onTertiaryContainer,
    )
    return pairs.all { (bg, fg) -> fg.contrastRatio(bg) >= 3.0 }
}

private fun buildPaletteAssertionMessage(pc: PaletteColors, palette: PalettePreference, dark: Boolean): String {
    val failures = mutableListOf<String>()
    val checks = mapOf(
        "primary→onPrimary" to (pc.primary to pc.onPrimary),
        "secondary→onSecondary" to (pc.secondary to pc.onSecondary),
        "tertiary→onTertiary" to (pc.tertiary to pc.onTertiary),
        "background→onBackground" to (pc.background to pc.onBackground),
        "surface→onSurface" to (pc.surface to pc.onSurface),
        "surfaceVariant→onSurfaceVariant" to (pc.surfaceVariant to pc.onSurfaceVariant),
        "primaryContainer→onPrimaryContainer" to (pc.primaryContainer to pc.onPrimaryContainer),
        "tertiaryContainer→onTertiaryContainer" to (pc.tertiaryContainer to pc.onTertiaryContainer),
    )
    for ((name, pair) in checks) {
        val (bg, fg) = pair
        val ratio = fg.contrastRatio(bg)
        val meetsLarge = if (ratio >= 3.0) "✓" else "✗"
        val meetsBody = if (ratio >= 4.5) "✓" else "✗"
        failures.add("  $name = ${ratio.toPrecision(2)}:1  large=$meetsLarge body=$meetsBody")
    }
    return buildString {
        appendLine("=== Palette Contrast Validation FAILED ===")
        appendLine("Palette: $palette ${if (dark) "Dark" else "Light"}")
        failures.forEach { appendLine(it) }
    }
}

private fun Double.toPrecision(decimals: Int): String = "%.${decimals}f".format(this)
