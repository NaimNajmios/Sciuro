package com.najmi.sciuro.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.najmi.sciuro.core.ui.theme.IBMPlexMono
import java.util.Locale

@Composable
fun HeroFigure(
    amount: Double,
    currency: String = "RM",
    decimals: Int = 2,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val formatted = String.format(Locale.US, "%,.${decimals}f", amount)
    val prefixSize = (style.fontSize.value * 0.55f).sp

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = prefixSize, color = color.copy(alpha = 0.55f))) {
                append("$currency ")
            }
            withStyle(SpanStyle(fontSize = style.fontSize, fontWeight = FontWeight.Medium, color = color)) {
                append(formatted)
            }
        },
        fontFamily = IBMPlexMono,
        maxLines = 1,
        style = style,
        modifier = modifier
    )
}

@Composable
fun HeroFigurePair(
    first: Double,
    second: Double,
    currency: String = "RM",
    decimals: Int = 0,
    style: TextStyle = MaterialTheme.typography.headlineLarge,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroFigure(
            amount = first,
            currency = currency,
            decimals = decimals,
            style = style,
            color = color
        )
        Text(
            text = "  /  ",
            style = style,
            color = color.copy(alpha = 0.35f),
            fontFamily = IBMPlexMono
        )
        HeroFigure(
            amount = second,
            currency = currency,
            decimals = decimals,
            style = style,
            color = color
        )
    }
}
