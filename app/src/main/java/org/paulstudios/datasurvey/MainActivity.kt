package org.paulstudios.datasurvey

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.launch
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.ui.screens.auth.LoginScreen
import org.paulstudios.datasurvey.ui.screens.auth.RegisterScreen
import org.paulstudios.datasurvey.ui.theme.DataSurveyTheme
import org.paulstudios.datasurvey.viewmodels.AuthState
import org.paulstudios.datasurvey.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    private lateinit var authViewModel: AuthViewModel

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        authViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(AuthViewModel::class.java)

        setContent {
            navController = rememberAnimatedNavController()
            LaunchedEffect(Unit) {
                val (isLoggedIn, provider) = authViewModel.checkLoginState()
                if (isLoggedIn) {
                    when (provider) {
                        "google", "github", "email" -> {
                            navController.navigate(Screen.ProjectIdForm.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    }
                }
            }
            DataSurveyTheme {
                MyApp(navController, this@MainActivity)
            }
        }
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Success -> {
                        // Navigate to main screen or update UI
                        navController.navigate(Screen.ProjectIdForm.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                    is AuthState.Error -> {
                        // Show error message
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {} // Handle other states if needed
                }
            }
        }
    }

    private fun checkLoginState(navController: NavController) {
        val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            navController.navigate(Screen.ProjectIdForm.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("Auth", "Google sign in failed", e)
            // Update UI to show error
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuthWithCredential(credential)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { authViewModel.firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                authViewModel.handleAuthError(e)
            }
        }
    }

    fun signInWithGithub() {
        val provider = OAuthProvider.newBuilder("github.com")
        FirebaseAuth.getInstance().startActivityForSignInWithProvider(this, provider.build())
            .addOnCompleteListener { task ->
                authViewModel.handleGithubSignInResult(task)
            }
    }

    private fun firebaseAuthWithCredential(credential: AuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Auth", "signInWithCredential:success")
                    val user = FirebaseAuth.getInstance().currentUser
                    saveUserSession(user)
                    navController.navigate(Screen.ProjectIdForm.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    handleAuthError(task.exception)
                }
            }
    }

    private fun handleAuthError(exception: Exception?) {
        Log.w("Auth", "signInWithCredential:failure", exception)
        val errorMessage = when (exception) {
            is FirebaseAuthUserCollisionException -> "An account already exists with this email."
            is FirebaseAuthInvalidCredentialsException -> "Invalid credentials. Please try again."
            else -> "Authentication failed. Please try again later."
        }
        // Update UI to show error
        // You might want to use a ViewModel or some other method to communicate this error to your UI
    }

    private fun saveUserSession(user: FirebaseUser?) {
        user?.let {
            val sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString("userId", it.uid)
                putString("userEmail", it.email)
                putBoolean("isLoggedIn", true)
                apply()
            }
        }
    }

    companion object {
        const val RC_SIGN_IN = 9001

        fun saveUserSession(context: Context, user: FirebaseUser?) {
            user?.let {
                val sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("userId", it.uid)
                    putString("userEmail", it.email)
                    putBoolean("isLoggedIn", true)
                    apply()
                }
            }
        }
    }


}