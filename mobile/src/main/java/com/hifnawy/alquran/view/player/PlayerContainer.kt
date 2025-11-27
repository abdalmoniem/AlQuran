package com.hifnawy.alquran.view.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.hifnawy.alquran.utils.FloatEx.isApproximately
import com.hifnawy.alquran.utils.ModifierEx.verticalDraggable
import com.hifnawy.alquran.viewModel.MediaViewModel
import com.hifnawy.alquran.viewModel.PlayerState

@Composable
fun BoxScope.PlayerContainer(mediaViewModel: MediaViewModel) {
    val state = mediaViewModel.playerState
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    val minHeight = 90.dp
    val minHeightPx = with(density) { minHeight.toPx() }
    val maxHeightPx = windowInfo.containerSize.height.toFloat()

    val snapThreshold = 0.25f
    val expandThreshold = maxHeightPx * snapThreshold
    val minimizeThreshold = maxHeightPx - (maxHeightPx * snapThreshold)

    val startExpanded = true
    var isSnapped by remember { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(startExpanded) }

    val heightPx = remember { Animatable(if (isExpanded) maxHeightPx else minHeightPx) }

    val progress = ((heightPx.value - minHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)
    val horizontalPadding = lerp(20.dp, 0.dp, progress)
    val verticalPadding = lerp(WindowInsets.statusBars.getBottom(density).dp + 30.dp, 0.dp, progress)
    val cardRadius = lerp(25.dp, 0.dp, progress)

    var lastSurahSelectionTimeStamp by rememberSaveable { mutableLongStateOf(state.surahSelectionTimeStamp) }

    LaunchedEffect(state.surahSelectionTimeStamp) {
        if (state.surahSelectionTimeStamp != lastSurahSelectionTimeStamp) {
            isExpanded = startExpanded
            lastSurahSelectionTimeStamp = state.surahSelectionTimeStamp
        }
    }

    LaunchedEffect(maxHeightPx) {
        val target = when {
            isExpanded -> maxHeightPx
            else       -> minHeightPx
        }

        heightPx.snapTo(target)
    }

    PlayerContainerContent(
            modifier = Modifier.verticalDraggable(
                    heightPx = heightPx,
                    minHeightPx = minHeightPx,
                    maxHeightPx = maxHeightPx,
                    minimizeThreshold = minimizeThreshold,
                    expandThreshold = expandThreshold,
                    isExpanded = isExpanded,
                    onHeight = { isExpanded = it },
                    onSnapped = {
                        isSnapped = !isSnapped
                        mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanding = false, isMinimizing = false)
                    },
                    onDragDirectionChanged = { isDraggingUp, isDraggingDown ->
                        mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanding = isDraggingUp, isMinimizing = isDraggingDown)
                    }
            ),
            state = state,
            heightPx = heightPx,
            minHeightPx = minHeightPx,
            maxHeightPx = maxHeightPx,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            cardRadius = cardRadius,
            isExpanded = isExpanded,
            isSnapped = isSnapped,
            onCloseClicked = mediaViewModel::closePlayer,
            onExpand = { isExpanded = true },
            onExpandStarted = {
                mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanding = true, isMinimizing = false)
            },
            onExpandFinished = {
                mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanding = false, isMinimizing = false)
            },
            onMinimizeStarted = {
                mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanding = false, isMinimizing = true)
            },
            onMinimizeFinished = {
                mediaViewModel.playerState = mediaViewModel.playerState.copy(isExpanding = false, isMinimizing = false)
            },
            onMinimize = { isExpanded = false },
            onSeekProgress = mediaViewModel::seekTo,
            onSkipToPreviousSurah = mediaViewModel::skipToPreviousSurah,
            onSkipToNextSurah = mediaViewModel::skipToNextSurah,
            onTogglePlayback = mediaViewModel::togglePlayback,
    )
}

