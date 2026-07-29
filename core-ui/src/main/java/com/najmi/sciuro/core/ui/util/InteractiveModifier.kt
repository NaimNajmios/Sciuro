package com.najmi.sciuro.core.ui.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import com.najmi.sciuro.core.ui.theme.SciuroMotion
import com.najmi.sciuro.core.ui.theme.reducedMotion

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.bounceClick(
    onClick: () -> Unit,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val haptic = LocalHapticFeedback.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val skipAnim = reducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !skipAnim) 0.97f else 1f,
        animationSpec = SciuroMotion.micro,
        label = "bounceClick"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                SciuroHaptics.selection(haptic)
                onClick()
            },
            onLongClick = onLongClick
        )
}
