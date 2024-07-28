package org.paulstudios.datasurvey.ui.screens.collector

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.paulstudios.datasurvey.data.models.Screen
import org.paulstudios.datasurvey.viewmodels.GPSDataCollection
import org.paulstudios.datasurvey.viewmodels.ServerStatusViewModel
import org.paulstudios.datasurvey.viewmodels.UploadStatus

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCollectionScreen(serverStatusViewModel: ServerStatusViewModel, navController: NavHostController) {
    val viewModel = rememberGPSDataCollectionViewModel(serverStatusViewModel)
    val serverStatus by serverStatusViewModel.serverStatus.collectAsState()
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionDialogType by remember { mutableStateOf<PermissionDialogType>(PermissionDialogType.Initial) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val uploadStatus by viewModel.uploadStatus.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()

    val requiredPermissions = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.onPermissionsGranted(context)
        } else {
            permissionDialogType = PermissionDialogType.Settings
            showPermissionDialog = true
        }
    }
    val status by viewModel.status.collectAsState()
    val storedData by viewModel.storedData.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    var showEnableGpsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        if (storedData.isNotEmpty()) {
            isLoading = false
        }
        serverStatusViewModel.statusMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Data Collection") },
                actions = {
                    ServerStatusIndicator(
                        isOnline = serverStatus,
                        onClick = { serverStatusViewModel.showStatusMessage() }
                    )
                    IconButton(
                        onClick = {
                            showExitConfirmationDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Exit"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = status)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    if (!arePermissionsGranted(context, requiredPermissions)) {
                        requestPermissions(permissionLauncher, requiredPermissions)
                    } else {
                        if (!isGpsEnabled(context)) {
                            showEnableGpsDialog = true
                        } else {
                            viewModel.startDataCollection(context)
                        }
                    }
                }) {
                    Text("Start Data Collection")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { viewModel.stopDataCollection(context) }) {
                    Text("Stop Data Collection")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        isLoading = true
                        coroutineScope.launch {
                            viewModel.loadStoredData()
                            isLoading = false
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        Text("Loading...")
                    } else {
                        Text("Load Stored Data")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.uploadData() },
                    enabled = uploadStatus !is UploadStatus.Uploading
                ) {
                    Text(when (uploadStatus) {
                        is UploadStatus.Uploading -> "Uploading..."
                        else -> "Upload Data"
                    })
                }
                Spacer(modifier = Modifier.height(20.dp))
                when (uploadStatus) {
                    is UploadStatus.Uploading -> {
                        LinearProgressIndicator(
                            progress = uploadProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Uploading: ${(uploadProgress * 100).toInt()}%")
                        Log.d("DataCollectionScreen", "Upload progress: ${uploadProgress * 100}%")
                    }
                    is UploadStatus.Success -> Text("Upload successful!", color = MaterialTheme.colorScheme.primary)
                    is UploadStatus.Error -> Text("Upload failed. Please try again.", color = MaterialTheme.colorScheme.error)
                    is UploadStatus.NoData -> Text("No data to upload.", color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
                Spacer(modifier = Modifier.height(20.dp))
                if (storedData.isNotEmpty()) {
                    LazyColumn {
                        items(storedData) { gpsDataList ->
                            Text(
                                text = ": Data ID - ${gpsDataList.fileName} :",
                                fontWeight = FontWeight.Bold
                            )
                            gpsDataList.data.forEach { gpsData ->
                                Text("------------------------------------------------")
                                Text("Latitude: ${gpsData.latitude}")
                                Text("Longitude: ${gpsData.longitude}")
                                Text("Timestamp: ${gpsData.timestamp}")
                            }
                            Text(
                                text = "----------------------------------------------",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                if (error != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = "Error: $error", color = MaterialTheme.colorScheme.error)
                }

            }
        }
    )
    if (showExitConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            title = { Text("Confirm Exit") },
            text = { Text("Are you sure you want to exit? This will delete all stored data. Please upload all data before exiting.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmationDialog = false
                        viewModel.cleanCollector(context)
                        Log.d("DataCollectionScreen", "Project ID deleted. Exiting Data Collector")
                        navController.navigate(Screen.ProjectIdForm.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }

                    }
                ) {
                    Text("Exit Anyway")
                }
            },
            dismissButton = {
                Button(onClick = { showExitConfirmationDialog = false }) {
                    Text("Go Back")
                }
            }
        )
    }

if (showPermissionDialog) {
    PermissionDialog(
        type = permissionDialogType,
        onDismiss = { showPermissionDialog = false },
        onConfirm = {
            showPermissionDialog = false
            when (permissionDialogType) {
                PermissionDialogType.Initial -> requestPermissions(permissionLauncher, requiredPermissions)
                PermissionDialogType.Settings -> openAppSettings(context)
            }
        }
    )
}
    if (showEnableGpsDialog) {
        EnableGpsDialog(
            onConfirm = {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                showEnableGpsDialog = false
            },
            onDismiss = {
                showEnableGpsDialog = false
            }
        )
    }
    LaunchedEffect(uploadStatus) {
        when (uploadStatus) {
            is UploadStatus.Success -> {
                snackbarHostState.showSnackbar("Data uploaded successfully!")
                viewModel.loadStoredData() // Refresh the stored data list
            }
            is UploadStatus.Error -> {
                snackbarHostState.showSnackbar("Failed to upload data. Please try again.")
            }
            else -> {}
        }
    }
}

@Composable
fun PermissionDialog(
    type: PermissionDialogType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == PermissionDialogType.Initial) "Permission Information" else "Permission Required") },
        text = {
            Text(
                when (type) {
                    PermissionDialogType.Initial -> "This app requires location permissions to collect GPS data for research purposes. We need access to your location in the background to continue collecting data even when the app is not in use. Your data will be anonymized and used solely for the purpose of this study."
                    PermissionDialogType.Settings -> "Some permissions were denied. Please grant all required permissions in the app settings to use this feature."
                }
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(if (type == PermissionDialogType.Initial) "OK, Request Permissions" else "Open Settings")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class PermissionDialogType {
    Initial, Settings
}

fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requestPermissions(
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    permissions: Array<String>
) {
    permissionLauncher.launch(permissions)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Composable
fun rememberGPSDataCollectionViewModel(serverStatusViewModel: ServerStatusViewModel): GPSDataCollection {
    val context = LocalContext.current
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GPSDataCollection(
                    application = context.applicationContext as Application,
                    serverStatusViewModel = serverStatusViewModel
                ) as T
            }
        }
    )
}

// Function to check if GPS is enabled
fun isGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

// Function to prompt the user to enable GPS
@Composable
fun EnableGpsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable GPS") },
        text = { Text("GPS is not enabled. Do you want to go to settings menu?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}