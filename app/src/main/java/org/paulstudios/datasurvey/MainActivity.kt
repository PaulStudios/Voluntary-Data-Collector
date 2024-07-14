package org.paulstudios.datasurvey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import org.paulstudios.datasurvey.data.storage.JsonStorage
import org.paulstudios.datasurvey.ui.theme.DataSurveyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DataSurveyTheme {
                MyApp()
            }
        }
    }
}