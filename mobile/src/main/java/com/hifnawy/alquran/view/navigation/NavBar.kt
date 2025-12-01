/*
 * An exceptionally complex and intricate composable that is a pivotal component in managing the
 * exceptionally complex and intricate UI state of the `NavBar`. It encapsulates the dynamic aspects
 * of a `NavigationItem` that must change in response to user interaction and the current navigation state.
 * The properties held by this class—such as the icon, label, and selection status—are not static but are
 * computed based on a sophisticated set of rules and conditions.
 *
 * The intricacy stems primarily from the dual-purpose nature of the "Reciters" navigation tab.
 * This single tab can represent two distinct destinations: the main `Reciters` list screen and
 * a specific `Surahs` list screen associated with a previously selected reciter. Consequently,
 * the `NavBar` must maintain awareness of the navigation history to correctly determine the
 * appearance and behavior of this tab.
 *
 * **WARNING: Extreme Complexity Ahead**
 * This file is not for the faint of heart. It orchestrates a delicate and convoluted ballet of
 * navigation logic. The transitions are not simple taps to navigate between screens; they are
 * meticulously crafted, state-dependent, and direction-aware. Modifying any part of this
 * component, from the transition definitions to the NavigationBar setup, requires a profound
 * understanding of the entire navigation flow and its potential side effects. Proceed with
 * extreme caution, and think twice before making any changes. You will most likely regret
 * hasty modifications.
 *
 * It is a highly specialized and state-aware composable responsible for:
 * - Rendering the bottom navigation bar with items for Reciters, Favorites, and Settings.
 * - Dynamically adjusting its height and visibility through an animated transition, controlled by `navBarHeightProgress`.
 * - Managing complex navigation logic, particularly for the "Reciters" tab, which can represent two different screens:
 *   the `Reciters` screen and the `Surahs` screen.
 * - Remembering the last visited `Surahs` screen (with its specific `reciter` and `moshaf` arguments) and allowing the user
 *   to navigate back to it directly from the navigation bar.
 * - Updating the appearance (icon and label) of the "Reciters" navigation item to reflect whether it will navigate to the
 *   `Reciters` list or the `Surahs` list.
 * - Handling the selection state of each navigation item based on the current route in the `NavHostController`'s back stack.
 * - Ensuring efficient navigation by using `launchSingleTop = true` to avoid creating multiple instances of the same screen on the back stack.
 *
 * The implementation relies on careful state management using `rememberSaveable` to persist navigation context across configuration changes
 * and process death. It closely observes the `navController`'s back stack to make decisions about routing and UI presentation.
 *
 * ### Core Components:
 * - **`NavBar`**: The main composable function that builds and displays the navigation bar.
 * - **`NavigationItem`**: A private data class to model the basic properties of a navigation item.
 * - **`NavigationItemProperties`**: A private data class to hold the computed, state-dependent properties (icon, label, selection) of a navigation item.
 *
 * ### Responsibilities and Complexities:
 *
 * 1.  **Dynamic Icon and Label Logic**: The `icon` and `label` properties are the result of
 *     complex conditional logic. For the "Reciters" tab, this class determines whether to
 *     display the "person" icon and "Reciters" label, or the "book" icon and "Surahs" label.
 *     This decision is based on whether the user has previously navigated to a `Surahs` screen
 *     and what the current destination on the navigation back stack is. This prevents UI
 *     inconsistencies and provides clear visual feedback about the item's navigational target.
 *
 * 2.  **State-Aware Selection Identification**: The `isSelected` flag is not a simple comparison
 *     of the current route. Its calculation is highly nuanced, especially for the "Reciters" tab.
 *     It must correctly identify the tab as "selected" when the user is on the `Reciters` screen
 *     *or* when they are on the `Surahs` screen that was launched from the `Reciters` flow. This
 *
 * THINK TWICE AND CAREFULLY BEFORE MODIFYING THIS FILE OR ADDING ANYTHING TO IT
 * YOU'LL MOST PROBABLY REGRET IT 🤣🤣🤣
 */

package com.hifnawy.alquran.view.navigation

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hifnawy.alquran.R
import com.hifnawy.alquran.view.screens.Screen
import com.hifnawy.alquran.shared.R as Rs

/**
 * A data class representing a single static item within the navigation bar.
 * This class serves as a model for the fundamental, non-stateful properties of a navigation destination.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @property title [String] The default text label to be displayed for the navigation item.
 * @property icon [Painter] The default icon resource to be displayed for the navigation item.
 * @property route [String] The unique navigation route string associated with this item, used by the `NavHostController`.
 */
private data class NavigationItem(val title: String, val icon: Painter, val route: String)

