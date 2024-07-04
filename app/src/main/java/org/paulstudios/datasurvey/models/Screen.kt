package org.paulstudios.datasurvey.models

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Info : Screen("info")
    object DataCollection : Screen("data_collection")
    object ProjectIdForm : Screen("project_id_form")
}
