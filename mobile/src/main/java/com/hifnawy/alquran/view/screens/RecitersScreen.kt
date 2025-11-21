package com.hifnawy.alquran.view.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.domain.MediaManager
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.Result
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.asJsonString
import com.hifnawy.alquran.view.DataErrorScreen
import com.hifnawy.alquran.view.PullToRefreshIndicator
import com.hifnawy.alquran.view.grids.RecitersGrid
import com.hifnawy.alquran.view.player.PlayerContainer
import com.hifnawy.alquran.viewModel.MediaViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun RecitersScreen(mediaViewModel: MediaViewModel, navController: NavController) = Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    val pullToRefreshState = rememberPullToRefreshState()
    var isLoading by remember { mutableStateOf(true) }
    var dataError: DataError? by remember { mutableStateOf(null) }
    var reciters by remember { mutableStateOf(listOf<Reciter>()) }

    LaunchedEffect(isLoading, reciters) {
        if (isLoading) {
            if (dataError != null) delay(3.seconds) // for testing
            MediaManager.whenRecitersReady { result ->
                when (result) {
                    is Result.Success -> {
                        reciters = result.data
                        dataError = null
                    }

                    is Result.Error   -> {
                        dataError = result.error
                        reciters = emptyList()
                    }
                }

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
                navController = navController,
                mediaViewModel = mediaViewModel,
                dataError = dataError,
                reciters = reciters
        )
    }
}

@Composable
private fun BoxScope.Content(
        isLoading: Boolean,
        navController: NavController,
        mediaViewModel: MediaViewModel,
        dataError: DataError?,
        reciters: List<Reciter>
) = when {
    !isLoading && dataError != null -> DataErrorScreen(dataError = dataError)

    else                            -> {
        RecitersGrid(
                reciters = reciters,
                isSkeleton = isLoading
        ) { reciter, moshaf ->
            val reciterJson = reciter.asJsonString
            val moshafJson = moshaf.asJsonString

            navController.navigate(Screen.Surahs.route + "?reciter=$reciterJson&moshaf=$moshafJson")
        }

        PlayerContainer(mediaViewModel = mediaViewModel)
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
