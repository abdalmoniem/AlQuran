package com.hifnawy.alquran.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.shared.domain.MediaManager
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.view.composables.PlayerContainer
import com.hifnawy.alquran.view.composables.SurahsGrid
import com.hifnawy.alquran.viewModel.MediaViewModel
import timber.log.Timber
import com.hifnawy.alquran.shared.R as Rs

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SurahsScreen(
        reciter: Reciter,
        moshaf: Moshaf,
        mediaViewModel: MediaViewModel
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val context = LocalContext.current
        val mediaManager = remember { MediaManager(context) }
        var surahs by remember { mutableStateOf(listOf<Surah>()) }
        var reciterSurahs by remember { mutableStateOf(listOf<Surah>()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            mediaManager.whenSurahsReady(context) {
                surahs = it

                val moshafSurahs = moshaf.surah_list.split(",").map { surahIdStr -> surahIdStr.toInt() }
                reciterSurahs = surahs.filter { surah -> surah.id in moshafSurahs }
                isLoading = false

                mediaViewModel.playerState = mediaViewModel.playerState.copy(
                        reciter = reciter,
                        surahsServer = moshaf.server
                )
            }
        }

        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularWavyProgressIndicator(
                        modifier = Modifier.size(100.dp),
                        stroke = Stroke(width = 10f)
                )

                else      -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                                text = reciter.name,
                                fontSize = 50.sp,
                                fontFamily = FontFamily(Font(Rs.font.decotype_thuluth_2))
                        )

                        Spacer(modifier = Modifier.size(5.dp))

                        SurahsGrid(surahs = reciterSurahs, mediaViewModel = mediaViewModel)
                    }

                    PlayerContainer(mediaViewModel = mediaViewModel)
                }
            }
        }
    }
}
