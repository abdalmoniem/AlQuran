package com.hifnawy.alquran.view.player

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.critical
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.utils.FloatEx.isApproximately
import com.hifnawy.alquran.utils.ModifierEx.verticalDraggable
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.utils.sampleSurahs
import com.hifnawy.alquran.viewModel.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A container composable that manages the display and interaction of a media player.
 * It can be either minimized at the bottom of the screen or expanded to full screen.
 * The transition between states is animated and can be controlled by user gestures (dragging).
 *
 * This container is responsible for handling the state transitions (minimized/expanded),
 * drag gestures, and animations. It delegates the actual UI rendering for the minimized
 * and full-screen player to [MiniPlayer] and [FullScreenPlayer] respectively, crossfading
 * between them based on the drag progress.
 *
 * @param state [PlayerState] The current state of the player, including visibility, expanded status, and media info.
 * @param onHeightChanged [(progress: Float) -> Unit][onHeightChanged] A callback invoked when the height of the player is changed due to an active drag, providing the current
 *                  progress (`0.0f` to `1.0f`).
 * @param onSnapped [(shouldExpand: Boolean) -> Unit][onSnapped] A callback invoked when the player snaps to either its minimized or expanded state after a drag gesture.
 *          The lambda receives a boolean indicating if the player should expand.
 * @param onDragDirectionChanged [(isDraggingUp: Boolean, isDraggingDown: Boolean) -> Unit = { _, _ -> }][onDragDirectionChanged] A callback that reports the current drag direction.
 * @param onCloseClicked [() -> Unit][onCloseClicked] A lambda to be invoked when the user clicks the close button on the player.
 * @param onExpand [() -> Unit][onExpand] A lambda to be invoked when the player is triggered to expand (e.g., by clicking the minimized player).
 * @param onExpandStarted [() -> Unit][onExpandStarted] A lambda invoked when the expansion animation begins.
 * @param onExpandFinished [() -> Unit][onExpandFinished] A lambda invoked when the expansion animation completes.
 * @param onMinimize [() -> Unit][onMinimize] A lambda to be invoked when the player is triggered to minimize (e.g., by clicking the minimize button or using the back gesture).
 * @param onMinimizeStarted [() -> Unit][onMinimizeStarted] A lambda invoked when the minimization animation begins.
 * @param onMinimizeFinished [() -> Unit][onMinimizeFinished] A lambda invoked when the minimization animation completes.
 */
