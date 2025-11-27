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
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            var searchQuery by remember { mutableStateOf("") }
            var lastAnimatedIndex by remember { mutableIntStateOf(-1) }

            val listState = rememberLazyGridState()
            val filteredSurahs = remember(reciterSurahs, searchQuery) { filterSurahs(reciterSurahs, searchQuery) }

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
                    state = listState,
                    modifier = modifier,
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                gridItems(isSkeleton = isSkeleton, items = filteredSurahs) { index, surah ->
                    val isScrollingDown = index > lastAnimatedIndex

                    SurahCard(
                            modifier = Modifier.animateItemPosition(
                                    durationMs = 300,
                                    animationType = when {
                                        isScrollingDown -> AnimationType.FallDown
                                        else            -> AnimationType.RiseUp
                                    }
                            ),
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
        }
    }
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
