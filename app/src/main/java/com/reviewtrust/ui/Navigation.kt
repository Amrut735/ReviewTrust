package com.reviewtrust.ui

/**
 * Sealed class defining the navigation routes in the app.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Analysis : Screen("analysis")
}
