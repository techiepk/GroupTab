package com.pennywiseai.tracker.presentation.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    viewModel: ManageAccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.accounts.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No accounts yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add an account to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(Dimensions.Padding.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Show hidden accounts toggle if any accounts are hidden
                if (uiState.hiddenAccounts.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimensions.Padding.content),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = "${uiState.hiddenAccounts.size} account(s) hidden",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                
                items(uiState.accounts) { account ->
                    AccountItem(
                        account = account,
                        isHidden = viewModel.isAccountHidden(account.bankName, account.accountLast4),
                        onToggleVisibility = {
                            viewModel.toggleAccountVisibility(account.bankName, account.accountLast4)
                        },
                        onUpdateBalance = {
                            selectedAccount = account.bankName to account.accountLast4
                            showUpdateDialog = true
                        },
                        onViewHistory = {
                            historyAccount = account.bankName to account.accountLast4
                            viewModel.loadBalanceHistory(account.bankName, account.accountLast4)
                            showHistoryDialog = true
                        }
                    )
                }
            }
        }
        
        // FAB positioned at bottom end
        FloatingActionButton(
            onClick = onNavigateToAddAccount,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimensions.Padding.content),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Account")
        }
    }
    
    // Update Balance Dialog
    if (showUpdateDialog && selectedAccount != null) {
        UpdateBalanceDialog(
            bankName = selectedAccount!!.first,
            accountLast4 = selectedAccount!!.second,
            onDismiss = {
                showUpdateDialog = false
                selectedAccount = null
            },
            onConfirm = { newBalance ->
                viewModel.updateAccountBalance(
                    selectedAccount!!.first,
                    selectedAccount!!.second,
                    newBalance
                )
                showUpdateDialog = false
                selectedAccount = null
            }
        )
    }
    
    // Balance History Dialog
    if (showHistoryDialog && historyAccount != null) {
        BalanceHistoryDialog(
            bankName = historyAccount!!.first,
            accountLast4 = historyAccount!!.second,
            balanceHistory = uiState.balanceHistory,
            onDismiss = {
                showHistoryDialog = false
                historyAccount = null
                viewModel.clearBalanceHistory()
            },
            onDeleteBalance = { id ->
                viewModel.deleteBalanceRecord(id, historyAccount!!.first, historyAccount!!.second)
            },
            onUpdateBalance = { id, newBalance ->
                viewModel.updateBalanceRecord(id, newBalance, historyAccount!!.first, historyAccount!!.second)
            }
        )
    }
}

@Composable
private fun AccountItem(
    account: com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity,
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHidden) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Account Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (account.creditLimit != null) {
                            Icons.Default.CreditCard
                        } else {
                            Icons.Default.AccountBalance
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = account.bankName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "••${account.accountLast4}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isHidden) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = "Hidden",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Show balance and credit info
                        if (account.creditLimit != null) {
                            val utilization = if (account.creditLimit > BigDecimal.ZERO) {
                                ((account.balance / account.creditLimit) * BigDecimal(100)).toInt()
                            } else 0
                            
                            Text(
                                text = "Credit: ${CurrencyFormatter.formatCurrency(account.balance)} / ${CurrencyFormatter.formatCurrency(account.creditLimit)} ($utilization%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Balance
                Text(
                    text = CurrencyFormatter.formatCurrency(account.balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Action Buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                OutlinedButton(
                    onClick = onUpdateBalance
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update")
                }
                
                OutlinedButton(
                    onClick = onViewHistory
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History")
                }
                
                OutlinedButton(
                    onClick = onToggleVisibility
                ) {
                    Icon(
                        imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHidden) "Show" else "Hide")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateBalanceDialog(
    bankName: String,
    accountLast4: String,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal) -> Unit
) {
    var balanceText by remember { mutableStateOf("") }
    var isValid by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Update Balance")
                Text(
                    text = "$bankName ••$accountLast4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            OutlinedTextField(
                value = balanceText,
                onValueChange = { text ->
                    if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
                        balanceText = text
                        isValid = text.isNotBlank() && text.toDoubleOrNull() != null
                    }
                },
                label = { Text("New Balance") },
                placeholder = { Text("0.00") },
                leadingIcon = {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    balanceText.toBigDecimalOrNull()?.let { balance ->
                        onConfirm(balance)
                    }
                },
                enabled = isValid
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}