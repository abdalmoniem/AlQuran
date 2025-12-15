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
import androidx.compose.ui.text.style.TextAlign
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
import com.hifnawy.alquran.viewModel.PlayerState
import com.hifnawy.alquran.shared.R as Rs

/**
 * A composable that displays a card for a single Surah (chapter of the Quran).
 *
 * This card shows the Surah's name, its type (Meccan or Medinan), and can display
 * a loading skeleton state. It also features an animation to indicate when the
 * Surah is currently being played by the audio player. The Surah name can be
 * highlighted based on a search query.
 *
 * @param modifier [Modifier] The modifier to be applied to the card.
 * @param surah [Surah?][Surah] The [Surah] object to display. If null, the card might show a skeleton or nothing, depending on [isSkeleton].
 * @param isSkeleton [Boolean] A [Boolean] flag to indicate if the card should display a skeleton loading UI.
 * @param isPlaying [Boolean] A [Boolean] flag to indicate if this Surah is currently being played, which triggers the [AnimatedAudioBars].
 * @param searchQuery [String] A [String] used to highlight matching parts of the Surah's name.
 * @param brush [Brush?][Brush] A [Brush] used for the skeleton loading animation. Required if [isSkeleton] is true.
 * @param onClick [onClick: (surah: Surah) -> Unit][onClick] A lambda function that will be invoked when the card is clicked.
 */
@Composable
fun SurahCard(
        modifier: Modifier = Modifier,
        surah: Surah?,
        isSkeleton: Boolean = false,
        isPlaying: Boolean = false,
        searchQuery: String = "",
        brush: Brush? = null,
        onClick: (surah: Surah) -> Unit = {}
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                    modifier = Modifier.fillMaxWidth(),
                    visible = !isSkeleton && isPlaying,
                    enter = scaleIn(
                            animationSpec = floatAnimationSpec,
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                    ) + fadeIn(animationSpec = floatAnimationSpec) + expandIn(
                            animationSpec = intSizeAnimationSpec,
                            expandFrom = Alignment.Center
                    ),
                    exit = scaleOut(
                            animationSpec = floatAnimationSpec,
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                    ) + fadeOut(animationSpec = floatAnimationSpec) + shrinkOut(
                            animationSpec = intSizeAnimationSpec,
                            shrinkTowards = Alignment.Center
                    )

            ) {
                AnimatedAudioBars()
            }
        }
    }
}

/**
 * A composable function that displays the name of a Surah.
 *
 * This function has two states:
 * - **Skeleton state** ([isSkeleton] is `true`): It displays a placeholder a [Spacer] with a shimmering background [Brush]
 *   to indicate that content is loading. The [Brush] is required for this state.
 * - **Content state** ([isSkeleton] is `false`): It displays the actual Surah name using a [Text] composable.
 *   The surah object is required for this state. It highlights parts of the name that match the [searchQuery].
 *   The font size and family are adjusted based on the current locale (RTL or LTR).
 *
 * @param isSkeleton [Boolean] A [Boolean] flag to determine whether to show the skeleton loader or the actual content.
 * @param brush [Brush?][Brush] The [Brush] to be used for the background of the skeleton loader. Null if not in skeleton state.
 * @param surah [Surah?][Surah] The [Surah] object containing the name to be displayed. Null if in skeleton state.
 * @param searchQuery [String] The [String] text query to highlight within the Surah's name.
 */
@Composable
private fun SurahName(
        isSkeleton: Boolean,
        brush: Brush?,
        surah: Surah?,
        searchQuery: String
) = when {
    isSkeleton -> brush?.let {
        Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(it)
        )
    }

    else       -> surah?.let {
        Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
                text = highlightMatchingText(
                        fullText = it.name,
                        query = searchQuery,
                        highlightColor = MaterialTheme.colorScheme.primary,
                        defaultColor = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
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

/**
 * A private composable function that displays the revelation type of a Surah (Meccan or Medinan).
 * It can also display a `skeleton` loading state.
 *
 * @param isSkeleton [Boolean] A [Boolean] flag to indicate if the composable should render in a loading ([isSkeleton]) state.
 * @param brush [Brush?][Brush] The [Brush] to be used for the background of the `skeleton` loader. It's ignored if [isSkeleton] is `false`.
 * @param surah [Surah?][Surah] The [Surah] object containing the data to be displayed. If `null` and not in a skeleton state, nothing is rendered.
 */
@Composable
private fun SurahType(
        isSkeleton: Boolean,
        brush: Brush?,
        surah: Surah?
) = when {
    isSkeleton -> brush?.let {
        Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(50.dp)
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(it)
        )
    }

    else       -> surah?.let {
        Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
                text = when (it.makkia) {
                    1 -> stringResource(R.string.surah_meccan)
                    else -> stringResource(R.string.surah_medinan)
                },
                textAlign = TextAlign.Center,
                fontSize = 25.sp,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
        )
    }
}

/**
 * A [Preview] composable for the [SurahCard].
 *
 * This preview demonstrates the [SurahCard] in a specific state,
 * displaying a random sample Surah, with the [PlayerState.isPlaying] state set to `true`.
 * It's configured for a `150x150` dp size and uses the Arabic (`ar`) locale.
 */
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
