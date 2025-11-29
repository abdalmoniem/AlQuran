package com.hifnawy.alquran.view.screens

enum class ScreenRoute {
    RecitersScreen,
    SurahsScreen,
    FavoritesScreen,
    SettingsScreen
}

sealed class Screen(val route: String) {
    object Reciters : Screen(ScreenRoute.RecitersScreen.name)
    object Surahs : Screen(ScreenRoute.SurahsScreen.name)
    object Favorites : Screen(ScreenRoute.FavoritesScreen.name)
    object Settings : Screen(ScreenRoute.SettingsScreen.name)
}