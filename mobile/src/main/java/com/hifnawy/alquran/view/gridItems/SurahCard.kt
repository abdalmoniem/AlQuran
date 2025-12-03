package com.hifnawy.alquran.view.gridItems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.utils.TextUtil.highlightMatchingText
import com.hifnawy.alquran.utils.sampleSurahs
import com.hifnawy.alquran.view.player.AnimatedAudioBars
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun SurahCard(
        modifier: Modifier = Modifier,
        surah: Surah?,
        isSkeleton: Boolean = false,
        isPlaying: Boolean = false,
        searchQuery: String = "",
        brush: Brush? = null,
        onClick: (surah: Surah) -> Unit
) {
    val animationDurationMillis = 500
    val floatAnimationSpec = tween<Float>(durationMillis = animationDurationMillis)
    val intSizeAnimationSpec = tween<IntSize>(durationMillis = animationDurationMillis)

    Card(
            modifier = modifier.aspectRatio(1f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(20.dp),
            onClick = { if (surah != null) onClick(surah) },
    ) {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SurahName(
                    isSkeleton = isSkeleton,
                    brush = brush,
                    surah = surah,
                    searchQuery = searchQuery
            )

            SurahType(
                    isSkeleton = isSkeleton,
                    brush = brush,
                    surah = surah
            )

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(
                    visible = !isSkeleton && isPlaying,
                    enter = scaleIn(
                            animationSpec = floatAnimationSpec,
                            transformOrigin = TransformOrigin(0f, 0f)
                    ) + fadeIn(animationSpec = floatAnimationSpec) + expandIn(
                            animationSpec = intSizeAnimationSpec,
                            expandFrom = Alignment.TopStart
                    ),
                    exit = scaleOut(
                            animationSpec = floatAnimationSpec,
                            transformOrigin = TransformOrigin(0f, 0f)
                    ) + fadeOut(animationSpec = floatAnimationSpec) + shrinkOut(
                            animationSpec = intSizeAnimationSpec,
                            shrinkTowards = Alignment.TopStart
                    )

            ) {
                AnimatedAudioBars()
            }
        }
    }
}

@Composable
private fun SurahName(
        isSkeleton: Boolean,
        brush: Brush?,
        surah: Surah?,
        searchQuery: String
) {
    when {
        isSkeleton -> {
            if (brush == null) return
            Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )
        }

        else       -> {
            if (surah == null) return
            Text(
                    modifier = Modifier.basicMarquee(),
                    text = highlightMatchingText(
                            fullText = surah.name,
                            query = searchQuery,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            defaultColor = MaterialTheme.colorScheme.onSurface
                    ),
                    fontSize = when {
                        QuranApplication.currentLocaleInfo.isRTL -> 45.sp
                        else                                     -> 30.sp
                    },
                    fontFamily = when {
                        QuranApplication.currentLocaleInfo.isRTL -> FontFamily(Font(Rs.font.decotype_thuluth_2))
                        else                                     -> FontFamily(Font(Rs.font.aref_ruqaa))
                    },
            )
        }
    }
}

@Composable
private fun SurahType(
        isSkeleton: Boolean,
        brush: Brush?,
        surah: Surah?
) {
    when {
        isSkeleton -> {
            if (brush == null) return
            Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(30.dp)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )
        }

        else       -> {
            if (surah == null) return
            Text(
                    text = when (surah.makkia) {
                        1 -> stringResource(R.string.surah_makkia)
                        else -> stringResource(R.string.surah_madaneyya)
                    },
                    fontSize = 25.sp,
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
            )
        }
    }
}

@Composable
@Preview(widthDp = 150, heightDp = 150, locale = "ar")
private fun SurahCardPreview() {
    val surah = sampleSurahs.random()
    SurahCard(
            surah = surah,
            isPlaying = true,
            onClick = {}
    )
}
