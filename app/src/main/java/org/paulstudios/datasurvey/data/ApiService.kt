package org.paulstudios.datasurvey.data

import org.paulstudios.datasurvey.models.Project
import org.paulstudios.datasurvey.models.UserData
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("projects/{projectId}")
    suspend fun getProject(@Path("projectId") projectId: String): Response<Project>

    @POST("projects/{projectId}/upload")
    suspend fun uploadUserData(
        @Path("projectId") projectId: String,
        @Body userData: UserData
    ): Response<Unit>

    companion object {
        private const val BASE_URL = "http://127.0.0.1:8000/"

        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}