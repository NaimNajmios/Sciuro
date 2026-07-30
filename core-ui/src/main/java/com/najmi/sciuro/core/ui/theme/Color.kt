package com.najmi.sciuro.core.ui.theme

import androidx.compose.ui.graphics.Color

// Semantic signals
val SignalIncome = Color(0xFF3DAE5C)
val SignalTransfer = Color(0xFF7C9CBF)
val SignalWarning = Color(0xFFE8B84B)
val SignalDanger = Color(0xFFE3543D)

// Account color presets
val AccountColorGreen = Color(0xFF4CAF50)
val AccountColorBlue = Color(0xFF2196F3)
val AccountColorRed = Color(0xFFF44336)
val AccountColorPurple = Color(0xFF9C27B0)
val AccountColorOrange = Color(0xFFFF9800)
val AccountColorGrey = Color(0xFF607D8B)
val AccountColorBlack = Color(0xFF1A1A1A)
val AccountColorBrown = Color(0xFF795548)

// ── Theme Palettes ──────────────────────────────────────────────────────────

enum class PalettePreference {
    MONOCHROME, AMBER, OCEAN, FOREST, PLUM, SLATE, DYNAMIC
}

data class PaletteColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color = primary,
    val onPrimaryContainer: Color = onPrimary,
    val tertiaryContainer: Color = tertiary,
    val onTertiaryContainer: Color = onTertiary,
    val errorContainer: Color = SignalDanger,
    val onErrorContainer: Color = Color.White,
)

private fun containerColors(
    seed: Color, onSeed: Color, surface: Color, dark: Boolean
): Pair<Color, Color> {
    val alpha = if (dark) 0.15f else 0.08f
    val container = blendOnto(seed, surface, alpha)
    val onContainer = if (dark) {
        seed
    } else {
        val darkened = darkened(seed, 0.5f)
        if (darkened.contrastRatio(container) >= 4.5) darkened else onSeed
    }
    return container to onContainer
}

private fun darkened(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}

private fun PaletteColors.withContainers(dark: Boolean): PaletteColors {
    val (pc, opc) = containerColors(primary, onPrimary, surface, dark)
    val (tc, otc) = containerColors(tertiary, onTertiary, surface, dark)
    return copy(
        primaryContainer = pc,
        onPrimaryContainer = opc,
        tertiaryContainer = tc,
        onTertiaryContainer = otc,
    )
}

private val MonochromeLight = PaletteColors(
    primary = Color(0xFF000000), onPrimary = Color.White,
    secondary = Color.Gray, onSecondary = Color.Black,
    tertiary = SignalTransfer, onTertiary = Color.Black,
    background = Color(0xFFF7F7F5), onBackground = Color.Black,
    surface = Color(0xFFFFFFFF), onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F2F2), onSurfaceVariant = Color(0xFF666666),
).withContainers(dark = false)

private val MonochromeDark = PaletteColors(
    primary = Color(0xFFFFFFFF), onPrimary = Color(0xFF1C1C1E),
    secondary = Color.Gray, onSecondary = Color.White,
    tertiary = SignalTransfer, onTertiary = Color.White,
    background = Color(0xFF121316), onBackground = Color.White,
    surface = Color(0xFF1C1D21), onSurface = Color.White,
    surfaceVariant = Color(0xFF25262A), onSurfaceVariant = Color(0xFFAAAAAA),
).withContainers(dark = true)

private val AmberLight = PaletteColors(
    primary = Color(0xFFD97757), onPrimary = Color.White,
    secondary = Color(0xFFA67C6B), onSecondary = Color.White,
    tertiary = Color(0xFFE8A87C), onTertiary = Color(0xFF2C1810),
    background = Color(0xFFFDF8F2), onBackground = Color(0xFF2C1810),
    surface = Color(0xFFFFFBF9), onSurface = Color(0xFF2C1810),
    surfaceVariant = Color(0xFFF4EDE5), onSurfaceVariant = Color(0xFF9A8578),
).withContainers(dark = false)

private val AmberDark = PaletteColors(
    primary = Color(0xFFE8A87C), onPrimary = Color(0xFF2C1810),
    secondary = Color(0xFFA67C6B), onSecondary = Color.White,
    tertiary = Color(0xFFD97757), onTertiary = Color.White,
    background = Color(0xFF1A1512), onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF221D19), onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF2C2520), onSurfaceVariant = Color(0xFF9A8578),
).withContainers(dark = true)

private val OceanLight = PaletteColors(
    primary = Color(0xFF2563EB), onPrimary = Color.White,
    secondary = Color(0xFF475569), onSecondary = Color.White,
    tertiary = Color(0xFF38BDF8), onTertiary = Color(0xFF0C4A6E),
    background = Color(0xFFF5F8FC), onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F7), onSurfaceVariant = Color(0xFF64748B),
).withContainers(dark = false)

