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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.ReciterId
import com.hifnawy.alquran.shared.model.asReciterId
import com.hifnawy.alquran.utils.LazyGridScopeEx.gridItems
import com.hifnawy.alquran.utils.ModifierEx.AnimationType
import com.hifnawy.alquran.utils.ModifierEx.animateItemPosition
import com.hifnawy.alquran.view.SearchBar
import com.hifnawy.alquran.view.ShimmerAnimation
import com.hifnawy.alquran.view.gridItems.ReciterCard
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun RecitersGrid(
        modifier: Modifier = Modifier,
        reciters: List<Reciter>,
        isSkeleton: Boolean = false,
        isPlaying: Boolean = false,
        playingReciterId: ReciterId? = null,
        playingMoshafId: Int? = null,
        onMoshafClick: (Reciter, Moshaf) -> Unit = { _, _ -> }
) {
    RecitersGridContainer(isSkeleton = isSkeleton) { brush ->
        Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(10.dp)
        ) {
            var searchQuery by rememberSaveable { mutableStateOf("") }
            var lastAnimatedIndex by rememberSaveable { mutableIntStateOf(-1) }
            var expandedReciterCardId by rememberSaveable(stateSaver = Saver(save = { it.value }, restore = { ReciterId(it) })) { mutableStateOf((-1).asReciterId) }

            val listState = rememberRecitersGridState()
            val filteredReciters = rememberSaveable(reciters, searchQuery) {
                reciters.filter { reciter ->
                    reciter.name.contains(searchQuery)
                }
            }

            TitleBar(isSkeleton = isSkeleton, brush = brush)

            Spacer(modifier = Modifier.size(5.dp))

            SearchBar(
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
                            onMoshafClick = onMoshafClick
                    )

                    lastAnimatedIndex = index
                }
            }
        }
    }
}

@Composable
private fun rememberRecitersGridState(
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0
) = rememberSaveable(saver = LazyGridState.Saver) {
    LazyGridState(firstVisibleItemIndex = firstVisibleItemIndex, firstVisibleItemScrollOffset = firstVisibleItemScrollOffset)
}

@Composable
private fun RecitersGridContainer(
        isSkeleton: Boolean,
        content: @Composable (brush: Brush?) -> Unit
) = when {
    isSkeleton -> ShimmerAnimation { brush -> content(brush) }
    else       -> content(null)
}

@Composable
private fun TitleBar(
        isSkeleton: Boolean,
        brush: Brush?
) {
    if (isSkeleton) {
        if (brush == null) return
        Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush)
        )
    } else Text(
            text = stringResource(Rs.string.quran),
            fontSize = 50.sp,
            fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2)),
            color = MaterialTheme.colorScheme.onSurface
    )
}

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
