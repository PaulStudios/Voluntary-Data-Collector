package org.paulstudios.datasurvey.ui.screens.collector

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.data.storage.UserIdManager
import org.paulstudios.datasurvey.network.RetrofitInstance
import org.paulstudios.datasurvey.utils.MarkdownViewerScreen
import org.paulstudios.datasurvey.viewmodels.ServerStatusViewModel
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectIdFormScreen(navController: NavHostController, serverStatusViewModel: ServerStatusViewModel) {
    val serverStatus by serverStatusViewModel.serverStatus.collectAsState()
    var projectId by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()
    val maxRetries = 2
    val logTag = "ProjectIdScreen"
    val context = LocalContext.current
    val userIdManager = remember { UserIdManager(context) }
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    var showConsent by remember { mutableStateOf(false) }
    var showConsentRequiredInfo by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        val savedProjectId = userIdManager.getProjectId()
        val consentGiven = sharedPreferences.getBoolean("consent_given", false)

        if (savedProjectId != null && consentGiven) {
            navController.navigate(Screen.DataCollection.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        } else if (savedProjectId != null) {
            projectId = TextFieldValue(savedProjectId)
        }

        serverStatusViewModel.statusMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    if (showConsentRequiredInfo) {
        ConsentRequiredInfoScreen(
            onDismiss = {
                showConsentRequiredInfo = false
                errorMessage = ""
            }
        )
    } else if (showConsent) {
        MarkdownViewerScreen(
            onConsentGiven = {
                showConsent = false
                navController.navigate(Screen.DataCollection.route)
            },
            onDismiss = {
                showConsent = false
                showConsentRequiredInfo = true
            }
        )
    } else {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Enter Project ID") },
                    actions = {
                        ServerStatusIndicator(
                            isOnline = serverStatus,
                            onClick = { serverStatusViewModel.showStatusMessage() }
                        )
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = projectId,
                        onValueChange = {
                            projectId = it
                            errorMessage = ""
                        },
                        label = { Text("Project ID") },
                        isError = errorMessage.isNotEmpty(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(onClick = {
                            if (projectId.text.isBlank()) {
                                errorMessage = "Project ID cannot be empty"
                            } else if (projectId.text.length != 6 || !projectId.text.all { it.isDigit() }) {
                                errorMessage = "Project ID must be a 6-digit number"
                            } else {
                                coroutineScope.launch {
                                    isLoading = true
                                    var success = false
                                    var lastException: Exception? = null
                                    retryCount = 0
                                    while (retryCount < maxRetries && !success) {
                                        Log.d(
                                            logTag,
                                            "Attempt ${retryCount + 1} to validate project ID ${projectId.text}"
                                        )
                                        snackbarHostState.showSnackbar("Attempt ${retryCount + 1} to validate project ID")
                                        try {
                                            val response = RetrofitInstance.api.getProjectDetails(projectId.text)
                                            if (response.isSuccessful) {
                                                Log.d(logTag, "Project ID ${projectId.text} validated successfully.")
                                                // Save the project ID
                                                userIdManager.saveProjectId(projectId.text)

                                                // Check if consent has been given
                                                val consentGiven = sharedPreferences.getBoolean("consent_given", false)
                                                if (consentGiven) {
                                                    navController.navigate(Screen.DataCollection.route)
                                                } else {
                                                    showConsent = true
                                                }
                                                success = true
                                            } else {
                                                errorMessage = handleHttpError(response.code())
                                                Log.e(
                                                    logTag,
                                                    "Error validating project ID: ${errorMessage}"
                                                )
                                                snackbarHostState.showSnackbar(errorMessage)
                                            }
                                        } catch (e: HttpException) {
                                            errorMessage = handleHttpError(e.code())
                                            Log.e(logTag, "HTTP exception: ${e.message}")
                                            snackbarHostState.showSnackbar(errorMessage)
                                        } catch (e: IOException) {
                                            lastException = e
                                            errorMessage = "Network error. Retrying..."
                                            Log.e(logTag, "Network error: ${e.message}")
                                            snackbarHostState.showSnackbar(errorMessage)
                                            delay(2000)
                                        } catch (e: Exception) {
                                            errorMessage =
                                                "An unexpected error occurred: ${e.message}"
                                            lastException = e
                                            Log.e(logTag, "Unexpected error: ${e.message}")
                                            snackbarHostState.showSnackbar(errorMessage)
                                        } finally {
                                            retryCount++
                                            if (retryCount >= maxRetries && !success) {
                                                errorMessage = lastException?.message
                                                    ?: "Failed to validate project ID"
                                                Log.e(
                                                    logTag,
                                                    "Max retries reached. ${errorMessage}"
                                                )
                                                snackbarHostState.showSnackbar("Max retries reached. $errorMessage")
                                            }
                                        }
                                    }
                                    isLoading = false
                                }
                            }
                        }) {
                            Text("Submit")
                        }
                    }
                }
            }
        )
    }
}

fun handleHttpError(code: Int): String {
    return when (code) {
        400 -> "Invalid request. Please check the project ID."
        404 -> "Project ID not found. Please enter a valid ID."
        500 -> "Server error. Please try again later."
        else -> "An unknown error occurred. Please try again."
    }
}

@Composable
fun ConsentRequiredInfoScreen(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consent Required") },
        text = { Text("To use this app, you must agree to the Privacy Policy and Terms & Conditions. Without consent, the app cannot be used.") },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}