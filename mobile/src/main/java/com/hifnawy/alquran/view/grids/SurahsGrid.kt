package com.hifnawy.alquran.view.grids

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hifnawy.alquran.QuranDownloadServiceObserver
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.domain.QuranDownloadService.QuranDownloadManager
import com.hifnawy.alquran.shared.domain.QuranDownloadService.QuranDownloadManager.DownloadState
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.ReciterId
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LongEx.asLocalizedHumanReadableSize
import com.hifnawy.alquran.utils.ArabicPluralStringResource.arabicPluralStringResource
import com.hifnawy.alquran.utils.LazyGridScopeEx.gridItems
import com.hifnawy.alquran.utils.ModifierEx.AnimationType
import com.hifnawy.alquran.utils.ModifierEx.animateItemPosition
import com.hifnawy.alquran.utils.StringEx.stripFormattingChars
import com.hifnawy.alquran.view.SearchBar
import com.hifnawy.alquran.view.ShimmerAnimation
import com.hifnawy.alquran.view.gridItems.SurahCard
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sign
import com.hifnawy.alquran.shared.R as Rs

/**
 * A Composable that displays a grid of Surahs for a specific reciter and moshaf.
 *
 * It includes a title bar with the reciter and moshaf name, a search bar to filter surahs,
 * and a lazy grid of [SurahCard] items. It also handles scrolling to the currently playing
 * surah card.
 *
 * @param modifier [Modifier] The modifier to apply to the grid.
 * @param reciter [Reciter] The [Reciter] for whom the surahs are being displayed.
 * @param moshaf [Moshaf] The current [Moshaf].
 * @param reciterSurahs [List< Surah >][List] The [List] of [Surah]s available for the reciter and moshaf.
 * @param isSkeleton [Boolean] A boolean indicating if the surahs list is currently being fetched.
 * @param isPlaying [Boolean] A boolean indicating if any surah is being played.
 * @param playingSurahId [Int?][Int] The id of the currently playing surah.
 * @param playingMoshafId [Int?][Int] The id of the currently playing moshaf.
 * @param playingReciterId [ReciterId?][ReciterId] The id of the currently playing reciter.
 * @param onSurahCardClick [((surah: Surah) -> Unit)][onSurahCardClick] A callback to be called when a surah card is clicked.
 */
@Composable
fun SurahsGrid(
    modifier: Modifier = Modifier,
    reciter: Reciter,
    moshaf: Moshaf,
    reciterSurahs: List<Surah>,
    isSkeleton: Boolean = false,
    isPlaying: Boolean = false,
    playingSurahId: Int? = null,
    playingMoshafId: Int? = null,
    playingReciterId: ReciterId? = null,
    onSurahCardClick: (surah: Surah) -> Unit
) {
    var lazyGridHeight by remember { mutableIntStateOf(0) }
    var surahCardHeight by remember { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var lastAnimatedIndex by rememberSaveable { mutableIntStateOf(-1) }

    var areDownloadsPaused by rememberSaveable { mutableStateOf(false) }
    var isDownloadProgressDialogShown by rememberSaveable { mutableStateOf(false) }

    val listState = rememberSurahsGridState()
    val filteredSurahs = rememberSaveable(reciterSurahs, searchQuery) {
        reciterSurahs.filter { surah ->
            surah.name.stripFormattingChars.trim().lowercase()
                .contains(searchQuery.stripFormattingChars.trim().lowercase())
        }
    }

    SurahsGridContainer(isSkeleton = isSkeleton) { brush ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 10.dp, top = 20.dp, end = 10.dp)
        ) {

            Column(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                TitleBar(
                    isSkeleton = isSkeleton,
                    brush = brush,
                    reciter = reciter,
                    moshaf = moshaf
                )


                Spacer(modifier = Modifier.size(5.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBar(
                        modifier = Modifier.weight(1f),
                        isSkeleton = isSkeleton,
                        brush = brush,
                        query = searchQuery,
                        placeholder = stringResource(R.string.search_surahs),
                        label = stringResource(R.string.search_surahs),
                        onQueryChange = { newQuery -> searchQuery = newQuery },
                        onClearQuery = { searchQuery = "" }
                    )

                    Spacer(modifier = Modifier.size(5.dp))

                    DownloadButton(
                        modifier = Modifier.fillMaxSize(),
                        isSkeleton = isSkeleton,
                        brush = brush,
                        onClick = { isDownloadProgressDialogShown = true }
                    )
                }
            }



            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
//                modifier = modifier
//                    .fillMaxWidth(),
                state = listState,
//                columns = GridCells.Adaptive(minSize = 150.dp),
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                gridItems(
                    isSkeleton = isSkeleton,
                    mockCount = 114,
                    items = filteredSurahs
                ) { index, surah ->
                    val isScrollingDown = index > lastAnimatedIndex
                    val animationType = when {
                        isScrollingDown -> AnimationType.FallDown
                        else -> AnimationType.RiseUp
                    }

                    SurahCard(
                        modifier = Modifier
                            .animateItemPosition(durationMs = 300, animationType = animationType)
                            .onSizeChanged { size -> surahCardHeight = size.height },
                        surah = surah,
                        isSkeleton = isSkeleton,
                        isPlaying = isPlaying && playingSurahId == surah?.id && playingMoshafId == moshaf.id && playingReciterId == reciter.id,
                        searchQuery = searchQuery,
                        brush = brush,
                        onClick = onSurahCardClick
                    )

                    lastAnimatedIndex = index
                }
            }

            ScrollToPlayingSurah(
                listState = listState,
                isPlaying = isPlaying,
                reciter = reciter,
                filteredSurahs = filteredSurahs,
                playingSurahId = playingSurahId,
                playingReciterId = playingReciterId,
                lazyGridHeight = lazyGridHeight,
                surahCardHeight = surahCardHeight
            )

            if (!isDownloadProgressDialogShown) return@Column
            DownloadProgressDialog(
                reciter = reciter,
                moshaf = moshaf,
                reciterSurahs = reciterSurahs,
                areDownloadsPaused = areDownloadsPaused,
                onDownloadsPaused = { areDownloadsPaused = true },
            ) {
                isDownloadProgressDialogShown = false
            }
        }
    }
}

