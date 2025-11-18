package com.hifnawy.alquran.view.grids

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.utils.ModifierExt.AnimationType
import com.hifnawy.alquran.utils.ModifierExt.animateItemPosition
import com.hifnawy.alquran.view.gridItems.SurahCard
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun SurahsGrid(
        modifier: Modifier = Modifier,
        reciter: Reciter,
        surahs: List<Surah>,
        onSurahCardClick: (surah: Surah) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var lastAnimatedIndex by remember { mutableIntStateOf(-1) }

    val listState = rememberLazyGridState()
    val filteredSurahs = remember(surahs, searchQuery) { filterSurahs(surahs, searchQuery) }

    Column(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp)
    ) {
        Text(
                text = reciter.name,
                fontSize = 50.sp,
                fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2))
        )

        Spacer(modifier = Modifier.size(5.dp))

        TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 25.dp, shape = RoundedCornerShape(4.dp)),
                value = searchQuery,
                onValueChange = { searchQuery = it },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_surahs)) },
                label = { Text(stringResource(R.string.search_surahs)) },
                trailingIcon = {
                    Icon(
                            painter = painterResource(id = R.drawable.search_24px),
                            contentDescription = "Search Icon"
                    )
                }
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
                state = listState,
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                columns = GridCells.Adaptive(minSize = 150.dp)
        ) {
            itemsIndexed(filteredSurahs, key = { _, surah -> surah.id }) { index, surah ->
                val isScrollingDown = index > lastAnimatedIndex

                SurahCard(
                        modifier = Modifier.animateItemPosition(
                                duration = 300,
                                animationType = when {
                                    isScrollingDown -> AnimationType.FallDown
                                    else            -> AnimationType.RiseUp
                                }
                        ),
                        surah = surah,
                        searchQuery = searchQuery,
                        onClick = onSurahCardClick
                )

                lastAnimatedIndex = index
            }
        }
    }
}

private fun filterSurahs(surahs: List<Surah>, query: String): List<Surah> {
    if (query.isBlank()) return surahs

    val normalizedQuery = query.trim().lowercase()

    return surahs.filter { reciter ->
        reciter.name.lowercase().contains(normalizedQuery)
    }
}
