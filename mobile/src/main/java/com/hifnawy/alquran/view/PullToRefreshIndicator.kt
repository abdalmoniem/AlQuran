package com.hifnawy.alquran.view

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun BoxScope.PullToRefreshIndicator(isLoading: Boolean, pullToRefreshState: PullToRefreshState) {
    val scaleFraction = {
        if (isLoading) 1f
        else LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
    }
    Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                }
    ) {
        PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isLoading,
                elevation = 30.dp
        )
    }
}
