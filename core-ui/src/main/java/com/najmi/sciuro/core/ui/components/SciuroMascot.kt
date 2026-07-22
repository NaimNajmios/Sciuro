package com.najmi.sciuro.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Placeholder component for the upcoming Lottie Mascot Animations.
 * When the .lottie files are ready, this component will be replaced with LottieAnimation.
 * For now, it uses basic Compose shapes and animations to reserve space and logic.
 */
@Composable
fun SciuroMascot(
    animationState: MascotState,
    modifier: Modifier = Modifier,
    iterations: Int = 1, // LottieConstants.IterateForever equivalent logic can be added later
    isPlaying: Boolean = true
) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        when (animationState) {
            MascotState.IDLE -> MascotPlaceholder(Icons.Filled.Face, "Idle")
            MascotState.THINKING -> ThinkingPlaceholder()
            MascotState.CELEBRATE -> CelebratePlaceholder()
            MascotState.EMPTY -> MascotPlaceholder(Icons.Filled.Search, "Empty")
            MascotState.REFRESH -> RefreshPlaceholder(isPlaying)
            MascotState.ERROR -> MascotPlaceholder(Icons.Filled.Warning, "Error", MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MascotPlaceholder(icon: ImageVector, description: String, color: Color = MaterialTheme.colorScheme.primary) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = Modifier.size(64.dp),
        tint = color
    )
}

@Composable
private fun ThinkingPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking_alpha"
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (index == 1) alpha else 1f - alpha), CircleShape)
            )
        }
    }
}

@Composable
private fun CelebratePlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "celebrate")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrate_scale"
    )

    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = "Celebrate",
        modifier = Modifier.size(64.dp).scale(scale),
        tint = com.najmi.sciuro.core.ui.theme.SignalWarning // Using warning/amber for celebration star
    )
}

@Composable
private fun RefreshPlaceholder(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_rotate"
    )

    Icon(
        imageVector = Icons.Filled.Refresh,
        contentDescription = "Refresh",
        modifier = Modifier.size(64.dp).rotate(if (isPlaying) rotation else 0f),
        tint = MaterialTheme.colorScheme.primary
    )
}
