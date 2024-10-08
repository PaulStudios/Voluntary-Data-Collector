package org.paulstudios.datasurvey

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
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.ui.theme.DataSurveyTheme
import org.paulstudios.datasurvey.viewmodels.AuthState
import org.paulstudios.datasurvey.viewmodels.AuthViewModel
import org.paulstudios.datasurvey.viewmodels.ServerStatusViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController
    private lateinit var authViewModel: AuthViewModel
    private lateinit var serverStatusViewModel: ServerStatusViewModel

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        authViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[AuthViewModel::class.java]
        serverStatusViewModel = ViewModelProvider(this)[ServerStatusViewModel::class.java]
        setContent {
            navController = rememberAnimatedNavController()
            LaunchedEffect(Unit) {
                checkAndNavigateIfLoggedIn()
            }
            DataSurveyTheme {
                MyApp(navController, this)
            }
        }
        lifecycle.addObserver(serverStatusViewModel)
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            // Send token to your backend if needed
        }
        observeAuthState()
    }

    private fun checkAndNavigateIfLoggedIn() {
        val (isLoggedIn, _) = authViewModel.checkLoginState()
        if (isLoggedIn) {
            navigateToMainScreen()
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Success -> navigateToMainScreen()
                    is AuthState.Error -> showErrorToast(state.message)
                    else -> {} // Handle other states if needed
                }
            }
        }
    }

    private fun navigateToMainScreen() {
        navController.navigate(Screen.ProjectIdForm.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            handleGoogleSignInResult(data)
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
            account.idToken?.let { authViewModel.firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            authViewModel.handleAuthError(e)
        }
    }

    fun signInWithGithub() {
        val provider = OAuthProvider.newBuilder("github.com")
        FirebaseAuth.getInstance().startActivityForSignInWithProvider(this, provider.build())
            .addOnCompleteListener { task ->
                authViewModel.handleGithubSignInResult(task)
            }
    }

    companion object {
        const val RC_SIGN_IN = 9001
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(serverStatusViewModel)
    }
}