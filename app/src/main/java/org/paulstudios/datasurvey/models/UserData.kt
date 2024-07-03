package org.paulstudios.datasurvey.models

data class UserData(
    val userId: String,
    val data: List<GPSData>
)
