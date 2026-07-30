package com.najmi.sciuro.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.najmi.sciuro.core.ui.theme.reducedMotion

@Composable
fun Modifier.shimmerEffect(): Modifier {
    if (reducedMotion()) return this

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return this.drawBehind {
        drawRect(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(translateAnim - 300f, 0f),
                end = Offset(translateAnim, 0f)
            ),
            size = size
        )
    }
}

@Composable
private fun ShimmerBlock(
    width: Dp,
    height: Dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .shimmerEffect()
    )
}

@Composable
fun TransactionSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBlock(width = 140.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBlock(width = 100.dp, height = 12.dp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        ShimmerBlock(width = 80.dp, height = 16.dp, shape = RoundedCornerShape(4.dp))
    }
}

@Composable
fun DashboardSkeleton(transactionCount: Int = 6) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 72.dp, start = 24.dp, end = 24.dp)
            ) {
                ShimmerBlock(width = 120.dp, height = 14.dp)
                Spacer(modifier = Modifier.height(16.dp))
                ShimmerBlock(width = 200.dp, height = 36.dp, shape = RoundedCornerShape(6.dp))
                Spacer(modifier = Modifier.height(24.dp))
                ShimmerBlock(width = 280.dp, height = 48.dp, shape = RoundedCornerShape(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 24.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    ShimmerBlock(width = 80.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        repeat(transactionCount) {
            TransactionSkeletonRow()
            if (it < transactionCount - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun BudgetCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .shimmerEffect()
        )
    }
}

@Composable
fun AccountCardSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBlock(width = 160.dp, height = 16.dp)
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBlock(width = 80.dp, height = 12.dp)
        }
        ShimmerBlock(width = 100.dp, height = 20.dp, shape = RoundedCornerShape(4.dp))
    }
}
