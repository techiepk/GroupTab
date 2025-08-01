package com.pennywiseai.tracker.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimensions.Padding.content),
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
                        totalAmount = uiState.upcomingSubscriptionsTotal
                    )
                }
            }
            
            // Recent Transactions Section
            item {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
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
                    Card(
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
        
        // Scanning overlay
        AnimatedVisibility(
            visible = uiState.isScanning,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier.padding(Dimensions.Padding.content),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.Component.progressIndicatorSize),
                        strokeWidth = Constants.UI.PROGRESS_STROKE_WIDTH.dp
                    )
                    Text(
                        text = "Scanning SMS messages...",
                        style = MaterialTheme.typography.bodyMedium
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This Month",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Dimensions.Alpha.subtitle)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            val isPositive = monthTotal >= BigDecimal.ZERO
            val displayAmount = if (isPositive) {
                "+${formatCurrency(monthTotal)}"
            } else {
                formatCurrency(monthTotal) // Already has minus sign
            }
            val amountColor = if (isPositive) {
                MaterialTheme.colorScheme.primary // Green for savings
            } else {
                MaterialTheme.colorScheme.error // Red for net spending
            }
            
            Text(
                text = displayAmount,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            if (monthlyChange != BigDecimal.ZERO) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isIncrease = monthlyChange > BigDecimal.ZERO
                    val changeColor = if (isIncrease) {
                        MaterialTheme.colorScheme.error // Red for increased spending
                    } else {
                        MaterialTheme.colorScheme.primary // Green/Primary for decreased spending
                    }
                    
                    Text(
                        text = "${if (isIncrease) "↑" else "↓"} ${Math.abs(monthlyChangePercent)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = changeColor
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = "from last month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = Dimensions.Alpha.surface)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.merchantName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = transaction.dateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${if (transaction.transactionType == TransactionType.EXPENSE) "-" else "+"}${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.transactionType == TransactionType.EXPENSE) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatCurrency(amount: BigDecimal): String {
    // Using Locale.Builder for India locale instead of deprecated constructor
    val indiaLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()
    val formatter = NumberFormat.getCurrencyInstance(indiaLocale)
    // Show decimals only if they exist
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 2
    return formatter.format(amount)
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
                        text = "Monthly total: ${formatCurrency(totalAmount)}",
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