package com.hifnawy.alquran.view.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.utils.DrawableResUtil.defaultSurahDrawableId
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawableId
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.hoursComponent
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.toLocalizedFormattedTime
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.utils.DeviceConfiguration
import com.hifnawy.alquran.utils.DeviceConfiguration.Companion.deviceConfiguration
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.Companion.toHeightSize
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.Companion.toWidthSize
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.utils.sampleSurahs
import com.hifnawy.alquran.viewModel.PlayerState
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.hifnawy.alquran.shared.R as Rs

/**
 * A full-screen player composable that displays media controls and information for the currently playing surah.
 * It adapts its layout based on the device's configuration (compact, portrait, landscape).
 *
 * @param state [PlayerState] The [PlayerState] containing all necessary information about the current playback,
 *   such as the surah, reciter, duration, current position, and playback status.
 * @param expandProgress [Float] A float value between `0f` and `1f` indicating the progress of the
 *   full-screen player's expansion animation. Used to fade in the content.
 * @param onMinimizeClicked [onMinimizeClicked: () -> Unit][onMinimizeClicked] A lambda function to be invoked when the user clicks the minimize or
 *   close button on the player.
 * @param onSeekProgress [onSeekProgress: (progress: Long) -> Unit][onSeekProgress] A lambda function that reports the new playback position (in milliseconds)
 *   when the user seeks using the progress slider.
 * @param onSkipToPreviousSurah [onSkipToPreviousSurah: () -> Unit][onSkipToPreviousSurah] A lambda function to be invoked when the user clicks the "skip to previous" button.
 * @param onTogglePlayback [onTogglePlayback: () -> Unit][onTogglePlayback] A lambda function to be invoked when the user clicks the play/pause button.
 * @param onSkipToNextSurah [onSkipToNextSurah: () -> Unit][onSkipToNextSurah] A lambda function to be invoked when the user clicks the "skip to next" button.
 */
@Composable
fun FullScreenPlayer(
        state: PlayerState,
        expandProgress: Float,
        onMinimizeClicked: () -> Unit = {},
        onSeekProgress: (progress: Long) -> Unit = {},
        onSkipToPreviousSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {}
) {
    val surahDrawableId = remember(state.surah?.id) { state.surah?.surahDrawableId ?: defaultSurahDrawableId }

    val playbackPosition = when {
        state.durationMs > 0 -> state.currentPositionMs.toFloat()
        else                 -> 0f
    }

    val bufferedProgress = when {
        state.durationMs > 0 -> state.bufferedPositionMs.toFloat() / state.durationMs.toFloat()
        else                 -> 0f
    }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isChangingPosition by remember { mutableStateOf(false) }

    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val deviceConfiguration = windowSize.deviceConfiguration

    windowSize.run {
        Timber.debug("size: ${minWidthDp.toWidthSize} : ${minHeightDp.toHeightSize}, deviceConfiguration: $deviceConfiguration")
    }

    LaunchedEffect(playbackPosition, isChangingPosition) {
        if (!isChangingPosition) sliderPosition = playbackPosition
    }

    Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
    ) {
        PlayerBackground(
                isVisible = state.isVisible,
                surahDrawableId = surahDrawableId,
        )

        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(expandProgress),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            if (deviceConfiguration != DeviceConfiguration.COMPACT) PlayerTopBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    onMinimizeClicked = onMinimizeClicked
            )

            when (deviceConfiguration) {
                DeviceConfiguration.COMPACT          -> PlayerContentCompact(
                        modifier = Modifier
                            .fillMaxSize(),
                        surahDrawableId = surahDrawableId,
                        state = state,
                        onSkipToNextSurah = onSkipToNextSurah,
                        onTogglePlayback = onTogglePlayback,
                        onSkipToPreviousSurah = onSkipToPreviousSurah
                )

                DeviceConfiguration.PHONE_PORTRAIT,
                DeviceConfiguration.TABLET_PORTRAIT  -> PlayerContentPortrait(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        surahDrawableId = surahDrawableId,
                        state = state,
                        bufferedProgress = bufferedProgress,
                        sliderPosition = sliderPosition,
                        isChangingPosition = isChangingPosition,
                        onSeekStarted = { isChangingPosition = true },
                        onSeekProgress = { sliderPosition = it },
                        onSeekFinished = {
                            isChangingPosition = false
                            onSeekProgress(sliderPosition.toLong())
                        },
                        onSkipToNextSurah = onSkipToNextSurah,
                        onTogglePlayback = onTogglePlayback,
                        onSkipToPreviousSurah = onSkipToPreviousSurah
                )

                DeviceConfiguration.PHONE_LANDSCAPE,
                DeviceConfiguration.TABLET_LANDSCAPE -> PlayerContentLandScape(
                        modifier = Modifier
                            .fillMaxSize()
                            .displayCutoutPadding()
                            .padding(horizontal = 10.dp),
                        surahDrawableId = surahDrawableId,
                        state = state,
                        bufferedProgress = bufferedProgress,
                        sliderPosition = sliderPosition,
                        isChangingPosition = isChangingPosition,
                        onSeekStarted = { isChangingPosition = true },
                        onSeekProgress = { sliderPosition = it },
                        onSeekFinished = {
                            isChangingPosition = false
                            onSeekProgress(sliderPosition.toLong())
                        },
                        onSkipToNextSurah = onSkipToNextSurah,
                        onTogglePlayback = onTogglePlayback,
                        onSkipToPreviousSurah = onSkipToPreviousSurah
                )
            }
        }
    }
}

/**
 * A composable that displays the player content in a portrait orientation.
 *
 * This layout is optimized for portrait mode on phones and tablets. It arranges the surah image,
 * recitation information (surah name and reciter), progress bar, and player controls vertically.
 *
 * It dynamically adjusts its layout based on the available screen height. If there isn't enough
 * vertical space to show all components (e.g., on smaller screens), it hides the standard progress
 * and control bars and instead shows a set of floating overlay controls at the bottom of the screen
 * when the user interacts with the player.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the composable.
 * @param state [PlayerState] The [PlayerState] containing the current playback information.
 * @param surahDrawableId [Int] The resource ID for the surah's artwork.
 * @param bufferedProgress [Float] The progress of the buffered media, as a float between `0.0` and `1.0`.
 * @param sliderPosition [Float] The current position of the progress slider, controlled by user interaction.
 * @param isChangingPosition [Boolean] A boolean indicating if the user is currently scrubbing the progress slider.
 * @param onSeekStarted [() -> Unit][onSeekStarted] A lambda invoked when the user starts dragging the slider.
 * @param onSeekProgress [(progress: Float) -> Unit][onSeekProgress] A lambda that reports the new slider position as the user drags it.
 * @param onSeekFinished [() -> Unit][onSeekFinished] A lambda invoked when the user releases the slider.
 * @param onSkipToNextSurah [() -> Unit][onSkipToNextSurah] A lambda to be invoked when the `skip to next` button is clicked.
 * @param onTogglePlayback [() -> Unit][onTogglePlayback] A lambda to be invoked when the `play`/`pause` button is clicked.
 * @param onSkipToPreviousSurah [() -> Unit][onSkipToPreviousSurah] A lambda to be invoked when the `skip to previous` button is clicked.
 */
