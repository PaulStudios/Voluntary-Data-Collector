package org.paulstudios.datasurvey.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.ui.screens.auth.components.AuthScreen
import org.paulstudios.datasurvey.ui.screens.auth.components.PasswordStrengthBar
import org.paulstudios.datasurvey.ui.screens.auth.components.calculatePasswordStrength

@Composable
fun RegisterScreen(navController: NavController) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val context = navController.context
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)

    fun saveUserId(userId: String) {
        with(sharedPreferences.edit()) {
            putString("userId", userId)
            apply()
        }
    }

    fun signInWithGithub() {
        isLoading = true
        val provider = OAuthProvider.newBuilder("github.com")
        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask.addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    saveUserId(auth.currentUser?.uid ?: "")
                    navController.navigate(Screen.ProjectIdForm.route)
                } else {
                    val exception = task.exception
                    Log.w("Register", "signInWithGithub:failure", exception)
                    errorMessage = exception?.localizedMessage ?: "GitHub sign-in failed"
                }
            }
        } else {
            auth.startActivityForSignInWithProvider(navController.context as Activity, provider.build())
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        saveUserId(auth.currentUser?.uid ?: "")
                        navController.navigate(Screen.ProjectIdForm.route)
                    } else {
                        val exception = task.exception
                        Log.w("Register", "signInWithGithub:failure", exception)
                        errorMessage = exception?.localizedMessage ?: "GitHub sign-in failed"
                    }
                }
        }
    }

    fun signInWithGoogle() {
        isLoading = true
        val provider = GoogleAuthProvider.getCredential(null, null)
        auth.signInWithCredential(provider)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    saveUserId(auth.currentUser?.uid ?: "")
                    navController.navigate(Screen.ProjectIdForm.route)
                } else {
                    val exception = task.exception
                    Log.w("Register", "signInWithGoogle:failure", exception)
                    errorMessage = exception?.localizedMessage ?: "Google sign-in failed"
                }
            }
    }

    AuthScreen(
        email = email,
        password = password,
        buttonText = "Register",
        onSubmit = {
            isLoading = true
            auth.createUserWithEmailAndPassword(email.value, password.value)
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        saveUserId(auth.currentUser?.uid ?: "")
                        navController.navigate(Screen.ProjectIdForm.route)
                    } else {
                        val exception = task.exception
                        Log.w("Register", "createUserWithEmail:failure", exception)
                        errorMessage = exception?.localizedMessage ?: "Registration failed"
                    }
                }
        },
        secondaryButtonText = "Already have an account? Login",
        onSecondaryButtonClick = { navController.navigate(Screen.Login.route) },
        passwordStrengthComposable = {
            val strength = calculatePasswordStrength(password.value)
            PasswordStrengthBar(strength)
        },
        errorMessage = errorMessage,
        isLoading = isLoading,
        onGithubLogin = ::signInWithGithub,
        onGoogleLogin = ::signInWithGoogle
    )
}
