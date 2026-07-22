package com.najmi.sciuro.core.ui.theme

import androidx.compose.animation.core.*

object SciuroMotion {
    val micro: FiniteAnimationSpec<Float> = tween(120, easing = FastOutSlowInEasing)
    val transitionSpec: FiniteAnimationSpec<Float> = tween(280, easing = FastOutSlowInEasing)
    val cardMove: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val celebration: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    val count: FiniteAnimationSpec<Float> = tween(500, easing = LinearOutSlowInEasing)

    // Navigation Compose needs IntOffset/duration-based specs separately
    const val TRANSITION_DURATION_MS = 280
    const val MICRO_DURATION_MS = 120
}
