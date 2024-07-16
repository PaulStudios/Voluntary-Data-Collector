package org.paulstudios.datasurvey.ui.screens.auth

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import org.paulstudios.datasurvey.MainActivity
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.ui.screens.auth.components.AuthScreen
import org.paulstudios.datasurvey.viewmodels.AuthState
import org.paulstudios.datasurvey.viewmodels.AuthViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    val authState by viewModel.authState.collectAsState()

    AuthScreen(
        email = viewModel.email,
        onEmailChange = { viewModel.email = it },
        password = viewModel.password,
        onPasswordChange = { viewModel.password = it },
        buttonText = "Login",
        onSubmit = {
            viewModel.login(context) {
                navController.navigate(Screen.ProjectIdForm.route)
            }
        },
        secondaryButtonText = "Don't have an account? Register",
        onSecondaryButtonClick = { navController.navigate(Screen.Register.route) },
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