package com.pennywiseai.tracker

import android.app.Application
import android.os.Build
import com.google.android.material.color.DynamicColors
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.pennywiseai.tracker.firebase.FirebaseHelper

class PennyWiseApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // App follows system theme automatically via DayNight theme
        // Enable Material You Dynamic Colors (Android 12+)
        // Only apply if device supports it, otherwise use Material 3 defaults
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        
        // Initialize Firebase services
        FirebaseHelper.initialize(this)
        initializeFirebase()
        
        // Add global crash handler for debugging
        setupCrashHandler()
    }
    
    private fun initializeFirebase() {
        // Enable Firebase Analytics
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
        
        // Enable Firebase Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // Enable Firebase Performance Monitoring
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        
        // Set user properties for debugging
        FirebaseAnalytics.getInstance(this).apply {
            setUserProperty("app_version", "1.0")
            setUserProperty("device_type", "android")
            setUserProperty("build_type", if (com.pennywiseai.tracker.BuildConfig.DEBUG) "debug" else "release")
        }
    }
    
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                // Log to Firebase
                FirebaseHelper.logException(exception)
                FirebaseHelper.logDebugInfo("CRASH", "Uncaught exception in thread: ${thread.name}")
                FirebaseHelper.sendUnsentReports()
                
                // Log locally for debugging
                android.util.Log.e("CRASH", "Uncaught exception", exception)
                
                // Save crash to local file for debugging
                saveCrashToFile(exception)
            } catch (e: Exception) {
                android.util.Log.e("CRASH_HANDLER", "Error in crash handler", e)
            }
            
            // Call default handler
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    private fun saveCrashToFile(exception: Throwable) {
        try {
            val file = java.io.File(filesDir, "crash_log.txt")
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val stackTrace = android.util.Log.getStackTraceString(exception)
            
            file.appendText("\n--- CRASH $timestamp ---\n$stackTrace\n")
        } catch (e: Exception) {
            android.util.Log.e("CRASH_FILE", "Failed to save crash to file", e)
        }
    }
}