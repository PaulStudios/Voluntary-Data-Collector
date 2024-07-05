package org.paulstudios.datasurvey.data.collector

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import org.paulstudios.datasurvey.data.models.GPSData
import org.paulstudios.datasurvey.data.models.GPSDataList
import org.paulstudios.datasurvey.data.storage.JsonStorage
import org.paulstudios.datasurvey.data.storage.UserIdManager
import java.text.SimpleDateFormat
import java.util.*

class GPSDataCollection(application: Application) : AndroidViewModel(application) {
    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> get() = _status

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    private val _storedData = MutableStateFlow<List<GPSDataList>>(emptyList())
    val storedData: StateFlow<List<GPSDataList>> get() = _storedData

    private val jsonStorage = JsonStorage(application)
    private val userIdManager = UserIdManager(application)
    private val gpsDataList = mutableListOf<GPSData>()
    private var collectionJob: Job? = null
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    fun startDataCollection(context: Context) {
        if (collectionJob == null) {
            _status.value = "Collecting Data..."
            collectionJob = viewModelScope.launch {
                while (isActive) {
                    try {
                        val location = getLocation(context)
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        gpsDataList.add(GPSData(location.latitude, location.longitude, timestamp))
                        delay(10000)
                    } catch (e: SecurityException) {
                        _status.value = "Permission denied. Please enable location permissions."
                        stopDataCollection()
                    } catch (e: Exception) {
                        _status.value = "Error collecting data: ${e.message}. Retrying..."
                        delay(10000) // Retry after a delay
                    }
                }
            }
        }
    }

    fun stopDataCollection() {
        collectionJob?.cancel()
        collectionJob = null
        saveData()
        _status.value = "Data Collection Stopped"
    }

    private fun saveData() {
        viewModelScope.launch {
            val userId = userIdManager.getUserId()
            val gpsDataList = GPSDataList(userId, gpsDataList)
            val result = jsonStorage.saveGPSDataList(gpsDataList)
            if (result.isSuccess) {
                _status.value = "Data saved successfully"
            } else {
                _error.value = "Error saving data: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(context: Context): Location {
        val cancellationTokenSource = CancellationTokenSource()
        return fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).await()
    }

    fun loadStoredData() {
        viewModelScope.launch {
            val result = jsonStorage.loadAllGPSData()
            if (result.isSuccess) {
                _storedData.value = result.getOrThrow()
            } else {
                _error.value = "Error loading data: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}

