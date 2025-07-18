package com.pennywiseai.tracker.viewmodel

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.AppSettings
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.firebase.FirebaseHelper
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    
    fun trackOnboardingStart() {
        // Track onboarding analytics
        val bundle = Bundle().apply {
            putLong("timestamp", System.currentTimeMillis())
            putString("app_version", getAppVersion())
        }
        FirebaseHelper.logEvent("onboarding_started", bundle)
    }
    
    fun trackOnboardingPageView(pageName: String, position: Int) {
        // Track page views in onboarding
        val bundle = Bundle().apply {
            putString("page_name", pageName)
            putInt("page_position", position)
            putLong("timestamp", System.currentTimeMillis())
        }
        FirebaseHelper.logEvent("onboarding_page_view", bundle)
    }
    
    fun trackOnboardingSkipped(atPosition: Int) {
        // Track when users skip onboarding
        val bundle = Bundle().apply {
            putInt("skipped_at_position", atPosition)
            putLong("timestamp", System.currentTimeMillis())
        }
        FirebaseHelper.logEvent("onboarding_skipped", bundle)
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                // Save to database
                val existingSettings = repository.getSettingsSync()
                val updatedSettings = if (existingSettings != null) {
                    existingSettings.copy(
                        hasCompletedOnboarding = true,
                        onboardingCompletedAt = System.currentTimeMillis()
                    )
                } else {
                    AppSettings(
                        hasCompletedOnboarding = true,
                        onboardingCompletedAt = System.currentTimeMillis()
                    )
                }
                
                repository.insertSettings(updatedSettings)
                
                // Track completion analytics
                val bundle = Bundle().apply {
                    putLong("completion_timestamp", System.currentTimeMillis())
                    putString("app_version", getAppVersion())
                }
                FirebaseHelper.logEvent("onboarding_completed", bundle)
                
            } catch (e: Exception) {
                // Log error but don't fail onboarding
                FirebaseHelper.logException(e)
            }
        }
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}