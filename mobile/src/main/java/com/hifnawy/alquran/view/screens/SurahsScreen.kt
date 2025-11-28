package com.hifnawy.alquran.view.screens

import android.widget.Toast
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
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

/**
 * A Composable screen that displays a grid of Quran surahs for a given reciter and moshaf.
 *
 * This screen is responsible for fetching the list of available surahs for the selected reciter.
 * It handles loading states, displaying a skeleton UI while fetching data, and showing an error
 * screen if the data fails to load. It also integrates pull-to-refresh functionality to allow
 * users to manually reload the surah list.
 *
 * When a user selects a surah from the grid, it triggers playback via the [mediaViewModel].
 *
 * @param reciter [Reciter] The [Reciter] for whom the surahs are being displayed.
 * @param moshaf [Moshaf] The specific [Moshaf] (Quran narration type) to be used.
 * @param mediaViewModel [MediaViewModel] The [MediaViewModel] used to manage media playback state and actions.
 *
 * @see [Reciter]
 * @see [Moshaf]
 * @see [MediaViewModel]
 * @see [SurahsGrid]
 */
@Composable
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
                    mediaViewModel.updateState { surahs = reciterSurahs }
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
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding(),
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

/**
 * A private composable function that determines which content to display based on the loading and error states.
 *
 * This function acts as a router for the main content area of the `SurahsScreen`.
 * - If the data has finished loading, there is an error, and the surah list is empty, it displays a `DataErrorScreen`.
 * - Otherwise, it displays the `SurahsGrid`, either in a loading (skeleton) state or with the actual surah data.
 *
 * @param isLoading [Boolean] A boolean indicating if the data is currently being fetched.
 * @param dataError [DataError?][DataError] An optional [DataError] object representing an error that occurred during data fetching.
 * @param reciter [Reciter] The current [Reciter].
 * @param moshaf [Moshaf] The current [Moshaf].
 * @param moshafServer [String] The server URL for the current moshaf.
 * @param reciterSurahs [List< Surah >] [List] The [List] of [Surah]s available for the reciter and moshaf.
 * @param mediaViewModel [MediaViewModel] The [MediaViewModel] to handle media playback actions.
 */
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
