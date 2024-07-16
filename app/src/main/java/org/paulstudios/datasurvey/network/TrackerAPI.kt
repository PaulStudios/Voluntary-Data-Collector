package org.paulstudios.datasurvey.network

import okhttp3.RequestBody
import org.paulstudios.datasurvey.data.models.GPSDataList
import org.paulstudios.datasurvey.data.models.Project
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface TrackerAPI {
    @GET("/project/{project_id}")
    suspend fun getProjectDetails(@Path("project_id") projectId: String): Response<Project>

    @Multipart
    @POST("/project/{project_id}/user_data")
    suspend fun uploadUserData(
        @Path("project_id") projectId: String,
        @Part("user_id") userId: RequestBody,
        @Part("upload_id") uploadId: RequestBody,
        @Part("user_data") userData: RequestBody
    ): Response<Unit>

    @GET("/status")
    suspend fun getServerStatus(): Response<Unit>
}

