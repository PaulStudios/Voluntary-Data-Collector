package org.paulstudios.datasurvey.viewmodels

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.remember
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import org.paulstudios.datasurvey.data.storage.UserIdManager
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

    private val jsonStorage = JsonStorage(application)
    private val userIdManager = UserIdManager(application)

    private val workManager = WorkManager.getInstance(application)

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress

    fun uploadData() {
        viewModelScope.launch {
            _uploadStatus.value = UploadStatus.Uploading
            _uploadProgress.value = 0f

            val uploadWorkRequest = OneTimeWorkRequestBuilder<DataUploadWorker>()
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            workManager.enqueue(uploadWorkRequest)

            val workInfoLiveData = workManager.getWorkInfoByIdLiveData(uploadWorkRequest.id)
            workInfoLiveData.asFlow().collect { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getFloat("progress", 0f)
                        _uploadProgress.value = progress
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val outputData = workInfo.outputData
                        if (outputData.getString("status") == "NO_DATA") {
                            _uploadStatus.value = UploadStatus.NoData
                        } else {
                            _uploadStatus.value = UploadStatus.Success
                            loadStoredData()
                        }
                        _uploadProgress.value = 1f
                    }
                    WorkInfo.State.FAILED -> _uploadStatus.value = UploadStatus.Error
                    WorkInfo.State.CANCELLED -> _uploadStatus.value = UploadStatus.Idle
                    else -> {} // Do nothing for other states
                }
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

    fun cleanCollector(context: Context) {
        viewModelScope.launch {
            stopDataCollection(context)
            jsonStorage.clearAllData()
            userIdManager.deleteProjectId()
            _status.value="Cleaning Complete"
            Log.d("GPSDataCollection", "Cleaned")
        }
    }

    suspend fun loadStoredData(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = jsonStorage.loadAllGPSData()
                if (result.isSuccess) {
                    _storedData.value = result.getOrThrow()
                    true
                } else {
                    _error.value = "Error loading data: ${result.exceptionOrNull()?.message}"
                    false
                }
            } catch (e: Exception) {
                _error.value = "Error loading data: ${e.message}"
                false
            }
        }
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
            val logEntries = jsonStorage.getLogEntries().getOrThrow()
            if (logEntries.isEmpty()) {
                Log.d("DataUploadWorker", "No data to upload")
                return Result.success(workDataOf("status" to "NO_DATA"))
            }
            Log.d("DataUploadWorker", "Uploading ${logEntries.size} files")

            var uploadedFiles = 0
            for (entry in logEntries) {
                val fileName = entry["fileName"] ?: continue
                val result = uploadSingleFile(fileName)
                if (result) {
                    uploadedFiles++
                    val progress = uploadedFiles.toFloat() / logEntries.size
                    setProgress(workDataOf("progress" to progress))
                } else {
                    break
                }
            }

            if (uploadedFiles == logEntries.size) {
                jsonStorage.clearAllData()
                showNotification("Data Upload Successful", "All data has been uploaded successfully.")
                Result.success()
            } else {
                showNotification("Data Upload Incomplete", "Some files couldn't be uploaded. The process will retry later.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("DataUploadWorker", "Error during upload", e)
            showNotification("Data Upload Failed", "There was an error uploading the data. Please try again later.")
            Result.failure()
        }
    }

    private suspend fun uploadSingleFile(fileName: String): Boolean {
        val projectId = userIdManager.getProjectId() ?: run {
            Log.e("DataUploadWorker", "No project ID available")
            return false
        }

        val gpsDataListResult = jsonStorage.loadSingleGPSDataFile(fileName)
        if (gpsDataListResult.isFailure) {
            Log.e("DataUploadWorker", "Failed to load file $fileName")
            return false
        }
        val uploadId = UUID.randomUUID().toString()
        val gpsDataList = gpsDataListResult.getOrThrow()
        return uploadDataInBatches(listOf(gpsDataList), projectId, uploadId)
    }

    private suspend fun uploadDataInBatches(allData: List<GPSDataList>, projectId: String, uploadId: String): Boolean {
        for (gpsDataList in allData) {
            val batches = gpsDataList.data.chunked(100)
            Log.d("DataUploadWorker", "Uploading ${batches.size} batches for user ${gpsDataList.userId} with uploadId $uploadId")
            val filename = gpsDataList.fileName
            for (batch in batches) {
                val batchDataList = GPSDataList(gpsDataList.userId, batch, filename)
                if (!uploadBatch(batchDataList, projectId, uploadId)) {
                    return false
                }
            }
        }
        return true
    }

    private suspend fun uploadBatch(batchDataList: GPSDataList, projectId: String, uploadId: String): Boolean {
        var retries = 3
        while (retries > 0) {
            try {
                val requestBody = createRequestBody(
                    batchDataList.userId,
                    uploadId,
                    batchDataList.data
                )
                val request = createRequest(projectId, requestBody)

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("DataUploadWorker", "Batch uploaded successfully for user ${batchDataList.userId} with uploadId $uploadId")
                        return true
                    } else {
                        Log.w("DataUploadWorker", "Upload failed with status code: ${response.code()} for uploadId $uploadId")
                        Log.w("DataUploadWorker", "Response body: ${response.body()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("DataUploadWorker", "Error during upload for uploadId $uploadId", e)
            }
            retries--
            if (retries > 0) {
                delay(5000) // Wait 5 seconds before retrying
            }
        }
        return false
    }

    private fun createRequestBody(userId: String, uploadId: String, data: List<GPSData>): RequestBody {
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

        val channel = NotificationChannel(
            "data_upload_channel",
            "Data Upload Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

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
    object NoData : UploadStatus()
}