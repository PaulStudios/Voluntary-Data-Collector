package org.paulstudios.datasurvey.data.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.paulstudios.datasurvey.data.models.GPSDataList
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JsonStorage(private val context: Context) {
    private val json = Json { prettyPrint = true }
    private val logFileName = "gpsdatalog.json"

    suspend fun saveGPSDataList(gpsDataList: GPSDataList): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            var uniqueFileName = gpsDataList.fileName
            val file = File(context.filesDir, "gpsdata_$uniqueFileName.json")

            file.writeText(json.encodeToString(gpsDataList))
            Log.d("JsonStorage", "Saved GPS data to $uniqueFileName")
            saveLogEntry(uniqueFileName)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("JsonStorage", "Error saving GPS data", e)
            Result.failure(e)
        }
    }

    private fun saveLogEntry(fileName: String) {
        val logFile = File(context.filesDir, logFileName)
        val tempFileName = "temp_$logFileName"
        val tempFile = File(context.filesDir, tempFileName)
        val currentTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = mapOf("fileName" to fileName, "timestamp" to currentTimestamp)

        try {
            val existingLogEntries = if (logFile.exists()) {
                loadLogEntries().getOrDefault(emptyList()).toMutableList()
            } else {
                mutableListOf()
            }
            existingLogEntries.add(logEntry)

            tempFile.writeText(json.encodeToString(existingLogEntries))

            if (logFile.exists()) {
                logFile.delete()
            }
            tempFile.renameTo(logFile)
            Log.d("JsonStorage", "Saved log entry: $logEntry")
        } catch (e: Exception) {
            Log.e("JsonStorage", "Error saving log entry", e)
        }
    }

    private fun loadLogEntries(): Result<List<Map<String, String>>> {
        return try {
            val logFile = File(context.filesDir, logFileName)
            if (logFile.exists()) {
                val content = logFile.readText()
                val logcontent = json.decodeFromString<List<Map<String, String>>>(content)
                Log.d("JsonStorage", "Loaded log entries: $logcontent")
                Result.success(json.decodeFromString(content))
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e("JsonStorage", "Error loading log entries", e)
            Result.failure(e)
        }
    }

    suspend fun loadAllGPSData(): Result<List<GPSDataList>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val allFiles = context.filesDir.listFiles()?.filter { it.name.startsWith("gpsdata_") && it.name.endsWith(".json") }
            val allGPSData = allFiles?.map { file ->
                val content = file.readText()
                json.decodeFromString<GPSDataList>(content)
            } ?: emptyList()

            Result.success(allGPSData)
        } catch (e: Exception) {
            Log.e("JsonStorage", "Error loading GPS data", e)
            Result.failure(e)
        }
    }

    suspend fun loadSingleGPSDataFile(fileName: String): Result<GPSDataList> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(context.filesDir, "gpsdata_$fileName.json")
            if (file.exists()) {
                val content = file.readText()
                val gpsDataList = json.decodeFromString<GPSDataList>(content)
                Result.success(gpsDataList)
            } else {
                Result.failure(FileNotFoundException("File $fileName not found"))
            }
        } catch (e: Exception) {
            Log.e("JsonStorage", "Error loading single GPS data file", e)
            Result.failure(e)
        }
    }

    fun getLogEntries(): Result<List<Map<String, String>>> {
        return loadLogEntries()
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".json")) {
                    file.delete()
                }
            }
        }
    }
}