@Composable
private fun PlayerContentPortrait(
        modifier: Modifier = Modifier,
        state: PlayerState,
        surahDrawableId: Int,
        bufferedProgress: Float,
        sliderPosition: Float,
        isChangingPosition: Boolean,
        onSeekStarted: () -> Unit = {},
        onSeekProgress: (progress: Float) -> Unit = {},
        onSeekFinished: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToPreviousSurah: () -> Unit = {}
) {
    val density = LocalDensity.current

    var recitationInfoHeight by remember { mutableStateOf(0.dp) }
    var playerProgressHeight by remember { mutableStateOf(0.dp) }

    MiniPlayerControlsContainer(
            modifier = modifier,
            state = state,
            contentAlignment = Alignment.BottomCenter,
    ) { areControlsVisible ->
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
            ) {
                SurahImage(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        surahDrawableId = surahDrawableId
                )
            }

            BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
            ) {
                val totalAvailableHeight = maxHeight
                val usedHeight = recitationInfoHeight + playerProgressHeight
                val remainingHeight = totalAvailableHeight - usedHeight

                // Minimum height needed for regular controls ((3 icons * 64dp) + 10dp top padding + 10dp bottom padding)
                // TODO: find a way of calculating this without hardcoding
                val minRequiredHeight = 84.dp // 64dp + 20dp padding
                val hasEnoughSpace = remainingHeight >= minRequiredHeight

                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                ) {
                    val slideAnimationSpec = tween<IntOffset>(durationMillis = 300, easing = FastOutLinearInEasing)
                    val fadeAnimationSpec = tween<Float>(durationMillis = 300, easing = FastOutLinearInEasing)

                    RecitationInfo(
                            modifier = Modifier.onSizeChanged { size -> recitationInfoHeight = with(density) { size.height.toDp() } },
                            state = state
                    )

                    AnimatedVisibility(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 10.dp)
                                .onSizeChanged { size -> playerProgressHeight = with(density) { size.height.toDp() } },
                            visible = hasEnoughSpace || playerProgressHeight > 0.dp,
                            enter = slideInVertically(animationSpec = slideAnimationSpec, initialOffsetY = { it }) + fadeIn(animationSpec = fadeAnimationSpec),
                            exit = slideOutVertically(animationSpec = slideAnimationSpec, targetOffsetY = { it }) + fadeOut(animationSpec = fadeAnimationSpec)
                    ) {
                        PlayerProgress(
                                state = state,
                                bufferedProgress = bufferedProgress,
                                sliderPosition = sliderPosition,
                                isChangingPosition = isChangingPosition,
                                onSeekStarted = onSeekStarted,
                                onSeekProgress = onSeekProgress,
                                onSeekFinished = onSeekFinished
                        )
                    }

                    AnimatedVisibility(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            visible = hasEnoughSpace,
                            enter = slideInVertically(animationSpec = slideAnimationSpec, initialOffsetY = { it }) + fadeIn(animationSpec = fadeAnimationSpec),
                            exit = slideOutVertically(animationSpec = slideAnimationSpec, targetOffsetY = { it }) + fadeOut(animationSpec = fadeAnimationSpec)
                    ) {
                        PlayerControls(
                                state = state,
                                onSkipToNextSurah = onSkipToNextSurah,
                                onTogglePlayback = onTogglePlayback,
                                onSkipToPreviousSurah = onSkipToPreviousSurah
                        )
                    }
                }

                if (hasEnoughSpace || state.isMinimizing || state.isExpanding) return@BoxWithConstraints
                OverlayPlayerControls(
                        state = state,
                        areControlsVisible = areControlsVisible,
                        onSkipToNextSurah = onSkipToNextSurah,
                        onTogglePlayback = onTogglePlayback,
                        onSkipToPreviousSurah = onSkipToPreviousSurah
                )
            }
        }
    }
}

/**
 * A composable that displays the player content in a landscape orientation.
 *
 * This layout is optimized for landscape mode on phones and tablets. It arranges the surah image on the
 * start, and the recitation information (surah name and reciter), progress bar, and player controls
 * vertically on the end. The content animates into view with a slide and fade effect.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the composable.
 * @param state [PlayerState] The [PlayerState] containing the current playback information.
 * @param surahDrawableId [Int] The resource ID for the surah's artwork.
 * @param bufferedProgress [Float] The progress of the buffered media, as a float between `0f` and `1f`.
 * @param sliderPosition [Float] The current position of the progress slider, controlled by user interaction.
 * @param isChangingPosition [Boolean] A boolean indicating if the user is currently scrubbing the progress slider.
 * @param onSeekStarted [() -> Unit][onSeekStarted] A lambda invoked when the user starts dragging the slider.
 * @param onSeekProgress [(progress: Float) -> Unit][onSeekProgress] A lambda that reports the new slider position as the user drags it.
 * @param onSeekFinished [() -> Unit][onSeekFinished] A lambda invoked when the user releases the slider.
 * @param onSkipToNextSurah [() -> Unit][onSkipToNextSurah] A lambda to be invoked when the `skip to next` button is clicked.
 * @param onTogglePlayback [() -> Unit][onTogglePlayback] A lambda to be invoked when the `play`/`pause` button is clicked.
 * @param onSkipToPreviousSurah [() -> Unit][onSkipToPreviousSurah] A lambda to be invoked when the `skip to previous` button is clicked.
 */
