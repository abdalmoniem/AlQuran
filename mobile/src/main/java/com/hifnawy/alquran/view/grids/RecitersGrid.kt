package com.hifnawy.alquran.view.grids

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.ReciterId
import com.hifnawy.alquran.shared.model.asReciterId
import com.hifnawy.alquran.utils.LazyGridScopeEx.gridItems
import com.hifnawy.alquran.utils.ModifierEx.AnimationType
import com.hifnawy.alquran.utils.ModifierEx.animateItemPosition
import com.hifnawy.alquran.utils.StringEx.stripFormattingChars
import com.hifnawy.alquran.view.SearchBar
import com.hifnawy.alquran.view.ShimmerAnimation
import com.hifnawy.alquran.view.gridItems.ReciterCard
import com.hifnawy.alquran.view.player.AnimatedAudioBars
import com.hifnawy.alquran.shared.R as Rs

/**
 * A Composable that displays a grid of Quran reciters.
 *
 * This grid supports searching, displaying a `skeleton`/`loading` state, and `highlighting` the
 * currently playing reciter and their specific Moshaf (Quran narration). Each reciter can be
 * expanded to show their available Moshafs.
 *
 * @param modifier [Modifier] The modifier to be applied to the grid container.
 * @param reciters [List< Reciter >][List] The [List] of [Reciter] objects to display.
 * @param isSkeleton [Boolean] A [Boolean] flag to indicate whether to show a shimmer-animated skeleton UI
 * while data is loading. Defaults to `false`.
 * @param isPlaying [Boolean] A [Boolean] flag to indicate if any audio is currently playing.
 * @param playingReciterId [ReciterId?][ReciterId] The [ReciterId] of the reciter that is currently playing. Used for highlighting.
 * @param playingMoshafId [Int]The ID of the [Moshaf] that is currently playing. Used for highlighting within an expanded reciter card.
 * @param onMoshafClick [(reciter: Reciter, moshaf: Moshaf) -> Unit][onMoshafClick] A callback lambda that is invoked when a user clicks on a [Moshaf] within a reciter's card.
 *   It provides the parent [Reciter] and the clicked [Moshaf].
 */
@Composable
fun RecitersGrid(
        modifier: Modifier = Modifier,
        reciters: List<Reciter>,
        isSkeleton: Boolean = false,
        isPlaying: Boolean = false,
        playingReciterId: ReciterId? = null,
        playingMoshafId: Int? = null,
        onMoshafClick: (reciter: Reciter, moshaf: Moshaf) -> Unit = { _, _ -> }
) {
    RecitersGridContainer(isSkeleton = isSkeleton) { brush ->
        Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, top = 10.dp, end = 10.dp)
        ) {
            var searchQuery by rememberSaveable { mutableStateOf("") }
            var lastAnimatedIndex by rememberSaveable { mutableIntStateOf(-1) }
            var selectedReciterId by rememberSaveable(stateSaver = Saver(save = { it.value }, restore = { ReciterId(it) })) { mutableStateOf((-1).asReciterId) }
            var expandedReciterCardId by rememberSaveable(stateSaver = Saver(save = { it.value }, restore = { ReciterId(it) })) { mutableStateOf((-1).asReciterId) }

            val listState = rememberRecitersGridState()
            val filteredReciters = rememberSaveable(reciters, searchQuery) {
                reciters.filter { reciter ->
                    reciter.name.stripFormattingChars.trim().lowercase().contains(searchQuery.stripFormattingChars.trim().lowercase())
                }
            }

            LaunchedEffect(searchQuery) {
                if (selectedReciterId == (-1).asReciterId) return@LaunchedEffect
                val reciterIndex = filteredReciters.indexOfFirst { it.id == selectedReciterId }.takeIf { it != -1 } ?: return@LaunchedEffect

                listState.scrollToItem(reciterIndex)
            }

            TitleBar(isSkeleton = isSkeleton, brush = brush)

            Spacer(modifier = Modifier.size(5.dp))

            SearchBar(
                    modifier = Modifier.fillMaxWidth(),
                    isSkeleton = isSkeleton,
                    brush = brush,
                    query = searchQuery,
                    placeholder = stringResource(R.string.search_reciters),
                    label = stringResource(R.string.search_reciters),
                    onQueryChange = { newQuery -> searchQuery = newQuery },
                    onClearQuery = { searchQuery = "" }
            )

            Spacer(Modifier.height(10.dp))

            LazyVerticalGrid(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Adaptive(minSize = 250.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                gridItems(isSkeleton = isSkeleton, mockCount = 300, items = filteredReciters) { index, reciter ->
                    val isScrollingDown = index > lastAnimatedIndex

                    GridItem(
                            isScrollingDown = isScrollingDown,
                            reciter = reciter,
                            expandedReciterId = expandedReciterCardId,
                            searchQuery = searchQuery,
                            isSkeleton = isSkeleton,
                            isPlaying = isPlaying && playingReciterId == reciter?.id,
                            playingMoshafId = playingMoshafId,
                            brush = brush,
                            onToggleExpand = { reciterId ->
                                expandedReciterCardId = when (expandedReciterCardId) {
                                    reciterId -> (-1).asReciterId // Collapse it
                                    else      -> reciterId // Expand the new one
                                }
                            },
                            onMoshafClick = { reciter, moshaf ->
                                selectedReciterId = reciter.id
                                onMoshafClick(reciter, moshaf)
                            }
                    )

                    lastAnimatedIndex = index
                }
            }
        }
    }
}