/**
 * A Composable function that remembers the state of a [LazyGridState] across recompositions
 * and configuration changes. It uses [rememberSaveable] with the default [LazyGridState.Saver]
 * to persist the grid's scroll position.
 *
 * @param firstVisibleItemIndex [Int] The initial index of the first visible item in the grid.
 * @param firstVisibleItemScrollOffset [Int] The initial scroll offset of the first visible item.
 *
 * @return [LazyGridState] A new [LazyGridState] instance that is saved and restored automatically.
 */
@Composable
private fun rememberSurahsGridState(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0
) = rememberSaveable(saver = LazyGridState.Saver) {
    LazyGridState(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemScrollOffset
    )
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
private fun SurahsGridContainer(
    isSkeleton: Boolean,
    content: @Composable (brush: Brush?) -> Unit
) = when {
    isSkeleton -> ShimmerAnimation { brush -> content(brush) }
    else -> content(null)
}

/**
 * TODO: check if this is the best way to calculate the mini player height and bottom padding
 * FIX: don't hardcode the mini player height and bottom padding
 *
 * TODO: check which is better???
 * add [isPlaying] as a key for LaunchedEffect or not?
 *
 * - if added, the LaunchedEffect will be called every time [isPlaying] changes and the grid
 *   will be scrolled to the playing surah when any surah is chosen
 * - if not added, the LaunchedEffect will be called only when [lazyGridHeight] or [surahCardHeight]
 *   changes and the grid will be scrolled to the playing surah only when the composable is recomposed
 *
 * @param listState [LazyGridState] The state of the grid.
 * @param isPlaying [Boolean] A boolean indicating if any surah is being played.
 * @param reciter [Reciter] The [Reciter] for whom the surahs are being displayed.
 * @param filteredSurahs [List< Surah >][List] The [List] of [Surah]s available for the reciter and moshaf.
 * @param playingSurahId [Int?][Int] The id of the currently playing surah.
 * @param playingReciterId [ReciterId?][ReciterId] The id of the currently playing reciter.
 * @param lazyGridHeight [Int] The height of the grid.
 * @param surahCardHeight [Int] The height of a surah card.
 */
@Composable
private fun ScrollToPlayingSurah(
    listState: LazyGridState,
    isPlaying: Boolean,
    reciter: Reciter,
    filteredSurahs: List<Surah>,
    playingSurahId: Int?,
    playingReciterId: ReciterId?,
    lazyGridHeight: Int,
    surahCardHeight: Int
) {
    val density = LocalDensity.current
    val miniPlayerHeight = with(density) { 90.dp.toPx() }
    val miniPlayerBottomPadding = with(density) { 30.dp.toPx() }

    LaunchedEffect(isPlaying, lazyGridHeight) {
        if (!isPlaying) return@LaunchedEffect
        if (lazyGridHeight == 0) return@LaunchedEffect
        if (surahCardHeight == 0) return@LaunchedEffect
        if (playingSurahId == null) return@LaunchedEffect
        if (playingReciterId != reciter.id) return@LaunchedEffect

        val index = filteredSurahs.indexOfFirst { surah -> surah.id == playingSurahId }
        if (index < 0) return@LaunchedEffect

        val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        val miniPlayerTopEdge = lazyGridHeight - miniPlayerHeight - miniPlayerBottomPadding

        val availableViewportHeight =
            lazyGridHeight - miniPlayerHeight - (miniPlayerBottomPadding * 2.1f)
        val scrollDistance = getScrollDistance(
            itemInfo = itemInfo,
            surahCardHeight = surahCardHeight,
            availableViewportHeight = availableViewportHeight
        )

        if (!shouldScroll(
                itemInfo = itemInfo,
                miniPlayerTopEdge = miniPlayerTopEdge,
                scrollDistance = scrollDistance
            )
        ) return@LaunchedEffect
        executeScroll(
            index = index,
            itemInfo = itemInfo,
            listState = listState,
            scrollDistance = scrollDistance,
            surahCardHeight = surahCardHeight,
            availableViewportHeight = availableViewportHeight
        )
    }
}

/**
 * Calculates the distance to scroll to center the currently playing surah card within the available viewport.
 *
 * If the item is visible (`itemInfo` is not null), it calculates the scroll distance needed to move the
 * vertical center of the card to the vertical center of the `availableViewportHeight`.
 *
 * If the item is not visible (`itemInfo` is null), it returns `0f`, as the scroll will be handled
 * by `scrollToItem` first.
 *
 * @param surahCardHeight [Int] The height of a single surah card in pixels.
 * @param itemInfo [LazyGridItemInfo?][LazyGridItemInfo] for the item to be centered, or `null` if it's not currently visible.
 * @param availableViewportHeight [Float] The height of the visible area of the grid, excluding obstructions like a mini-player.
 *
 * @return [Float] The calculated scroll distance in pixels. A positive value means scrolling down, a negative value means scrolling up.
 */
private fun getScrollDistance(
    surahCardHeight: Int,
    itemInfo: LazyGridItemInfo?,
    availableViewportHeight: Float
) = when {
    itemInfo != null -> (itemInfo.offset.y + surahCardHeight / 2f) - (availableViewportHeight / 2f)
    else -> 0f
}

/**
 * Determines whether the grid should scroll to the currently playing surah.
 *
 * Scrolling is necessary under the following conditions:
 * - The surah card is not currently visible on screen ([itemInfo] is `null`).
 * - The surah card is partially obscured by the `mini-player`.
 * - The surah card is not perfectly centered and requires a scroll of more than `1 pixel` to be centered.
 *
 * @param scrollDistance [Float] The calculated distance required to center the item.
 * @param miniPlayerTopEdge [Float] The `Y-coordinate` of the top edge of the mini-player, used to check for obstruction.
 * @param itemInfo [LazyGridItemInfo?][LazyGridItemInfo] Information about the item if it's visible, otherwise `null`.
 *
 * @return [Boolean] `true` if a scroll should be executed, `false` otherwise.
 */
private fun shouldScroll(
    scrollDistance: Float,
    miniPlayerTopEdge: Float,
    itemInfo: LazyGridItemInfo?
) = when {
    itemInfo == null -> true
    itemInfo.offset.y + itemInfo.size.height > miniPlayerTopEdge -> true
    abs(scrollDistance) > 1f -> true
    else -> false
}

/**
 * Executes a scroll animation to bring a specific surah card into view and center it.
 *
 * It uses a different strategy depending on whether the target item is already visible:
 * - If the item is **not visible** ([itemInfo] is `null`):
 *    - It first performs a small, quick scroll in the direction of the target item.
 *    - It then recalculates the [scrollDistance] based on the new layout information.
 *    - Finally, it recursively calls itself to perform the main centering animation.
 * - If the item is **already visible** ([itemInfo] is not `null`):
 *    - It directly performs a smooth scroll animation ([LazyGridState.animateScrollBy]) using the
 *      pre-calculated [scrollDistance] to center it.
 *
 * @param index [Int] The index of the target item in the lazy grid.
 * @param surahCardHeight [Int] The height of a single surah card in pixels.
 * @param scrollDistance [Float] The distance in pixels to scroll to center the item. This is only
 *   used if the item is already visible.
 * @param listState [LazyGridState] The [LazyGridState] that controls the grid's scroll position.
 * @param itemInfo [LazyGridItemInfo?][LazyGridItemInfo] The [LazyGridItemInfo] for the item if it's currently visible, or `null` otherwise.
 * @param availableViewportHeight [Float] The height of the visible area of the grid, used for centering calculations.
 *
 * @return [Float] The amount of scrolled distance in pixels.
 */
private suspend fun executeScroll(
    index: Int,
    surahCardHeight: Int,
    scrollDistance: Float,
    listState: LazyGridState,
    itemInfo: LazyGridItemInfo?,
    availableViewportHeight: Float
): Float = listState.run {
    val animationDuration = 700

    when (itemInfo) {
        null -> {
            val scrollDirection = sign((index - firstVisibleItemIndex).toFloat())

            val initialScrollDistance = animateScrollBy(
                value = scrollDirection * surahCardHeight,
                animationSpec = tween(durationMillis = 50, easing = FastOutLinearInEasing)
            )

            val newItemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            val scrollDistance = getScrollDistance(
                itemInfo = newItemInfo,
                surahCardHeight = surahCardHeight,
                availableViewportHeight = availableViewportHeight
            )

            initialScrollDistance + executeScroll(
                index = index,
                surahCardHeight = surahCardHeight,
                scrollDistance = scrollDistance,
                listState = this,
                itemInfo = newItemInfo,
                availableViewportHeight = availableViewportHeight
            )
        }

        else -> animateScrollBy(
            value = scrollDistance,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = FastOutLinearInEasing
            )
        )
    }
}

