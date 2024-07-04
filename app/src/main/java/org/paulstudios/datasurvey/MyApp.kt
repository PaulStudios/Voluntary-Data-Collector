package org.paulstudios.datasurvey

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.ui.screens.auth.LoginScreen
import org.paulstudios.datasurvey.ui.screens.auth.RegisterScreen
import org.paulstudios.datasurvey.ui.screens.collector.DataCollectionScreen
import org.paulstudios.datasurvey.ui.screens.collector.ProjectIdFormScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MyApp(navController: NavHostController = rememberAnimatedNavController()) {
    AnimatedNavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.ProjectIdForm.route) {
            ProjectIdFormScreen(navController = navController) // New screen destination
        }
        composable(Screen.DataCollection.route) {
            DataCollectionScreen()
        }
        // Add more screens as composable destinations here
    }
}