/**
 * A private composable function that creates and remembers a [LazyGridState] for the reciters grid.
 *
 * This function uses [rememberSaveable] with [LazyGridState.Saver] to ensure that the grid's
 * scroll state (the first visible item index and its scroll offset) is preserved across
 * configuration changes and process death.
 *
 * @param firstVisibleItemIndex [Int] The initial index to scroll to.
 * @param firstVisibleItemScrollOffset [Int] The initial scroll offset of the first visible item.
 *
 * @return [LazyGridState] A remembered [LazyGridState] instance.
 */
@Composable
private fun rememberRecitersGridState(
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0
) = rememberSaveable(saver = LazyGridState.Saver) {
    LazyGridState(firstVisibleItemIndex = firstVisibleItemIndex, firstVisibleItemScrollOffset = firstVisibleItemScrollOffset)
}

/**
 * A container Composable that conditionally applies a shimmer animation.
 *
 * If [isSkeleton] is `true`, it wraps the [content] within a [ShimmerAnimation],
 * providing a [Brush] that can be used to draw shimmering placeholder UI.
 * If [isSkeleton] is `false`, it simply renders the [content] directly, passing `null`
 * for the brush.
 *
 * @param isSkeleton [Boolean] A [Boolean] indicating whether to show the shimmer effect.
 * @param content [@Composable (brush: Brush?) -> Unit][content] A composable lambda that receives an optional [Brush].
 *   The brush is non-null only when [isSkeleton] is `true`.
 */
@Composable
private fun RecitersGridContainer(
        isSkeleton: Boolean,
        content: @Composable (brush: Brush?) -> Unit
) = when {
    isSkeleton -> ShimmerAnimation { brush -> content(brush) }
    else       -> content(null)
}

/**
 * A private composable that displays the main title bar of the screen, including the app icon and name.
 *
 * This function is responsible for rendering the title. It has two states:
 * - **Normal State:** Shows the app icon and the app name styled with a specific font.
 * - **Skeleton State:** Shows placeholder shapes with a shimmer effect ([Brush]) where the icon and title would be. This is used during data loading.
 *
 * It dynamically calculates the size of the title text to ensure the skeleton placeholder has the correct dimensions.
 *
 * @param isSkeleton [Boolean] If `true`, the composable renders in its `skeleton` (`loading`) state.
 * @param brush [Brush?][Brush] The [Brush] to use for the shimmer animation in the skeleton state. It's expected to be non-null when [isSkeleton] is `true`.
 */
