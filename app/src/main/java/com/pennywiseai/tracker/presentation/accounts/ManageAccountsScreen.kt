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
import androidx.compose.ui.graphics.Color
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
    var selectedAccountEntity by remember { mutableStateOf<com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity?>(null) }
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
                
                // Separate regular accounts and credit cards
                val regularAccounts = uiState.accounts.filter { !it.isCreditCard }
                val creditCards = uiState.accounts.filter { it.isCreditCard }
                
                // Regular Bank Accounts Section
                if (regularAccounts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Bank Accounts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = Spacing.xs)
                        )
                    }
                    
                    items(regularAccounts) { account ->
                        AccountItem(
                            account = account,
                            isHidden = viewModel.isAccountHidden(account.bankName, account.accountLast4),
                            onToggleVisibility = {
                                viewModel.toggleAccountVisibility(account.bankName, account.accountLast4)
                            },
                            onUpdateBalance = {
                                selectedAccount = account.bankName to account.accountLast4
                                selectedAccountEntity = account
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
                
                // Credit Cards Section
                if (creditCards.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = "Credit Cards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = Spacing.xs)
                        )
                    }
                    
                    items(creditCards) { card ->
                        CreditCardItem(
                            card = card,
                            isHidden = viewModel.isAccountHidden(card.bankName, card.accountLast4),
                            onToggleVisibility = {
                                viewModel.toggleAccountVisibility(card.bankName, card.accountLast4)
                            },
                            onUpdateBalance = {
                                selectedAccount = card.bankName to card.accountLast4
                                selectedAccountEntity = card
                                showUpdateDialog = true
                            },
                            onViewHistory = {
                                historyAccount = card.bankName to card.accountLast4
                                viewModel.loadBalanceHistory(card.bankName, card.accountLast4)
                                showHistoryDialog = true
                            }
                        )
                    }
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
    if (showUpdateDialog && selectedAccount != null && selectedAccountEntity != null) {
        if (selectedAccountEntity!!.isCreditCard) {
            // Credit Card Update Dialog
            UpdateCreditCardDialog(
                bankName = selectedAccount!!.first,
                accountLast4 = selectedAccount!!.second,
                currentOutstanding = selectedAccountEntity!!.balance,
                currentLimit = selectedAccountEntity!!.creditLimit ?: BigDecimal.ZERO,
                onDismiss = {
                    showUpdateDialog = false
                    selectedAccount = null
                    selectedAccountEntity = null
                },
                onConfirm = { newBalance, newLimit ->
                    viewModel.updateCreditCard(
                        selectedAccount!!.first,
                        selectedAccount!!.second,
                        newBalance,
                        newLimit
                    )
                    showUpdateDialog = false
                    selectedAccount = null
                    selectedAccountEntity = null
                }
            )
        } else {
            // Regular Account Update Dialog
            UpdateBalanceDialog(
                bankName = selectedAccount!!.first,
                accountLast4 = selectedAccount!!.second,
                onDismiss = {
                    showUpdateDialog = false
                    selectedAccount = null
                    selectedAccountEntity = null
                },
                onConfirm = { newBalance ->
                    viewModel.updateAccountBalance(
                        selectedAccount!!.first,
                        selectedAccount!!.second,
                        newBalance
                    )
                    showUpdateDialog = false
                    selectedAccount = null
                    selectedAccountEntity = null
                }
            )
        }
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
private fun CreditCardItem(
    card: com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity,
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit
) {
    val available = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
    val utilization = if (card.creditLimit != null && card.creditLimit > BigDecimal.ZERO) {
        ((card.balance.toDouble() / card.creditLimit.toDouble()) * 100).toInt()
    } else {
        0
    }
    
    val utilizationColor = when {
        utilization > 70 -> MaterialTheme.colorScheme.error
        utilization > 30 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFF4CAF50) // Green
    }
    
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
            // Credit Card Header
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
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = card.bankName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "••${card.accountLast4}",
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
                    }
                }
            }
            
            // Credit Card Details
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                // Outstanding Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Outstanding",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(card.balance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (card.balance > BigDecimal.ZERO) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                
                // Available Credit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(available),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Credit Limit with Utilization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Credit Limit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CurrencyFormatter.formatCurrency(card.creditLimit ?: BigDecimal.ZERO),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "($utilization% used)",
                            style = MaterialTheme.typography.bodySmall,
                            color = utilizationColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
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
                        imageVector = Icons.Default.AccountBalance,
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
                        Text(
                            text = "Account Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateCreditCardDialog(
    bankName: String,
    accountLast4: String,
    currentOutstanding: BigDecimal,
    currentLimit: BigDecimal,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, BigDecimal) -> Unit
) {
    var outstandingText by remember { mutableStateOf(currentOutstanding.toString()) }
    var limitText by remember { mutableStateOf(currentLimit.toString()) }
    var isValid by remember { mutableStateOf(false) }
    
    LaunchedEffect(outstandingText, limitText) {
        isValid = outstandingText.isNotBlank() && 
                  outstandingText.toDoubleOrNull() != null &&
                  limitText.isNotBlank() && 
                  limitText.toDoubleOrNull() != null
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Update Credit Card")
                Text(
                    text = "$bankName ••$accountLast4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                OutlinedTextField(
                    value = outstandingText,
                    onValueChange = { text ->
                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
                            outstandingText = text
                        }
                    },
                    label = { Text("Outstanding Balance") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                    },
                    supportingText = {
                        Text("Amount currently owed on the card")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { text ->
                        if (text.isEmpty() || text.matches(Regex("^\\d*\\.?\\d*$"))) {
                            limitText = text
                        }
                    },
                    label = { Text("Credit Limit") },
                    placeholder = { Text("50000.00") },
                    leadingIcon = {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                    },
                    supportingText = {
                        Text("Total credit limit of the card")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Show available credit
                val outstanding = outstandingText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val limit = limitText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (limit > BigDecimal.ZERO) {
                    val available = limit - outstanding
                    val utilization = ((outstanding.toDouble() / limit.toDouble()) * 100).toInt()
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Available Credit:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = CurrencyFormatter.formatCurrency(available),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Utilization:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "$utilization%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        utilization > 70 -> MaterialTheme.colorScheme.error
                                        utilization > 30 -> Color(0xFFFF9800)
                                        else -> Color(0xFF4CAF50)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val outstanding = outstandingText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val limit = limitText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onConfirm(outstanding, limit)
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