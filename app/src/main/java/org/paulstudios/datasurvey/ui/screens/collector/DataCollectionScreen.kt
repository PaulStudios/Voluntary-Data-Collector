package org.paulstudios.datasurvey.ui.screens.collector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.paulstudios.datasurvey.data.collector.GPSDataCollection
import org.paulstudios.datasurvey.data.collector.UploadStatus

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCollectionScreen(viewModel: GPSDataCollection = viewModel()) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionDialogType by remember { mutableStateOf<PermissionDialogType>(PermissionDialogType.Initial) }

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
    val batteryOptimizationStatus by viewModel.batteryOptimizationStatus.observeAsState(initial = false)
    var permissionsGranted by remember { mutableStateOf(false) }

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    val uploadStatus by viewModel.uploadStatus.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Data Collection") }
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
                        viewModel.startDataCollection(context)
                    }
                }) {
                    Text("Start Data Collection")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { viewModel.stopDataCollection(context) }) {
                    Text("Stop Data Collection")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { viewModel.loadStoredData() }) {
                    Text("Load Stored Data")
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
                    is UploadStatus.Uploading -> CircularProgressIndicator()
                    is UploadStatus.Success -> Text("Upload successful!", color = MaterialTheme.colorScheme.primary)
                    is UploadStatus.Error -> Text("Upload failed. Please try again.", color = MaterialTheme.colorScheme.error)
                    else -> {}
                }
                Spacer(modifier = Modifier.height(20.dp))
                if (storedData.isNotEmpty()) {
                    LazyColumn {
                        items(storedData) { gpsDataList ->
                            Text("User ID: ${gpsDataList.userId}")
                            gpsDataList.data.forEach { gpsData ->
                                Text("Lat: ${gpsData.latitude}, Lon: ${gpsData.longitude}, Time: ${gpsData.timestamp}")
                            }
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