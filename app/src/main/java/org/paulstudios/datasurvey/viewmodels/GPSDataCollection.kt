package org.paulstudios.datasurvey.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
import org.paulstudios.datasurvey.data.storage.UserIdManager
import org.paulstudios.datasurvey.network.LocationService
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class GPSDataCollection(application: Application,
                        private val serverStatusViewModel: ServerStatusViewModel
) : AndroidViewModel(application) {
    val _status = MutableStateFlow("Idle")
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

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unknown)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage: SharedFlow<String> = _statusMessage

    init {
        viewModelScope.launch {
            serverStatusViewModel.serverStatus.collect { isOnline ->
                _connectionStatus.value = if (isOnline) ConnectionStatus.Online else ConnectionStatus.Offline
            }
        }
    }

    init {
        checkServiceStatus()
    }

    init {
        Log.d("GPSDataCollection", "ViewModel initialized")
        registerReceivers(application)
    }

    private fun checkServiceStatus() {
        viewModelScope.launch {
            val isServiceRunning = isLocationServiceRunning()
            _status.value = if (isServiceRunning) "Collecting Data..." else "Idle"
        }
    }

    private fun isLocationServiceRunning(): Boolean {
        return getApplication<Application>().getSharedPreferences("LocationService", Context.MODE_PRIVATE)
            .getBoolean(LocationService.PREF_SERVICE_RUNNING, false)
    }

    private var serviceStopReceiver: BroadcastReceiver? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceivers(context: Context) {
        Log.d("GPSDataCollection", "Registering receivers")
        val filter = IntentFilter().apply {
            addAction("org.paulstudios.datasurvey.ACTION_LOCATION_SERVICE_STOPPED")
            addAction("org.paulstudios.datasurvey.ACTION_GPS_DISABLED")
        }

        serviceStopReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("GPSDataCollection", "Broadcast received: ${intent?.action}")
                when (intent?.action) {
                    "org.paulstudios.datasurvey.ACTION_LOCATION_SERVICE_STOPPED",
                    "org.paulstudios.datasurvey.ACTION_GPS_DISABLED" -> {
                        Log.d("GPSDataCollection", "Updating status to Idle")
                        _status.value = "Idle"
                        // Add any additional handling for GPS disabled state
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(serviceStopReceiver!!, filter)
        Log.d("GPSDataCollection", "Receivers registered")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("GPSDataCollection", "ViewModel cleared")
        unregisterReceivers(getApplication())
    }

    private fun unregisterReceivers(context: Context) {
        serviceStopReceiver?.let {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(it)
            serviceStopReceiver = null
        }
        Log.d("GPSDataCollection", "Receivers unregistered")
    }

    fun uploadData() {
        viewModelScope.launch {
            if (_connectionStatus.value != ConnectionStatus.Online) {
                _statusMessage.emit("No internet connection. Please check your network and try again.")
                Log.d("GPSData", "No internet connection. Please check your network and try again.")
                return@launch
            }

            _uploadStatus.value = UploadStatus.Uploading
            _uploadProgress.value = 0f

            val uploadWorkRequest = OneTimeWorkRequestBuilder<DataUploadWorker>()
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .setInputData(workDataOf("serverStatusViewModelId" to serverStatusViewModel.hashCode()))
                .build()

            workManager.enqueue(uploadWorkRequest)

            workManager.getWorkInfoByIdLiveData(uploadWorkRequest.id).asFlow().collect { workInfo ->
                Log.d("GPSDataCollection", "WorkInfo state: ${workInfo.state}")
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getFloat("progress", 0f)
                        _uploadProgress.value = progress
                        Log.d("GPSDataCollection", "Upload progress: $progress")
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val outputData = workInfo.outputData
                        if (outputData.getString("status") == "NO_DATA") {
                            _uploadStatus.value = UploadStatus.NoData
                            Log.d("GPSDataCollection", "Upload status: No data")
                        } else {
                            _uploadStatus.value = UploadStatus.Success
                            Log.d("GPSDataCollection", "Upload status: Success")
                            loadStoredData()
                        }
                        _uploadProgress.value = 1f
                    }
                    WorkInfo.State.FAILED -> {
                        _uploadStatus.value = UploadStatus.Error
                        Log.d("GPSDataCollection", "Upload status: Error")
                    }
                    WorkInfo.State.CANCELLED -> {
                        _uploadStatus.value = UploadStatus.Idle
                        Log.d("GPSDataCollection", "Upload status: Cancelled")
                    }
                    else -> {
                        Log.d("GPSDataCollection", "Upload status: ${workInfo.state}")
                    }
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val logEntries = jsonStorage.getLogEntries().getOrNull() ?: emptyList()
            if (logEntries.isEmpty()) {
                Log.d("DataUploadWorker", "No data to upload")
                return@withContext Result.success(workDataOf("status" to "NO_DATA"))
            }
            Log.d("DataUploadWorker", "Uploading ${logEntries.size} files")

            var uploadedBatches = 0
            var totalBatches = 0

            // Calculate total number of batches
            for (entry in logEntries) {
                val fileName = entry["fileName"] ?: continue
                val gpsDataListResult = jsonStorage.loadSingleGPSDataFile(fileName)
                if (gpsDataListResult.isSuccess) {
                    val gpsDataList = gpsDataListResult.getOrThrow()
                    totalBatches += (gpsDataList.data.size + 24) / 25 // Ceiling division by 25
                }
            }

            var allUploadsSuccessful = true
            for (entry in logEntries) {
                val fileName = entry["fileName"] ?: continue
                val result = uploadSingleFile(fileName) { batchesUploaded ->
                    uploadedBatches += batchesUploaded
                    val progress = uploadedBatches.toFloat() / totalBatches
                    setProgress(workDataOf("progress" to progress))
                }
                if (!result) {
                    allUploadsSuccessful = false
                    break
                }
            }

            if (allUploadsSuccessful) {
                Log.d("DataUploadWorker", "All uploads successful. Clearing data.")
                jsonStorage.clearAllData()
                showNotification("Data Upload Successful", "All data has been uploaded successfully.")
                return@withContext Result.success()
            } else {
                Log.d("DataUploadWorker", "Some uploads failed. Retrying later.")
                showNotification("Data Upload Incomplete", "Some batches couldn't be uploaded. The process will retry later.")
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            Log.e("DataUploadWorker", "Error during upload", e)
            showNotification("Data Upload Failed", "There was an error uploading the data. Please try again later.")
            return@withContext Result.failure()
        }
    }

    private suspend fun uploadSingleFile(fileName: String, onBatchUploaded: suspend (Int) -> Unit): Boolean {
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
        val result = uploadDataInBatches(listOf(gpsDataList), projectId, uploadId, onBatchUploaded)
        Log.d("DataUploadWorker", "Upload result for file $fileName: $result")
        return result
    }

    private suspend fun uploadDataInBatches(
        allData: List<GPSDataList>,
        projectId: String,
        uploadId: String,
        onBatchUploaded: suspend (Int) -> Unit
    ): Boolean {
        for (gpsDataList in allData) {
            val batches = gpsDataList.data.chunked(25)
            Log.d("DataUploadWorker", "Uploading ${batches.size} batches for user ${gpsDataList.userId} with uploadId $uploadId")
            val filename = gpsDataList.fileName
            for ((index, batch) in batches.withIndex()) {
                val batchDataList = GPSDataList(gpsDataList.userId, batch, filename)
                if (!uploadBatch(batchDataList, projectId, uploadId)) {
                    Log.e("DataUploadWorker", "Failed to upload batch ${index + 1}/${batches.size} for user ${gpsDataList.userId}")
                    return false
                }
                onBatchUploaded(1)
                Log.d("DataUploadWorker", "Uploaded batch ${index + 1}/${batches.size} for user ${gpsDataList.userId}")
            }
        }
        Log.d("DataUploadWorker", "All batches uploaded successfully for uploadId $uploadId")
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

                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    Log.d("DataUploadWorker", "Batch uploaded successfully for user ${batchDataList.userId} with uploadId $uploadId")
                    return true
                } else {
                    Log.w("DataUploadWorker", "Upload failed with status code: ${response.code()} for uploadId $uploadId")
                    Log.w("DataUploadWorker", "Response body: ${response.body()?.string()}")
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

enum class ConnectionStatus {
    Unknown, Online, Offline
}