package com.pennywiseai.tracker.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.ui.components.BrandIcon
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter

@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val transaction by viewModel.transaction.collectAsStateWithLifecycle()
    
    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }
    
    PennyWiseScaffold(
        title = "Transaction Details",
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    ) { paddingValues ->
        transaction?.let { txn ->
            TransactionDetailContent(
                transaction = txn,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun TransactionDetailContent(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(Dimensions.Padding.content)
    ) {
        // Header with amount and merchant
        TransactionHeader(transaction)
        
        Spacer(modifier = Modifier.height(Spacing.lg))
        
        // SMS Body - Most Important
        if (!transaction.smsBody.isNullOrBlank()) {
            SmsBodyCard(transaction.smsBody)
            Spacer(modifier = Modifier.height(Spacing.md))
        }
        
        // Extracted Information
        ExtractedInfoCard(transaction)
        
        Spacer(modifier = Modifier.height(Spacing.md))
        
        // Additional Details
        if (transaction.balanceAfter != null || transaction.accountNumber != null) {
            AdditionalDetailsCard(transaction)
        }
    }
}

@Composable
private fun TransactionHeader(transaction: TransactionEntity) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Icon
            BrandIcon(
                merchantName = transaction.merchantName,
                size = 64.dp,
                showBackground = true
            )
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Merchant Name
            Text(
                text = transaction.merchantName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            // Amount with sign
            val amountColor = when (transaction.transactionType) {
                TransactionType.INCOME -> Color(0xFF4CAF50)
                TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
            }
            val sign = when (transaction.transactionType) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE -> "-"
            }
            
            Text(
                text = "$sign${CurrencyFormatter.formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            
            // Date and Time
            Text(
                text = transaction.dateTime.format(
                    DateTimeFormatter.ofPattern("MMMM d, yyyy • h:mm a")
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmsBodyCard(smsBody: String) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Message,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Original SMS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // SMS text in monospace font
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = smsBody,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(Spacing.md)
                )
            }
        }
    }
}

@Composable
private fun ExtractedInfoCard(transaction: TransactionEntity) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Extracted Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Category
            InfoRow(
                label = "Category",
                value = transaction.category,
                icon = Icons.Default.Category
            )
            
            // Bank
            transaction.bankName?.let {
                InfoRow(
                    label = "Bank",
                    value = it,
                    icon = Icons.Default.AccountBalance
                )
            }
            
            // Transaction Type
            InfoRow(
                label = "Type",
                value = transaction.transactionType.name.lowercase().capitalize(),
                icon = if (transaction.transactionType == TransactionType.INCOME) 
                    Icons.Default.TrendingUp else Icons.Default.TrendingDown
            )
            
            // Description
            transaction.description?.let {
                InfoRow(
                    label = "Description",
                    value = it,
                    icon = Icons.Default.Description
                )
            }
            
            // Recurring status
            if (transaction.isRecurring) {
                InfoRow(
                    label = "Status",
                    value = "Recurring Transaction",
                    icon = Icons.Default.Repeat
                )
            }
        }
    }
}

@Composable
private fun AdditionalDetailsCard(transaction: TransactionEntity) {
    PennyWiseCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Additional Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Account Number (masked)
            transaction.accountNumber?.let {
                val masked = if (it.length > 4) {
                    "*".repeat(it.length - 4) + it.takeLast(4)
                } else it
                InfoRow(
                    label = "Account",
                    value = masked,
                    icon = Icons.Default.CreditCard
                )
            }
            
            // Balance After
            transaction.balanceAfter?.let {
                InfoRow(
                    label = "Balance After",
                    value = CurrencyFormatter.formatCurrency(it),
                    icon = Icons.Default.AccountBalanceWallet
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}