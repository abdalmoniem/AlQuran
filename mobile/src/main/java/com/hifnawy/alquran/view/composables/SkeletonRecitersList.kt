package com.hifnawy.alquran.view.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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

@Composable
fun SkeletonRecitersList() {
    val listState = rememberLazyListState()
    var lastAnimatedIndex by remember { mutableIntStateOf(-1) }

    ShimmerAnimation { brush ->
        Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
        ) {

            Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush)
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val items = (0..300).toList()
                itemsIndexed(items, key = { _, item -> item }) { index, _ ->
                    val isScrollingDown = index > lastAnimatedIndex

                    SkeletonReciterCard(
                            modifier = Modifier.animateItemPosition(
                                    duration = 300,
                                    animationType = when {
                                        isScrollingDown -> AnimationType.FallDown
                                        else            -> AnimationType.RiseUp
                                    }
                            ),
                            brush = brush
                    )
                }
            }
        }
    }
}
