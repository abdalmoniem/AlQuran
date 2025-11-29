package com.hifnawy.alquran.view.grids

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.ReciterId
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.utils.ArabicPluralStringResource.arabicPluralStringResource
import com.hifnawy.alquran.utils.LazyGridScopeEx.gridItems
import com.hifnawy.alquran.utils.ModifierEx.AnimationType
import com.hifnawy.alquran.utils.ModifierEx.animateItemPosition
import com.hifnawy.alquran.view.SearchBar
import com.hifnawy.alquran.view.ShimmerAnimation
import com.hifnawy.alquran.view.gridItems.SurahCard
import kotlin.math.abs
import com.hifnawy.alquran.shared.R as Rs

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
    SurahsGridContainer(isSkeleton = isSkeleton) { brush ->
        Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, top = 10.dp, end = 10.dp)
        ) {
            var lazyGridHeight by remember { mutableIntStateOf(0) }
            var surahCardHeight by remember { mutableIntStateOf(0) }
            var searchQuery by rememberSaveable { mutableStateOf("") }
            var lastAnimatedIndex by rememberSaveable { mutableIntStateOf(-1) }

            val listState = rememberSurahsGridState()
            val filteredSurahs = rememberSaveable(reciterSurahs, searchQuery) {
                reciterSurahs.filter { surah ->
                    surah.name.contains(searchQuery)
                }
            }

            TitleBar(
                    isSkeleton = isSkeleton,
                    brush = brush,
                    reciter = reciter,
                    moshaf = moshaf
            )

            Spacer(modifier = Modifier.size(5.dp))

            SearchBar(
                    isSkeleton = isSkeleton,
                    brush = brush,
                    query = searchQuery,
                    placeholder = stringResource(R.string.search_surahs),
                    label = stringResource(R.string.search_surahs),
                    onQueryChange = { newQuery -> searchQuery = newQuery },
                    onClearQuery = { searchQuery = "" }
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                    modifier = modifier.onSizeChanged { size -> lazyGridHeight = size.height },
                    state = listState,
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                gridItems(isSkeleton = isSkeleton, mockCount = 114, items = filteredSurahs) { index, surah ->
                    val isScrollingDown = index > lastAnimatedIndex
                    val animationType = when {
                        isScrollingDown -> AnimationType.FallDown
                        else            -> AnimationType.RiseUp
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
        }
    }
}

@Composable
private fun rememberSurahsGridState(
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0
) = rememberSaveable(saver = LazyGridState.Saver) {
    LazyGridState(firstVisibleItemIndex = firstVisibleItemIndex, firstVisibleItemScrollOffset = firstVisibleItemScrollOffset)
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

        val availableViewportHeight = lazyGridHeight - miniPlayerHeight - (miniPlayerBottomPadding * 2.1f)
        val scrollDistance = getScrollDistance(
                itemInfo = itemInfo,
                surahCardHeight = surahCardHeight,
                availableViewportHeight = availableViewportHeight
        )

        if (!shouldScroll(itemInfo = itemInfo, miniPlayerTopEdge = miniPlayerTopEdge, scrollDistance = scrollDistance)) return@LaunchedEffect
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

private fun getScrollDistance(surahCardHeight: Int, itemInfo: LazyGridItemInfo?, availableViewportHeight: Float) = when {
    itemInfo != null -> (itemInfo.offset.y + surahCardHeight / 2f) - (availableViewportHeight / 2f)
    else             -> 0f
}

private fun shouldScroll(scrollDistance: Float, miniPlayerTopEdge: Float, itemInfo: LazyGridItemInfo?) = when {
    itemInfo == null -> true
    itemInfo.offset.y + itemInfo.size.height > miniPlayerTopEdge -> true
    abs(scrollDistance) > 1f -> true
    else -> false
}

private suspend fun executeScroll(
        index: Int,
        surahCardHeight: Int,
        scrollDistance: Float,
        listState: LazyGridState,
        itemInfo: LazyGridItemInfo?,
        availableViewportHeight: Float
) = listState.run {
    when (itemInfo) {
        null -> {
            val scrollOffset = (availableViewportHeight / 2f) - (surahCardHeight / 2f)
            scrollToItem(index = index, scrollOffset = 0)
            animateScrollBy(value = -scrollOffset, animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing))
        }

        else -> animateScrollBy(value = scrollDistance, animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing))
    }
}

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
            fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2))
    )

    val moshafText = "${moshaf.name} - ${arabicPluralStringResource(R.plurals.surah_count, moshaf.surahsCount)}"
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
        isSkeleton -> Column {
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

        else       -> Column {
            Text(
                    modifier = Modifier.basicMarquee(),
                    text = reciterText,
                    style = reciterStyle,
                    color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                    modifier = Modifier.basicMarquee(),
                    text = moshafText,
                    style = moshafStyle,
                    color = MaterialTheme.colorScheme.onSurface
            )

        }
    }
}

@Composable
private fun SurahsGridContainer(
        isSkeleton: Boolean,
        content: @Composable (brush: Brush?) -> Unit
) = when {
    isSkeleton -> ShimmerAnimation { brush -> content(brush) }
    else       -> content(null)
}
