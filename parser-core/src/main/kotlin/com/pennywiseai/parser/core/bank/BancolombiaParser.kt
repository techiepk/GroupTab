package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for Bancolombia (Colombian bank) SMS messages
 *
 * Sender IDs: 87400, 85540
 * Language: Spanish
 * Currency: COP (Colombian Peso)
 *
 * Transaction types:
 * - Transferiste: Transfer (EXPENSE)
 * - Compraste: Purchase (EXPENSE)
 * - Pagaste: Payment (EXPENSE)
 * - Recibiste: Received (INCOME)
 */
class BancolombiaParser : BankParser() {

    override fun getBankName() = "Bancolombia"

    override fun canHandle(sender: String): Boolean {
        return sender == "87400" || sender == "85540"
    }

    override fun getCurrency() = "COP"

    override fun isTransactionMessage(message: String): Boolean {
        // Override base class to handle Spanish transaction keywords
        val lowerMessage = message.lowercase()
        val spanishKeywords = listOf(
            "transferiste", "compraste", "pagaste", "recibiste"
        )
        return spanishKeywords.any { lowerMessage.contains(it) }
    }

    override fun extractAmount(message: String): BigDecimal? {
        // Super flexible: Find ANY number after transaction keywords
        // Handles any format: "20.00", "30,000.00", "$50000", etc.
        val pattern = Regex("""(Transferiste|Compraste|Pagaste|Recibiste)\s+\$?([0-9.,]+)""", RegexOption.IGNORE_CASE)
        pattern.find(message)?.let { match ->
            // Clean up the number - remove commas and any non-digit except period
            val amount = match.groupValues[2]
                .replace(",", "")
                .replace("$", "")
                .trim()
            return try {
                BigDecimal(amount)
            } catch (e: Exception) {
                null
            }
        }

        return null
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lower = message.lowercase()
        return when {
            lower.contains("transferiste") -> TransactionType.EXPENSE
            lower.contains("compraste") -> TransactionType.EXPENSE
            lower.contains("pagaste") -> TransactionType.EXPENSE
            lower.contains("recibiste") -> TransactionType.INCOME
            else -> null
        }
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Simple: just return the transaction type in Spanish for now
        val lower = message.lowercase()
        return when {
            lower.contains("transferiste") -> "Transferencia"
            lower.contains("compraste") -> "Compra"
            lower.contains("pagaste") -> "Pago"
            lower.contains("recibiste") -> "Dinero recibido"
            else -> "Bancolombia"
        }
    }
}