@Composable
private fun PlayerContentLandScape(
        modifier: Modifier = Modifier,
        state: PlayerState,
        surahDrawableId: Int,
        bufferedProgress: Float,
        sliderPosition: Float,
        isChangingPosition: Boolean,
        onSeekStarted: () -> Unit = {},
        onSeekProgress: (progress: Float) -> Unit = {},
        onSeekFinished: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToPreviousSurah: () -> Unit = {}
) {
    val animationDuration = 150
    val slideAnimationSpec = tween<IntOffset>(durationMillis = animationDuration, easing = FastOutLinearInEasing)
    val fadeAnimationSpec = tween<Float>(durationMillis = animationDuration, easing = FastOutLinearInEasing)
    var isShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(10.milliseconds)
        isShown = true
    }

    AnimatedVisibility(
            modifier = modifier,
            visible = isShown,
            enter = slideInVertically(animationSpec = slideAnimationSpec, initialOffsetY = { it }) + fadeIn(animationSpec = fadeAnimationSpec),
            exit = slideOutVertically(animationSpec = slideAnimationSpec, targetOffsetY = { it }) + fadeOut(animationSpec = fadeAnimationSpec)
    ) {
        Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
        ) {
            SurahImage(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    surahDrawableId = surahDrawableId,
                    alignment = Alignment.Center
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
            ) {
                RecitationInfo(
                        modifier = Modifier.weight(1f),
                        state = state
                )

                PlayerProgress(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        state = state,
                        bufferedProgress = bufferedProgress,
                        sliderPosition = sliderPosition,
                        isChangingPosition = isChangingPosition,
                        onSeekStarted = onSeekStarted,
                        onSeekProgress = onSeekProgress,
                        onSeekFinished = onSeekFinished
                )

                PlayerControls(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        state = state,
                        onSkipToNextSurah = onSkipToNextSurah,
                        onTogglePlayback = onTogglePlayback,
                        onSkipToPreviousSurah = onSkipToPreviousSurah
                )
            }
        }
    }
}

/**
 * A composable that displays the player content in a compact, minimal layout.
 *
 * This layout is specifically designed for very small screen sizes or compact windows where
 * space is extremely limited. It shows only the surah image and a set of overlay controls
 * that appear on user interaction. There are no visible progress bars or recitation info text
 * to maximize the visual area for the background artwork.
 *
 * The controls (`play`/`pause`, `next`, `previous`) are housed within a [MiniPlayerControlsContainer],
 * which manages their visibility. The controls appear when the screen is tapped and
 * automatically hide after a short delay if media is playing.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the composable.
 * @param state [PlayerState] The [PlayerState] containing the current playback information.
 * @param surahDrawableId [Int] The resource ID for the surah's artwork.
 * @param onSkipToNextSurah [() -> Unit][onSkipToNextSurah] A lambda to be invoked when the `skip to next` button is clicked.
 * @param onTogglePlayback [() -> Unit][onTogglePlayback] A lambda to be invoked when the `play`/`pause` button is clicked.
 * @param onSkipToPreviousSurah [() -> Unit][onSkipToPreviousSurah] A lambda to be invoked when the `skip to previous` button is clicked.
 */
@Composable
private fun PlayerContentCompact(
        modifier: Modifier = Modifier,
        state: PlayerState,
        surahDrawableId: Int,
        onSkipToNextSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToPreviousSurah: () -> Unit = {}
) {
    MiniPlayerControlsContainer(modifier = modifier.fillMaxSize(), state = state) { areControlsVisible ->
        SurahImage(
                modifier = Modifier.padding(10.dp),
                surahDrawableId = surahDrawableId,
                alignment = Alignment.Center
        )

        OverlayPlayerControls(
                state = state,
                areControlsVisible = areControlsVisible,
                onSkipToNextSurah = onSkipToNextSurah,
                onTogglePlayback = onTogglePlayback,
                onSkipToPreviousSurah = onSkipToPreviousSurah
        )
    }
}

/**
 * A container composable that manages the visibility of its content, typically player controls.
 * It shows its content when tapped and automatically hides it after a delay if media is playing.
 * The content remains visible if the media is paused.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the container.
 * @param state [PlayerState] The [PlayerState] used to determine visibility logic (e.g., [PlayerState.isPlaying]).
 * @param isClickable [Boolean] A boolean to enable or disable the clickable behavior of the container. Defaults to `true`.
 * @param contentAlignment [Alignment] The alignment of the content within the container. Defaults to [Alignment.Center].
 * @param content [@Composable (areControlsVisible: Boolean) -> Unit][content] A composable lambda that receives a boolean `areControlsVisible` indicating whether the
 *   content should be visible. This allows for animation within the content composable itself.
 */
@Composable
private fun MiniPlayerControlsContainer(
        modifier: Modifier = Modifier,
        state: PlayerState,
        isClickable: Boolean = true,
        contentAlignment: Alignment = Alignment.Center,
        content: @Composable BoxScope.(areControlsVisible: Boolean) -> Unit
) {
    val delayAmount = 3.seconds
    var areControlsVisible by remember { mutableStateOf(true) }
    var clickTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayAmount)
        areControlsVisible = false
    }

    LaunchedEffect(state.isPlaying, clickTrigger) {
        when {
            state.isPlaying -> {
                areControlsVisible = true

                delay(delayAmount)

                areControlsVisible = false
            }

            else            -> areControlsVisible = true
        }
    }

    Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                        enabled = isClickable,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }) {
                    val targetVisibility = !areControlsVisible
                    areControlsVisible = targetVisibility
                    if (targetVisibility && state.isPlaying) clickTrigger = !clickTrigger
                },
            contentAlignment = contentAlignment
    ) {
        content(areControlsVisible)
    }
}

/**
 * A composable that displays player controls (`previous`, `play`/`pause`, `next`) as a floating overlay.
 *
 * This is typically used in layouts where there is not enough space for the standard, permanently
 * visible controls. The controls are housed within a [Card] to give them a distinct, elevated
 * appearance. Their visibility is controlled by the [areControlsVisible] parameter, allowing for
 * animations like sliding and fading in/out.
 *
 * @param state [PlayerState] The [PlayerState] containing the current playback information, used here to
 *   determine the play/pause icon.
 * @param areControlsVisible [Boolean] A boolean that determines whether the controls should be visible.
 *   This is used to trigger the enter/exit animations.
 * @param onSkipToNextSurah [() -> Unit][onSkipToNextSurah] A lambda to be invoked when the `skip to next` button is clicked.
 * @param onTogglePlayback [() -> Unit][onTogglePlayback] A lambda to be invoked when the `play`/`pause` button is clicked.
 * @param onSkipToPreviousSurah [() -> Unit][onSkipToPreviousSurah] A lambda to be invoked when the `skip to previous` button is clicked.
 */
