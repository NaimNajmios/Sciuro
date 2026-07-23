package com.najmi.sciuro.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.SurfaceHero

@Composable
fun HeroPanel(
    title: String,
    heroFigure: @Composable () -> Unit,
    toggleOptions: List<String>,
    selectedToggle: String,
    onToggleSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    chartData: List<Float>? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceHero)
            .padding(top = 48.dp, bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            if (navigationIcon != null) {
                Box(modifier = Modifier.padding(end = 8.dp)) {
                    navigationIcon()
                }
            }
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                heroFigure()
            }
            
            PillToggle(
                options = toggleOptions,
                selectedOption = selectedToggle,
                onOptionSelected = onToggleSelected,
                isOnDarkSurface = true
            )
        }
        
        if (!chartData.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            WaveChart(
                data = chartData, 
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )
        }

        content()
    }
}

@Composable
fun WaveChart(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        
        val maxVal = data.maxOrNull() ?: 1f
        val minVal = data.minOrNull() ?: 0f
        val range = if (maxVal == minVal) 1f else (maxVal - minVal)
        
        val stepX = size.width / (data.size - 1)
        val path = Path()
        
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = 1f - ((value - minVal) / range)
            val y = normalizedY * (size.height - 12.dp.toPx()) + 6.dp.toPx() // padding for dot
            
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
            color = Color.White,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Highlight last point
        val lastX = (data.size - 1) * stepX
        val lastNormY = 1f - ((data.last() - minVal) / range)
        val lastY = lastNormY * (size.height - 12.dp.toPx()) + 6.dp.toPx()
        
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = SurfaceHero,
            radius = 5.dp.toPx(),
            center = Offset(lastX, lastY),
            style = Stroke(width = 1.5f.dp.toPx())
        )
    }
}

