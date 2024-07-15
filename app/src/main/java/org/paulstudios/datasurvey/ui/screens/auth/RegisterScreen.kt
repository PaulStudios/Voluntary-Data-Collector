package org.paulstudios.datasurvey.ui.screens.auth

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.*
import org.paulstudios.datasurvey.MainActivity
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.ui.screens.auth.components.AuthScreen
import org.paulstudios.datasurvey.viewmodels.AuthState
import org.paulstudios.datasurvey.viewmodels.AuthViewModel

@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    val authState by viewModel.authState.collectAsState()

    AuthScreen(
        email = viewModel.email,
        onEmailChange = { viewModel.email = it },
        password = viewModel.password,
        onPasswordChange = { viewModel.password = it },
        buttonText = "Register",
        onSubmit = {
            viewModel.register(context) {
                navController.navigate(Screen.ProjectIdForm.route)
            }
        },
        secondaryButtonText = "Already have an account? Login",
        onSecondaryButtonClick = { navController.navigate(Screen.Login.route) },
        errorMessage = if (authState is AuthState.Error) (authState as AuthState.Error).message else "",
        isLoading = authState is AuthState.Loading,
        onGithubLogin = { (context as? MainActivity)?.signInWithGithub() },
        onGoogleLogin = { viewModel.signInWithGoogle(context as Activity) }
    )

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate(Screen.ProjectIdForm.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            else -> {} // Handle other states if needed
        }
    }
}