@Composable
private fun BoxScope.OverlayPlayerControls(
        state: PlayerState,
        areControlsVisible: Boolean,
        onSkipToNextSurah: () -> Unit,
        onTogglePlayback: () -> Unit,
        onSkipToPreviousSurah: () -> Unit
) {
    val slideAnimationSpec = spring<IntOffset>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val fadeAnimationSpec = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

    AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight()
                .padding(vertical = 10.dp)
                .align(Alignment.BottomCenter),
            visible = areControlsVisible,
            enter = slideInVertically(animationSpec = slideAnimationSpec, initialOffsetY = { it }) + fadeIn(animationSpec = fadeAnimationSpec),
            exit = slideOutVertically(animationSpec = slideAnimationSpec, targetOffsetY = { it }) + fadeOut(animationSpec = fadeAnimationSpec)
    ) {
        Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(50.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                elevation = CardDefaults.cardElevation(20.dp)
        ) {
            PlayerControls(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp, vertical = 10.dp),
                    state = state,
                    isOverlay = true,
                    onSkipToNextSurah = onSkipToNextSurah,
                    onTogglePlayback = onTogglePlayback,
                    onSkipToPreviousSurah = onSkipToPreviousSurah
            )
        }
    }
}

/**
 * A composable that displays a blurred background image for the player.
 * The image is conditionally rendered based on the `isVisible` parameter. When visible, it fills the
 * entire available space, applies a blur effect, and has a slight transparency. This creates a
 * visually appealing and non-distracting backdrop for the player controls and information.
 *
 * @param isVisible [Boolean] If `true`, the background image is displayed. If `false`, nothing is rendered.
 * @param surahDrawableId [Int] The drawable resource ID for the image to be displayed as the background.
 */
@Composable
private fun PlayerBackground(
        isVisible: Boolean,
        @DrawableRes
        surahDrawableId: Int
) {
    if (!isVisible) return
    Image(
            modifier = Modifier
                .fillMaxSize()
                .blur(radiusX = 25.dp, radiusY = 25.dp)
                .alpha(0.5f),
            painter = painterResource(id = surahDrawableId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
    )
}

/**
 * A composable that displays the top bar for the full-screen player.
 * It contains an icon button to minimize or close the full-screen view.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the top bar. Defaults to [Modifier].
 * @param onMinimizeClicked [() -> Unit][onMinimizeClicked] A lambda function that is invoked when the minimize/close
 *   button is clicked.
 */
@Composable
private fun PlayerTopBar(
        modifier: Modifier = Modifier,
        onMinimizeClicked: () -> Unit = {}
) {
    Row(
            modifier = modifier,
    ) {
        IconButton(
                onClick = onMinimizeClicked,
                modifier = Modifier.size(64.dp)
        ) {
            Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.arrow_down_24px),
                    contentDescription = "Close Full Screen Player",
                    tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * A composable that displays the artwork for a surah. It adapts its size based on the device
 * configuration to ensure it fits well within different layouts (`portrait`, `landscape`, `compact`).
 *
 * The image is clipped to a rounded rectangle and has a drop shadow for a modern, elevated look.
 * The size of the image is calculated as a percentage of the available minimum dimension (`width` or `height`)
 * of its container, with a specific sizing factor applied for different device types.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the composable.
 * @param surahDrawableId [Int] The resource ID for the surah's artwork.
 * @param alignment [Alignment] The alignment of the image within its container. Defaults to [Alignment.BottomCenter].
 */
@Composable
private fun SurahImage(
        modifier: Modifier = Modifier,
        @DrawableRes
        surahDrawableId: Int,
        alignment: Alignment = Alignment.BottomCenter
) {
    BoxWithConstraints(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = alignment
    ) {
        val deviceConfiguration = currentWindowAdaptiveInfo().windowSizeClass.deviceConfiguration

        val sizingFactor = when (deviceConfiguration) {
            DeviceConfiguration.COMPACT,
            DeviceConfiguration.PHONE_PORTRAIT,
            DeviceConfiguration.TABLET_PORTRAIT,
            DeviceConfiguration.TABLET_LANDSCAPE -> 1f

            DeviceConfiguration.PHONE_LANDSCAPE  -> 0.9f
        }

        val surahImageSize = minOf(maxWidth, maxHeight) * sizingFactor

        Image(
                modifier = Modifier
                    .size(surahImageSize)
                    .aspectRatio(1f)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .dropShadow(
                            shape = RoundedCornerShape(25.dp),
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), radius = 25.dp)
                    ),
                painter = painterResource(id = surahDrawableId),
                contentDescription = "Surah Image",
                contentScale = ContentScale.FillBounds,
        )
    }
}

/**
 * A composable that displays information about the current recitation, including the surah name
 * and the reciter's name. It is designed to be responsive, adjusting the font sizes of the text
 * based on the device's screen size and orientation.
 *
 * For smaller screens or in compact layouts, the text size is reduced to fit the available space.
 * The surah and reciter names are also configured with a marquee effect, which automatically scrolls
 * the text if it is too long to fit in the allocated width.
 *
 * Font styles are chosen based on the current locale (e.g., specific Arabic fonts for RTL languages).
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the composable.
 * @param state [PlayerState] The [PlayerState] containing the `surah` and `reciter` information to be displayed.
 */
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun RecitationInfo(
        modifier: Modifier = Modifier,
        state: PlayerState
) {
    BoxWithConstraints(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
    ) {
        val deviceConfiguration = currentWindowAdaptiveInfo().windowSizeClass.deviceConfiguration

        val (surahSizingFactor, reciterSizingFactor) = when (deviceConfiguration) {
            DeviceConfiguration.COMPACT          -> 0f to 0f
            DeviceConfiguration.PHONE_PORTRAIT   -> 0.2f to 0.07f
            DeviceConfiguration.PHONE_LANDSCAPE  -> 0.3f to 0.1f
            DeviceConfiguration.TABLET_PORTRAIT  -> 0.12f to 0.08f
            DeviceConfiguration.TABLET_LANDSCAPE -> 0.3f to 0.1f
        }

        val surahFontSize = (maxHeight.value * surahSizingFactor).sp
        val reciterFontSize = (maxHeight.value * reciterSizingFactor).sp

        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            AutoSizeText(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
            ) {
                Text(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .basicMarquee(),
                        text = state.surah?.name ?: stringResource(R.string.loading),
                        fontSize = surahFontSize,
                        fontFamily = when {
                            QuranApplication.currentLocaleInfo.isRTL -> FontFamily(Font(Rs.font.decotype_thuluth_2))
                            else                                     -> FontFamily(Font(Rs.font.aref_ruqaa))
                        },
                        color = Color.White.copy(alpha = 0.8f),
                )
            }

            AutoSizeText(
                    modifier = Modifier
                        .wrapContentHeight()
            ) {
                Text(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .basicMarquee(),
                        text = state.reciter?.name ?: stringResource(R.string.loading),
                        fontSize = reciterFontSize,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = Color.White.copy(alpha = 0.8f),
                )
            }
        }
    }
}

