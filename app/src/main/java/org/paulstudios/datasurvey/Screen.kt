package org.paulstudios.datasurvey

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    // Add more screens as needed
}