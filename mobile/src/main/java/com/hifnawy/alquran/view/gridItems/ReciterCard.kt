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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.asReciterId
import com.hifnawy.alquran.utils.TextUtil.highlightMatchingText
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.view.ShimmerAnimation
import com.hifnawy.alquran.view.player.AnimatedAudioBars
import com.hifnawy.alquran.shared.R as Rs

/**
 * A card Composable that displays information about a Quran reciter.
 *
 * This card shows the reciter's name and the number of available Moshafs (recitation styles).
 * It can be expanded to reveal a list of [MoshafCard]s for each of the reciter's Moshafs.
 * The card also supports a skeleton loading state, highlights search queries in the reciter's name,
 * and shows an animation when a recitation by this reciter is playing.
 *
 * @param modifier [Modifier] The [Modifier] to be applied to the card.
 * @param reciter [Reciter?][Reciter] The [Reciter] data to display. If null, the card might show a skeleton.
 * @param isExpanded [Boolean] Whether the card is expanded to show the list of Moshafs.
 * @param searchQuery [String] A string to highlight within the reciter's name.
 * @param isSkeleton [Boolean] If true, the card displays a shimmer-animated skeleton placeholder.
 * @param isPlaying [Boolean] If true, indicates that audio from this reciter is currently playing and shows an animation.
 * @param playingMoshafId [Int?][Int] The ID of the [Moshaf] that is currently playing. Used to highlight the specific active Moshaf.
 * @param brush [Brush?][Brush] The [Brush] to be used for the skeleton's shimmer animation. Required when [isSkeleton] is `true`.
 * @param onToggleExpand [() -> Unit][onToggleExpand] A lambda function to be invoked when the card is clicked, typically to toggle the [isExpanded] state.
 * @param onMoshafClick [(reciter: Reciter, moshaf: Moshaf) -> Unit][onMoshafClick] A lambda function to be invoked when a specific [Moshaf] within the expanded list is clicked.
 */
@Composable
fun ReciterCard(
        modifier: Modifier = Modifier,
        reciter: Reciter? = null,
        isExpanded: Boolean,
        searchQuery: String = "",
        isSkeleton: Boolean = false,
        isPlaying: Boolean = false,
        playingMoshafId: Int? = null,
        brush: Brush? = null,
        onToggleExpand: () -> Unit = {},
        onMoshafClick: (reciter: Reciter, moshaf: Moshaf) -> Unit = { _, _ -> }
) {
    val animationDurationMillis = 500
    val floatAnimationSpec = tween<Float>(durationMillis = animationDurationMillis)
    val intSizeAnimationSpec = tween<IntSize>(durationMillis = animationDurationMillis)

    Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp),
            onClick = onToggleExpand,
    ) {
        Column(
                modifier = Modifier
                    .fillMaxWidth()
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Chevron(isSkeleton, brush, reciter, isExpanded)


                Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            ReciterName(
                                    isSkeleton = isSkeleton,
                                    brush = brush,
                                    reciter = reciter,
                                    searchQuery = searchQuery
                            )

                            MoshafCount(
                                    isSkeleton = isSkeleton,
                                    brush = brush,
                                    moshafCount = reciter?.moshafList?.size
                            )
                        }

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
                            AnimatedAudioBars(modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }

            if (reciter == null) return@Card
            AnimatedVisibility(visible = isExpanded) {
                Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, bottom = 10.dp, end = 10.dp)
                ) {
                    reciter.moshafList.forEach { moshaf ->
                        MoshafCard(
                                reciter = reciter,
                                moshaf = moshaf,
                                isPlaying = isPlaying && playingMoshafId == moshaf.id,
                                onMoshafClick = onMoshafClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * A composable that displays a chevron icon (up or down arrow) indicating the expanded/collapsed state of the [ReciterCard].
 * It also handles the display of a skeleton placeholder when [isSkeleton] is `true`. If the reciter has no Moshafs,
 * the chevron is not displayed.
 *
 * @param isSkeleton [Boolean] A boolean flag indicating whether to show the skeleton loader.
 * @param brush [Brush?][Brush] The [Brush] to use for the skeleton's background. Only used when [isSkeleton] is `true`.
 * @param reciter [Reciter?][Reciter] The [Reciter] data. Used to check if the reciter has any Moshafs.
 * @param isExpanded [Boolean] A boolean flag indicating whether the parent card is expanded, which determines the chevron's direction.
 */
@Composable
private fun Chevron(
        isSkeleton: Boolean,
        brush: Brush?,
        reciter: Reciter?,
        isExpanded: Boolean
) {
    when {
        isSkeleton -> {
            if (brush == null) return
            Box(
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(brush)
            )
        }

        else       -> {
            if (reciter == null) return
            if (reciter.moshafList.isEmpty()) return

            val iconDrawableId = when {
                isExpanded -> R.drawable.arrow_up_24px
                else       -> R.drawable.arrow_down_24px
            }

            Icon(
                    modifier = Modifier
                        .size(50.dp)
                        .padding(10.dp),
                    painter = painterResource(id = iconDrawableId),
                    contentDescription = "Show Moshafs"
            )
        }
    }
}

/**
 * A composable that displays the name of a reciter.
 *
 * This function can display either a skeleton loader or the actual reciter's name.
 * When displaying the name, it highlights any parts of the name that match the search query.
 * The text uses a specific Arabic font and has a marquee effect if it's too long to fit.
 *
 * @param isSkeleton [Boolean] A boolean flag to indicate whether to show the skeleton loader UI.
 * @param brush [Brush?][Brush] The [Brush] to be used for the skeleton loader's background. It's only used when [isSkeleton] is `true`.
 * @param reciter [Reciter?][Reciter] The [Reciter] object containing the name to display. It's only used when [isSkeleton] is `false`.
 * @param searchQuery [String] The search query string used to highlight matching parts of the reciter's name.
 */
@Composable
private fun ReciterName(
        isSkeleton: Boolean,
        brush: Brush?,
        reciter: Reciter?,
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
            if (reciter == null) return
            Text(
                    modifier = Modifier.basicMarquee(),
                    text = highlightMatchingText(
                            fullText = reciter.name,
                            query = searchQuery,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            defaultColor = MaterialTheme.colorScheme.onSurface
                    ),
                    fontSize = 40.sp,
                    fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),

                    )
        }
    }
}