/**
 * A wrapper composable that provides a consistent layout structure for text elements
 * within different device configurations.
 *
 * This function's primary role is to apply a `modifier` to its content `Box` only when the
 * device configuration is landscape (`PHONE_LANDSCAPE`, `TABLET_LANDSCAPE`). In `portrait` or
 * `compact` modes, it acts as a simple `Box` without any additional modifiers, allowing the
 * child content to define its own layout constraints.
 *
 * This is particularly useful in `RecitationInfo` where text elements need to be weighted
 * in landscape to correctly share space, but should wrap their content in portrait to avoid
 * taking up unnecessary vertical space.
 *
 * @param modifier [Modifier] The [Modifier] to be conditionally applied to the content `Box`.
 *   It is only used in landscape configurations.
 * @param content [@Composable BoxScope.() -> Unit][content] The composable content to be rendered inside the `Box`.
 */
@Composable
private fun AutoSizeText(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit
) {
    val deviceConfiguration = currentWindowAdaptiveInfo().windowSizeClass.deviceConfiguration

    when (deviceConfiguration) {
        DeviceConfiguration.COMPACT,
        DeviceConfiguration.PHONE_PORTRAIT,
        DeviceConfiguration.TABLET_PORTRAIT  -> Box { content() }

        DeviceConfiguration.PHONE_LANDSCAPE,
        DeviceConfiguration.TABLET_LANDSCAPE -> Box(modifier = modifier) { content() }
    }
}

/**
 * A composable that displays the media playback progress, including a progress slider and duration text.
 *
 * This component visualizes the current playback time, total duration, and buffered amount.
 * It includes a custom [Slider] that allows the user to seek to a different position in the media.
 * The layout and styling of the progress bar and text are determined by [playerProgressConfig], which adapts
 * to the device's configuration and screen size. While the user is scrubbing the slider, the current time
 * text updates to reflect the slider's position.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the composable.
 * @param state [PlayerState] The [PlayerState] containing current playback information like duration and buffering status.
 * @param bufferedProgress [Float] The progress of the buffered media, as a float between `0f` and `1f`.
 * @param sliderPosition [Float] The current position of the progress slider, controlled by user interaction.
 * @param isChangingPosition [Boolean] A boolean indicating if the user is currently scrubbing the progress slider.
 * @param onSeekStarted [() -> Unit][onSeekStarted] A lambda invoked when the user starts dragging the slider.
 * @param onSeekProgress [(progress: Float) -> Unit][onSeekProgress] A lambda that reports the new slider position as the user drags it.
 * @param onSeekFinished [() -> Unit][onSeekFinished] A lambda invoked when the user releases the slider, finalizing the seek action.
 */
@Composable
private fun PlayerProgress(
        modifier: Modifier = Modifier,
        state: PlayerState,
        bufferedProgress: Float,
        sliderPosition: Float,
        isChangingPosition: Boolean,
        onSeekStarted: () -> Unit = {},
        onSeekProgress: (progress: Float) -> Unit = {},
        onSeekFinished: () -> Unit = {}
) {
    BoxWithConstraints(
            modifier = modifier,
            contentAlignment = Alignment.TopCenter
    ) {
        val config = playerProgressConfig(maxHeight = maxHeight)

        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
        ) {
            Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
            ) {
                PlayerDuration(
                        state = state,
                        sliderPosition = sliderPosition,
                        isChangingPosition = isChangingPosition,
                        fontSize = config.fontSize,
                        fontFamily = config.fontFamily
                )
            }

            Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
            ) {
                val trackGap = 5.dp
                val trackHeight = config.sliderHeight
                val trackActiveColor = Color.White
                val trackInactiveColor = Color.White.copy(alpha = 0.5f)
                val trackShape = RoundedCornerShape(15.dp)

                val thumbWidth = 10.dp
                val thumbHeight = trackHeight + 10.dp

                LinearProgressIndicator(
                        state = state,
                        thumbWidth = thumbWidth,
                        thumbHeight = thumbHeight,
                        trackHeight = trackHeight,
                        trackGap = trackGap,
                        trackShape = trackShape,
                        trackActiveColor = trackActiveColor,
                        trackInactiveColor = trackInactiveColor,
                        sliderPosition = sliderPosition,
                        bufferedProgress = bufferedProgress,
                        onSeekStarted = onSeekStarted,
                        onSeekProgress = onSeekProgress,
                        onSeekFinished = onSeekFinished
                )
            }
        }
    }
}

/**
 * A composable that displays the current playback position and total duration of the media.
 *
 * This component shows the elapsed time and the total time of the recitation, separated by a slash.
 * For example: `01:23 / 05:45`.
 *
 * The displayed current position updates in real-time. It reflects the [sliderPosition] when the user
 * is actively scrubbing the progress bar ([isChangingPosition] is true), otherwise it shows the
 * [PlayerState.currentPositionMs] from the [state].
 *
 * The time format automatically includes hours if the total duration is one hour or longer.
 *
 * @param state [PlayerState] The [PlayerState] containing the media's total duration and current playback position.
 * @param sliderPosition [Float] The current position of the progress slider, used to display the time while the user is seeking.
 * @param isChangingPosition [Boolean] A boolean indicating if the user is currently scrubbing the progress slider.
 * @param fontSize [TextUnit] The font size to be applied to the duration text.
 * @param fontFamily [FontFamily] The font family to be applied to the duration text.
 */
@Composable
private fun PlayerDuration(
        state: PlayerState,
        sliderPosition: Float,
        isChangingPosition: Boolean,
        fontSize: TextUnit,
        fontFamily: FontFamily
) {
    val showHours = state.durationMs.milliseconds.hoursComponent > 0

    val totalDuration = state.durationMs.milliseconds.toLocalizedFormattedTime(showHours = showHours)

    val currentPosition = when {
        state.durationMs.milliseconds.isInfinite() -> state.durationMs  // the duration is infinite when the player is in the buffering state
        isChangingPosition                         -> sliderPosition.toLong()
        else                                       -> state.currentPositionMs
    }.milliseconds.toLocalizedFormattedTime(showHours = showHours)

    val textStyle = TextStyle(
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White.copy(alpha = 0.8f)
    )

    Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = currentPosition, style = textStyle)

        Spacer(Modifier.width(8.dp))

        Text(text = "/", style = textStyle)

        Spacer(Modifier.width(8.dp))

        Text(text = totalDuration, style = textStyle)
    }
}

