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

private data class NavigationItem(
        val title: String,
        val icon: Painter,
        val route: String
)

@Composable
fun NavigationBar(navController: NavHostController, navBarHeightProgress: Float) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navBarHeight = lerp(100.dp, 0.dp, navBarHeightProgress)

    val navigationItems = listOf(
            NavigationItem(
                    title = stringResource(R.string.navbar_reciters),
                    icon = painterResource(R.drawable.person_24px),
                    route = Screen.Reciters.route
            ),
            NavigationItem(
                    title = stringResource(R.string.navbar_favorites),
                    icon = painterResource(R.drawable.favorite_24px),
                    route = Screen.Favorites.route
            ),
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
            val currentRoute = currentDestination?.route?.replace(Regex("\\?.*"), "")
            val isSurahsScreen = currentRoute == Screen.Surahs.route && item.route == Screen.Reciters.route
            val onNavBarItemClicked = callback@{
                if (currentRoute == item.route) return@callback
                navController.navigate(item.route) {
                    popUpTo(item.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
            val navBarItemIcon = when {
                isSurahsScreen -> painterResource(R.drawable.book_24px)
                else           -> item.icon
            }
            val navBarItemLabel = when {
                isSurahsScreen -> stringResource(R.string.navbar_surahs)
                else           -> item.title
            }
            NavigationBarItem(
                    onClick = onNavBarItemClicked,
                    selected = currentRoute == item.route || isSurahsScreen,
                    icon = { Icon(painter = navBarItemIcon, contentDescription = navBarItemLabel) },
                    label = {
                        Text(
                                modifier = Modifier.basicMarquee(),
                                text = navBarItemLabel,
                                maxLines = 1,
                                fontSize = 20.sp,
                                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                                color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    alwaysShowLabel = false
            )
        }
    }
}
