package com.hifnawy.alquran.view.grids

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.ReciterId
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.utils.ModifierEx.AnimationType
import com.hifnawy.alquran.utils.ModifierEx.animateItemPosition
import com.hifnawy.alquran.view.ShimmerAnimation
import com.hifnawy.alquran.view.gridItems.SurahCard
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun SurahsGrid(
        modifier: Modifier = Modifier,
        reciter: Reciter,
        reciterSurahs: List<Surah>,
        isSkeleton: Boolean = false,
        isPlaying: Boolean = false,
        playingSurahId: Int? = null,
        playingReciterId: ReciterId? = null,
        onSurahCardClick: (surah: Surah) -> Unit
) {
    SurahsGridContainer(isSkeleton = isSkeleton) { brush ->
        Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(10.dp)
        ) {
            var lazyGridHeight by remember { mutableIntStateOf(0) }
            var surahCardHeight by remember { mutableIntStateOf(0) }
            var searchQuery by rememberSaveable { mutableStateOf("") }
            var lastAnimatedIndex by rememberSaveable { mutableIntStateOf(-1) }

            val listState = rememberSurahsGridState()
            val filteredSurahs = rememberSaveable(reciterSurahs, searchQuery) { filterSurahs(reciterSurahs, searchQuery) }

            ReciterName(
                    isSkeleton = isSkeleton,
                    brush = brush,
                    reciter = reciter
            )

            Spacer(modifier = Modifier.size(5.dp))

            SearchBar(
                    isSkeleton = isSkeleton,
                    brush = brush,
                    query = searchQuery,
                    onQueryChange = { newQuery -> searchQuery = newQuery }
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
                gridItems(isSkeleton = isSkeleton, items = filteredSurahs) { index, surah ->
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
                            isPlaying = isPlaying && playingSurahId == surah?.id && playingReciterId == reciter.id,
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
    val miniPlayerHeight = with(density) { 90.dp.toPx() }.toInt()
    val miniPlayerBottomPadding = with(density) { 30.dp.toPx() }.toInt()

    LaunchedEffect(isPlaying, lazyGridHeight) {
        if (!isPlaying) return@LaunchedEffect
        if (lazyGridHeight == 0) return@LaunchedEffect
        if (surahCardHeight == 0) return@LaunchedEffect
        if (playingSurahId == null) return@LaunchedEffect
        if (playingReciterId != reciter.id) return@LaunchedEffect

        val index = filteredSurahs.indexOfFirst { surah -> surah.id == playingSurahId }
        if (index < 0) return@LaunchedEffect

        val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == index }
        val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        val miniPlayerTopEdge = lazyGridHeight - miniPlayerHeight - miniPlayerBottomPadding
        val targetScrollOffset = (lazyGridHeight / 2) - (surahCardHeight / 2) - (miniPlayerHeight / 2) - miniPlayerBottomPadding

        if (!shouldScroll(isVisible = isVisible, itemInfo = itemInfo, miniPlayerTopEdge = miniPlayerTopEdge, targetScrollOffset = targetScrollOffset)) return@LaunchedEffect
        listState.animateScrollToItem(index = index, scrollOffset = -targetScrollOffset)
    }
}

private fun shouldScroll(
        isVisible: Boolean,
        itemInfo: LazyGridItemInfo?,
        miniPlayerTopEdge: Int,
        targetScrollOffset: Int
) = when {
    !isVisible                                                                         -> true
    itemInfo != null && itemInfo.offset.y + itemInfo.size.height > miniPlayerTopEdge   -> true
    itemInfo != null && itemInfo.offset.y + itemInfo.size.height != targetScrollOffset -> true

    else                                                                               -> false
}

@Composable
private fun ReciterName(isSkeleton: Boolean, brush: Brush?, reciter: Reciter) {
    when {
        isSkeleton -> {
            if (brush == null) return

            Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )
        }

        else       -> Text(
                text = reciter.name,
                fontSize = 50.sp,
                fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
                color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SearchBar(
        isSkeleton: Boolean,
        brush: Brush?,
        query: String,
        onQueryChange: (String) -> Unit = {}
) {
    if (isSkeleton) {
        if (brush == null) return
        Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush)
        )
    } else TextField(
            value = query,
            onValueChange = onQueryChange,
            shape = RoundedCornerShape(20.dp),
            colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_surahs)) },
            label = { Text(stringResource(R.string.search_surahs)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                        painter = painterResource(id = R.drawable.search_24px),
                        contentDescription = "Search Icon"
                )
            }
    )
}

@Composable
private fun SurahsGridContainer(
        isSkeleton: Boolean,
        content: @Composable (brush: Brush?) -> Unit
) = when {
    isSkeleton -> ShimmerAnimation { brush -> content(brush) }
    else       -> content(null)
}

private fun <T> LazyGridScope.gridItems(isSkeleton: Boolean, items: List<T>, content: @Composable LazyGridItemScope.(Int, T?) -> Unit) = when {
    isSkeleton -> itemsIndexed(items = (1..114).toList(), key = { _, item -> item }) { index, _ -> content(index, null) }
    else       -> itemsIndexed(items = items, key = { _, item -> item.hashCode() }) { index, item -> content(index, item) }
}

private fun filterSurahs(surahs: List<Surah>, query: String): List<Surah> {
    if (query.isBlank()) return surahs

    val normalizedQuery = query.trim().lowercase()

    return surahs.filter { reciter ->
        reciter.name.lowercase().contains(normalizedQuery)
    }
}
