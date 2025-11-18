package com.hifnawy.alquran.view.grids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.utils.ModifierExt.AnimationType
import com.hifnawy.alquran.utils.ModifierExt.animateItemPosition
import com.hifnawy.alquran.view.gridItems.ReciterCard

@Composable
fun RecitersGrid(
        modifier: Modifier = Modifier,
        reciters: List<Reciter>,
        onMoshafClick: (Reciter, Moshaf) -> Unit = { _, _ -> }
) {
    Column(
            modifier = modifier
                .fillMaxSize()
                .padding(10.dp)
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var expandedReciterId by remember { mutableIntStateOf(-1) }
        var lastAnimatedIndex by remember { mutableIntStateOf(-1) }

        val listState = rememberLazyGridState()
        val filteredReciters = remember(reciters, searchQuery) { filterReciters(reciters, searchQuery) }

        TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_reciters)) },
                label = { Text(stringResource(R.string.search_reciters)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                            painter = painterResource(id = R.drawable.search_24px),
                            contentDescription = "Search Icon"
                    )
                }
        )

        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
                state = listState,
                columns = GridCells.Adaptive(minSize = 250.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(filteredReciters, key = { _, reciter -> reciter.id }) { index, reciter ->
                val isScrollingDown = index > lastAnimatedIndex

                ReciterCard(
                        modifier = Modifier.animateItemPosition(
                                duration = 300,
                                animationType = when {
                                    isScrollingDown -> AnimationType.FallDown
                                    else            -> AnimationType.RiseUp
                                }
                        ),
                        reciter = reciter,
                        searchQuery = searchQuery,
                        isExpanded = expandedReciterId == reciter.id,
                        onToggleExpand = {
                            expandedReciterId = when (expandedReciterId) {
                                reciter.id -> -1
                                else       -> reciter.id
                            }
                        },
                        onMoshafClick = onMoshafClick
                )

                lastAnimatedIndex = index
            }
        }
    }
}

private fun filterReciters(reciters: List<Reciter>, query: String): List<Reciter> {
    if (query.isBlank()) return reciters

    val normalizedQuery = query.trim().lowercase()

    return reciters.filter { reciter ->
        reciter.name.lowercase().contains(normalizedQuery)
    }
}
