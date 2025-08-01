package com.pennywiseai.tracker

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.pennywiseai.tracker.navigation.Home
import com.pennywiseai.tracker.navigation.Permission
import com.pennywiseai.tracker.navigation.PennyWiseNavHost
import com.pennywiseai.tracker.ui.theme.PennyWiseTheme
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

@Composable
fun PennyWiseApp(
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val darkTheme = themeUiState.isDarkTheme ?: isSystemInDarkTheme()
    
    // Check if SMS permission is granted
    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED
    
    // Check if user has previously skipped the permission
    val hasSkippedPermission = themeUiState.hasSkippedSmsPermission
    
    // Determine start destination based on permission status
    val startDestination = if (hasSmsPermission || hasSkippedPermission) Home else Permission
    
    PennyWiseTheme(
        darkTheme = darkTheme,
        dynamicColor = themeUiState.isDynamicColorEnabled
    ) {
        val navController = rememberNavController()
        
        PennyWiseNavHost(
            navController = navController,
            themeViewModel = themeViewModel,
            startDestination = startDestination
        )
    }
}