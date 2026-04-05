package com.example.way.data.local

import android.content.SharedPreferences
import com.example.way.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around SharedPreferences for local key-value storage.
 * Provides typed accessors for common app settings.
 *
 * Hilt will auto-inject the SharedPreferences instance that we
 * defined in AppModule.
 */
@Singleton
class PrefsManager @Inject constructor(
    private val prefs: SharedPreferences
) {

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(Constants.PREF_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(Constants.PREF_SETUP_COMPLETE, value).apply()

    var userUid: String
        get() = prefs.getString(Constants.PREF_USER_UID, "") ?: ""
        set(value) = prefs.edit().putString(Constants.PREF_USER_UID, value).apply()

    var userName: String
        get() = prefs.getString(Constants.PREF_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(Constants.PREF_USER_NAME, value).apply()

    var emergencyCode: String
        get() = prefs.getString(Constants.PREF_EMERGENCY_CODE, "") ?: ""
        set(value) = prefs.edit().putString(Constants.PREF_EMERGENCY_CODE, value).apply()

    var themeMode: Int
        get() = prefs.getInt("theme_mode", -1) // -1 = MODE_NIGHT_FOLLOW_SYSTEM
        set(value) = prefs.edit().putInt("theme_mode", value).apply()

    // Local fail-safe: cache last walk session JSON in case internet fails
    var cachedWalkSessionJson: String
        get() = prefs.getString("cached_walk_session", "") ?: ""
        set(value) = prefs.edit().putString("cached_walk_session", value).apply()

    fun clearCachedWalkSession() {
        prefs.edit().remove("cached_walk_session").apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

