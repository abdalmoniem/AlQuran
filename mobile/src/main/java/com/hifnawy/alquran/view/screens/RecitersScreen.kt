package com.hifnawy.alquran.view.screens

import android.annotation.SuppressLint
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

/**
 * A Composable screen that displays a list of Quran reciters.
 *
 * This screen fetches a list of reciters from the [MediaManager] and displays them in a grid using [RecitersGrid].
 * It handles loading states, showing a skeleton UI while fetching, and displays an error screen
 * if the data fails to load. The screen also supports pull-to-refresh functionality to reload the reciters list.
 *
 * When a user selects a reciter and a specific moshaf (recitation style), it navigates to the [SurahsScreen],
 * passing the selected reciter and moshaf data.
 *
 * @param navController [NavController] The [NavController] used for navigating to other screens, specifically to the [SurahsScreen].
 * @param mediaViewModel [MediaViewModel] The [MediaViewModel] that holds the state of the media player, including the list
 * of reciters and the currently playing media information.
 *
 * @see [SurahsScreen]
 * @see [MediaViewModel]
 * @see [RecitersGrid]
 */
@Composable
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
                    mediaViewModel.updateState { this@updateState.reciters = result.data }
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
                navController = navController,
                mediaViewModel = mediaViewModel,
                dataError = dataError,
                reciters = reciters
        )
    }
}

/**
 * A private composable function that determines which UI to display based on the current loading and error state.
 *
 * It acts as a content switcher for the [RecitersScreen].
 * - If there is a data loading error and the list of reciters is empty, it displays the [DataErrorScreen].
 * - Otherwise, it displays the [RecitersGrid], showing either a skeleton loading UI or the actual list of reciters.
 *
 * @param isLoading [Boolean] A boolean flag indicating if the data is currently being loaded.
 * @param navController [NavController] The [NavController] used for navigation when a reciter is selected.
 * @param mediaViewModel [MediaViewModel] The [MediaViewModel] containing the state of the media player, used to determine which reciter is currently playing.
 * @param dataError [DataError] An optional [DataError] object that contains information about any data loading failures.
 * @param reciters [List< Reciter >][List] The [List] of [Reciter] objects to be displayed.
 */
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

            val route = Screen.Surahs.route + "?reciter=$reciterJson&moshaf=$moshafJson"
            navController.navigate(route) {
                popUpTo(route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
}

/**
 * A Composable function that provides a preview of the [RecitersScreen] within the Android Studio IDE.
 *
 * This function is annotated with [@Preview], allowing developers to visualize the [RecitersScreen]'s layout
 * and behavior without needing to run the app on an emulator or a physical device. It initializes mock
 * instances of [NavController] and [MediaViewModel] to satisfy the dependencies of the [RecitersScreen].
 *
 * The `@SuppressLint("ViewModelConstructorInComposable")` is used to suppress the lint warning against
 * instantiating a [MediaViewModel] directly within a Composable, which is an acceptable practice for preview purposes.
 */
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
