package com.auramusic.offline.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * Animated spring scale feedback on press without attaching an extra click handler.
 * Uses Compose InteractionSource so it never intercepts or drops pointer events.
 */
fun Modifier.bounceScale(
    interactionSource: MutableInteractionSource,
    scaleDown: Float = 0.88f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "bounceScaleAnim"
    )
    this.scale(scale)
}

/**
 * Convenience bounceScale for generic modifiers with internal interaction source.
 */
fun Modifier.bounceScale(scaleDown: Float = 0.88f): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "bounceScaleAnim"
    )
    this.scale(scale)
}

/**
 * Animated spring scale feedback combined with an onClick handler on a Box.
 * Powered by Jetpack Compose InteractionSource to guarantee 100% reliable tap detection,
 * zero touch consumption bugs, and authentic Apple spring physics.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.92f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "bounceAnim"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
