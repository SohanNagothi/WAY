package com.example.way.util

/**
 * App-wide constants.
 */
object Constants {

    // Walk Session
    const val WALK_LOCATION_UPDATE_INTERVAL_MS = 5_000L   // 5 seconds
    const val WALK_FASTEST_LOCATION_INTERVAL_MS = 2_000L  // 2 seconds

    // Safety Thresholds
    const val INACTIVITY_THRESHOLD_SECONDS = 60L          // 1 minute without movement
    const val SPEED_THRESHOLD_MPS = 4.2f                  // ~15 km/h sustained indicates unusual movement
    const val SPEED_SUSTAINED_READINGS = 3
    const val FALL_MAGNITUDE_LOW = 2.0f                    // Free-fall threshold (m/s²)
    const val FALL_MAGNITUDE_HIGH = 20.0f                  // Impact threshold (m/s²)
    const val FALL_DETECTION_WINDOW_MS = 500L              // Time window for fall detection
    const val DISTANCE_INCREASING_COUNT = 4                // Consecutive readings before trigger
    const val DISTANCE_INCREASE_TOLERANCE_METERS = 20f
    const val MIN_MOVEMENT_METERS = 6f
    const val MIN_MOVEMENT_SPEED_MPS = 1.2f
    const val MOVEMENT_CONFIRMATION_READINGS = 2
    const val MIN_VALID_GPS_ACCURACY_METERS = 45f
    const val SAFETY_TRIGGER_COOLDOWN_MS = 45_000L

    // Safety Check
    const val SAFETY_CHECK_COUNTDOWN_SECONDS = 30
    const val MAX_CODE_ATTEMPTS = 2

    // Foreground Service
    const val WALK_SERVICE_NOTIFICATION_ID = 1001
    const val WALK_SERVICE_CHANNEL_ID = "walk_service_channel"
    const val EMERGENCY_NOTIFICATION_ID = 1002
    const val EMERGENCY_CHANNEL_ID = "emergency_channel"

    // SharedPreferences
    const val PREFS_NAME = "way_prefs"
    const val PREF_SETUP_COMPLETE = "setup_complete"
    const val PREF_USER_UID = "user_uid"
    const val PREF_USER_NAME = "user_name"
    const val PREF_EMERGENCY_CODE = "emergency_code"

    // Firestore Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CONTACTS = "contacts"
    const val COLLECTION_WALKS = "walks"
    const val COLLECTION_LOCATIONS = "locations"
    const val COLLECTION_EMERGENCIES = "emergencies"
}
