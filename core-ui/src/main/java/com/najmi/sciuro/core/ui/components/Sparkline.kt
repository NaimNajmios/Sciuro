package com.najmi.sciuro.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    lineWidth: Dp = 2.dp,
    height: Dp = 48.dp,
    showDot: Boolean = true
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    if (data.size < 2) return

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(height)
    ) {
        val maxVal = data.maxOrNull() ?: 1f
        val minVal = data.minOrNull() ?: 0f
        val range = if (maxVal == minVal) 1f else (maxVal - minVal)

        val stepX = size.width / (data.size - 1)
        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = 1f - ((value - minVal) / range)
            val y = normalizedY * (size.height - 12.dp.toPx()) + 6.dp.toPx()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevNormY = 1f - ((data[index - 1] - minVal) / range)
                val prevY = prevNormY * (size.height - 12.dp.toPx()) + 6.dp.toPx()
                val cpX = (prevX + x) / 2
                path.cubicTo(cpX, prevY, cpX, y, x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = lineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        if (showDot && data.isNotEmpty()) {
            val lastX = (data.size - 1) * stepX
            val lastNormY = 1f - ((data.last() - minVal) / range)
            val lastY = lastNormY * (size.height - 12.dp.toPx()) + 6.dp.toPx()

            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = surfaceColor,
                radius = 5.dp.toPx(),
                center = Offset(lastX, lastY),
                style = Stroke(width = 1.5f.dp.toPx())
            )
        }
    }
}
