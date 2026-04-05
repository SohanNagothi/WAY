package com.example.way.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.way.R
import com.example.way.data.repository.ContactsRepository
import com.example.way.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository
) {

    companion object {
        private const val TAG = "EmergencyHandler"
    }

    suspend fun sendEmergencySms(latitude: Double, longitude: Double) {
        val contacts = try {
            contactsRepository.getContacts().first()
        } catch (_: Exception) {
            emptyList()
        }

        if (contacts.isEmpty()) {
            Log.w(TAG, "No emergency contacts to send SMS")
            return
        }

        val mapLink = "https://maps.google.com/?q=$latitude,$longitude"
        val message = "🚨 EMERGENCY ALERT from WAY App!\n" +
                "I may be in danger. My last known location:\n$mapLink\n" +
                "Please check on me immediately."

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "SMS permission not granted")
            return
        }

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        contacts.forEach { contact ->
            try {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(
                    contact.phone, null, parts, null, null
                )
                Log.d(TAG, "SMS sent to ${contact.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS to ${contact.name}", e)
            }
        }
    }

    suspend fun dialPriorityContact() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "CALL_PHONE permission not granted")
            return
        }

        val result = contactsRepository.getHighestPriorityContact()
        if (result is com.example.way.util.Result.Success) {
            val contact = result.data
            try {
                // Acquire wake lock to ensure call can be made
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "way:emergency_call"
                ).apply {
                    acquire(30 * 1000L)
                }

                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = "tel:${contact.phone}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                // For API 29+, set flags to show on lock screen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    callIntent.putExtra("android.intent.extra.PHONE_NUMBER", contact.phone)
                }

                context.startActivity(callIntent)
                Log.d(TAG, "Calling ${contact.name}")

                // Release wake lock after 30s (call should be established by then)
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (wakeLock.isHeld) wakeLock.release()
                    } catch (_: Exception) {}
                }, 30000)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to call ${contact.name}", e)
            }
        }
    }

    fun showEmergencyNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.EMERGENCY_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency alerts when safety triggers activate"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, Constants.EMERGENCY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("🚨 EMERGENCY MODE ACTIVE")
            .setContentText("Safety alert triggered. Contacts are being notified.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        nm.notify(Constants.EMERGENCY_NOTIFICATION_ID, notification)
    }

    fun clearEmergencyNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(Constants.EMERGENCY_NOTIFICATION_ID)
    }
}

