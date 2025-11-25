package com.hifnawy.alquran.view.player

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.view.WindowInsetsCompat
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.viewModel.MediaViewModel
import timber.log.Timber
import java.util.Locale

@Composable
fun BoxScope.PlayerContainer(mediaViewModel: MediaViewModel) {
    val activity = LocalActivity.current
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val state = mediaViewModel.playerState

    val minHeight = 90.dp
    val minHeightPx = with(density) { minHeight.toPx() }
    val maxHeightPx = windowInfo.containerSize.height.toFloat() /* * 0.85f */

    val expandThreshold = maxHeightPx * 0.20f     // drag up > 20% to expand
    val minimizeThreshold = maxHeightPx - (maxHeightPx * 0.20f)   // drag down < 20% to minimize

    var heightPx by remember { mutableFloatStateOf(minHeightPx) }
    val animatedHeight by animateFloatAsState(targetValue = heightPx, label = "mini-player-height")

    val progress = ((heightPx - minHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)
    val horizontalPadding = lerp(20.dp, 0.dp, progress)
    val verticalPadding = lerp(20.dp, 0.dp, progress)
    val cardRadius = lerp(25.dp, 0.dp, progress)

    val dragGesture = Modifier.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { delta ->
                val oldHeight = heightPx
                heightPx = (heightPx - delta).coerceIn(minHeightPx, maxHeightPx)

                val draggingUp = heightPx > oldHeight
                val draggingDown = heightPx < oldHeight

                mediaViewModel.playerState = state.copy(isExpanding = draggingUp, isMinimizing = draggingDown)
            },
            onDragStopped = {
                val snapped = calculateHeight(
                        heightPx = heightPx,
                        minimizeThreshold = minimizeThreshold,
                        expandThreshold = expandThreshold,
                        maxHeightPx = maxHeightPx,
                        minHeightPx = minHeightPx,
                        mediaViewModel = mediaViewModel
                )
                heightPx = snapped

                // Drag ended → clear flags
                mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanding = false, isMinimizing = false)
            }
    )

    val slideAnimationSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutLinearInEasing)
    val fadeAnimationSpec = tween<Float>(durationMillis = 300, easing = FastOutLinearInEasing)

    LaunchedEffect(state.isVisible, state.isExpanded, maxHeightPx) {
        handleSystemBars(activity?.window, state.isExpanded)

        if (!state.isVisible) return@LaunchedEffect
        heightPx = when {
            state.isExpanded -> maxHeightPx
            else             -> minHeightPx
        }
    }

    BackHandler(enabled = state.isExpanded) {
        heightPx = minHeightPx
        mediaViewModel.playerState = state.copy(isExpanded = false)
    }

    Timber.debug("state: $state")

    AnimatedVisibility(
            visible = state.isVisible,
            enter = slideInVertically(animationSpec = slideAnimationSpec, initialOffsetY = { it }) + fadeIn(animationSpec = fadeAnimationSpec),
            exit = slideOutVertically(animationSpec = slideAnimationSpec, targetOffsetY = { it }) + fadeOut(animationSpec = fadeAnimationSpec),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                        horizontal = horizontalPadding,
                        vertical = verticalPadding
                )
                .height(with(LocalDensity.current) { animatedHeight.toDp() })
                .then(dragGesture)
    ) {
        ElevatedCard(
                modifier = when {
                    !state.isExpanded -> Modifier
                        .clickable {
                            heightPx = maxHeightPx
                            mediaViewModel.playerState = state.copy(isExpanded = true)
                        }

                    else              -> Modifier
                },
                shape = RoundedCornerShape(cardRadius),
                elevation = CardDefaults.elevatedCardElevation(25.dp),
                colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary
                )
        ) {
            Timber.debug("isExpanding: ${mediaViewModel.playerState.isExpanding}, isMinimizing: ${mediaViewModel.playerState.isMinimizing}")

            if (!mediaViewModel.playerState.isVisible) return@ElevatedCard

            when {
                !mediaViewModel.playerState.isExpanded -> {
                    val expandProgress = 1f - ((animatedHeight - minHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)
                    Timber.debug("expandProgress: ${String.format(Locale.ENGLISH, "%06.02f%%", expandProgress * 100f)}")

                    MiniPlayer(mediaViewModel = mediaViewModel, expandProgress = expandProgress)
                }

                else                                   -> {
                    val minimizeProgress = ((animatedHeight - minHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)
                    FullScreenPlayer(
                            minimizeProgress = minimizeProgress,
                            state = mediaViewModel.playerState,
                            onMinimizeClicked = {
                                heightPx = minHeightPx
                                mediaViewModel.playerState = state.copy(isExpanded = false)
                            },
                            onSeekProgress = { mediaViewModel.seekTo(it) },
                            onSkipToPreviousSurah = mediaViewModel::skipToPreviousSurah,
                            onTogglePlayback = mediaViewModel::togglePlayback,
                            onSkipToNextSurah = mediaViewModel::skipToNextSurah
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun handleSystemBars(window: Window?, isExpanded: Boolean) {
    // Return early if the window is null
    if (window == null) return

    // Set layout flags to draw content edge-to-edge
    // This allows the content to be drawn under the system bars.
    window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                          )

    // Clear any previous color settings for a consistent edge-to-edge look
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT

    when {
        // MAKE BARS TRANSPARENT / EDGE-TO-EDGE
        isExpanded -> {
            when {
                // API 30+ (R) - Use WindowInsetsController and appropriate flags
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // Modern method for edge-to-edge/transparent status bar
                    window.insetsController?.show(WindowInsetsCompat.Type.systemBars())
                    // Set light status and navigation bar colors if needed,
                    // otherwise their background is transparent anyway due to above colors
                    // window.insetsController?.setSystemBarsAppearance(
                    //     WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    //     WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    // )
                }

                // Deprecated approach for older APIs (< API 30)
                else                                           -> {
                    // The flags set at the start handle transparency for older APIs
                    // No need to set the fullscreen/hide flags
                }
            }
        }

        // EXIT TRANSPARENT MODE / RESTORE DEFAULT BARS
        else       -> {
            // API 30+ (R) - Restore default system bars appearance/color if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.show(WindowInsetsCompat.Type.systemBars())
                // Optionally restore previous system bars appearance here
            } else {
                // Restore original flags (no flags is generally equivalent to View.SYSTEM_UI_FLAG_VISIBLE)
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
            // You may also need to restore default colors if your theme defines them:
            // window.statusBarColor = context.getColor(R.color.default_status_bar_color)
            // window.navigationBarColor = context.getColor(R.color.default_navigation_bar_color)
        }
    }
}

private fun calculateHeight(
        heightPx: Float,
        minimizeThreshold: Float,
        expandThreshold: Float,
        maxHeightPx: Float,
        minHeightPx: Float,
        mediaViewModel: MediaViewModel
): Float = when {
    // MINIMIZE if dragged below minimizeThreshold
    heightPx < minimizeThreshold && mediaViewModel.playerState.isExpanded -> {
        mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanded = false)
        minHeightPx
    }

    // EXPAND if dragged above expandThreshold
    heightPx > expandThreshold && !mediaViewModel.playerState.isExpanded  -> {
        mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanded = true)
        maxHeightPx
    }

    // OTHERWISE → snap to closest
    else                                                                  -> {
        val middle = (maxHeightPx + minHeightPx) / 2f
        if (heightPx > middle) {
            mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanded = true)
            maxHeightPx
        } else {
            mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanded = false)
            minHeightPx
        }
    }
}
