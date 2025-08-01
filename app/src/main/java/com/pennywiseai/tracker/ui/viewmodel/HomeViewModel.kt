package com.pennywiseai.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.manager.SmsScanManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val smsScanManager: SmsScanManager
) : ViewModel() {
    
    /**
     * Starts SMS logging scan for testing.
     */
    fun startSmsLogging() {
        viewModelScope.launch {
            smsScanManager.startSmsLoggingScan()
        }
    }
}