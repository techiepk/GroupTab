package com.pennywiseai.tracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import androidx.compose.ui.unit.dp

@Composable
fun AccountBalancesCard(
    accountBalances: List<AccountBalanceEntity>,
    totalBalance: BigDecimal,
    onViewAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayBalances = if (isExpanded || accountBalances.size <= 3) {
        accountBalances
    } else {
        accountBalances.take(3)
    }
    
    PennyWiseCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.content)
        ) {
            // Header with total balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Account Balances",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Account Balances",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                if (accountBalances.size > 3) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Show less" else "Show more"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Total balance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimensions.CornerRadius.medium))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(totalBalance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            if (accountBalances.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.md))
                HorizontalDivider(thickness = Dimensions.Component.dividerThickness)
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Individual account balances
                displayBalances.forEachIndexed { index, balance ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                    
                    AccountBalanceItem(balance = balance)
                }
                
                // View all button
                if (accountBalances.size > 3 && !isExpanded) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    TextButton(
                        onClick = onViewAllClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View all ${accountBalances.size} accounts")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(Spacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimensions.Padding.empty),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No account balances available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountBalanceItem(
    balance: AccountBalanceEntity,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.CornerRadius.small))
            .clickable { /* TODO: Navigate to account details */ }
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = balance.bankName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "••${balance.accountLast4}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Updated ${dateFormatter.format(balance.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = CurrencyFormatter.formatCurrency(balance.balance),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}