/**
 * A composable that displays the number of Moshafs (Quranic recitations) available for a reciter.
 * It can also display a skeleton loader view.
 *
 * @param isSkeleton [Boolean] A boolean flag to indicate whether to show the skeleton loader UI.
 * @param brush [Brush?][Brush] The [Brush] to use for the skeleton loader's background. It's only used when [isSkeleton] is true.
 * @param moshafCount [Int?][Int] The number of Moshafs to display. If null, and [isSkeleton] is false, nothing is rendered.
 */
@Composable
private fun MoshafCount(
        isSkeleton: Boolean,
        brush: Brush?,
        moshafCount: Int?
) {
    when {
        isSkeleton -> {
            if (brush == null) return
            Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(50.dp)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )
        }

        else       -> {
            if (moshafCount == null) return
            Text(
                    modifier = Modifier.basicMarquee(),
                    text = pluralStringResource(R.plurals.moshaf_count, moshafCount, moshafCount),
                    fontSize = 30.sp,
                    fontFamily = FontFamily(Font(Rs.font.diwany_1))
            )
        }
    }
}

@Composable
@Preview(locale = "ar")
private fun ReciterCardPreview() {
    val reciter = sampleReciters.first { it.id == 118.asReciterId }
    val playingMoshafId = reciter.moshafList.random().id
    val randomChars = reciter.name.toList().shuffled().take(5)
    val searchQuery = randomChars.joinToString("")

    ReciterCard(
            reciter = reciter,
            isExpanded = true,
            searchQuery = searchQuery,
            isSkeleton = false,
            isPlaying = true,
            playingMoshafId = playingMoshafId,
    )
}

/**
 * A Jetpack Compose preview function that showcases the [ReciterCard]
 * in its skeleton loading state. This preview is crucial for visualizing
 * the user interface while data is being fetched.
 *
 * It wraps the [ReciterCard] within a [ShimmerAnimation] to demonstrate
 * the loading effect. The card is configured with `isSkeleton` is `true`
 * and uses a sample reciter from the [sampleReciters] collection as
 * placeholder data, although it won't be visible due to the skeleton
 * state. This provides an accurate visual representation of the
 * component's appearance during initial loading or data refresh cycles.
 */
@Composable
@Preview(locale = "ar")
private fun ReciterCardSkeletonPreview() {
    val reciter = sampleReciters.first { it.id == 118.asReciterId }
    val randomChars = reciter.name.toList().shuffled().take(5)
    val searchQuery = randomChars.joinToString("")

    ShimmerAnimation { brush ->
        ReciterCard(
                reciter = reciter,
                isExpanded = true,
                searchQuery = searchQuery,
                isSkeleton = true,
                brush = brush
        )
    }
}