/**
 * A data class representing the computed properties of a navigation item.
 * This class holds the dynamic aspects of a `NavigationItem` that must change in response to user interaction and the current navigation state.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @property icon [Painter] The icon resource to be displayed for the navigation item.
 * @property label [String] The text label to be displayed for the navigation item.
 * @property isSelected [Boolean] Whether the navigation item is currently selected.
 */
private data class NavigationItemProperties(val icon: Painter, val label: String, val isSelected: Boolean)

/**
 * A highly specialized and state-aware composable responsible for rendering the bottom navigation bar.
 *
 * This component manages the UI and navigation logic for the app's main destinations: Reciters, Favorites, and Settings.
 * Its complexity stems from the dual-purpose nature of the "Reciters" tab, which can navigate to either the main
 * `Reciters` list or a specific `Surahs` list for a previously selected reciter. The `NavBar` intelligently
 * remembers the last `Surahs` screen visited and updates its appearance (icon and label) and behavior accordingly.
 *
 * Key Responsibilities:
 * - **Animated Visibility**: The navigation bar's height is animated based on the `navBarHeightProgress`,
 *   allowing it to slide in and out of view smoothly.
 * - **Stateful Navigation**: It observes the `navController`'s back stack to determine the current screen.
 *   This state is used to decide the target of the "Reciters" tab.
 * - **Dynamic Item Appearance**: The icon and label for the "Reciters" item change dynamically. It shows a "person" icon
 *   and "Reciters" label by default, but switches to a "book" icon and "Surahs" label if the user has previously
 *   navigated to a `Surahs` screen from the reciters flow.
 * - **Context-Aware Selection**: It correctly highlights the "Reciters" tab when the user is on either the `Reciters`
 *   screen or the associated `Surahs` screen.
 * - **Persistent State**: It uses `rememberSaveable` to persist the context of the last visited `Surahs` screen across
 *   configuration changes and process death, ensuring a consistent user experience.
 *
 * @param navController [NavHostController] The [NavHostController] used to manage navigation state and actions. It is crucial for
 *   determining the current route and dispatching navigation events.
 */
