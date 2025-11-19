package com.hifnawy.alquran.view.grids.skeleton

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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hifnawy.alquran.utils.ModifierExt.AnimationType
import com.hifnawy.alquran.utils.ModifierExt.animateItemPosition
import com.hifnawy.alquran.view.ShimmerAnimation
import com.hifnawy.alquran.view.gridItems.skeleton.SkeletonSurahCard

@Composable
fun SkeletonSurahsGrid() {
    var lastAnimatedIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyGridState()

    ShimmerAnimation { brush ->
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
        ) {
            // placeholder for reciter name
            Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )

            Spacer(modifier = Modifier.size(5.dp))

            // placeholder for search bar
            Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )

            Spacer(Modifier.height(10.dp))

            LazyVerticalGrid(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    columns = GridCells.Adaptive(minSize = 150.dp)
            ) {
                val items = (0..114).toList()
                itemsIndexed(items, key = { _, item -> item }) { index, _ ->
                    val isScrollingDown = index > lastAnimatedIndex

                    SkeletonSurahCard(
                            modifier = Modifier.animateItemPosition(
                                    durationMs = 300,
                                    animationType = when {
                                        isScrollingDown -> AnimationType.FallDown
                                        else            -> AnimationType.RiseUp
                                    }
                            ),
                            brush = brush
                    )

                    lastAnimatedIndex = index
                }
            }
        }
    }
}
