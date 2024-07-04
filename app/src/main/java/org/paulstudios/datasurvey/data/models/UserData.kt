package org.paulstudios.datasurvey.data.models

import androidx.room.PrimaryKey

data class UserData(
    @PrimaryKey val userId: String,
    val data: List<GPSData>
)
