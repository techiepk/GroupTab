package com.pennywiseai.tracker.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Chat
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.ui.components.SummaryCard
import com.pennywiseai.tracker.ui.components.ListItemCard
import com.pennywiseai.tracker.ui.components.SectionHeader
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.components.spotlightTarget
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onFabPositioned: (Rect) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content,
                bottom = Dimensions.Padding.content + 80.dp // Space for FAB
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Month Summary Card
            item {
                MonthSummaryCard(
                    monthTotal = uiState.currentMonthTotal,
                    monthlyChange = uiState.monthlyChange,
                    monthlyChangePercent = uiState.monthlyChangePercent
                )
            }
            
            // Upcoming Subscriptions Alert
            if (uiState.upcomingSubscriptions.isNotEmpty()) {
                item {
                    UpcomingSubscriptionsCard(
                        subscriptions = uiState.upcomingSubscriptions,
                        totalAmount = uiState.upcomingSubscriptionsTotal,
                        onClick = onNavigateToSubscriptions
                    )
                }
            }
            
            // Recent Transactions Section
            item {
                SectionHeader(
                    title = "Recent Transactions",
                    action = {
                        TextButton(onClick = onNavigateToTransactions) {
                            Text("View All")
                        }
                    }
                )
            }
            
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.Component.minTouchTarget * 2),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.recentTransactions.isEmpty()) {
                item {
                    PennyWiseCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.Padding.empty),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentTransactions) { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }
        
        // FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Chat FAB
            SmallFloatingActionButton(
                onClick = onNavigateToChat,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Open AI Assistant"
                )
            }
            
            // Sync FAB
            FloatingActionButton(
                onClick = { viewModel.scanSmsMessages() },
                modifier = Modifier.spotlightTarget(onFabPositioned)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync SMS"
                )
            }
        }
        
        // Scanning overlay
        AnimatedVisibility(
            visible = uiState.isScanning,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .padding(Dimensions.Padding.content)
                    .widthIn(max = 280.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Scanning SMS messages",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Looking for transactions...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(
    monthTotal: BigDecimal,
    monthlyChange: BigDecimal,
    monthlyChangePercent: Int
) {
    val isPositive = monthTotal >= BigDecimal.ZERO
    val displayAmount = if (isPositive) {
        "+${CurrencyFormatter.formatCurrency(monthTotal)}"
    } else {
        CurrencyFormatter.formatCurrency(monthTotal)
    }
    val amountColor = if (isPositive) {
        if (!isSystemInDarkTheme()) income_light else income_dark
    } else {
        if (!isSystemInDarkTheme()) expense_light else expense_dark
    }
    
    val subtitle = if (monthlyChange != BigDecimal.ZERO) {
        val isIncrease = monthlyChange > BigDecimal.ZERO
        val arrow = if (isIncrease) "↑" else "↓"
        "$arrow ${Math.abs(monthlyChangePercent)}% from last month"
    } else null
    
    SummaryCard(
        title = "This Month",
        amount = displayAmount,
        subtitle = subtitle,
        amountColor = amountColor
    )
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity
) {
    val amountText = "${if (transaction.transactionType == TransactionType.EXPENSE) "-" else "+"}${CurrencyFormatter.formatCurrency(transaction.amount)}"
    val amountColor = if (transaction.transactionType == TransactionType.EXPENSE) {
        if (!isSystemInDarkTheme()) expense_light else expense_dark
    } else {
        if (!isSystemInDarkTheme()) income_light else income_dark
    }
    
    ListItemCard(
        title = transaction.merchantName,
        subtitle = transaction.dateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
        amount = amountText,
        amountColor = amountColor,
        leadingContent = {
            BrandIcon(
                merchantName = transaction.merchantName,
                size = 40.dp,
                showBackground = true
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingSubscriptionsCard(
    subscriptions: List<SubscriptionEntity>,
    totalAmount: BigDecimal,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
                Column {
                    Text(
                        text = "${subscriptions.size} active subscriptions",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Monthly total: ${CurrencyFormatter.formatCurrency(totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = Dimensions.Alpha.subtitle)
                    )
                }
            }
            Text(
                text = "View",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}