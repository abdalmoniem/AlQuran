package com.hifnawy.alquran.view.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.google.gson.Gson
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.domain.MediaManager
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.Result
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.view.composables.PlayerContainer
import com.hifnawy.alquran.view.composables.PullToRefreshIndicator
import com.hifnawy.alquran.view.composables.RecitersList
import com.hifnawy.alquran.view.composables.SkeletonRecitersList
import com.hifnawy.alquran.viewModel.MediaViewModel
import timber.log.Timber

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun RecitersScreen(mediaViewModel: MediaViewModel, navController: NavController) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val context = LocalContext.current
        val mediaManager = remember { MediaManager(context) }
        val pullToRefreshState = rememberPullToRefreshState()
        var isLoading by remember { mutableStateOf(true) }
        var dataError: DataError? by remember { mutableStateOf(null) }
        var reciters by remember { mutableStateOf(listOf<Reciter>()) }

        LaunchedEffect(isLoading, reciters) {
            if (isLoading) {
                // delay(10.seconds) // for testing
                mediaManager.whenRecitersReady(context) { result ->
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
            when {
                isLoading         -> SkeletonRecitersList()
                dataError != null -> DataErrorScreen(dataError = dataError!!)
                else              -> RecitersScreen(navController = navController, mediaViewModel = mediaViewModel, reciters = reciters)
            }
        }
    }
}

@Composable
fun DataErrorScreen(dataError: DataError) {
    when (dataError) {
        is DataError.LocalError   -> LocalErrorScreen()
        is DataError.NetworkError -> NetworkErrorScreen()
        is DataError.ParseError   -> ParseErrorScreen()
    }
}

@Composable
fun LocalErrorScreen() {
}

@Composable
fun NetworkErrorScreen() {
    val scrollState = rememberScrollState()
    Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                painter = painterResource(id = R.drawable.cloud_off_24px),
                contentDescription = "Network Error"
        )

        Text(
                text = "حدث خطأ أثناء تحميل القراء، برجاء المحاولة مرة أخرى.",
                style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
fun ParseErrorScreen() {
}

@Composable
fun BoxScope.RecitersScreen(navController: NavController, mediaViewModel: MediaViewModel, reciters: List<Reciter>) {

    RecitersList(reciters = reciters) { reciter, moshaf ->
        val reciterJson = Gson().toJson(reciter)
        val moshafJson = Gson().toJson(moshaf)

        navController.navigate(Screen.Surahs.route + "?reciter=$reciterJson&moshaf=$moshafJson")
    }

    PlayerContainer(mediaViewModel = mediaViewModel)
}