/**
 * A Composable that displays the title for the [SurahsGrid].
 *
 * It shows the name of the reciter and the name of the moshaf along with the total surah count.
 * The text uses specific fonts and sizes for styling. If [isSkeleton] is `true`, it displays
 * placeholder shimmer bars with dimensions matching the expected text size. The texts also have
 * a marquee effect applied to handle long names that might overflow.
 *
 * @param isSkeleton [Boolean] If `true`, displays shimmering placeholder bars instead of text.
 * @param brush [Brush?][Brush] The [Brush] to be used for the background of the shimmer placeholders.
 *   This is only used when [isSkeleton] is `true`.
 * @param reciter [Reciter] The reciter whose name is to be displayed.
 * @param moshaf [Moshaf] The moshaf whose name and surah count are to be displayed.
 */
@Composable
private fun TitleBar(
    isSkeleton: Boolean,
    brush: Brush?,
    reciter: Reciter,
    moshaf: Moshaf
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val reciterText = reciter.name
    val reciterStyle = TextStyle(
        fontSize = 50.sp,
        fontFamily = when {
            QuranApplication.currentLocaleInfo.isRTL -> FontFamily(Font(Rs.font.decotype_thuluth_2))
            else -> FontFamily(Font(Rs.font.aref_ruqaa))
        }
    )

    val moshafText =
        "${moshaf.name} - ${arabicPluralStringResource(R.plurals.surah_count, moshaf.surahsCount)}"
    val moshafStyle = TextStyle(
        fontSize = 25.sp,
        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa))
    )

    val reciterLayoutResult: TextLayoutResult = remember(reciterText, reciterStyle) {
        textMeasurer.measure(
            text = AnnotatedString(reciterText),
            style = reciterStyle,
            constraints = Constraints(maxWidth = Int.MAX_VALUE)
        )
    }

    val moshafLayoutResult: TextLayoutResult = remember(moshafText, moshafStyle) {
        textMeasurer.measure(
            text = AnnotatedString(moshafText),
            style = moshafStyle,
            constraints = Constraints(maxWidth = Int.MAX_VALUE)
        )
    }

    val reciterHeight = with(density) { reciterLayoutResult.size.height.toDp() }
    val moshafHeight = with(density) { moshafLayoutResult.size.height.toDp() }
    val reciterWidth = with(density) { reciterLayoutResult.size.width.toDp() }
    val moshafWidth = with(density) { moshafLayoutResult.size.width.toDp() }

    when {
        isSkeleton -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (brush == null) return

            Spacer(
                modifier = Modifier
                    .width(reciterWidth)
                    .height(reciterHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Spacer(
                modifier = Modifier
                    .width(moshafWidth)
                    .height(moshafHeight)
                    .clip(RoundedCornerShape(15.dp))
                    .background(brush)
            )
        }

        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                modifier = Modifier
//                        .fillMaxWidth()
                    .basicMarquee(),
                text = reciterText,
                style = reciterStyle,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                modifier = Modifier
//                        .fillMaxWidth()
                    .basicMarquee(),
                text = moshafText,
                style = moshafStyle,
                color = MaterialTheme.colorScheme.onSurface
            )

        }
    }
}

