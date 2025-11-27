package com.hifnawy.alquran.view.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.domain.MediaManager
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.Result
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.asJsonString
import com.hifnawy.alquran.view.DataErrorScreen
import com.hifnawy.alquran.view.PullToRefreshIndicator
import com.hifnawy.alquran.view.grids.RecitersGrid
import com.hifnawy.alquran.viewModel.MediaViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun RecitersScreen(
        navController: NavController,
        mediaViewModel: MediaViewModel
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    var dataError: DataError? by rememberSaveable { mutableStateOf(null) }
    var reciters by rememberSaveable { mutableStateOf(mediaViewModel.playerState.reciters) }
    var isLoading by remember { mutableStateOf(reciters.isEmpty() && dataError == null) }
    val recitersLoadingError = stringResource(R.string.reciters_loading_error)

    LaunchedEffect(isLoading) {
        if (!isLoading) cancel("No longer loading!")
        if (dataError != null) delay(3.seconds) // for testing

        MediaManager.whenRecitersReady { result ->
            when (result) {
                is Result.Success -> {
                    reciters = result.data
                    dataError = null
                    mediaViewModel.playerState = mediaViewModel.playerState.copy(reciters = result.data)
                }

                is Result.Error   -> {
                    dataError = result.error

                    MainScope().launch { Toast.makeText(context, recitersLoadingError, Toast.LENGTH_LONG).show() }
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
                navController = navController,
                mediaViewModel = mediaViewModel,
                dataError = dataError,
                reciters = reciters
        )
    }
}

@Composable
private fun Content(
        isLoading: Boolean,
        navController: NavController,
        mediaViewModel: MediaViewModel,
        dataError: DataError?,
        reciters: List<Reciter>
) {
    when {
        !isLoading && dataError != null && reciters.isEmpty() -> DataErrorScreen(dataError = dataError, errorMessage = stringResource(R.string.reciters_loading_error))

        else                                                  -> RecitersGrid(
                reciters = reciters,
                isSkeleton = isLoading,
                isPlaying = mediaViewModel.playerState.isPlaying,
                playingReciterId = mediaViewModel.playerState.reciter?.id,
                playingMoshafId = mediaViewModel.playerState.moshaf?.id
        ) { reciter, moshaf ->
            val reciterJson = reciter.asJsonString
            val moshafJson = moshaf.asJsonString

            navController.navigate(Screen.Surahs.route + "?reciter=$reciterJson&moshaf=$moshafJson")
        }
    }
}

@Preview
@Composable
@SuppressLint("ViewModelConstructorInComposable")
fun RecitersScreenPreview() {
    val context = LocalContext.current
    val navController = NavController(context)
    val quranApplication = QuranApplication()
    val mediaViewModel = MediaViewModel(quranApplication)

    RecitersScreen(mediaViewModel = mediaViewModel, navController = navController)
}