/**
 * Calculates and provides a [PlayerProgressConfig] based on the current device configuration
 * and available height. This function is responsible for determining the appropriate font size,
 * font family, and slider height for the [PlayerProgress] composable to ensure a responsive and
 * visually consistent layout across different screen sizes and orientations.
 *
 * The sizing factors are adjusted based on whether the device is in `portrait`, `landscape`, or a
 * `compact` layout. It also selects an appropriate font family based on the current locale's
 * text direction (`RTL` or `LTR`).
 *
 * @param maxHeight [Dp] The maximum available height for the container, used as a basis for calculating
 *   the size of the UI elements.
 * @return [PlayerProgressConfig] A data class containing the calculated [PlayerProgressConfig.fontSize], [PlayerProgressConfig.fontFamily],
 *   and [PlayerProgressConfig.sliderHeight] to be used for styling the player progress components.
 */
@Composable
private fun playerProgressConfig(maxHeight: Dp): PlayerProgressConfig {
    val deviceConfiguration = currentWindowAdaptiveInfo().windowSizeClass.deviceConfiguration
    val isRTL = QuranApplication.currentLocaleInfo.isRTL

    val sizingFactor = when (deviceConfiguration) {
        DeviceConfiguration.COMPACT          -> 0f
        DeviceConfiguration.PHONE_PORTRAIT,
        DeviceConfiguration.TABLET_PORTRAIT  -> 0.12f

        DeviceConfiguration.PHONE_LANDSCAPE  -> 0.08f
        DeviceConfiguration.TABLET_LANDSCAPE -> 0.05f
    }

    val sliderHeightFactor = when (deviceConfiguration) {
        DeviceConfiguration.COMPACT          -> 0f
        DeviceConfiguration.PHONE_PORTRAIT,
        DeviceConfiguration.TABLET_PORTRAIT  -> 0.2f

        DeviceConfiguration.PHONE_LANDSCAPE,
        DeviceConfiguration.TABLET_LANDSCAPE -> 0.05f
    }

    val baseFontSize = (maxHeight.value * sizingFactor).sp
    val fontSize = if (isRTL) baseFontSize * 1.5f else baseFontSize

    val fontFamily = when {
        isRTL -> FontFamily(Font(Rs.font.decotype_thuluth_2))
        else  -> FontFamily(Font(Rs.font.aref_ruqaa))
    }

    val sliderHeight = (maxHeight.value * sliderHeightFactor).dp

    return PlayerProgressConfig(fontSize = fontSize, fontFamily = fontFamily, sliderHeight = sliderHeight)
}

/**
 * A custom linear progress indicator that visually represents the playback progress,
 * buffering state, and allows for user interaction to seek through the media.
 *
 * This composable acts as a container for several other components:
 * - A [Slider] for user-controlled seeking.
 * - A [BufferProgressIndicator] to show the amount of media that has been buffered.
 * - A [BufferingIndicator] which displays a pulsing animation when the media is in a buffering state.
 *
 * It intelligently switches between the [Slider] and the [BufferingIndicator] based on [PlayerState.isBuffering].
 * The underlying [Slider] is built using custom `thumb` and `track` composables ([SliderThumb] and [SliderTrack])
 * to achieve a specific visual style.
 *
 * @param state [PlayerState] The current state of the player, used to determine duration, buffering status, and current position.
 * @param thumbWidth [Dp] The width of the slider's thumb.
 * @param thumbHeight [Dp] The height of the slider's thumb.
 * @param trackHeight [Dp] The height of the slider's track.
 * @param trackGap [Dp] The gap between the active and inactive parts of the track, around the thumb.
 * @param trackShape [RoundedCornerShape] The shape applied to the slider track.
 * @param trackActiveColor [Color] The color of the active (played) portion of the track.
 * @param trackInactiveColor [Color] The color of the inactive (unplayed) portion of the track.
 * @param sliderPosition [Float] The current position of the slider, controlled by user interaction or playback progress.
 * @param bufferedProgress [Float] The amount of media that has been buffered.
 * @param onSeekStarted [() -> Unit][onSeekStarted] A callback function that is called when the user starts seeking.
 * @param onSeekProgress [(progress: Float) -> Unit][onSeekProgress] A callback function that is called when the user is seeking.
 * @param onSeekFinished [() -> Unit][onSeekFinished] A callback function that is called when the user finishes seeking.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BoxScope.LinearProgressIndicator(
        state: PlayerState,
        thumbWidth: Dp,
        thumbHeight: Dp,
        trackHeight: Dp,
        trackGap: Dp,
        trackShape: RoundedCornerShape,
        trackActiveColor: Color = Color.White,
        trackInactiveColor: Color = Color.White.copy(alpha = 0.5f),
        sliderPosition: Float,
        bufferedProgress: Float,
        onSeekStarted: () -> Unit = {},
        onSeekProgress: (progress: Float) -> Unit = {},
        onSeekFinished: () -> Unit = {}
) {
    // Safety Check: Ensure duration is at least 1f to prevent "0..0" range crash
    // When the media is first loaded, the duration is 0, so we set it to 1f to prevent division by zero
    val trackDuration = when {
        state.durationMs > 0 -> state.durationMs.toFloat()
        else                 -> 1f
    }

    // Calculate safe progress for the buffer (to avoid division by zero)
    val trackCurrentProgress = when {
        state.durationMs > 0 -> sliderPosition / state.durationMs.toFloat()
        else                 -> 0f
    }

    if (state.isBuffering) {
        BufferingIndicator(
                modifier = Modifier
                    .padding(horizontal = thumbWidth / 2)
                    .align(Alignment.Center),
                trackHeight = trackHeight,
                trackColor = trackActiveColor,
                trackShape = trackShape
        )
    } else {
        Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                value = sliderPosition.coerceIn(0f, trackDuration),
                onValueChange = {
                    onSeekStarted()
                    onSeekProgress(it)
                },
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..trackDuration,
                thumb = {
                    SliderThumb(
                            thumbWidth = thumbWidth,
                            thumbHeight = thumbHeight,
                            thumbColor = trackActiveColor,
                            thumbShape = trackShape
                    )
                },
                track = { sliderState ->
                    SliderTrack(
                            sliderState = sliderState,
                            thumbWidth = thumbWidth,
                            trackGap = trackGap,
                            trackHeight = trackHeight,
                            trackShape = trackShape,
                            trackActiveColor = trackActiveColor,
                            trackInactiveColor = trackInactiveColor
                    )
                }
        )

        BufferProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                currentProgress = trackCurrentProgress,
                bufferProgress = bufferedProgress,
                thumbWidth = thumbWidth,
                trackGap = trackGap,
                trackHeight = trackHeight,
                trackColor = Color.DarkGray.copy(alpha = 0.3f),
                trackShape = trackShape
        )
    }
}

/**
 * A composable that visualizes the buffered portion of the media on top of the main progress slider.
 * It is designed to be layered underneath the main interactive [Slider] but above the slider's track.
 *
 * This indicator shows a background segment that represents the amount of media that has been
 * downloaded (buffered) but not yet played. It works by taking the [currentProgress] and [bufferProgress]
 * and drawing a [Box] with a specified [trackColor] for the duration between these two points.
 *
 * It uses a [Row] with weighted [Box] components to represent three segments:
 * - The space already covered by the played progress (an empty [Box]).
 * - The [SliderThumb]'s position.
 * - The buffered segment (a [Box] with the [trackColor]).
 * - The unbuffered, inactive segment (an empty [Box]).
 *
 * This composable is intended for visual feedback only and does not handle user interaction.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the composable.
 * @param currentProgress [Float] The current playback progress, as a float between `0f` and `1f`.
 * @param bufferProgress [Float] The current buffer progress, as a float between `0f` and `1f`.
 * @param thumbWidth [Dp] The width of the slider's thumb. This is used to correctly offset the progress segments.
 * @param trackGap [Dp] The gap between the played portion and the buffered portion of the track.
 * @param trackHeight [Dp] The height of the buffer indicator track.
 * @param trackColor [Color] The color of the buffer indicator track.
 * @param trackShape [Shape] The shape of the buffer indicator track.
 */
