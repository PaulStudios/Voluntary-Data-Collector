package org.paulstudios.datasurvey.models

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Info : Screen("info")
    data object DataCollection : Screen("data_collection")
    data object ProjectIdForm : Screen("project_id_form")
}