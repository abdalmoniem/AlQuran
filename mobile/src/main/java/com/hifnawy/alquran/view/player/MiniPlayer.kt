package com.hifnawy.alquran.view.player

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.utils.DrawableResUtil.defaultSurahDrawableId
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawableId
import com.hifnawy.alquran.view.theme.AppTheme
import com.hifnawy.alquran.viewModel.PlayerState
import com.hifnawy.alquran.shared.R as Rs

private val slideAnimationSpec = tween<IntOffset>(durationMillis = 150, easing = FastOutLinearInEasing)
private val fadeAnimationSpec = tween<Float>(durationMillis = 150, easing = FastOutLinearInEasing)

@Composable
fun MiniPlayer(
        state: PlayerState,
        minimizeProgress: Float,
        onTogglePlayback: () -> Unit,
        onCloseClicked: () -> Unit
) {
    val surahDrawableId = remember(state.surah?.id) { state.surah?.surahDrawableId ?: defaultSurahDrawableId }

    Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
    ) {
        MiniPlayerBackground(surahDrawableId = surahDrawableId)

        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(minimizeProgress),
                verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                    visible = minimizeProgress >= 1f,
                    enter = slideInVertically(animationSpec = slideAnimationSpec, initialOffsetY = { it }) + fadeIn(animationSpec = fadeAnimationSpec),
                    exit = slideOutVertically(animationSpec = slideAnimationSpec, targetOffsetY = { it }) + fadeOut(animationSpec = fadeAnimationSpec),
            ) {
                MiniPlayerProgress(state = state)
            }

            MiniPlayerContent(
                    modifier = Modifier.weight(1f),
                    state = state,
                    surahDrawableId = surahDrawableId,
                    expandProgress = minimizeProgress,
                    onTogglePlayback = onTogglePlayback,
                    onCloseMiniPlayer = onCloseClicked
            )
        }
    }
}

@Composable
private fun MiniPlayerBackground(surahDrawableId: Int) = Image(
        modifier = Modifier
            .fillMaxSize()
            .blur(radiusX = 15.dp, radiusY = 15.dp)
            .alpha(0.5f),
        painter = painterResource(id = surahDrawableId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
)

@Composable
private fun MiniPlayerProgress(state: PlayerState) {
    LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            progress = { state.currentPositionMs.toFloat() / state.durationMs.toFloat() },
            color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun MiniPlayerContent(
        modifier: Modifier = Modifier,
        state: PlayerState,
        surahDrawableId: Int,
        expandProgress: Float,
        onTogglePlayback: () -> Unit,
        onCloseMiniPlayer: () -> Unit
) {
    Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {

        MiniPlayerSurahInfo(modifier = Modifier.weight(1f), state = state, surahDrawableId = surahDrawableId)

        AnimatedVisibility(
                visible = expandProgress >= 1f,
                enter = slideInVertically(animationSpec = slideAnimationSpec, initialOffsetY = { it }) + fadeIn(animationSpec = fadeAnimationSpec),
                exit = slideOutVertically(animationSpec = slideAnimationSpec, targetOffsetY = { it }) + fadeOut(animationSpec = fadeAnimationSpec),
        ) {
            OverlayPlayerControls(state = state, onTogglePlayback = onTogglePlayback, onCloseMiniPlayer = onCloseMiniPlayer)
        }
    }
}

@Composable
private fun MiniPlayerSurahInfo(
        modifier: Modifier = Modifier,
        state: PlayerState,
        @DrawableRes
        surahDrawableId: Int
) {
    BoxWithConstraints(
            modifier = modifier,
            contentAlignment = Alignment.CenterStart
    ) {
        val textSize = (maxHeight.value * 0.35f).sp
        val surahImageSize = (maxHeight.value * 0.8f).dp

        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                    modifier = Modifier
                        .size(surahImageSize)
                        .clip(RoundedCornerShape(15.dp)),
                    painter = painterResource(id = surahDrawableId),
                    contentDescription = "Surah Image",
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(),
                        text = state.surah?.name ?: stringResource(R.string.loading),
                        fontSize = textSize,
                        fontFamily = when {
                            QuranApplication.currentLocaleInfo.isRTL -> FontFamily(Font(Rs.font.decotype_thuluth_2))
                            else                                     -> FontFamily(Font(Rs.font.aref_ruqaa))
                        },
                        color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.size(2.dp))

                Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(),
                        text = state.reciter?.name ?: stringResource(R.string.loading),
                        fontSize = textSize * 0.5f,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun OverlayPlayerControls(
        state: PlayerState,
        onTogglePlayback: () -> Unit,
        onCloseMiniPlayer: () -> Unit
) {
    Row {
        IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onTogglePlayback
        ) {
            val icon = when {
                state.isPlaying -> Rs.drawable.pause_24px
                else            -> Rs.drawable.play_arrow_24px
            }

            Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = icon),
                    contentDescription = "Play/Pause",
                    tint = Color.White.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        IconButton(
                modifier = Modifier.size(32.dp),
                onClick = onCloseMiniPlayer
        ) {
            Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.close_24px),
                    contentDescription = "Close Player",
                    tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
@Preview(widthDp = 500, heightDp = 100, locale = "ar")
@SuppressLint("ViewModelConstructorInComposable")
private fun MiniPlayerPreview() {
    AppTheme {
        ElevatedCard(
                shape = RoundedCornerShape(25.dp),
                elevation = CardDefaults.elevatedCardElevation(25.dp),
                colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary
                )
        ) {
            // val quranApplication = QuranApplication()
            // val mediaViewModel = MediaViewModel(quranApplication)
            // mediaViewModel.playerState = PlayerState(
            //         reciter = sampleReciters.random(),
            //         surah = sampleSurahs.random(),
            //         currentPositionMs = 4_000,
            //         durationMs = 10_000,
            //         isVisible = true,
            //         isPlaying = true
            // )
            // MiniPlayer(
            //         mediaViewModel = mediaViewModel,
            //         expandProgress = 1f
            // )
        }
    }
}
