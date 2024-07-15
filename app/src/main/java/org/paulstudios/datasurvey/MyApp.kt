package org.paulstudios.datasurvey

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.ui.screens.auth.LoginScreen
import org.paulstudios.datasurvey.ui.screens.auth.RegisterScreen
import org.paulstudios.datasurvey.ui.screens.collector.DataCollectionScreen
import org.paulstudios.datasurvey.ui.screens.collector.ProjectIdFormScreen
import org.paulstudios.datasurvey.viewmodels.AuthViewModel

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MyApp(navController: NavHostController, context: Context) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(context.applicationContext as android.app.Application))

    AnimatedNavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController, viewModel = authViewModel)
        }
        composable(Screen.ProjectIdForm.route) {
            ProjectIdFormScreen(navController = navController)
        }
        composable(Screen.DataCollection.route) {
            DataCollectionScreen()
        }
    }
}