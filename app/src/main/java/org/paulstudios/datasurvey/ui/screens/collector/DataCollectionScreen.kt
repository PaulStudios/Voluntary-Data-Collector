package org.paulstudios.datasurvey.ui.screens.collector

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.paulstudios.datasurvey.data.collector.GPSDataCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCollectionScreen(viewModel: GPSDataCollection = viewModel()) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsState()
    val storedData by viewModel.storedData.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val shouldShowRationale = shouldShowRequestPermissionRationale(context)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.startDataCollection(context)
        } else {
            showDialog = true
            if (!shouldShowRationale) {
                showSettingsDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Collection Screen") }
            )
        },
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
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) {
                    Text("Start Data Collection")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { viewModel.stopDataCollection() }) {
                    Text("Stop Data Collection")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { viewModel.loadStoredData() }) {
                    Text("Load Stored Data")
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Permission Denied") },
            text = { Text("Location permissions are required to collect GPS data. Please enable location permissions in the app settings.") },
            confirmButton = {
                Button(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Location permissions are required to collect GPS data. Please enable location permissions in the app settings.") },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun shouldShowRequestPermissionRationale(context: Context): Boolean {
    val activity = context as? Activity
    return activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION)
    } ?: false
}