@Composable
private fun TitleBar(
        isSkeleton: Boolean,
        brush: Brush?
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val titleText = stringResource(R.string.app_name)
    val titleStyle = TextStyle(
            fontSize = 50.sp,
            fontFamily = when {
                QuranApplication.currentLocaleInfo.isRTL -> FontFamily(Font(Rs.font.decotype_thuluth_2))
                else                                     -> FontFamily(Font(Rs.font.aref_ruqaa))
            }
    )

    val titleLayoutResult = remember(titleText, titleStyle) {
        textMeasurer.measure(
                text = AnnotatedString(titleText),
                style = titleStyle,
                constraints = Constraints(maxWidth = Int.MAX_VALUE)
        )
    }

    val titleHeight = with(density) { titleLayoutResult.size.height.toDp() }
    val titleWidth = with(density) { titleLayoutResult.size.width.toDp() }

    if (isSkeleton) {
        if (brush == null) return
        Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(brush)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Spacer(
                    modifier = Modifier
                        .width(titleWidth)
                        .height(titleHeight)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )
        }
    } else {
        Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                    modifier = Modifier.size(64.dp),
                    bitmap = ImageBitmap.imageResource(R.drawable.app_icon),
                    contentDescription = titleText
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
                    text = titleText,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * A private composable that acts as a wrapper for a [ReciterCard] within the [LazyVerticalGrid].
 *
 * This function is responsible for:
 * - Applying an animation to the grid item based on the scroll direction. It uses [animateItemPosition] to create
 *   a [AnimationType.FallDown] or [AnimationType.RiseUp] effect.
 * - Determining if the current card should be in an expanded state by comparing the [reciter]'s ID
 *   with the [expandedReciterId].
 * - Safely handling callbacks ([onToggleExpand], [onMoshafClick]) by providing no-op lambdas
 *   when in the `skeleton` state to prevent crashes.
 * - Passing all relevant state down to the [ReciterCard] which handles the actual UI rendering.
 *
 * @param modifier [Modifier] The modifier to be applied to the item.
 * @param isScrollingDown [Boolean] A flag indicating the scroll direction. `true` if scrolling down, `false` otherwise.
 *   This determines the animation type.
 * @param reciter [Reciter?][Reciter] The [Reciter] data for this grid item. Can be `null` in the skeleton state.
 * @param expandedReciterId [ReciterId] The ID of the currently expanded reciter in the grid.
 * @param searchQuery [String] The current search query, used for highlighting matching text in the [ReciterCard].
 * @param isSkeleton [Boolean] If `true`, the composable renders in its `skeleton` (`loading`) state.
 * @param isPlaying [Boolean] A [Boolean] flag to indicate if this reciter is currently being played, which triggers the [AnimatedAudioBars].
 * @param playingMoshafId [Int?][Int] The ID of the currently playing moshaf. `null` if no moshaf is playing.
 * @param brush [Brush?][Brush] A [Brush] used for the skeleton loading animation. Required if [isSkeleton] is true.
 * @param onToggleExpand [onToggleExpand: (ReciterId) -> Unit][onToggleExpand] A lambda function that will be invoked when the expand/collapse button is clicked.
 * @param onMoshafClick [onMoshafClick: (Reciter, Moshaf) -> Unit][onMoshafClick] A lambda function that will be invoked when a moshaf is clicked.
 */
@Composable
private fun GridItem(
        modifier: Modifier = Modifier,
        isScrollingDown: Boolean,
        reciter: Reciter?,
        expandedReciterId: ReciterId,
        searchQuery: String,
        isSkeleton: Boolean,
        isPlaying: Boolean,
        playingMoshafId: Int?,
        brush: Brush?,
        onToggleExpand: (ReciterId) -> Unit = { },
        onMoshafClick: (Reciter, Moshaf) -> Unit
) {
    val animationType = when {
        isScrollingDown -> AnimationType.FallDown
        else            -> AnimationType.RiseUp
    }

    val isExpanded = when {
        reciter == null -> false
        else            -> expandedReciterId == reciter.id
    }

    val onToggleExpand = when {
        isSkeleton -> { -> Unit }
        else       -> { -> if (reciter != null) onToggleExpand(reciter.id) }
    }

    val onMoshafClick = when {
        isSkeleton -> { _, _ -> Unit }
        else       -> onMoshafClick
    }

    ReciterCard(
            modifier = modifier.animateItemPosition(durationMs = 300, animationType = animationType),
            reciter = reciter,
            isExpanded = isExpanded,
            searchQuery = searchQuery,
            isSkeleton = isSkeleton,
            isPlaying = isPlaying,
            playingMoshafId = playingMoshafId,
            brush = brush,
            onToggleExpand = onToggleExpand,
            onMoshafClick = onMoshafClick
    )
}
