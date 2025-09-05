package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun BalanceHistoryDialog(
    bankName: String,
    accountLast4: String,
    balanceHistory: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onDeleteBalance: (Long) -> Unit,
    onUpdateBalance: (Long, BigDecimal) -> Unit
) {
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf<Long?>(null) }
    var expandedSources by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimensions.Padding.content)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Balance History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$bankName ••$accountLast4",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                if (balanceHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No balance history available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Balance History List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(balanceHistory) { balance ->
                            val isLatest = balance == balanceHistory.first()
                            val isOnlyRecord = balanceHistory.size == 1
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLatest) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.md)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                                            ) {
                                                Text(
                                                    text = balance.timestamp.format(
                                                        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
                                                    ),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                if (isLatest) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = MaterialTheme.shapes.small,
                                                        modifier = Modifier.align(Alignment.Start)
                                                    ) {
                                                        Text(
                                                            text = "CURRENT",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.padding(
                                                                horizontal = 6.dp,
                                                                vertical = 2.dp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            if (editingId == balance.id) {
                                                // Edit mode
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = Spacing.sm),
                                                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                                                ) {
                                                    OutlinedTextField(
                                                        value = editingValue,
                                                        onValueChange = { value ->
                                                            if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                                                editingValue = value
                                                            }
                                                        },
                                                        keyboardOptions = KeyboardOptions(
                                                            keyboardType = KeyboardType.Decimal
                                                        ),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Default.CurrencyRupee,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    )
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                editingValue.toBigDecimalOrNull()?.let { newBalance ->
                                                                    onUpdateBalance(balance.id, newBalance)
                                                                    editingId = null
                                                                    editingValue = ""
                                                                }
                                                            },
                                                            enabled = editingValue.toBigDecimalOrNull() != null,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Save")
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                editingId = null
                                                                editingValue = ""
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Cancel")
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Display mode
                                                Text(
                                                    text = CurrencyFormatter.formatCurrency(balance.balance),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isLatest) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                            
                                            // Show source information
                                            Column(
                                                modifier = Modifier.padding(top = Spacing.xs),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                // Show source type
                                                val sourceTypeText = when (balance.sourceType) {
                                                    "TRANSACTION" -> "📱 From transaction SMS"
                                                    "SMS_BALANCE" -> "💬 From balance SMS"
                                                    "CARD_LINK" -> "💳 From linked card"
                                                    "MANUAL" -> "✏️ Manual entry"
                                                    else -> if (balance.transactionId != null) "📱 From SMS transaction" else null
                                                }
                                                
                                                sourceTypeText?.let {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                
                                                // Show SMS source if available
                                                balance.smsSource?.let { smsSource ->
                                                    val isExpanded = expandedSources.contains(balance.id)
                                                    Surface(
                                                        onClick = { 
                                                            expandedSources = if (isExpanded) {
                                                                expandedSources - balance.id
                                                            } else {
                                                                expandedSources + balance.id
                                                            }
                                                        },
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = MaterialTheme.shapes.small,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = if (isExpanded) {
                                                                "SMS: $smsSource"
                                                            } else {
                                                                "SMS: ${smsSource.take(60)}... (tap to expand)"
                                                            },
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(Spacing.xs),
                                                            maxLines = if (isExpanded) Int.MAX_VALUE else 2
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Action buttons
                                        if (editingId != balance.id && !isOnlyRecord) {
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        editingId = balance.id
                                                        editingValue = balance.balance.toPlainString()
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { showDeleteConfirmation = balance.id }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Info text
                Text(
                    text = "${balanceHistory.size} record(s) • Latest balance is shown in accounts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteConfirmation?.let { balanceId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Balance Record") },
            text = { Text("Are you sure you want to delete this balance record? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBalance(balanceId)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}