@Composable
private fun BoxScope.PlayerContainerContent(
        modifier: Modifier,
        state: PlayerState,
        heightPx: Animatable<Float, *>,
        minHeightPx: Float,
        maxHeightPx: Float,
        horizontalPadding: Dp,
        verticalPadding: Dp,
        cardRadius: Dp,
        isExpanded: Boolean,
        isSnapped: Boolean,
        onCloseClicked: () -> Unit,
        onExpand: () -> Unit,
        onExpandStarted: () -> Unit,
        onExpandFinished: () -> Unit,
        onMinimize: () -> Unit,
        onMinimizeStarted: () -> Unit,
        onMinimizeFinished: () -> Unit,
        onSeekProgress: (progress: Long) -> Unit,
        onSkipToPreviousSurah: () -> Unit,
        onTogglePlayback: () -> Unit,
        onSkipToNextSurah: () -> Unit,
) {
    val density = LocalDensity.current
    val heightAnimationDuration = 300
    val miniPlayerAnimationDuration = 300
    val heightAnimationSpec = tween<Float>(durationMillis = heightAnimationDuration, easing = FastOutLinearInEasing)
    val fadeAnimationSpec = tween<Float>(durationMillis = miniPlayerAnimationDuration, easing = FastOutLinearInEasing)
    val slideAnimationSpec = tween<IntOffset>(durationMillis = miniPlayerAnimationDuration, easing = FastOutLinearInEasing)

    LaunchedEffect(isExpanded, isSnapped) {
        if (!state.isVisible) return@LaunchedEffect

        val targetValue = when {
            isExpanded -> maxHeightPx
            else       -> minHeightPx
        }

        val isExpanding = heightPx.value < targetValue
        when {
            isExpanding -> onExpandStarted()
            else        -> onMinimizeStarted()
        }

        heightPx.animateTo(targetValue = targetValue, animationSpec = heightAnimationSpec)

        when {
            isExpanding -> onExpandFinished()
            else        -> onMinimizeFinished()
        }
    }

    BackHandler(enabled = heightPx.value isApproximately maxHeightPx within 5f) { onMinimize() }

    AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .height(with(density) { heightPx.value.toDp() })
                .then(modifier),
            visible = state.isVisible,
            enter = fadeIn(animationSpec = fadeAnimationSpec) + slideInVertically(animationSpec = slideAnimationSpec, initialOffsetY = { it }),
            exit = fadeOut(animationSpec = fadeAnimationSpec) + slideOutVertically(animationSpec = slideAnimationSpec, targetOffsetY = { it })
    ) {
        Card(
                modifier = when {
                    !isExpanded -> Modifier.clickable { onExpand() }
                    else        -> Modifier
                },
                shape = RoundedCornerShape(cardRadius),
                elevation = CardDefaults.cardElevation(25.dp)
        ) {
            if (!state.isVisible) return@Card

            Player(
                    state = state,
                    heightPx = heightPx,
                    minHeightPx = minHeightPx,
                    maxHeightPx = maxHeightPx,
                    onCloseClicked = onCloseClicked,
                    onMinimize = onMinimize,
                    onSeekProgress = onSeekProgress,
                    onSkipToPreviousSurah = onSkipToPreviousSurah,
                    onTogglePlayback = onTogglePlayback,
                    onSkipToNextSurah = onSkipToNextSurah
            )
        }
    }
}

@Composable
private fun Player(
        state: PlayerState,
        heightPx: Animatable<Float, *>,
        minHeightPx: Float,
        maxHeightPx: Float,
        onCloseClicked: () -> Unit,
        onMinimize: () -> Unit,
        onSeekProgress: (Long) -> Unit,
        onSkipToPreviousSurah: () -> Unit,
        onTogglePlayback: () -> Unit,
        onSkipToNextSurah: () -> Unit
) {
    val progress = ((heightPx.value - minHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)

    // percentage of the full height
    val fullPlayerThreshold = 0.3f
    val miniPlayerThreshold = 0.7f

    val miniPlayerAlpha = when {
        progress < fullPlayerThreshold -> 1f
        progress > miniPlayerThreshold -> 0f
        else                           -> 1f - ((progress - fullPlayerThreshold) / (miniPlayerThreshold - fullPlayerThreshold))
    }

    val fullPlayerAlpha = when {
        progress < fullPlayerThreshold -> 0f
        progress > miniPlayerThreshold -> 1f
        else                           -> (progress - fullPlayerThreshold) / (miniPlayerThreshold - fullPlayerThreshold)
    }

    Box {
        if (miniPlayerAlpha > 0f) {
            Box(modifier = Modifier.alpha(miniPlayerAlpha)) {
                MiniPlayer(
                        state = state,
                        minimizeProgress = 1f - progress,
                        onTogglePlayback = onTogglePlayback,
                        onCloseClicked = onCloseClicked
                )
            }
        }

        if (fullPlayerAlpha > 0f) {
            Box(modifier = Modifier.alpha(fullPlayerAlpha)) {
                FullScreenPlayer(
                        state = state,
                        expandProgress = progress,
                        onMinimizeClicked = onMinimize,
                        onSeekProgress = onSeekProgress,
                        onSkipToPreviousSurah = onSkipToPreviousSurah,
                        onTogglePlayback = onTogglePlayback,
                        onSkipToNextSurah = onSkipToNextSurah
                )
            }
        }
    }
}