@Composable
private fun BufferProgressIndicator(
        modifier: Modifier = Modifier,
        currentProgress: Float,
        bufferProgress: Float,
        thumbWidth: Dp = 10.dp,
        trackGap: Dp = 10.dp,
        trackHeight: Dp = 50.dp,
        trackColor: Color = Color.DarkGray.copy(alpha = 0.3f),
        trackShape: Shape = RoundedCornerShape(10.dp),
) {
    val progress = currentProgress.coerceIn(0f, 1f)
    val buffered = bufferProgress.coerceIn(0f, 1f)

    val playedProgress = progress.coerceAtMost(buffered)
    val bufferActiveProgress = (buffered - progress).coerceAtLeast(0f)
    val bufferInActiveProgress = (1f - buffered).coerceAtLeast(0f)

    Row(
            modifier = modifier
                .fillMaxWidth()
                .height(trackHeight)
    ) {
        if (playedProgress > 0f) Box(
                modifier = Modifier
                    .weight(playedProgress)
                    .fillMaxHeight()
                    .padding(end = trackGap, start = thumbWidth / 2)
                // .background(Color.Red.copy(alpha = 0.5f), trackShape) // debug color
        )

        // SliderThumb(thumbWidth = thumbWidth, thumbColor = Color.Yellow.copy(alpha = 0.5f)) // debug color
        SliderThumb(thumbWidth = thumbWidth)

        if (bufferActiveProgress > 0f) Box(
                modifier = Modifier
                    .weight(bufferActiveProgress)
                    .fillMaxHeight()
                    .padding(start = trackGap)
                    .background(trackColor, trackShape)
                // .background(Color.Green.copy(alpha = 0.5f), trackShape) // debug color
        )

        if (bufferInActiveProgress > 0f) Box(
                modifier = Modifier
                    .weight(bufferInActiveProgress)
                    .fillMaxHeight()
                    .padding(end = thumbWidth / 2)
                // .background(Color.Blue.copy(alpha = 0.5f), trackShape) // debug color
        )
    }
}


/**
 * A custom composable for the thumb of a [Slider]. It renders a simple [Box] with a
 * specified width, height, color, and shape.
 *
 * This composable is used within the `thumb` lambda of the [Slider] component to provide a
 * custom visual representation for the slider's handle, replacing the default circular thumb.
 *
 * @param thumbWidth [Dp] The width of the slider's `thumb`.
 * @param thumbHeight [Dp] The height of the slider's `thumb`.
 * @param thumbColor [Color] The color of the slider's `thumb`.
 * @param thumbShape [Shape] The shape of the slider's `thumb`.
 */
@Composable
private fun SliderThumb(
        thumbWidth: Dp = 10.dp,
        thumbHeight: Dp = 60.dp,
        thumbColor: Color = Color.White,
        thumbShape: Shape = RoundedCornerShape(5.dp)
) {
    Box(
            modifier = Modifier
                .width(thumbWidth)
                .height(thumbHeight)
                .background(thumbColor, thumbShape)
    )
}

/**
 * A custom track composable for a [Slider], responsible for drawing the active and inactive
 * segments of the progress bar.
 *
 * It uses the [sliderState] to calculate the current progress and divides the track into two
 * [Box] components: one for the `active` (`played`) portion and one for the `inactive` (`unplayed`)
 * portion. A gap is added between these segments to accommodate the slider's `thumb`. The padding
 * applied to the `active` and `inactive` segments ensures that they don't overlap with the `thumb`, creating
 * a visually clean separation.
 *
 * @param sliderState [SliderState] The state of the parent [Slider], which contains the current value and value range.
 * @param thumbWidth [Dp] The width of the slider's `thumb`. Used to calculate the padding around the track segments.
 * @param trackGap [Dp] The gap between the `active` and `inactive` track segments, centered around the `thumb`.
 * @param trackHeight [Dp] The height of the track.
 * @param trackShape [Shape] The shape of the track segments.
 * @param trackActiveColor [Color] The color of the `active` (`played`) part of the track.
 * @param trackInactiveColor [Color] The color of the `inactive` (`unplayed`) part of the track.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SliderTrack(
        sliderState: SliderState,
        thumbWidth: Dp = 10.dp,
        trackGap: Dp = 10.dp,
        trackHeight: Dp = 50.dp,
        trackShape: Shape = RoundedCornerShape(10.dp),
        trackActiveColor: Color = Color.White,
        trackInactiveColor: Color = Color.White.copy(alpha = 0.5f)
) {
    /**
     *  Calculates the progress of the slider's `value` relative to the `value range`
     *
     *  The progress is a value between `0` and `1`, where `0` represents the `start` of the value range and `1` represents the `end`
     *
     *  This progress is then used to calculate the `active` and `inactive` segments of the slider track
     *
     *  Safety Check inside Track: Prevent division by zero if range is somehow 0
     */
    val range = sliderState.valueRange.endInclusive - sliderState.valueRange.start
    val progress = when {
        range > 0 -> (sliderState.value - sliderState.valueRange.start) / range
        else      -> 0f
    }

    val activeProgress = progress.coerceIn(0f, 1f)
    val inactiveProgress = 1f - activeProgress

    Timber.debug("progress: $progress, activeProgress: $activeProgress, inactiveProgress: $inactiveProgress")

    Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight),
            horizontalArrangement = Arrangement.Center
    ) {
        // Active Segment
        if (activeProgress > 0f) Box(
                modifier = Modifier
                    .weight(activeProgress)
                    .fillMaxHeight()
                    .padding(end = trackGap + (thumbWidth / 2))
                    .background(trackActiveColor, trackShape)
        )

        // Inactive Segment
        if (inactiveProgress > 0f) Box(
                modifier = Modifier
                    .weight(inactiveProgress)
                    .fillMaxHeight()
                    .padding(start = trackGap + (thumbWidth / 2))
                    .background(trackInactiveColor, trackShape)
        )
    }
}

