package com.hifnawy.alquran.view.screens

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hifnawy.alquran.shared.domain.MediaManager
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.Result
import com.hifnawy.alquran.view.DataErrorScreen
import com.hifnawy.alquran.view.PullToRefreshIndicator
import com.hifnawy.alquran.view.grids.SurahsGrid
import com.hifnawy.alquran.view.grids.skeleton.SkeletonSurahsGrid
import com.hifnawy.alquran.view.player.PlayerContainer
import com.hifnawy.alquran.viewModel.MediaViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SurahsScreen(
        reciter: Reciter,
        moshaf: Moshaf,
        mediaViewModel: MediaViewModel
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val pullToRefreshState = rememberPullToRefreshState()
        var isLoading by remember { mutableStateOf(true) }
        var dataError: DataError? by remember { mutableStateOf(null) }
        var surahs by remember { mutableStateOf(listOf<Surah>()) }
        var reciterSurahs by remember { mutableStateOf(listOf<Surah>()) }

        LaunchedEffect(isLoading, surahs) {
            if (isLoading) {
                if (dataError != null) delay(3.seconds) // for testing
                MediaManager.whenSurahsReady { result ->
                    when (result) {
                        is Result.Success -> {
                            surahs = result.data
                            dataError = null
                        }

                        is Result.Error   -> {
                            dataError = result.error
                            surahs = emptyList()
                        }
                    }

                    val moshafSurahs = moshaf.surah_list.split(",").map { surahIdStr -> surahIdStr.toInt() }
                    reciterSurahs = surahs.filter { surah -> surah.id in moshafSurahs }

                    mediaViewModel.playerState = mediaViewModel.playerState.copy(reciter = reciter, moshaf = moshaf, surahsServer = moshaf.server)

                    isLoading = false
                }
            }
        }

        PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                state = pullToRefreshState,
                indicator = { PullToRefreshIndicator(isLoading, pullToRefreshState) },
                contentAlignment = Alignment.Center,
                isRefreshing = isLoading,
                onRefresh = { isLoading = true }
        ) {
            Content(
                    isLoading = isLoading,
                    dataError = dataError,
                    reciter = reciter,
                    reciterSurahs = reciterSurahs,
                    mediaViewModel = mediaViewModel
            )
        }
    }
}

@Composable
private fun BoxScope.Content(
        isLoading: Boolean,
        dataError: DataError?,
        reciter: Reciter,
        reciterSurahs: List<Surah>,
        mediaViewModel: MediaViewModel
) {
    if (isLoading) {
        SkeletonSurahsGrid()
        return
    }

    if (dataError != null) {
        val error = dataError
        DataErrorScreen(dataError = error)
        return
    }

    SurahsGrid(reciter = reciter, surahs = reciterSurahs) { surah ->
        mediaViewModel.playMedia(surah)
    }

    PlayerContainer(mediaViewModel = mediaViewModel)
}