@Composable
private fun DownloadButton(
    modifier: Modifier = Modifier,
    isSkeleton: Boolean,
    brush: Brush?,
    onClick: () -> Unit = {}
) = IconButton(onClick = if (!isSkeleton) onClick else { -> }) {
    when {
        isSkeleton -> {
            if (brush == null) return@IconButton
            Spacer(modifier = modifier.background(brush))
        }

        else ->
            Icon(
                modifier = modifier,
                painter = painterResource(id = R.drawable.download_24px),
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = null
            )
    }
}

@Composable
@SuppressLint("UnsafeOptInUsageError")
private fun DownloadProgressDialog(
    reciter: Reciter,
    moshaf: Moshaf,
    reciterSurahs: List<Surah>,
    areDownloadsPaused: Boolean,
    onDownloadsPaused: () -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    var downloadStatus by rememberSaveable { mutableStateOf<DownloadState?>(null) }
    var downloadedSurahsCount by rememberSaveable { mutableIntStateOf(0) }
    var downloadSurah by rememberSaveable { mutableStateOf<Surah?>(null) }

    val progressIndicatorHeight = 25.dp
    val fontSize = 25.sp

    LaunchedEffect(areDownloadsPaused) {
        when {
            areDownloadsPaused -> QuranDownloadManager.resumeDownloads(
                context = context,
                reciter = reciter,
                moshaf = moshaf,
                surahs = reciterSurahs
            )

            else -> QuranDownloadManager.queueDownloads(
                context = context,
                reciter = reciter,
                moshaf = moshaf,
                surahs = reciterSurahs
            )
        }
    }

    QuranDownloadServiceObserver { downloadState ->
        downloadStatus = downloadState

        downloadSurah = downloadState.data.surah

        if (downloadState.state == DownloadState.State.COMPLETED) downloadedSurahsCount++

        Timber.debug("$downloadState")
    }

    val downloadState = downloadStatus ?: return
    val surah = downloadSurah ?: return
    val downloadedSize = downloadState.downloaded.asLocalizedHumanReadableSize
    val totalSize = downloadState.total.asLocalizedHumanReadableSize
    val downloadPercentage = downloadState.percentage

    if (downloadedSurahsCount >= reciterSurahs.size) QuranDownloadManager.removeDownloads(context = context)

    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(25.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.downloading_surahs),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = fontSize,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa))
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    R.string.download_count,
                    downloadedSurahsCount,
                    reciterSurahs.size
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = fontSize,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa))
            )

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(progressIndicatorHeight),
                progress = { downloadedSurahsCount / reciterSurahs.size.toFloat() }
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.downloading_surah, surah.id, surah.name),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = fontSize,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa))
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "${stringResource(R.string.download_progress, downloadedSize, totalSize)} " +
                        "(${stringResource(R.string.download_percentage, downloadPercentage)})",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = fontSize,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa))
            )

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(progressIndicatorHeight),
                progress = {
                    if (downloadState.total == 0L) return@LinearProgressIndicator 0f

                    downloadState.downloaded.toFloat() / downloadState.total.toFloat()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    QuranDownloadManager.pauseDownloads(
                        context = context,
                        reciter = reciter,
                        moshaf = moshaf,
                        surahs = reciterSurahs
                    )
                    onDownloadsPaused()
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    text = stringResource(R.string.download_cancel),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = fontSize,
                    fontFamily = FontFamily(Font(Rs.font.aref_ruqaa))
                )
            }
        }
    }
}
