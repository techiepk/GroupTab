package com.pennywiseai.tracker.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
    
    // Handle chat screen separately to avoid keyboard issues
    if (currentRoute == "chat") {
        ChatScreenWrapper(
            navController = navController
        )
    } else {
        Scaffold(
        topBar = {
            PennyWiseTopAppBar(
                title = when (currentRoute) {
                    "home" -> "PennyWise"
                    "transactions" -> "Transactions"
                    "subscriptions" -> "Subscriptions"
                    "analytics" -> "Analytics"
                    "settings" -> "Settings"
                    else -> "PennyWise"
                },
                showBackButton = currentRoute in listOf("settings", "subscriptions", "transactions"),
                showSettingsButton = currentRoute != "settings",
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") }
            )
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
                // Empty composable - chat screen is rendered separately in ChatScreenWrapper
                // to avoid keyboard handling issues with edge-to-edge display
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
    navController: NavHostController
) {
    Scaffold(
        topBar = {
            PennyWiseTopAppBar(
                title = "AI Assistant",
                showBackButton = true,
                showSettingsButton = true,
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PennyWiseTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    showSettingsButton: Boolean = true,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column {
        TopAppBar(
            title = { Text(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
            ),
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = {
                if (showSettingsButton) {
                    IconButton(onClick = onSettingsClick) {
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
}