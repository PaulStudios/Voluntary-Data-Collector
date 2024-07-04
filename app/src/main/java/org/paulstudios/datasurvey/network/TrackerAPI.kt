package org.paulstudios.datasurvey.network

import org.paulstudios.datasurvey.models.Project
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface TrackerAPI {
    @GET("/project/{project_id}")
    suspend fun getProjectDetails(@Path("project_id") projectId: String): Response<Project>
}