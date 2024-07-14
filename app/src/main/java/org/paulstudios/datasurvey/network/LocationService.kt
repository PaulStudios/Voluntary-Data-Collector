package org.paulstudios.datasurvey.network

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.SystemClock
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
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var jsonStorage: JsonStorage
    private lateinit var userIdManager: UserIdManager
    private val gpsDataList = mutableListOf<GPSData>()
    private var collectionJob: Job? = null
    private lateinit var notificationManager: NotificationManager
    private val CHANNEL_ID = "LocationServiceChannel"
    private val NOTIFICATION_ID = 1
    private var startTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        jsonStorage = JsonStorage(applicationContext)
        userIdManager = UserIdManager(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startTime = SystemClock.elapsedRealtime()
        startForegroundService()
        startDataCollection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP_SERVICE" -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        collectionJob?.cancel()
        saveData()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Service",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ongoing notification for location data collection"
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        val notification = createNotification("Collecting location data...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(contentText: String): Notification {
        val stopIntent = Intent(this, LocationService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.baseline_share_location_24)
            .addAction(R.drawable.baseline_stop_circle_24, "Stop", stopPendingIntent)
            .build()
    }

    private fun startDataCollection() {
        collectionJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val location = getLocation()
                    if (location != null) {
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        gpsDataList.add(GPSData(location.latitude, location.longitude, timestamp))
                        val elapsedTime = SystemClock.elapsedRealtime() - startTime
                        val formattedElapsedTime = formatElapsedTime(elapsedTime)
                        updateNotification("Last location: ${location.latitude}, ${location.longitude}. Running time: $formattedElapsedTime")
                    }
                    delay(10000)
                } catch (e: SecurityException) {
                    Log.e("LocationService", "Location permission error", e)
                    stopSelf()
                } catch (e: Exception) {
                    Log.e("LocationService", "Error collecting location data", e)
                    delay(10000)
                }
            }
        }
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID, notification)
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

    private fun formatElapsedTime(elapsedTime: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(elapsedTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