/**
 * A composable that displays an animated indicator when the media is buffering.
 *
 * This indicator is typically shown in place of the interactive progress slider when the player
 * is in a buffering state. It consists of a simple [Box] with a specified [trackHeight], [trackColor],
 * and [trackShape]. It uses an `infinite transition` to continuously animate the `alpha` (`opacity`)
 * of the box, creating a `pulsing` or `breathing` effect that visually signals to the user
 * that content is being loaded.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the composable.
 * @param trackHeight [Dp] The height of the indicator's track.
 * @param trackColor [Color] The color of the indicator.
 * @param trackShape [Shape] The shape of the indicator.
 */
@Composable
private fun BufferingIndicator(
        modifier: Modifier = Modifier,
        trackHeight: Dp = 50.dp,
        trackColor: Color = Color.White,
        trackShape: Shape = RoundedCornerShape(10.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buffering")
    val alpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
    )

    Box(
            modifier = modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(trackColor.copy(alpha = alpha), trackShape)
    )
}

/**
 * A composable that displays the primary playback controls: `skip to previous`, `play`/`pause`, and `skip to next`.
 *
 * This component arranges the control buttons in a [Row]. The central `play`/`pause` button is styled
 * as a prominent [Card] to draw attention, and its icon dynamically changes based on the [PlayerState.isPlaying]
 * status. The `skip to previous` and `skip to next` buttons are standard [IconButton]s.
 *
 * The layout can be adapted for different contexts using the [isOverlay] flag. When `true`, the buttons
 * are given equal weight, causing them to spread out evenly across the available width. This is suitable
 * for floating or overlay control panels. When `false`, the buttons take their default size.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the control row. Defaults to [Modifier].
 * @param state [PlayerState] The current player state, used to determine the icon for the `play`/`pause` button.
 * @param isOverlay [Boolean] If `true`, the control buttons will be weighted to fill the available width evenly.
 *   Defaults to `false`.
 * @param onSkipToPreviousSurah [() -> Unit][onSkipToPreviousSurah] A lambda to be invoked when the `skip to previous` button is clicked.
 * @param onTogglePlayback [() -> Unit][onTogglePlayback] A lambda to be invoked when the `play`/`pause` button is clicked.
 * @param onSkipToNextSurah [() -> Unit][onSkipToNextSurah] A lambda to be invoked when the `skip to next` button is clicked.
 */
@Composable
private fun PlayerControls(
        modifier: Modifier = Modifier,
        state: PlayerState,
        isOverlay: Boolean = false,
        onSkipToPreviousSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {}
) {
    Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
    ) {
        val buttonModifier = when {
            isOverlay -> Modifier.weight(1f)
            else      -> Modifier
        }
        IconButton(
                modifier = buttonModifier.size(64.dp),
                onClick = onSkipToPreviousSurah
        ) {
            Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = Rs.drawable.skip_previous_24px),
                    contentDescription = "Skip To Previous Surah",
                    tint = Color.White.copy(alpha = 0.8f)
            )
        }

        Card(
                modifier = buttonModifier.size(64.dp),
                onClick = onTogglePlayback,
                shape = RoundedCornerShape(25.dp),
                elevation = CardDefaults.cardElevation(20.dp),
                colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.8f),
                        contentColor = Color.Black.copy(alpha = 0.8f)
                )
        ) {
            val icon = if (state.isPlaying) Rs.drawable.pause_24px else Rs.drawable.play_arrow_24px
            Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = icon),
                    contentDescription = "Play/Pause"
            )
        }

        IconButton(
                modifier = buttonModifier.size(64.dp),
                onClick = onSkipToNextSurah
        ) {
            Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = Rs.drawable.skip_next_24px),
                    contentDescription = "Skip To Next Surah",
                    tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * A preview composable for the [FullScreenPlayer].
 *
 * This function sets up a [Scaffold] and displays the [FullScreenPlayer] with sample data
 * to demonstrate its appearance and layout in different configurations. It includes multiple
 * [@Preview][Preview] annotations to render the player in various device formats:
 * - A standard phone in portrait orientation.
 * - A tablet in landscape orientation.
 * - A compact square layout (400x400 dp).
 *
 * The previews use sample `reciter` and `surah` data, along with mock playback progress,
 * to provide a realistic representation of the player UI.
 */
@Composable
@Preview(name = "FullScreenPlayerPreview - Portrait", device = Devices.PHONE, showSystemUi = true, locale = "ar")
@Preview(name = "FullScreenPlayerPreview - Landscape", device = Devices.TABLET, showSystemUi = true, locale = "ar")
@Preview(name = "FullScreenPlayerPreview - Compact", widthDp = 400, heightDp = 400, locale = "ar")
private fun FullScreenPlayerPreview() {
    val duration = (2.5f * 60 * 60 * 1000).toLong()
    val currentPosition = (duration * 0.25f).toLong()
    val bufferedPosition = (duration * 0.55f).toLong()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(WindowInsets.systemBars)
        ) {
            FullScreenPlayer(
                    state = PlayerState(
                            reciter = sampleReciters.random(),
                            surah = sampleSurahs.random(),
                            durationMs = duration,
                            currentPositionMs = currentPosition,
                            bufferedPositionMs = bufferedPosition,
                            isPlaying = true,
                            isVisible = true,
                            isExpanded = true
                    ),
                    expandProgress = 1f,
            )
        }
    }
}

/**
 * Configuration data class for the visual properties of the player progress UI.
 *
 * This class holds layout and styling parameters for the player's progress bar and duration text,
 * which are calculated based on the current device configuration and screen size.
 *
 * @param fontSize [TextUnit] The calculated font size for the current and total duration text.
 * @param fontFamily [FontFamily] The font family to be used for the duration text.
 * @param sliderHeight [Dp] The calculated height for the progress slider track.
 */
private data class PlayerProgressConfig(val fontSize: TextUnit, val fontFamily: FontFamily, val sliderHeight: Dp)
