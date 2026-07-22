package com.example.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role

/**
 * Apple-style interaction & motion primitives, translated to Compose.
 *
 * The through-line from Apple's "Designing Fluid Interfaces": an interface feels
 * alive when it responds on press-down, moves with springs (not fixed tweens),
 * and honours the user's reduced-motion setting. These helpers make that the
 * cheap default across the app.
 */

/** Snappy, slightly-overshooting spring for touch feedback (damping ~0.7). */
private val PressSpring get() = spring<Float>(
    dampingRatio = 0.7f,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * True when the user has turned system animations off ("Remove animations" /
 * developer animator-duration-scale = 0). Read once and cached.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) { false }
    }
}

/**
 * Drop-in replacement for `Modifier.clickable { }` that also scales down on
 * press-down (Apple principle #1: respond on press, not release). Keeps the
 * Material ripple. Honours reduced-motion.
 */
fun Modifier.bounceClick(
    pressedScale: Float = 0.97f,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduce = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduce) pressedScale else 1f,
        animationSpec = PressSpring,
        label = "bounceScale"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * Attach press-down scaling to a surface that keeps its OWN clickable/onClick
 * (e.g. a Material `Card`, or a `clickable` that needs its own interaction
 * source). Pass the same [interactionSource] you gave the clickable.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduce = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduce) pressedScale else 1f,
        animationSpec = PressSpring,
        label = "pressScale"
    )
    this.scale(scale)
}

/**
 * Spring-smoothed progress value for bars/rings so they FILL to a new value
 * instead of snapping (Apple principle #4). Feed the result into
 * `LinearProgressIndicator(progress = { animatedProgress(target) })`.
 */
@Composable
fun animatedProgress(target: Float): Float {
    val reduce = rememberReducedMotion()
    val animated by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )
    return if (reduce) target.coerceIn(0f, 1f) else animated
}

/**
 * Shimmer placeholder for skeleton loading — a moving highlight across a card
 * shape. Use on a sized Box in place of a centred spinner. Falls back to a
 * static card fill under reduced-motion.
 */
fun Modifier.shimmerLoading(cornerRadius: Int = 16): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius.dp())
    if (rememberReducedMotion()) {
        return@composed this.clip(shape).background(CyberBgCard)
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    this
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(CyberBgCard, CyberBgCardElevated, CyberBgCard),
                start = Offset(translate, 0f),
                end = Offset(translate + 400f, 0f)
            )
        )
}

// Small local helper so this file doesn't need a Dp import ceremony everywhere.
private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
