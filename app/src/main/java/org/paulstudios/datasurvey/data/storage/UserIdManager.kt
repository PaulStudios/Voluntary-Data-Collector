package org.paulstudios.datasurvey.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserIdManager(private val context: Context) {
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val PROJECT_ID_KEY = stringPreferencesKey("project_id")

    suspend fun getUserId(): String {
        val userId = context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY] ?: ""
        }.first()

        if (userId.isEmpty()) {
            val newUserId = UUID.randomUUID().toString()
            context.dataStore.edit { preferences ->
                preferences[USER_ID_KEY] = newUserId
            }
            return newUserId
        }

        return userId
    }

    suspend fun saveProjectId(projectId: String) {
        context.dataStore.edit { preferences ->
            preferences[PROJECT_ID_KEY] = projectId
        }
    }

    suspend fun getProjectId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[PROJECT_ID_KEY]
        }.first()
    }
}