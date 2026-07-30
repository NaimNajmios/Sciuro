package com.najmi.sciuro.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

fun Color.relativeLuminance(): Double {
    fun linearize(c: Float): Double {
        val srgb = (c.coerceIn(0f, 1f)).toDouble()
        return if (srgb <= 0.04045) srgb / 12.92 else ((srgb + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linearize(red) +
            0.7152 * linearize(green) +
            0.0722 * linearize(blue)
}

fun Color.contrastRatio(other: Color): Double {
    val l1 = this.relativeLuminance()
    val l2 = other.relativeLuminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

fun Color.meetsWcagAA(against: Color, isLargeText: Boolean = false): Boolean {
    return this.contrastRatio(against) >= (if (isLargeText) 3.0 else 4.5)
}

fun Color.meetsWcagAAA(against: Color, isLargeText: Boolean = false): Boolean {
    return this.contrastRatio(against) >= (if (isLargeText) 4.5 else 7.0)
}

fun Color.enforceContrast(
    against: Color,
    minRatio: Double = 4.5,
    lightenIfDarkBackground: Boolean = true,
): Color {
    if (this.contrastRatio(against) >= minRatio) return this
    val bgLuminance = against.relativeLuminance()
    val bgIsDark = bgLuminance < 0.18
    var adjusted = this
    val step = 0.02f
    val maxIterations = 150
    for (i in 0 until maxIterations) {
        if (adjusted.contrastRatio(against) >= minRatio) break
        val shift = if (bgIsDark == lightenIfDarkBackground) step else -step
        adjusted = Color(
            (adjusted.red + shift).coerceIn(0f, 1f),
            (adjusted.green + shift).coerceIn(0f, 1f),
            (adjusted.blue + shift).coerceIn(0f, 1f),
            adjusted.alpha
        )
    }
    return adjusted
}

fun blendOnto(foreground: Color, background: Color, alpha: Float): Color {
    val a = alpha.coerceIn(0f, 1f)
    return Color(
        red = foreground.red * a + background.red * (1f - a),
        green = foreground.green * a + background.green * (1f - a),
        blue = foreground.blue * a + background.blue * (1f - a),
        alpha = 1f,
    )
}

fun deriveContainerColors(
    seed: Color,
    onSeed: Color,
    surface: Color,
    dark: Boolean,
): Pair<Color, Color> {
    val containerAlpha = if (dark) 0.15f else 0.08f
    val container = blendOnto(seed, surface, containerAlpha)
    val onContainer = if (dark) {
        seed
    } else {
        val darkened = seed.darkenForContrast(against = container, minRatio = 4.5)
        if (darkened.contrastRatio(container) >= 4.5) darkened
        else onSeed
    }
    return container to onContainer
}

private fun Color.darkenForContrast(against: Color, minRatio: Double): Color {
    if (this.contrastRatio(against) >= minRatio) return this
    var result = this
    val step = 0.03f
    for (i in 0 until 100) {
        result = Color(
            (result.red - step).coerceIn(0f, 1f),
            (result.green - step).coerceIn(0f, 1f),
            (result.blue - step).coerceIn(0f, 1f),
            result.alpha
        )
        if (result.contrastRatio(against) >= minRatio) return result
    }
    return Color.Black
}
