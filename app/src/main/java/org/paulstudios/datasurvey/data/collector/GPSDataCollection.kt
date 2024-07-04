package org.paulstudios.datasurvey.data.collector

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.Location
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.paulstudios.datasurvey.data.models.UserData
import org.paulstudios.datasurvey.data.models.GPSData
import org.paulstudios.datasurvey.network.DatabaseInstance
import java.text.SimpleDateFormat
import java.util.*

class GPSDataCollection(application: Application) : AndroidViewModel(application) {
    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> get() = _status

    private val database = DatabaseInstance.getDatabase(application)
    private val userDataDao = database.userDataDao()
    private val gpsDataList = mutableListOf<GPSData>()
    private var collectionJob: Job? = null
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    fun startDataCollection(context: Context) {
        if (collectionJob == null) {
            _status.value = "Collecting Data..."
            collectionJob = viewModelScope.launch {
                while (isActive) {
                    val location = getLocation(context)
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    gpsDataList.add(GPSData(location.latitude, location.longitude, timestamp))
                    delay(10000)
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
            val userId = UUID.randomUUID().toString()
            val userData = UserData(userId, gpsDataList)
            userDataDao.insertUserData(userData)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(context: Context): Location {
        val cancellationTokenSource = CancellationTokenSource()
        return fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).await() // This requires the use of a coroutine and the 'kotlinx-coroutines-play-services' library
    }
}
