package com.hifnawy.alquran.view.gridItems

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.utils.ArabicPluralStringResource.arabicPluralStringResource
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.view.player.AnimatedAudioBars
import com.hifnawy.alquran.shared.R as Rs

/**
 * A Composable function that displays an elevated card for a specific Moshaf (a version of the Quran recitation).
 * The card shows the Moshaf's name, the count of its Surahs, and an icon.
 * It also includes an animated audio bar that is visible when the `isPlaying` parameter is true.
 * The card is clickable, triggering the `onMoshafClick` lambda.
 *
 * @param modifier [Modifier] The modifier to be applied to the card. Defaults to [Modifier].
 * @param reciter [Reciter] The [Reciter] associated with this Moshaf.
 * @param moshaf [Moshaf] The [Moshaf] data to display on the card.
 * @param isPlaying [Boolean] A boolean indicating whether this Moshaf is currently being played.
 *                  Controls the visibility of the animated audio bars. Defaults to `false`.
 * @param onMoshafClick [(reciter: Reciter, moshaf: Moshaf) -> Unit][onMoshafClick] A lambda function that is invoked when the card is clicked.
 *                      It provides the clicked [Reciter] and [Moshaf]. Defaults to an empty lambda.
 */
@Composable
fun MoshafCard(
        modifier: Modifier = Modifier,
        reciter: Reciter,
        moshaf: Moshaf,
        isPlaying: Boolean = false,
        onMoshafClick: (reciter: Reciter, moshaf: Moshaf) -> Unit = { _, _ -> }
) {
    val animationDurationMillis = 500
    val floatAnimationSpec = tween<Float>(durationMillis = animationDurationMillis)
    val intSizeAnimationSpec = tween<IntSize>(durationMillis = animationDurationMillis)

    ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.elevatedCardElevation(20.dp),
            onClick = { onMoshafClick(reciter, moshaf) },
    ) {
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                    painter = painterResource(id = R.drawable.book_24px),
                    contentDescription = "Moshaf Icon"
            )

            Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .basicMarquee(),
                    text = "${moshaf.name} - ${arabicPluralStringResource(R.plurals.surah_count, moshaf.surahsCount)}",
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa))
            )

            AnimatedVisibility(
                    visible = isPlaying,
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

/**
 * A Composable function that provides a preview of the [MoshafCard] in Android Studio's design view.
 * It uses [sampleReciters] to display the card, selecting a random reciter and one of their moshafs.
 * The preview is configured to display in the Arabic locale to ensure correct right-to-left
 * layout and plural string resources are rendered as expected for the target audience.
 * This preview helps in visualizing the static appearance of the card, without any
 * interactions or dynamic state changes like the audio animation. It showcases the default
 * state of the component with representative data, facilitating rapid UI development and review.
 * By randomly selecting a moshaf, it also helps in testing how the card handles different
 * text lengths for the moshaf name and surah count.
 */
@Composable
@Preview(locale = "ar")
fun MoshafCardPreview() {
    val reciter = sampleReciters.random()
    val moshaf = reciter.moshafList.random()
    MoshafCard(
            reciter = reciter,
            moshaf = moshaf
    )
}
