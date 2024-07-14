package org.paulstudios.datasurvey.data.collector

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.paulstudios.datasurvey.data.models.GPSData
import org.paulstudios.datasurvey.data.models.GPSDataList
import org.paulstudios.datasurvey.data.storage.JsonStorage
import org.paulstudios.datasurvey.network.LocationService
import org.paulstudios.datasurvey.network.RetrofitInstance
import org.paulstudios.datasurvey.utils.BatteryOptimizationUtil
import org.paulstudios.datasurvey.data.storage.UserIdManager
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class GPSDataCollection(application: Application) : AndroidViewModel(application) {
    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> get() = _status

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    private val _storedData = MutableStateFlow<List<GPSDataList>>(emptyList())
    val storedData: StateFlow<List<GPSDataList>> get() = _storedData

    private val _batteryOptimizationStatus = MutableLiveData<Boolean>()
    val batteryOptimizationStatus: LiveData<Boolean> get() = _batteryOptimizationStatus

    private val jsonStorage = JsonStorage(application)

    private val workManager = WorkManager.getInstance(application)

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus

    fun uploadData() {
        _uploadStatus.value = UploadStatus.Uploading
        val uploadWorkRequest = OneTimeWorkRequestBuilder<DataUploadWorker>()
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueue(uploadWorkRequest)

        workManager.getWorkInfoByIdLiveData(uploadWorkRequest.id)
            .observeForever { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _uploadStatus.value = UploadStatus.Success
                        loadStoredData() // Refresh the stored data list
                    }
                    WorkInfo.State.FAILED -> _uploadStatus.value = UploadStatus.Error
                    else -> {} // Do nothing for other states
                }
            }
    }


    fun startDataCollection(context: Context) {
        _status.value = "Collecting Data..."
        context.startForegroundService(Intent(context, LocationService::class.java))
    }

    fun stopDataCollection(context: Context) {
        context.stopService(Intent(context, LocationService::class.java))
        _status.value = "Data Collection Stopped"
    }

    fun handlePermissionsResult(granted: Boolean) {
        if (granted) {
            _status.value = "Permissions granted"
        } else {
            _error.value = "Location permissions are required to collect GPS data. Please enable location permissions in the app settings."
        }
    }

    fun onPermissionsGranted(context: Context) {
        viewModelScope.launch {
            startDataCollection(context)
        }
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

    fun updateBatteryOptimizationStatus(context: Context) {
        _batteryOptimizationStatus.value = BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context)
    }
}


class DataUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val jsonStorage = JsonStorage(context)
    private val userIdManager = UserIdManager(context)
    private val okHttpClient = OkHttpClient()

    override suspend fun doWork(): Result {
        return try {
            val allData = jsonStorage.loadAllGPSData().getOrThrow()
            val uploadResult = uploadDataInBatches(allData)
            if (uploadResult) {
                // Clear all JSON data after successful upload
                jsonStorage.clearAllData()

                // Show success message to user
                showNotification("Data Upload Successful", "All data has been uploaded successfully.")

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("DataUploadWorker", "Error during upload", e)

            // Show error message to user
            showNotification("Data Upload Failed", "There was an error uploading the data. Please try again later.")

            Result.failure()
        }
    }

    private suspend fun uploadDataInBatches(allData: List<GPSDataList>): Boolean {
        val projectId = userIdManager.getProjectId() ?: run {
            Log.e("DataUploadWorker", "No project ID available")
            return false
        }

        for (gpsDataList in allData) {
            val batches = gpsDataList.data.chunked(100)
            for (batch in batches) {
                val batchDataList = GPSDataList(gpsDataList.userId, batch)
                if (!uploadBatch(batchDataList, projectId)) {
                    return false
                }
            }
        }

        return true
    }

    private suspend fun uploadBatch(batchDataList: GPSDataList, projectId: String): Boolean {
        var retries = 3
        while (retries > 0) {
            try {
                val uploadId = UUID.randomUUID().toString()
                val requestBody = createRequestBody(batchDataList.userId, uploadId, batchDataList.data, projectId)
                val request = createRequest(projectId, requestBody)

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("DataUploadWorker", "Batch uploaded successfully for user ${batchDataList.userId}")
                        return true
                    } else {
                        Log.w("DataUploadWorker", "Upload failed with status code: ${response.code()}")
                        Log.w("DataUploadWorker", "Response body: ${response.body()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataUploadWorker", "Error during upload", e)
            }
            retries--
            if (retries > 0) {
                delay(5000) // Wait 5 seconds before retrying
            }
        }
        return false
    }

    private fun createRequestBody(userId: String, uploadId: String, data: List<GPSData>, projectId: String): RequestBody {
        val userDataJson = JSONObject().apply {
            put("entries", JSONArray().apply {
                data.forEach { gpsData ->
                    put(JSONObject().apply {
                        put("longitude", gpsData.longitude)
                        put("latitude", gpsData.latitude)
                        put("timestamp", gpsData.timestamp)
                    })
                }
            })
        }

        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", generateNumericUserId(userId))
            .addFormDataPart("upload_id", uploadId)
            .addFormDataPart("user_data", userDataJson.toString())
            .build()
    }

    private fun generateNumericUserId(userId: String): String {
        return userId.hashCode().absoluteValue.toString()
    }

    private fun createRequest(projectId: String, requestBody: RequestBody): Request {
        return Request.Builder()
            .url("https://glowworm-known-raven.ngrok-free.app/project/$projectId/user_data")
            .post(requestBody)
            .build()
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "data_upload_channel",
                "Data Upload Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "data_upload_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}


sealed class UploadStatus {
    object Idle : UploadStatus()
    object Uploading : UploadStatus()
    object Success : UploadStatus()
    object Error : UploadStatus()
}