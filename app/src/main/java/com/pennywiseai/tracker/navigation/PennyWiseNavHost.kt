package com.pennywiseai.tracker.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pennywiseai.tracker.ui.MainScreen
import com.pennywiseai.tracker.ui.screens.PermissionScreen
import com.pennywiseai.tracker.ui.screens.settings.SettingsScreen
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

@Composable
fun PennyWiseNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    startDestination: Any = Home
) {
    // Use a stable start destination
    val stableStartDestination = remember { startDestination }
    
    NavHost(
        navController = navController,
        startDestination = stableStartDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<Permission>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            PermissionScreen(
                onPermissionGranted = {
                    navController.navigate(Home) {
                        popUpTo(Permission) { inclusive = true }
                    }
                }
            )
        }
        composable<Home>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            MainScreen()
        }
        
        composable<Settings>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
    }
}