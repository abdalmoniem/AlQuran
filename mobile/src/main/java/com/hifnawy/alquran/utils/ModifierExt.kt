package com.hifnawy.alquran.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import com.hifnawy.alquran.utils.ModifierExt.AnimationType.FallDown
import com.hifnawy.alquran.utils.ModifierExt.AnimationType.None
import com.hifnawy.alquran.utils.ModifierExt.AnimationType.RiseUp
import kotlinx.coroutines.launch

/**
 * Util class providing extension functions for [Modifier].
 *
 * @author AbdElMoniem ElHifnawy
 */
object ModifierExt {

    /**
     * Animation type
     *
     * @property RiseUp - Animated item [translationY][GraphicsLayerScope.translationY] will rise up from below it's final position
     * @property FallDown - Animated item [translationY][GraphicsLayerScope.translationY] will fall down from above it's final position
     * @property None - Animated item [translationY][GraphicsLayerScope.translationY] will not be animated
     *
     * @author AbdElMoniem ElHifnawy
     */
    enum class AnimationType(val value: Float) {

        /**
         * item [translationY][GraphicsLayerScope.translationY] will rise up from below it's final position
         */
        RiseUp(-1f),

        /**
         * item [translationY][GraphicsLayerScope.translationY] will fall down from above it's final position
         */
        FallDown(1f),

        /**
         * item [translationY][GraphicsLayerScope.translationY] will not be animated
         */
        None(0f)
    }

    /**
     * Animate list item position, scale and alpha
     *
     * @param durationMs Duration of the animation in milliseconds
     * @param animationType Type of animation to perform
     */
    fun Modifier.animateItemPosition(durationMs: Int = 300, animationType: AnimationType = FallDown): Modifier = composed {
        val alpha = remember { Animatable(0f) }
        val scale = remember { Animatable(1.5f) }
        val translation = remember { Animatable(animationType.value) }

        LaunchedEffect(Unit) {
            launch {
                alpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
                )
            }

            launch {
                scale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
                )
            }

            launch {
                translation.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
                )
            }
        }

        graphicsLayer {
            this.alpha = alpha.value

            this.scaleX = scale.value
            this.scaleY = scale.value

            // translationY works in *pixels*, so multiply by height
            this.translationY = translation.value * size.height
        }
    }
}