@Composable
fun NavBar(navController: NavHostController, navBarHeightProgress: Float) {
    val navBarHeight = lerp(100.dp, 0.dp, navBarHeightProgress)
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentDestinationArgs = navBackStackEntry?.arguments
    val currentDestinationRoute = navBackStackEntry?.destination?.route
    val currentRouteWithoutArgs = currentDestinationRoute?.replace(Regex("\\?.*"), "")

    var surahsRoute by rememberSaveable { mutableStateOf("") }
    var didNavigateToSurahs by rememberSaveable { mutableStateOf(currentDestinationRoute?.startsWith(Screen.Surahs.route) == true) }

    when {
        currentDestinationRoute?.startsWith(Screen.Surahs.route) == true -> {
            val reciterJson = currentDestinationArgs?.getString("reciterJson")
            val moshafJson = currentDestinationArgs?.getString("moshafJson")
            surahsRoute = "${Screen.Surahs.route}?reciter=$reciterJson&moshaf=$moshafJson"
            didNavigateToSurahs = true
        }
        currentDestinationRoute == Screen.Reciters.route                 -> didNavigateToSurahs = false
    }

    val navigationItems = listOf(
            NavigationItem(
                    title = stringResource(R.string.navbar_reciters),
                    icon = painterResource(R.drawable.person_24px),
                    route = Screen.Reciters.route
            ),
            // NavigationItem(
            //         title = stringResource(R.string.navbar_favorites),
            //         icon = painterResource(R.drawable.favorite_24px),
            //         route = Screen.Favorites.route
            // ),
            NavigationItem(
                    title = stringResource(R.string.navbar_settings),
                    icon = painterResource(R.drawable.settings_24px),
                    route = Screen.Settings.route
            )
    )

    NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(navBarHeight)
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)),
            tonalElevation = 25.dp
    ) {
        navigationItems.forEach { item ->
            val isRecitersItem = item.route == Screen.Reciters.route

            val itemProperties = getNavigationBarItemProperties(
                    item = item,
                    isRecitersItem = isRecitersItem,
                    didNavigateToSurahs = didNavigateToSurahs,
                    cleanedCurrentRoute = currentRouteWithoutArgs
            )

            val onNavBarItemClicked = callback@{
                when {
                    isRecitersItem                        -> navController.handleRecitersNavigation(
                            cleanedCurrentRoute = currentRouteWithoutArgs,
                            didNavigatedToSurahs = didNavigateToSurahs,
                            surahsRoute = surahsRoute
                    )
                    currentRouteWithoutArgs == item.route -> return@callback
                    else                                  -> navController.navigate(item.route) {
                        popUpTo(item.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }

            NavigationBarItem(
                    alwaysShowLabel = false,
                    onClick = onNavBarItemClicked,
                    selected = itemProperties.isSelected,
                    icon = { Icon(painter = itemProperties.icon, contentDescription = itemProperties.label) },
                    label = {
                        Text(
                                modifier = Modifier.basicMarquee(),
                                text = itemProperties.label,
                                maxLines = 1,
                                fontSize = 20.sp,
                                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    }
            )
        }
    }
}

/**
 * A composable function that computes the dynamic properties of a [NavigationBarItem].
 *
 * This function encapsulates the complex logic required to determine the correct icon, label,
 * and selection state for a given navigation item. Its primary complexity lies in handling the
 * dual-purpose `Reciters` tab, which can either navigate to the `Reciters` list or back to a
 * previously visited `Surahs` list.
 *
 * @param item [NavigationItem] The static [NavigationItem] data for which properties are being computed.
 * @param isRecitersItem [Boolean] A [Boolean] flag indicating if the current item is the "Reciters" tab.
 * @param didNavigateToSurahs [Boolean] A [Boolean] flag indicating if the user has previously navigated from the
 *                                      `Reciters` tab to a `Surahs` screen. This state determines if the `Reciters`
 *                                      tab should transform into a `Surahs` tab.
 * @param cleanedCurrentRoute [String] The current navigation route, stripped of any arguments, used to determine the selection state.
 *
 * @return [NavigationItemProperties] A [NavigationItemProperties] object containing the computed icon, label, and selection state for the [NavigationBarItem].
 */
@Composable
private fun getNavigationBarItemProperties(
        item: NavigationItem,
        isRecitersItem: Boolean,
        didNavigateToSurahs: Boolean,
        cleanedCurrentRoute: String?
): NavigationItemProperties {
    val shouldShowSurahsLabel = isRecitersItem && didNavigateToSurahs

    val navBarItemIcon = when {
        shouldShowSurahsLabel -> painterResource(R.drawable.book_24px)
        else                  -> item.icon
    }

    val navBarItemLabel = when {
        shouldShowSurahsLabel -> stringResource(R.string.navbar_surahs)
        else                  -> item.title
    }

    val isSelected = when {
        shouldShowSurahsLabel -> cleanedCurrentRoute == Screen.Surahs.route
        else                  -> cleanedCurrentRoute == item.route
    }

    return NavigationItemProperties(icon = navBarItemIcon, label = navBarItemLabel, isSelected = isSelected)
}

/**
 * Manages the intricate navigation logic for the `Reciters` navigation bar item.
 *
 * This function determines the correct destination when the `Reciters` tab is clicked,
 * which can be either the main `Reciters` list screen or the last visited `Surahs` list screen.
 * The behavior is state-dependent, based on the current screen and whether the user has
 * previously navigated to a `Surahs` screen.
 *
 * The logic is as follows:
 * 1.  **If the user is on a different tab and has previously visited a `Surahs` screen ([didNavigatedToSurahs] is `true`):**
 *     It navigates back to that specific `Surahs` screen using the saved `surahsRoute`,
 *     ensuring the reciter and moshaf context is restored.
 *
 * 2.  **If the user is currently on the `Surahs` screen:**
 *     It navigates back to the main `Reciters` list screen, allowing them to choose a different reciter.
 *
 * 3.  **If the user is on a different tab and has *not* previously visited a `Surahs` screen (or has since returned to the reciters list):**
 *     It navigates to the main `Reciters` list screen.
 *
 * This function ensures a seamless and intuitive user experience by remembering the user's context
 * within the Reciters/Surahs flow, while using `launchSingleTop = true` to prevent creating duplicate
 * screens on the navigation back stack.
 *
 * @param cleanedCurrentRoute [String?][String] The current navigation route without any arguments.
 * @param didNavigatedToSurahs [Boolean] A boolean flag indicating if the user has navigated to a `Surahs` screen
 *                            from the `Reciters` screen in the current session.
 * @param surahsRoute [String] The complete, remembered route for the last visited `Surahs` screen, including its
 */
private fun NavHostController.handleRecitersNavigation(cleanedCurrentRoute: String?, didNavigatedToSurahs: Boolean, surahsRoute: String) {
    when {
        didNavigatedToSurahs && cleanedCurrentRoute != Screen.Surahs.route -> navigate(surahsRoute) {
            popUpTo(surahsRoute) { inclusive = false }
            launchSingleTop = true
        }
        cleanedCurrentRoute == Screen.Surahs.route                         -> navigate(Screen.Reciters.route) {
            popUpTo(Screen.Reciters.route) { inclusive = false }
            launchSingleTop = true
        }
        cleanedCurrentRoute != Screen.Reciters.route                       -> navigate(Screen.Reciters.route) {
            popUpTo(Screen.Reciters.route) { inclusive = false }
            launchSingleTop = true
        }
    }
}
