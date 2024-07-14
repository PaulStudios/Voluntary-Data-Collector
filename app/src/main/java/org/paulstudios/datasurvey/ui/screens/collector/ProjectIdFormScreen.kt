package org.paulstudios.datasurvey.ui.screens.collector

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.paulstudios.datasurvey.network.RetrofitInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.paulstudios.datasurvey.data.storage.UserIdManager
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectIdFormScreen(navController: NavController) {
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

    LaunchedEffect(Unit) {
        userIdManager.getProjectId()?.let {
            projectId = TextFieldValue(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Project ID") }
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
                    isError = errorMessage.isNotEmpty()
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
                                    Log.d(logTag, "Attempt ${retryCount + 1} to validate project ID ${projectId.text}")
                                    snackbarHostState.showSnackbar("Attempt ${retryCount + 1} to validate project ID")
                                    try {
                                        val response = RetrofitInstance.api.getProjectDetails(projectId.text)
                                        if (response.isSuccessful) {
                                            Log.d(logTag, "Project ID ${projectId.text} validated successfully.")
                                            // Save the project ID
                                            userIdManager.saveProjectId(projectId.text)
                                            navController.navigate("data_collection")
                                            success = true
                                        } else {
                                            errorMessage = handleHttpError(response.code())
                                            Log.e(logTag, "Error validating project ID: ${errorMessage}")
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
                                        errorMessage = "An unexpected error occurred: ${e.message}"
                                        lastException = e
                                        Log.e(logTag, "Unexpected error: ${e.message}")
                                        snackbarHostState.showSnackbar(errorMessage)
                                    } finally {
                                        retryCount++
                                        if (retryCount >= maxRetries && !success) {
                                            errorMessage = lastException?.message ?: "Failed to validate project ID"
                                            Log.e(logTag, "Max retries reached. ${errorMessage}")
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

fun handleHttpError(code: Int): String {
    return when (code) {
        400 -> "Invalid request. Please check the project ID."
        404 -> "Project ID not found. Please enter a valid ID."
        500 -> "Server error. Please try again later."
        else -> "An unknown error occurred. Please try again."
    }
}
