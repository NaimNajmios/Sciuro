package com.najmi.sciuro.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SciuroSemanticTokens(
    val signalIncome: Color,
    val signalTransfer: Color,
    val signalWarning: Color,
    val signalDanger: Color,
)

private val lightSemantic = SciuroSemanticTokens(
    signalIncome = Color(0xFF3DAE5C),
    signalTransfer = Color(0xFF7C9CBF),
    signalWarning = Color(0xFFE8B84B),
    signalDanger = Color(0xFFE3543D),
)

private val darkSemantic = SciuroSemanticTokens(
    signalIncome = Color(0xFF56D87A),
    signalTransfer = Color(0xFF9AB8DB),
    signalWarning = Color(0xFFE8B84B),
    signalDanger = Color(0xFFFF6B5E),
)

fun semanticTokensFor(dark: Boolean): SciuroSemanticTokens {
    return if (dark) darkSemantic else lightSemantic
}

val LocalSciuroSemanticTokens = staticCompositionLocalOf { lightSemantic }

@Composable
fun SciuroSemanticTokens(dark: Boolean, content: @Composable () -> Unit) {
    val tokens = semanticTokensFor(dark)
    androidx.compose.runtime.CompositionLocalProvider(LocalSciuroSemanticTokens provides tokens) {
        content()
    }
}
