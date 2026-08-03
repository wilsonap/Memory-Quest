package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Game : Screen("game")
    object Shop : Screen("shop")
    object Settings : Screen("settings")
    object Stats : Screen("stats")
    object Ranking : Screen("ranking")
    object Achievements : Screen("achievements")
    object Profile : Screen("profile")
}
