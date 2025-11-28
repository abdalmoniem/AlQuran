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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.utils.DrawableResUtil.defaultSurahDrawableId
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawableId
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.hoursLong
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

@Composable
fun FullScreenPlayer(
        state: PlayerState,
        expandProgress: Float,
        onMinimizeClicked: () -> Unit = {},
        onSeekProgress: (Long) -> Unit = {},
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

@Composable
private fun PlayerContentPortrait(
        modifier: Modifier = Modifier,
        surahDrawableId: Int,
        state: PlayerState,
        bufferedProgress: Float,
        sliderPosition: Float,
        isChangingPosition: Boolean,
        onSeekStarted: () -> Unit = {},
        onSeekProgress: (Float) -> Unit = {},
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

@Composable
private fun PlayerContentLandScape(
        modifier: Modifier = Modifier,
        surahDrawableId: Int,
        state: PlayerState,
        bufferedProgress: Float,
        sliderPosition: Float,
        isChangingPosition: Boolean,
        onSeekStarted: () -> Unit = {},
        onSeekProgress: (Float) -> Unit = {},
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

@Composable
private fun PlayerContentCompact(
        modifier: Modifier = Modifier,
        surahDrawableId: Int,
        state: PlayerState,
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

@Composable
private fun PlayerBackground(
        isVisible: Boolean,
        @DrawableRes
        surahDrawableId: Int
) {
    if (isVisible) {
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
}

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
                        fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerProgress(
        modifier: Modifier = Modifier,
        state: PlayerState,
        bufferedProgress: Float,
        sliderPosition: Float,
        isChangingPosition: Boolean,
        onSeekStarted: () -> Unit = {},
        onSeekProgress: (Float) -> Unit = {},
        onSeekFinished: () -> Unit = {}
) {
    BoxWithConstraints(
            modifier = modifier,
            contentAlignment = Alignment.TopCenter
    ) {
        val deviceConfiguration = currentWindowAdaptiveInfo().windowSizeClass.deviceConfiguration

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

        val fontSize = (maxHeight.value * sizingFactor).sp
        val sliderHeight = (maxHeight.value * sliderHeightFactor).dp

        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
        ) {
            Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentSliderPositionMs = if (isChangingPosition) sliderPosition.toLong() else state.currentPositionMs
                    val showHours = state.durationMs.milliseconds.hoursLong > 0

                    Text(
                            text = currentSliderPositionMs.milliseconds.toLocalizedFormattedTime(showHours = showHours),
                            fontSize = fontSize,
                            fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                            text = "\\",
                            fontSize = fontSize,
                            fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                            text = state.durationMs.milliseconds.toLocalizedFormattedTime(showHours = showHours),
                            fontSize = fontSize,
                            fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
            ) {
                val trackGap = 5.dp
                val trackHeight = sliderHeight
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
        onSeekProgress: (Float) -> Unit = {},
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

@Composable
private fun SliderThumb(
        thumbHeight: Dp = 60.dp,
        thumbWidth: Dp = 10.dp,
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
