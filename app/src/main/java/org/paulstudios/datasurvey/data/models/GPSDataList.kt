package org.paulstudios.datasurvey.data.models

import kotlinx.serialization.Serializable

@Serializable
data class GPSDataList(
    val userId: String,
    val data: List<GPSData>,
    val fileName: String
)
