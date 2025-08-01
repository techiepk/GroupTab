package com.pennywiseai.tracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pennywiseai.tracker.ui.screens.HomeScreen
import com.pennywiseai.tracker.ui.screens.SettingsScreen
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

@Composable
fun PennyWiseNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Home,
        modifier = modifier
    ) {
        composable<Home> {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Settings)
                }
            )
        }
        
        composable<Settings> {
            SettingsScreen(
                themeViewModel = themeViewModel
            )
        }
        
        composable<Transactions> {
            // TODO: Implement TransactionsScreen
        }
        
        composable<Analytics> {
            // TODO: Implement AnalyticsScreen
        }
    }
}