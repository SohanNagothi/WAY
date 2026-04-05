package com.example.way.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast

/**
 * Helper to request the user to disable battery optimization
 * so the foreground service runs reliably.
 */
object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestDisableBatteryOptimization(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) {
            Toast.makeText(context, "Battery optimization already disabled ✓", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open battery settings page
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Please disable battery optimization manually in Settings", Toast.LENGTH_LONG).show()
            }
        }
    }
}

