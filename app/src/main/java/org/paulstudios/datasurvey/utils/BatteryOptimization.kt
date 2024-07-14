package org.paulstudios.datasurvey.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

object BatteryOptimizationUtil {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        showBatteryOptimizationDialog(context)
    }

    private fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                openAppSettings(context)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening battery optimization settings: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Unable to open app settings. Please check battery optimization manually.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening app settings: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showBatteryOptimizationDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("Battery Optimization")
            .setMessage("To ensure proper data collection, please disable battery optimization for this app. Do you want to proceed?")
            .setPositiveButton("OK") { _, _ -> openBatteryOptimizationSettings(context) }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
