package com.pennywiseai.tracker.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalDensity
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
import com.pennywiseai.tracker.presentation.subscriptions.SubscriptionsScreen
import com.pennywiseai.tracker.presentation.transactions.TransactionsScreen
import com.pennywiseai.tracker.ui.components.PennyWiseBottomNavigation
import com.pennywiseai.tracker.ui.screens.settings.SettingsScreen
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Check if keyboard is visible
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val keyboardVisible = ime.getBottom(density) > 0
    
    // Handle chat screen separately to avoid keyboard issues
    if (currentRoute == "chat") {
        ChatScreenWrapper(
            navController = navController,
            themeViewModel = themeViewModel
        )
    } else {
        Scaffold(
        topBar = {
            Column {
                TopAppBar(
                title = { 
                    Text(
                        text = when (currentRoute) {
                            "home" -> "PennyWise"
                            "transactions" -> "Transactions"
                            "subscriptions" -> "Subscriptions"
                            "analytics" -> "Analytics"
                            "chat" -> "AI Assistant"
                            "settings" -> "Settings"
                            else -> "PennyWise"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                ),
                navigationIcon = {
                    if (currentRoute in listOf("settings", "subscriptions", "transactions", "chat")) {
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
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        },
        bottomBar = {
            // Show bottom navigation only for main screens
            if (currentRoute in listOf("home", "analytics")) {
                PennyWiseBottomNavigation(navController = navController)
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
                    },
                    onNavigateToTransactions = {
                        navController.navigate("transactions")
                    },
                    onNavigateToSubscriptions = {
                        navController.navigate("subscriptions")
                    },
                    onNavigateToChat = {
                        navController.navigate("chat")
                    }
                )
            }
            
            composable("transactions") {
                TransactionsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("subscriptions") {
                SubscriptionsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("analytics") {
                com.pennywiseai.tracker.ui.screens.analytics.AnalyticsScreen()
            }
            
            composable("chat") {
                // Empty composable - actual chat screen is handled separately
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenWrapper(
    navController: NavHostController,
    themeViewModel: ThemeViewModel
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("AI Assistant") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
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
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            com.pennywiseai.tracker.ui.screens.chat.ChatScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
    }
}