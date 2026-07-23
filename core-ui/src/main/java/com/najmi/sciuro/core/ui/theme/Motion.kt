package com.najmi.sciuro.core.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun reducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: SecurityException) {
            false
        }
    }
}
