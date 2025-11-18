package com.hifnawy.alquran.view.composables

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun ShimmerAnimation(content: @Composable (brush: Brush) -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")

    // The animation value controls the movement.
    val translateAnim by transition.animateFloat(
            initialValue = -1500f,
            targetValue = 1500f, // Increased target to ensure smooth movement across a wider area
            animationSpec = infiniteRepeatable(
                    animation = tween(
                            durationMillis = 500,
                            easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslateAnim"
    )

    val shimmerColorShades = listOf(
            Color.LightGray.copy(alpha = 0.9f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.9f)
    )

    // The width of the gradient is now 800f (1500f - 700f).
    val brush = remember(translateAnim) {
        Brush.linearGradient(
                colors = shimmerColorShades,
                // Start from the right side of the item
                start = Offset(x = 1500f - translateAnim, y = 0f),
                // Finish further away, making the gradient pattern wider
                end = Offset(x = 700f - translateAnim, y = 0f)
        )
    }

    content(brush)
}
