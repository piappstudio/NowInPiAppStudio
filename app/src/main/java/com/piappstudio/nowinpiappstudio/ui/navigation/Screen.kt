package com.piappstudio.nowinpiappstudio.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Contacts : Screen("contacts")
}

