package org.paulstudios.datasurvey.data.storage

import android.content.Context
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.paulstudios.datasurvey.data.models.GPSDataList
import java.io.File

class JsonStorage(private val context: Context) {
    private val json = Json { prettyPrint = true }
    private val fileName = "gpsdata.json"

    fun saveGPSDataList(gpsDataList: GPSDataList): Result<Unit> {
        return try {
            val file = File(context.filesDir, fileName)
            val storedData = loadAllGPSData().getOrDefault(emptyList()).toMutableList()
            storedData.add(gpsDataList)
            file.writeText(json.encodeToString(storedData))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("JsonStorage", "Error saving GPS data", e)
            Result.failure(e)
        }
    }

    fun loadAllGPSData(): Result<List<GPSDataList>> {
        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val content = file.readText()
                Result.success(json.decodeFromString(content))
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Log.e("JsonStorage", "Error loading GPS data", e)
            Result.failure(e)
        }
    }
}
