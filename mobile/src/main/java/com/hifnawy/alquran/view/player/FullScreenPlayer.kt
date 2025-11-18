package com.hifnawy.alquran.view.player

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.hoursLong
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.toLocalizedFormattedTime
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.utils.DrawableResUtil.getSurahDrawableId
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.utils.sampleSurahs
import com.hifnawy.alquran.viewModel.PlayerState
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun FullScreenPlayer(
        state: PlayerState,
        minimizeProgress: Float,
        onMinimizeClicked: () -> Unit = {},
        onSeekProgress: (Long) -> Unit = {},
        onSkipToPreviousSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {}
) {
    val context = LocalContext.current
    val surahDrawableId = remember(state.surah?.id) { getSurahDrawableId(context, state.surah?.id) }

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

    val windowInfo = LocalWindowInfo.current
    val containerSize = windowInfo.containerSize
    val screenWidth = containerSize.width
    val screenHeight = containerSize.height
    val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
    val isWide = aspectRatio > 0.7f

    Timber.debug("screenWidth: $screenWidth, screenHeight: $screenHeight, aspectRatio: $aspectRatio, isWide: $isWide")

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

        PlayerTopBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(minimizeProgress),
                onMinimizeClicked = onMinimizeClicked
        )

        if (isWide) {
            Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .padding(horizontal = 10.dp)
                        .alpha(minimizeProgress),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerInfo(
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth(),
                        isWide = isWide,
                        surahDrawableId = surahDrawableId
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .wrapContentWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    RecitationInfo(state = state, isWide = isWide)

                    PlayerProgress(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 10.dp),
                            state = state,
                            bufferedProgress = bufferedProgress,
                            sliderPosition = sliderPosition,
                            isChangingPosition = isChangingPosition,
                            onSeekStarted = { isChangingPosition = true },
                            onSeekProgress = { sliderPosition = it },
                            onSeekFinished = {
                                isChangingPosition = false
                                onSeekProgress(sliderPosition.toLong())
                            }
                    )

                    PlayerControls(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 10.dp),
                            state = state,
                            onSkipToNextSurah = onSkipToNextSurah,
                            onTogglePlayback = onTogglePlayback,
                            onSkipToPreviousSurah = onSkipToPreviousSurah
                    )
                }
            }
        } else {
            Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .padding(horizontal = 10.dp)
                        .alpha(minimizeProgress),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
            ) {
                PlayerInfo(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        isWide = isWide,
                        surahDrawableId = surahDrawableId
                )

                RecitationInfo(state = state, isWide = isWide)

                PlayerProgress(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(vertical = 10.dp),
                        state = state,
                        bufferedProgress = bufferedProgress,
                        sliderPosition = sliderPosition,
                        isChangingPosition = isChangingPosition,
                        onSeekStarted = { isChangingPosition = true },
                        onSeekProgress = { sliderPosition = it },
                        onSeekFinished = {
                            isChangingPosition = false
                            onSeekProgress(sliderPosition.toLong())
                        }
                )

                PlayerControls(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
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
private fun PlayerInfo(
        modifier: Modifier = Modifier,
        isWide: Boolean,
        @DrawableRes
        surahDrawableId: Int
) {
    Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = when {
                isWide -> Arrangement.Center
                else   -> Arrangement.Bottom
            }
    ) {
        Image(
                modifier = when {
                    isWide -> Modifier.fillMaxHeight(0.7f)
                    else   -> Modifier.fillMaxWidth()
                }
                    .aspectRatio(1f)
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
private fun RecitationInfo(state: PlayerState, isWide: Boolean) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Text(
                text = state.surah?.name ?: stringResource(R.string.loading),
                fontSize = when {
                    isWide -> 80.sp
                    else   -> 40.sp
                },
                fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
                color = Color.White.copy(alpha = 0.8f),
        )

        Text(
                text = state.reciter?.name ?: stringResource(R.string.loading),
                fontSize = when {
                    isWide -> 40.sp
                    else   -> 20.sp
                },
                fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
                color = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
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
    // Duration/Current Position Row
    Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            val currentSliderPositionMs = if (isChangingPosition) sliderPosition.toLong() else state.currentPositionMs
            val showHours = state.durationMs.milliseconds.hoursLong > 0

            Text(
                    text = currentSliderPositionMs.milliseconds.toLocalizedFormattedTime(showHours = showHours),
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(Modifier.width(8.dp))

            Text(text = "\\", fontSize = 15.sp, color = Color.White.copy(alpha = 0.8f))

            Spacer(Modifier.width(8.dp))

            Text(
                    text = state.durationMs.milliseconds.toLocalizedFormattedTime(showHours = showHours),
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.8f)
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                    progress = { bufferedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .align(Alignment.CenterStart),
                    color = Color.White.copy(alpha = 0.3f),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    gapSize = 0.dp,
                    strokeCap = StrokeCap.Round
            )

            if (state.durationMs > 0) {
                Slider(
                        value = sliderPosition,
                        onValueChange = {
                            onSeekStarted()
                            onSeekProgress(it)
                        },
                        onValueChangeFinished = onSeekFinished,
                        valueRange = 0f..state.durationMs.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .align(Alignment.CenterStart),
                        colors = SliderDefaults.colors(
                                thumbColor = Color.White.copy(alpha = 0.8f),
                                activeTrackColor = Color.White.copy(alpha = 0.8f),
                                inactiveTrackColor = Color.Transparent,
                        )
                )
            } else {
                LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .align(Alignment.CenterStart),
                        color = Color.White.copy(alpha = 0.3f),
                        trackColor = Color.White.copy(alpha = 0.1f),
                        gapSize = 0.dp,
                        strokeCap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun PlayerControls(
        modifier: Modifier = Modifier,
        state: PlayerState,
        onSkipToPreviousSurah: () -> Unit = {},
        onTogglePlayback: () -> Unit = {},
        onSkipToNextSurah: () -> Unit = {}
) {
    Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
                modifier = Modifier.size(64.dp),
                onClick = onSkipToPreviousSurah
        ) {
            Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = Rs.drawable.skip_previous_24px),
                    contentDescription = "Skip To Previous Surah",
                    tint = Color.White.copy(alpha = 0.8f)
            )
        }

        ElevatedCard(
                onClick = onTogglePlayback,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(25.dp),
                elevation = CardDefaults.elevatedCardElevation(20.dp),
                colors = CardDefaults.elevatedCardColors(
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
                modifier = Modifier.size(64.dp),
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
@Preview(widthDp = 1080, heightDp = 1920, name = "FullScreenPlayerPreview - Portrait")
@Preview(widthDp = 1920, heightDp = 1080, name = "FullScreenPlayerPreview - Landscape")
private fun FullScreenPlayerPreview() {
    val duration = (2.5f * 60 * 60 * 1000).toLong()
    val currentPosition = (duration * 0.5).toLong()
    val bufferedPosition = (duration * 0.75).toLong()

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
            minimizeProgress = 1f,
    )
}
