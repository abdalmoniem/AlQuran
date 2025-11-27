package com.hifnawy.alquran.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import com.hifnawy.alquran.utils.FlowEx.throttleFirst
import com.hifnawy.alquran.utils.ModifierEx.AnimationType.FallDown
import com.hifnawy.alquran.utils.ModifierEx.AnimationType.None
import com.hifnawy.alquran.utils.ModifierEx.AnimationType.RiseUp
import com.hifnawy.alquran.utils.ModifierEx.verticalDraggable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Util class providing extension functions for [Modifier].
 *
 * @author AbdElMoniem ElHifnawy
 */
object ModifierEx {

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

    /**
     * A modifier that enables vertical dragging on a composable to control its height, typically
     * for creating an expandable/collapsible panel.
     *
     * This modifier handles the drag gestures, updates the height of the component, and determines
     * whether the component should snap to an expanded or minimized state when the drag is released.
     * It also provides callbacks for drag direction changes and when the final snap state is determined.
     *
     * @param heightPx [Animatable<Float, *>][Animatable] The animatable current height of the component in pixels. This state is updated during the drag.
     * @param minHeightPx [Float] The minimum height the component can be dragged to.
     * @param maxHeightPx [Float] The maximum height the component can be dragged to.
     * @param minimizeThreshold [Float] The height in pixels below which the component will snap to its minimized state if it was previously expanded.
     * @param expandThreshold [Float] The height in pixels above which the component will snap to its expanded state if it was previously minimized.
     * @param isExpanded [Boolean] The current state (expanded or minimized) of the component.
     * @param onSnapped [(shouldExpand: Boolean) -> Unit][onSnapped] A callback invoked when the drag gesture ends, providing a boolean indicating whether the component should
     *                  expand (`true`) or minimize (`false`). This is typically used to trigger an animation to the final state.
     * @param onHeight [(isExpanded: Boolean) -> Unit][onHeight] A callback invoked when the drag gesture ends, providing the target expansion state.
     * @param onDragDirectionChanged [(isDraggingUp: Boolean, isDraggingDown: Boolean) -> Unit][onDragDirectionChanged] A debounced callback that indicates the current drag
     *                  direction (up or down).
     * @return [Modifier] A [Modifier] that applies the vertical drag gesture handling.
     */
    @Composable
    fun Modifier.verticalDraggable(
            heightPx: Animatable<Float, *>,
            minHeightPx: Float,
            maxHeightPx: Float,
            minimizeThreshold: Float,
            expandThreshold: Float,
            isExpanded: Boolean,
            onSnapped: (shouldExpand: Boolean) -> Unit,
            onHeight: (isExpanded: Boolean) -> Unit,
            onDragDirectionChanged: (isDraggingUp: Boolean, isDraggingDown: Boolean) -> Unit
    ): Modifier {
        data class DragDirection(val isDraggingUp: Boolean, val isDraggingDown: Boolean)

        val dragDebounce = 300.milliseconds
        val coroutineScope = rememberCoroutineScope()
        val dragEvents = remember { MutableSharedFlow<DragDirection>(extraBufferCapacity = 1) }

        LaunchedEffect(Unit) {
            dragEvents
                .throttleFirst(dragDebounce)
                .onEach { onDragDirectionChanged(it.isDraggingUp, it.isDraggingDown) }
                .collect()
        }

        return draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    val oldHeight = heightPx.value
                    val newHeight = (heightPx.value - delta).coerceIn(minHeightPx, maxHeightPx)
                    val draggingUp = newHeight >= oldHeight

                    coroutineScope.launch { heightPx.snapTo(newHeight) }
                    dragEvents.tryEmit(DragDirection(isDraggingUp = draggingUp, isDraggingDown = !draggingUp))
                },
                onDragStopped = {
                    dragEvents.tryEmit(DragDirection(isDraggingUp = false, isDraggingDown = false))

                    val shouldExpand = shouldExpand(
                            heightPx = heightPx.value,
                            minimizeThreshold = minimizeThreshold,
                            expandThreshold = expandThreshold,
                            maxHeightPx = maxHeightPx,
                            minHeightPx = minHeightPx,
                            isExpanded = isExpanded
                    )

                    onHeight(shouldExpand)
                    onSnapped(shouldExpand)
                }
        )
    }

    /**
     * Determines whether a vertically draggable component should expand or collapse
     * based on its current height and state after a drag gesture has finished.
     *
     * The logic is as follows:
     * 1. If the component is currently collapsed (![isExpanded]) and has been dragged
     *    above the [expandThreshold], it should expand.
     * 2. If the component is currently expanded ([isExpanded]) and has been dragged
     *    below the [minimizeThreshold], it should collapse.
     * 3. In all other cases (i.e., when the drag ends between the thresholds), the
     *    final state is determined by whether the current height is closer to the
     *    [maxHeightPx] or [minHeightPx]. If [heightPx] is greater than the midpoint,
     *    it should expand; otherwise, it should collapse.
     *
     * @param heightPx [Float] The current height of the component in pixels at the end of the drag.
     * @param minimizeThreshold [Float] The height in pixels below which an expanded component should collapse.
     * @param expandThreshold [Float] The height in pixels above which a collapsed component should expand.
     * @param maxHeightPx [Float] The maximum possible height of the component.
     * @param minHeightPx [Float] The minimum possible height of the component.
     * @param isExpanded [Boolean] `true` if the component was in an expanded state before the drag started, `false` otherwise.
     *
     * @return [Boolean] `true` if the component should snap to the expanded state, `false` if it should snap to the collapsed state.
     *
     * @see verticalDraggable
     */
    private fun shouldExpand(heightPx: Float, minimizeThreshold: Float, expandThreshold: Float, maxHeightPx: Float, minHeightPx: Float, isExpanded: Boolean) = when {
        heightPx > expandThreshold && !isExpanded -> true
        heightPx < minimizeThreshold && isExpanded -> false

        else -> {
            val middle = (maxHeightPx + minHeightPx) / 2f
            when {
                heightPx > middle -> true
                else              -> false
            }
        }
    }
}
