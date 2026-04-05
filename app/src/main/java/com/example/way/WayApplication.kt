package com.example.way

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.way.data.local.PrefsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for WAY.
 * @HiltAndroidApp triggers Hilt's code generation and serves as
 * the application-level dependency container.
 */
@HiltAndroidApp
class WayApplication : Application() {

    @Inject lateinit var prefsManager: PrefsManager

    override fun onCreate() {
        super.onCreate()

        // Restore saved theme preference
        val savedTheme = prefsManager.themeMode
        if (savedTheme != -1) {
            AppCompatDelegate.setDefaultNightMode(savedTheme)
        }

        // Temporary: log uncaught exceptions to help debug crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("WAY_CRASH", "Uncaught exception", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
