package com.hifnawy.alquran.view.player

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.utils.DrawableResUtil.defaultSurahDrawableId
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawableId
import com.hifnawy.alquran.viewModel.MediaViewModel
import com.hifnawy.alquran.viewModel.PlayerState
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun MiniPlayer(
        mediaViewModel: MediaViewModel,
        animatedHeight: Float,
        expandProgress: Float
) {
    val state = mediaViewModel.playerState

    val surahDrawableId = remember(state.surah?.id) { state.surah?.surahDrawableId ?: defaultSurahDrawableId }

    Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
    ) {
        MiniPlayerBackground(
                isVisible = state.isVisible,
                surahDrawableId = surahDrawableId,
                animatedHeight = animatedHeight
        )

        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(expandProgress)
        ) {
            MiniPlayerProgress(state)

            MiniPlayerContent(state, surahDrawableId, mediaViewModel)
        }
    }
}

@Composable
private fun MiniPlayerBackground(
        isVisible: Boolean,
        surahDrawableId: Int,
        animatedHeight: Float
) {
    if (isVisible) {
        Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { animatedHeight.toDp() })
                    .blur(radiusX = 15.dp, radiusY = 15.dp)
                    .alpha(0.5f),
                painter = painterResource(id = surahDrawableId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MiniPlayerProgress(state: PlayerState) {
    LinearProgressIndicator(
            progress = { state.currentPositionMs.toFloat() / state.durationMs.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun MiniPlayerContent(state: PlayerState, surahDrawableId: Int, mediaViewModel: MediaViewModel) {
    Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {

        MiniPlayerSurahInfo(
                modifier = Modifier.weight(1f),
                state = state,
                surahDrawableId = surahDrawableId
        )

        MiniPlayerControls(mediaViewModel = mediaViewModel)
    }
}

@Composable
private fun MiniPlayerSurahInfo(
        modifier: Modifier = Modifier,
        state: PlayerState,
        @DrawableRes
        surahDrawableId: Int
) {
    if (state.isVisible) {
        Image(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(15.dp)),
                painter = painterResource(id = surahDrawableId),
                contentDescription = "Surah Image",
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = modifier) {
        Text(
                text = state.surah?.name ?: stringResource(R.string.loading),
                fontSize = 25.sp,
                fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
                color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.size(2.dp))

        Text(
                text = state.reciter?.name ?: stringResource(R.string.loading),
                fontSize = 15.sp,
                fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
                color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun MiniPlayerControls(mediaViewModel: MediaViewModel) {
    IconButton(
            modifier = Modifier.size(32.dp),
            onClick = mediaViewModel::togglePlayback
    ) {
        val icon = when {
            mediaViewModel.playerState.isPlaying -> Rs.drawable.pause_24px
            else                                 -> Rs.drawable.play_arrow_24px
        }
        Icon(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = icon),
                contentDescription = "Play/Pause",
                tint = Color.White.copy(alpha = 0.8f)
        )
    }

    Spacer(modifier = Modifier.width(16.dp))

    IconButton(
            modifier = Modifier.size(32.dp),
            onClick = mediaViewModel::closePlayer
    ) {
        Icon(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.close_24px),
                contentDescription = "Close Player",
                tint = Color.White.copy(alpha = 0.8f)
        )
    }
}
