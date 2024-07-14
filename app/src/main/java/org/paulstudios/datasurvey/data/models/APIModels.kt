package org.paulstudios.datasurvey.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ProjectCreateRequest(
    val project_name: String,
    val project_description: String
)

@Serializable
data class UserDataUploadRequest(
    val userId: Int,
    val uploadId: String,
    val userData: GPSDataList
)

@Serializable
data class MessageResponse(
    val message: String
)