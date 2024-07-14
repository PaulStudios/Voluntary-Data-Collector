package org.paulstudios.datasurvey.network

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.paulstudios.datasurvey.R
import org.paulstudios.datasurvey.data.models.GPSData
import org.paulstudios.datasurvey.data.storage.JsonStorage
import org.paulstudios.datasurvey.data.storage.UserIdManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var jsonStorage: JsonStorage
    private lateinit var userIdManager: UserIdManager
    private val gpsDataList = mutableListOf<GPSData>()
    private var collectionJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        jsonStorage = JsonStorage(applicationContext)
        userIdManager = UserIdManager(applicationContext)
        startForegroundService()
        startDataCollection()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        collectionJob?.cancel()
        saveData()
    }

    private fun startForegroundService() {
        val channelId = "LocationServiceChannel"
        val channel = NotificationChannel(
            channelId,
            "Location Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Collecting location data in background")
            .setSmallIcon(R.drawable.baseline_share_location_24)
            .build()
        startForeground(1, notification)
    }

    private fun startDataCollection() {
        collectionJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val location = getLocation() ?: continue
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    gpsDataList.add(GPSData(location.latitude, location.longitude, timestamp))
                    delay(10000)
                } catch (e: SecurityException) {
                    Log.e("LocationService", "Location permission error", e)
                    stopSelf()
                } catch (e: Exception) {
                    Log.e("LocationService", "Error collecting location data", e)
                    delay(10000) // Retry after a delay
                }
            }
        }
    }

    private fun saveData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = userIdManager.getUserId()
                val gpsDataList = org.paulstudios.datasurvey.data.models.GPSDataList(userId, this@LocationService.gpsDataList)
                val result = jsonStorage.saveGPSDataList(gpsDataList)
                if (result.isFailure) {
                    Log.e("LocationService", "Error saving GPS data", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("LocationService", "Error in saveData", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(): Location? {
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                null
            } else {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Error getting location", e)
            null
        }
    }
}
