package com.pennywiseai.tracker.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.ui.theme.*
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun CreditCardsCard(
    creditCards: List<AccountBalanceEntity>,
    totalAvailableCredit: BigDecimal,
    onViewAllClick: () -> Unit = {},
    onCardClick: (bankName: String, accountLast4: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Log credit cards being displayed
    LaunchedEffect(creditCards) {
        Log.d("CreditCardsCard", "========================================")
        Log.d("CreditCardsCard", "Displaying ${creditCards.size} credit card(s) in UI:")
        creditCards.forEach { card ->
            Log.d("CreditCardsCard", """
                Credit Card - ${card.bankName} **${card.accountLast4}
                - Available Credit: ${card.creditLimit}
                - Outstanding: ${card.balance}
            """.trimIndent())
        }
        Log.d("CreditCardsCard", "========================================")
    }
    
    val displayCards = if (creditCards.size <= 5) {
        creditCards
    } else {
        creditCards.take(4)
    }

    PennyWiseCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.Padding.content)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = "Credit Cards",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Credit Cards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Total available credit
            Text(
                text = CurrencyFormatter.formatCurrency(totalAvailableCredit),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total Available Credit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (creditCards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                // Individual credit cards
                displayCards.forEach { card ->
                    CreditCardItem(
                        card = card,
                        onClick = {
                            onCardClick(card.bankName, card.accountLast4)
                        }
                    )
                }

                // View all link - only if more than 5 cards
                if (creditCards.size > 5) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "View all ${creditCards.size} credit cards →",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAllClick() }
                            .padding(vertical = Spacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditCardItem(
    card: AccountBalanceEntity,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.bankName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "••${card.accountLast4}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = CurrencyFormatter.formatCurrency(card.creditLimit ?: BigDecimal.ZERO),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (card.balance > BigDecimal.ZERO) {
                Text(
                    text = "Outstanding: ${CurrencyFormatter.formatCurrency(card.balance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Available",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}