@Composable
fun BoxScope.PlayerContainer(
        state: PlayerState,
        onHeightChanged: (progress: Float) -> Unit = {},
        onSnapped: (shouldExpand: Boolean) -> Unit = {},
        onDragDirectionChanged: (isDraggingUp: Boolean, isDraggingDown: Boolean) -> Unit = { _, _ -> },
        onCloseClicked: () -> Unit = {},
        onExpand: () -> Unit = {},
        onExpandStarted: () -> Unit = {},
        onExpandFinished: () -> Unit = {},
        onMinimize: () -> Unit = {},
        onMinimizeStarted: () -> Unit = {},
        onMinimizeFinished: () -> Unit = {},
        onSeekProgress: (progress: Long) -> Unit = {},
        onSkipToPreviousSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {}
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    val minHeight = 90.dp
    val minHeightPx = with(density) { minHeight.toPx() }
    val maxHeightPx = windowInfo.containerSize.height.toFloat()

    val snapThreshold = 0.25f
    val expandThreshold = maxHeightPx * snapThreshold
    val minimizeThreshold = maxHeightPx - (maxHeightPx * snapThreshold)

    val startExpanded = state.isExpanded
    var isSnapped by remember { mutableStateOf(false) }
    var isExpanded by rememberSaveable { mutableStateOf(startExpanded) }

    val heightPx = remember { Animatable(if (isExpanded) maxHeightPx else minHeightPx) }

    val progress = ((heightPx.value - minHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)
    val horizontalPadding = lerp(10.dp, 0.dp, progress)
    val verticalPadding = lerp(10.dp, 0.dp, progress)
    val cardRadius = lerp(25.dp, 0.dp, progress)

    var lastSurahSelectionTimeStamp by rememberSaveable { mutableStateOf(state.surahSelectionTimeStamp) }

    Timber.debug(state.toString())

    LaunchedEffect(state.surahSelectionTimeStamp) {
        if (state.surahSelectionTimeStamp != lastSurahSelectionTimeStamp) {
            isExpanded = startExpanded
            lastSurahSelectionTimeStamp = state.surahSelectionTimeStamp
        }
    }

    Content(
            modifier = Modifier.verticalDraggable(
                    heightPx = heightPx,
                    minHeightPx = minHeightPx,
                    maxHeightPx = maxHeightPx,
                    minimizeThreshold = minimizeThreshold,
                    expandThreshold = expandThreshold,
                    isExpanded = isExpanded,
                    onHeight = { isExpanded = it },
                    onHeightChanged = { onHeightChanged(it) },
                    onSnapped = { shouldExpand ->
                        isSnapped = !isSnapped
                        onSnapped(shouldExpand)
                    },
                    onDragDirectionChanged = { isDraggingUp, isDraggingDown -> onDragDirectionChanged(isDraggingUp, isDraggingDown) }
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
            onHeightChanged = { onHeightChanged(it) },
            onCloseClicked = onCloseClicked,
            onExpand = {
                isExpanded = true
                onExpand()
            },
            onExpandStarted = onExpandStarted,
            onExpandFinished = onExpandFinished,
            onMinimizeStarted = onMinimizeStarted,
            onMinimizeFinished = onMinimizeFinished,
            onMinimize = {
                isExpanded = false
                onMinimize()
            },
            onSeekProgress = onSeekProgress,
            onSkipToPreviousSurah = onSkipToPreviousSurah,
            onTogglePlayback = onTogglePlayback,
            onSkipToNextSurah = onSkipToNextSurah
    )
}

/**
 * A private composable that orchestrates the core visual presentation and animation
 * logic for the media player, acting as the content body for [PlayerContainer].
 * It is responsible for translating the abstract player state and drag progress into
 * tangible UI properties like size, shape, and visibility.
 *
 * ### Responsibilities:
 * #### **Enter/Exit Animation:**
 * It manages the overall visibility of the player using [AnimatedVisibility].
 * When the player becomes visible ([PlayerState.isVisible] is `true`), it animates in
 * by fading and sliding up from the bottom. Conversely, it fades and slides out
 * when hidden. This provides a smooth and non-jarring appearance and disappearance
 * of the player component on the screen.
 *
 * #### **Height Animation:**
 * A key responsibility is animating the [height] of the player card. This is
 * triggered by changes to the [isExpanded] flag and the [isSnapped] "one-shot"
 * event. When a drag gesture concludes or a click event occurs, this function
 *
 * launches a coroutine to animate the [heightPx] [Animatable] to either the
 * [minHeightPx] or [maxHeightPx]. It uses a [FastOutLinearInEasing] for a
'* natural-feeling acceleration and deceleration. Callbacks like [onExpandStarted]
 * and [onMinimizeFinished] are invoked at the appropriate lifecycle stages of this
 * animation.
 *
 * #### **Dynamic UI Properties:**
 * This composable doesn't just animate height; it also interpolates other visual
 * attributes based on the drag/animation progress. The [horizontalPadding],
 * [verticalPadding], and [cardRadius] are all dynamically calculated using [lerp]
 * between their minimized and expanded states. This creates a fluid transformation
 * where the player card seamlessly morphs from a small, rounded card at the
 *
 * @param modifier [Modifier] A [Modifier] that includes the drag gesture handling ([verticalDraggable]).
 * @param state [PlayerState] [PlayerState] The current state of the player.
 * @param heightPx [Animatable<Float, *>][Animatable] An [Animatable] float representing the current height of the player card in pixels.
 * @param minHeightPx [Float] The minimum height of the player card in pixels (when minimized).
 * @param maxHeightPx [Float] The maximum height of the player card in pixels (when expanded).
 * @param horizontalPadding [Dp] The calculated horizontal padding for the card, which changes during transition.
 * @param verticalPadding [Dp] The calculated vertical padding for the card, which changes during transition.
 * @param cardRadius [Dp] The calculated corner radius for the card, which changes during transition.
 * @param isExpanded [Boolean] A boolean indicating if the player's target state is expanded.
 * @param isSnapped [Boolean] A boolean that acts as a trigger to re-run the height animation when a drag gesture finishes.
 * @param onHeightChanged [(progress: Float) -> Unit][onHeightChanged] A callback invoked when the height of the player is changed due to an active drag, providing the current
 *                  progress (`0.0f` to `1.0f`).
 * @param onCloseClicked [() -> Unit][onCloseClicked] Lambda to be invoked when the close button is clicked.
 * @param onExpand [() -> Unit][onExpand] Lambda to be invoked to signal an expansion request.
 * @param onExpandStarted [() -> Unit][onExpandStarted] Lambda to be invoked when the expansion animation starts.
 * @param onExpandFinished [() -> Unit][onExpandFinished] Lambda to be invoked when the expansion animation finishes.
 * @param onMinimize [() -> Unit][onMinimize] Lambda to be invoked to signal a minimization request.
 * @param onMinimizeStarted [() -> Unit][onMinimizeStarted] Lambda to be invoked when the minimization animation starts.
 * @param onMinimizeFinished [() -> Unit][onMinimizeFinished] Lambda to be invoked when the minimization animation finishes.
 * @param onSeekProgress [(progress: Long) -> Unit][onSeekProgress] Callback that emits the new progress position in milliseconds when the user seeks using the progress bar.
 * @param onSkipToPreviousSurah [() -> Unit][onSkipToPreviousSurah] Lambda to be invoked when the "skip to previous" button is clicked.
 * @param onTogglePlayback [() -> Unit][onTogglePlayback] Lambda to be invoked to toggle the current playback state (play/pause).
 * @param onSkipToNextSurah [() -> Unit][onSkipToNextSurah] Lambda to be invoked when the "skip to next" button is clicked.
 *
 * @see CrossfadePlayer
 */
@Composable
private fun BoxScope.Content(
        modifier: Modifier = Modifier,
        state: PlayerState,
        heightPx: Animatable<Float, *>,
        minHeightPx: Float,
        maxHeightPx: Float,
        horizontalPadding: Dp,
        verticalPadding: Dp,
        cardRadius: Dp,
        isExpanded: Boolean,
        isSnapped: Boolean,
        onHeightChanged: (Float) -> Unit = {},
        onCloseClicked: () -> Unit = {},
        onExpand: () -> Unit = {},
        onExpandStarted: () -> Unit = {},
        onExpandFinished: () -> Unit = {},
        onMinimize: () -> Unit = {},
        onMinimizeStarted: () -> Unit = {},
        onMinimizeFinished: () -> Unit = {},
        onSeekProgress: (progress: Long) -> Unit = {},
        onSkipToPreviousSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {}
) {
    val density = LocalDensity.current

    val heightAnimationDuration = 300
    val isBackHandlerEnabled = state.isVisible && heightPx.value isApproximately maxHeightPx within 5f
    val heightAnimationSpec = tween<Float>(durationMillis = heightAnimationDuration, easing = FastOutLinearInEasing)

    LaunchedEffect(maxHeightPx) {
        val target = when {
            isExpanded -> maxHeightPx
            else       -> minHeightPx
        }

        heightPx.snapTo(target)
    }

    LaunchedEffect(isExpanded, isSnapped, state.isVisible) {
        if (!state.isVisible) {
            heightPx.snapTo(0f)
            return@LaunchedEffect
        }

        val targetValue = when {
            isExpanded -> maxHeightPx
            else       -> minHeightPx
        }

        val isExpanding = heightPx.value < targetValue
        when {
            isExpanding -> onExpandStarted()
            else        -> onMinimizeStarted()
        }

        heightPx.animateTo(targetValue = targetValue, animationSpec = heightAnimationSpec) {
            val progress = ((value - minHeightPx) / (maxHeightPx - minHeightPx)).coerceIn(0f, 1f)
            onHeightChanged(progress)
        }

        when {
            isExpanding -> onExpandFinished()
            else        -> onMinimizeFinished()
        }
    }

    OnBackHandler(
            isBackHandlerEnabled = isBackHandlerEnabled,
            onBackStarted = onMinimizeStarted,
            onBackProgress = { progress ->
                val newHeightPx = (1f - progress) * (maxHeightPx - minHeightPx) + minHeightPx
                heightPx.snapTo(newHeightPx)
            },
            onBackPressed = {
                onMinimize()
                onMinimizeFinished()
            }
    )

    Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .height(with(density) { heightPx.value.toDp() })
                .then(modifier),
            shape = RoundedCornerShape(cardRadius),
            elevation = CardDefaults.cardElevation(25.dp)
    ) {
        CrossfadePlayer(
                modifier = when {
                    !isExpanded -> Modifier.clickable { onExpand() }
                    else        -> Modifier
                },
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

/**
 * A private composable that crossfades between a [MiniPlayer] and a [FullScreenPlayer]
 * based on the vertical drag progress. It uses the calculated progress to determine
 * the alpha (opacity) of each player, creating a smooth transition effect as the
 * user drags the player container up or down.
 *
 * This function defines specific thresholds for the crossfade:
 * - The [MiniPlayer] is fully visible when the drag progress is below `fullPlayerThreshold` (e.g., 30%)
 *   and fades out as the progress moves towards `miniPlayerThreshold` (e.g., 70%).
 * - The [FullScreenPlayer] is fully transparent below `fullPlayerThreshold` and fades in as the
 *   progress moves towards `miniPlayerThreshold`, after which it is fully visible.
 *
 * This approach ensures that only the relevant player UI is visible and intractable at the extremes
 * of the collapsed and expanded states, while providing a seamless visual blend during the transition.
 *
 * @param modifier [Modifier] A [Modifier] to be applied to the crossfade player.
 * @param state [PlayerState] The current state of the player, containing information like the surah, reciter, and playback status.
 * @param heightPx [Animatable<Float, *>][Animatable] An [Animatable] float representing the current height of the player container in pixels.
 *                 This value drives the calculation of the crossfade progress.
 * @param minHeightPx [Float] The minimum height of the player in pixels (the height of the mini-player).
 * @param maxHeightPx [Float] The maximum height of the player in pixels (the height of the full-screen player).
 * @param onCloseClicked [() -> Unit][onCloseClicked] A lambda to be invoked when the close button on the [MiniPlayer] is clicked.
 * @param onMinimize [() -> Unit][onMinimize] A lambda to be invoked when the minimize button on the [MiniPlayer] is clicked.
 * @param onSeekProgress [() -> Unit][onSeekProgress] A lambda to be invoked when the seek progress changes.
 * @param onSkipToPreviousSurah [() -> Unit][onSkipToPreviousSurah] A lambda to be invoked when the skip to previous surah button is clicked.
 * @param onTogglePlayback [() -> Unit][onTogglePlayback] A lambda to be invoked when the toggle playback button is clicked.
 * @param onSkipToNextSurah [() -> Unit][onSkipToNextSurah] A lambda to be invoked when the skip to next surah button is clicked.
 *
 * @see MiniPlayer
 * @see FullScreenPlayer
 */
@Composable
private fun CrossfadePlayer(
        modifier: Modifier = Modifier,
        state: PlayerState,
        heightPx: Animatable<Float, *>,
        minHeightPx: Float,
        maxHeightPx: Float,
        onCloseClicked: () -> Unit = {},
        onMinimize: () -> Unit = {},
        onSeekProgress: (Long) -> Unit = {},
        onSkipToPreviousSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {}
) {
    val progressRange = maxHeightPx - minHeightPx
    val relativeHeight = heightPx.value - minHeightPx
    val heightProgress = (relativeHeight / progressRange).coerceIn(0f, 1f)

    // crossfade thresholds as percentages of the full height
    val fullPlayerThreshold = 0.3f
    val miniPlayerThreshold = 0.7f
    val thresholdRange = miniPlayerThreshold - fullPlayerThreshold

    val miniPlayerProgress = miniPlayerThreshold - heightProgress
    val fullPlayerProgress = heightProgress - fullPlayerThreshold

    require(thresholdRange > 0f) {
        val errorMessage = "Threshold range must be greater than 0"

        Timber.critical(errorMessage)
        errorMessage
    }

    val miniPlayerAlpha = (miniPlayerProgress / thresholdRange).coerceIn(0f, 1f)
    val fullPlayerAlpha = (fullPlayerProgress / thresholdRange).coerceIn(0f, 1f)

    Box(modifier = modifier) {
        Box(modifier = Modifier.alpha(miniPlayerAlpha)) {
            if (miniPlayerAlpha <= 0f) return@Box

            MiniPlayer(
                    state = state,
                    minimizeProgress = 1f - heightProgress,
                    onTogglePlayback = onTogglePlayback,
                    onCloseClicked = onCloseClicked
            )
        }

        Box(modifier = Modifier.alpha(fullPlayerAlpha)) {
            if (fullPlayerAlpha <= 0f) return@Box

            FullScreenPlayer(
                    state = state,
                    expandProgress = heightProgress,
                    onMinimizeClicked = onMinimize,
                    onSeekProgress = onSeekProgress,
                    onSkipToPreviousSurah = onSkipToPreviousSurah,
                    onTogglePlayback = onTogglePlayback,
                    onSkipToNextSurah = onSkipToNextSurah
            )
        }
    }
}

/**
 * A private composable that handles predictive back gestures for the player.
 * It integrates with [OnBackPressedCallback] to provide a responsive experience
 * when the user performs a back gesture, allowing the UI to react progressively
 * as the gesture unfolds.
 *
 * This handler is enabled only when the player is fully expanded. When a back gesture
 * starts, it triggers [onBackStarted] and then continuously calls [onBackProgress] with
 * the gesture's progress, allowing the caller (the `Content` composable) to animate
 * the player's height accordingly. If the gesture completes, [onBackPressed] is called
 * to finalize the minimization. If it's cancelled, the progress is reset to 0.
 *
 * @param isBackHandlerEnabled [Boolean] A boolean that controls whether the back handler is active.
 *        It should be `true` only when the player is fully expanded and ready to be minimized via a back gesture.
 * @param onBackStarted [() -> Unit][onBackStarted] A lambda invoked when the back gesture begins.
 * @param onBackProgress [suspend CoroutineScope.(Float) -> Unit][onBackProgress] A suspend lambda that receives the gesture's
 *        progress as a [Float] between 0.0 and 1.0. This is used to drive the minimization animation.
 * @param onBackPressed [() -> Unit][onBackPressed] A lambda invoked when the back gesture is completed, signaling
 *        that the player should be fully minimized.
 */
@Composable
private fun OnBackHandler(
        isBackHandlerEnabled: Boolean,
        onBackStarted: () -> Unit = {},
        onBackProgress: suspend CoroutineScope.(Float) -> Unit = {},
        onBackPressed: () -> Unit = {}
) {
    val onBackCallback = remember {
        object : OnBackPressedCallback(isBackHandlerEnabled) {
            private val coroutineScope = CoroutineScope(Dispatchers.Main)
            override fun handleOnBackStarted(backEvent: BackEventCompat) = onBackStarted()

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                coroutineScope.launch { onBackProgress(backEvent.progress) }
            }

            override fun handleOnBackPressed() = onBackPressed()

            override fun handleOnBackCancelled() {
                coroutineScope.launch { onBackProgress(0f) }
            }
        }
    }

    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(isBackHandlerEnabled) {
        onBackCallback.isEnabled = isBackHandlerEnabled
    }

    DisposableEffect(backPressedDispatcher) {
        backPressedDispatcher?.addCallback(onBackCallback)
        onDispose { onBackCallback.remove() }
    }
}

/**
 * A preview Composable for displaying the [PlayerContainer] on a phone device.
 * It demonstrates the player in its minimized state (`isExpanded = false`)
 * using sample data for the reciter and surah. The UI is set to show system UI
 * and uses an Arabic locale for context.
 */
@Composable
@Preview(device = Devices.PHONE, showSystemUi = true, locale = "ar")
private fun PlayerContainerPhonePreview() {
    val reciter = sampleReciters.random()
    val surah = sampleSurahs.random()
    val state = PlayerState(
            reciter = reciter,
            surah = surah,
            isVisible = true,
            isExpanded = false
    )
    Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
    ) {
        PlayerContainer(state = state)
    }
}

/**
 * A preview Composable for displaying the [PlayerContainer] on a tablet device.
 * It demonstrates the player in its expanded state (`isExpanded = true`)
 * using sample data for the reciter and surah. The UI is set to show system UI
 * and uses an Arabic locale for context.
 */
@Composable
@Preview(device = Devices.TABLET, showSystemUi = true, locale = "ar")
private fun PlayerContainerTabletPreview() {
    val reciter = sampleReciters.random()
    val surah = sampleSurahs.random()
    val state = PlayerState(
            reciter = reciter,
            surah = surah,
            isVisible = true,
            isExpanded = true
    )
    Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
    ) {
        PlayerContainer(state = state)
    }
}
