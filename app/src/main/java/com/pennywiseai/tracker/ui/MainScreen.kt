package com.pennywiseai.tracker.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pennywiseai.tracker.navigation.Analytics
import com.pennywiseai.tracker.navigation.Chat
import com.pennywiseai.tracker.navigation.Home
import com.pennywiseai.tracker.navigation.Settings
import com.pennywiseai.tracker.navigation.Transactions
import com.pennywiseai.tracker.presentation.home.HomeScreen
import com.pennywiseai.tracker.presentation.home.HomeViewModel
import com.pennywiseai.tracker.ui.components.PennyWiseBottomNavigation
import com.pennywiseai.tracker.ui.screens.SettingsScreen
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Get HomeViewModel when on home screen for FAB
    val homeViewModel: HomeViewModel? = if (currentRoute == "home") {
        hiltViewModel()
    } else null
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when (currentRoute) {
                            "home" -> "PennyWise"
                            "transactions" -> "Transactions"
                            "analytics" -> "Analytics"
                            "chat" -> "AI Assistant"
                            "settings" -> "Settings"
                            else -> "PennyWise"
                        }
                    )
                },
                navigationIcon = {
                    if (currentRoute == "settings") {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentRoute != "settings") {
                        IconButton(
                            onClick = { 
                                navController.navigate("settings")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentRoute != "settings") {
                PennyWiseBottomNavigation(navController = navController)
            }
        },
        floatingActionButton = {
            // Show FAB only on home screen
            if (currentRoute == "home" && homeViewModel != null) {
                FloatingActionButton(
                    onClick = { homeViewModel.scanSmsMessages() }
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan SMS"
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("transactions") {
                // TODO: TransactionsScreen()
            }
            
            composable("analytics") {
                // TODO: AnalyticsScreen()
            }
            
            composable("chat") {
                // TODO: ChatScreen()
            }
            
            composable("settings") {
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}