private val OceanDark = PaletteColors(
    primary = Color(0xFF60A5FA), onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF94A3B8), onSecondary = Color(0xFF0F172A),
    tertiary = Color(0xFF38BDF8), onTertiary = Color(0xFF0C4A6E),
    background = Color(0xFF0C1421), onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF151E30), onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B), onSurfaceVariant = Color(0xFF94A3B8),
).withContainers(dark = true)

private val ForestLight = PaletteColors(
    primary = Color(0xFF2E8B57), onPrimary = Color.White,
    secondary = Color(0xFF4A7C59), onSecondary = Color.White,
    tertiary = Color(0xFF34D399), onTertiary = Color(0xFF064E3B),
    background = Color(0xFFF4F8F5), onBackground = Color(0xFF052E16),
    surface = Color(0xFFF9FCFA), onSurface = Color(0xFF052E16),
    surfaceVariant = Color(0xFFECF1ED), onSurfaceVariant = Color(0xFF5A7A63),
).withContainers(dark = false)

private val ForestDark = PaletteColors(
    primary = Color(0xFF4ADE80), onPrimary = Color(0xFF052E16),
    secondary = Color(0xFF6EE7A7), onSecondary = Color(0xFF052E16),
    tertiary = Color(0xFF34D399), onTertiary = Color(0xFF064E3B),
    background = Color(0xFF08140C), onBackground = Color(0xFFD4EDDA),
    surface = Color(0xFF0E1C12), onSurface = Color(0xFFD4EDDA),
    surfaceVariant = Color(0xFF15271B), onSurfaceVariant = Color(0xFF74A884),
).withContainers(dark = true)

private val PlumLight = PaletteColors(
    primary = Color(0xFF7C3AED), onPrimary = Color.White,
    secondary = Color(0xFF6D6A8F), onSecondary = Color.White,
    tertiary = Color(0xFFA78BFA), onTertiary = Color(0xFF3B0764),
    background = Color(0xFFF9F7FC), onBackground = Color(0xFF1E0F3D),
    surface = Color(0xFFFEFDFF), onSurface = Color(0xFF1E0F3D),
    surfaceVariant = Color(0xFFF2EFF7), onSurfaceVariant = Color(0xFF6B5E8A),
).withContainers(dark = false)

private val PlumDark = PaletteColors(
    primary = Color(0xFFA78BFA), onPrimary = Color(0xFF3B0764),
    secondary = Color(0xFFC4B5FD), onSecondary = Color(0xFF3B0764),
    tertiary = Color(0xFF7C3AED), onTertiary = Color.White,
    background = Color(0xFF110C1F), onBackground = Color(0xFFE0D8F0),
    surface = Color(0xFF181328), onSurface = Color(0xFFE0D8F0),
    surfaceVariant = Color(0xFF221A35), onSurfaceVariant = Color(0xFF9A8CB8),
).withContainers(dark = true)

private val SlateLight = PaletteColors(
    primary = Color(0xFF374151), onPrimary = Color.White,
    secondary = Color(0xFF6B7280), onSecondary = Color.White,
    tertiary = Color(0xFF4B5563), onTertiary = Color.White,
    background = Color(0xFFF6F7F8), onBackground = Color(0xFF1F2937),
    surface = Color(0xFFFDFDFE), onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFF0F1F3), onSurfaceVariant = Color(0xFF64748B),
).withContainers(dark = false)

private val SlateDark = PaletteColors(
    primary = Color(0xFF9CA3AF), onPrimary = Color(0xFF1F2937),
    secondary = Color(0xFFD1D5DB), onSecondary = Color(0xFF1F2937),
    tertiary = Color(0xFF6B7280), onTertiary = Color.White,
    background = Color(0xFF0D0F13), onBackground = Color(0xFFD1D5DB),
    surface = Color(0xFF16181D), onSurface = Color(0xFFD1D5DB),
    surfaceVariant = Color(0xFF1E2026), onSurfaceVariant = Color(0xFF9CA3AF),
).withContainers(dark = true)

fun paletteColors(palette: PalettePreference, dark: Boolean): PaletteColors = when (palette) {
    PalettePreference.MONOCHROME -> if (dark) MonochromeDark else MonochromeLight
    PalettePreference.AMBER -> if (dark) AmberDark else AmberLight
    PalettePreference.OCEAN -> if (dark) OceanDark else OceanLight
    PalettePreference.FOREST -> if (dark) ForestDark else ForestLight
    PalettePreference.PLUM -> if (dark) PlumDark else PlumLight
    PalettePreference.SLATE -> if (dark) SlateDark else SlateLight
    PalettePreference.DYNAMIC -> if (dark) MonochromeDark else MonochromeLight
}
