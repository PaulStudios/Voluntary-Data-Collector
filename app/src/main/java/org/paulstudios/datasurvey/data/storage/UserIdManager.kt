package org.paulstudios.datasurvey.data.storage

import android.content.Context
import java.util.*

class UserIdManager(private val context: Context) {
    private val preferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getUserId(): String {
        var userId = preferences.getString("user_id", null)
        if (userId == null) {
            userId = generateRandomUserId()
            preferences.edit().putString("user_id", userId).apply()
        }
        return userId
    }

    private fun generateRandomUserId(): String {
        return (1..15).map { (0..9).random() }.joinToString("")
    }
}
