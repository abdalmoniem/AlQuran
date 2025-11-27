package com.hifnawy.alquran.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.view.player.PlayerContainer
import com.hifnawy.alquran.view.screens.RecitersScreen
import com.hifnawy.alquran.view.screens.Screen
import com.hifnawy.alquran.view.screens.SurahsScreen
import com.hifnawy.alquran.viewModel.MediaViewModel

@Composable
fun NavigationStack() {
    val navController = rememberNavController()
    val mediaViewModel: MediaViewModel = viewModel()

    Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
    ) {
        NavHost(navController = navController, startDestination = Screen.Reciters.route) {
            composable(route = Screen.Reciters.route) {
                RecitersScreen(mediaViewModel = mediaViewModel, navController = navController)
            }

            composable(
                    route = Screen.Surahs.route + "?reciter={reciterJson}&moshaf={moshafJson}",
                    arguments = listOf(
                            navArgument("reciterJson") {
                                type = NavType.StringType
                                nullable = false
                            },
                            navArgument("moshafJson") {
                                type = NavType.StringType
                                nullable = false
                            }
                    )
            ) { backStackEntry ->
                val reciter = Gson().fromJson(backStackEntry.arguments?.getString("reciterJson"), Reciter::class.java)
                val moshaf = Gson().fromJson(backStackEntry.arguments?.getString("moshafJson"), Moshaf::class.java)

                SurahsScreen(reciter = reciter, moshaf = moshaf, mediaViewModel = mediaViewModel)
            }
        }

        PlayerContainer(mediaViewModel = mediaViewModel)
    }
}
