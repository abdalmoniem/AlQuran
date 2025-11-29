package com.hifnawy.alquran.view.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.utils.DeviceConfiguration
import com.hifnawy.alquran.utils.DeviceConfiguration.Companion.deviceConfiguration
import com.hifnawy.alquran.utils.ModifierEx.onTouch
import com.hifnawy.alquran.view.focusManager
import com.hifnawy.alquran.view.player.PlayerContainer
import com.hifnawy.alquran.view.screens.FavoritesScreen
import com.hifnawy.alquran.view.screens.RecitersScreen
import com.hifnawy.alquran.view.screens.Screen
import com.hifnawy.alquran.view.screens.SettingsScreen
import com.hifnawy.alquran.view.screens.SurahsScreen
import com.hifnawy.alquran.viewModel.MediaViewModel

/**
 * A composable that sets up the main navigation structure of the application using Jetpack Navigation.
 *
 * This function defines the navigation graph, including the different screens (routes) and the
 * transitions between them. It also initializes and provides the [MediaViewModel] to the screens
t* hat need it.
 *
 * The main components managed by this [NavHost] are:
 * - [RecitersScreen]: The start destination, displaying a list of Quran reciters.
 * - [SurahsScreen]: Displays the list of Surahs for a selected reciter and moshaf. Navigation to
 *   this screen requires passing `reciter` and `moshaf` objects as JSON strings.
 *
 * It wraps the [NavHost] in a [Box] that handles clearing focus when tapping outside of an
 * input field and applies appropriate padding based on the device configuration.
 *
 * This composable also integrates the [PlayerContainer], which displays the media player controls.
 * The player's state and interactions are managed via the [MediaViewModel].
 *
 * @see NavHost
 * @see Screen
 * @see RecitersScreen
 * @see SurahsScreen
 * @see PlayerContainer
 * @see MediaViewModel
 */
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val mediaViewModel = viewModel<MediaViewModel>()
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val deviceConfiguration = windowSize.deviceConfiguration
    val columnModifier = when (deviceConfiguration) {
        DeviceConfiguration.COMPACT,
        DeviceConfiguration.PHONE_LANDSCAPE -> Modifier

        else                                -> Modifier.imePadding()
    }

    var navBarHeightProgress by remember { mutableFloatStateOf(0f) }

    focusManager = LocalFocusManager.current

    Column(
            modifier = columnModifier
                .fillMaxSize()
                .onTouch { focusManager?.clearFocus() }
    ) {
        Box(modifier = Modifier.weight(1f)) {
            val animationDuration = 300
            val fadeAnimationSpec = tween<Float>(animationDuration)
            val slideAnimationSpec = tween<IntOffset>(animationDuration)

            NavHost(
                    navController = navController,
                    startDestination = Screen.Reciters.route,
                    enterTransition = { fadeIn(fadeAnimationSpec) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, slideAnimationSpec) },
                    exitTransition = { fadeOut(fadeAnimationSpec) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, slideAnimationSpec) },
                    popEnterTransition = { fadeIn(fadeAnimationSpec) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, slideAnimationSpec) },
                    popExitTransition = { fadeOut(fadeAnimationSpec) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, slideAnimationSpec) }
            ) {
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

                composable(route = Screen.Favorites.route) {
                    FavoritesScreen()
                }

                composable(route = Screen.Settings.route) {
                    SettingsScreen()
                }
            }

            with(mediaViewModel) {
                PlayerContainer(
                        state = playerState,
                        onHeightChanged = { navBarHeightProgress = it },
                        onSnapped = {
                            updateState {
                                isExpanding = false
                                isMinimizing = false
                            }
                        },
                        onDragDirectionChanged = { isDraggingUp, isDraggingDown ->
                            updateState {
                                isExpanding = isDraggingUp
                                isMinimizing = isDraggingDown
                            }
                        },
                        onExpandStarted = {
                            updateState {
                                isExpanding = true
                                isMinimizing = false
                            }
                        },
                        onExpandFinished = {
                            updateState {
                                isExpanding = false
                                isMinimizing = false
                            }
                        },
                        onMinimizeStarted = {
                            updateState {
                                isExpanding = false
                                isMinimizing = true
                            }
                        },
                        onMinimizeFinished = {
                            updateState {
                                isExpanding = false
                                isMinimizing = false
                            }
                        },
                        onCloseClicked = ::closePlayer,
                        onSeekProgress = ::seekTo,
                        onSkipToPreviousSurah = ::skipToPreviousSurah,
                        onTogglePlayback = ::togglePlayback,
                        onSkipToNextSurah = ::skipToNextSurah
                )
            }
        }

        NavigationBar(navController = navController, navBarHeightProgress = navBarHeightProgress)
    }
}
