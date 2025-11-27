package com.hifnawy.alquran.view.screens

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.domain.MediaManager
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.Result
import com.hifnawy.alquran.view.DataErrorScreen
import com.hifnawy.alquran.view.PullToRefreshIndicator
import com.hifnawy.alquran.view.grids.SurahsGrid
import com.hifnawy.alquran.viewModel.MediaViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SurahsScreen(
        reciter: Reciter,
        moshaf: Moshaf,
        mediaViewModel: MediaViewModel
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    var dataError: DataError? by rememberSaveable { mutableStateOf(null) }
    var reciterSurahs by rememberSaveable { mutableStateOf(mediaViewModel.playerState.surahs) }
    var isLoading by remember { mutableStateOf(reciterSurahs.isEmpty() && dataError == null) }
    val surahsLoadingError = stringResource(R.string.surahs_loading_error)

    LaunchedEffect(isLoading) {
        if (!isLoading) cancel("No longer loading!")
        if (dataError != null) delay(3.seconds) // for testing

        MediaManager.whenSurahsReady { result ->
            when (result) {
                is Result.Success -> {
                    reciterSurahs = result.data.filter { surah -> surah.id in moshaf.surahIds }
                    dataError = null
                    mediaViewModel.playerState = mediaViewModel.playerState.copy(surahs = reciterSurahs)
                }

                is Result.Error   -> {
                    dataError = result.error

                    MainScope().launch { Toast.makeText(context, surahsLoadingError, Toast.LENGTH_LONG).show() }
                }
            }

            isLoading = false
        }
    }

    PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
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
                moshaf = moshaf,
                moshafServer = moshaf.server,
                reciterSurahs = reciterSurahs,
                mediaViewModel = mediaViewModel
        )
    }
}

@Composable
private fun Content(
        isLoading: Boolean,
        dataError: DataError?,
        reciter: Reciter,
        moshaf: Moshaf,
        moshafServer: String,
        reciterSurahs: List<Surah>,
        mediaViewModel: MediaViewModel
) {

    when {
        !isLoading && dataError != null && reciterSurahs.isEmpty() -> DataErrorScreen(dataError = dataError, errorMessage = stringResource(R.string.surahs_loading_error))

        else                                                       -> SurahsGrid(
                reciter = reciter,
                reciterSurahs = reciterSurahs,
                isSkeleton = isLoading,
                isPlaying = mediaViewModel.playerState.isPlaying,
                playingSurahId = mediaViewModel.playerState.surah?.id,
                playingReciterId = mediaViewModel.playerState.reciter?.id
        ) { surah ->
            mediaViewModel.playMedia(reciter, moshaf, moshafServer, surah)
        }
    }
}
