package com.pennywiseai.tracker.utils

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    
    private val indianLocale = Locale("en", "IN")
    private val currencyFormat = NumberFormat.getCurrencyInstance(indianLocale)
    
    /**
     * Formats currency with Indian notation (K for thousands, L for lakhs, Cr for crores)
     * Examples:
     * 1234 -> ₹1.23K
     * 123456 -> ₹1.23L
     * 12345678 -> ₹1.23Cr
     * -1234 -> -₹1.23K
     */
    fun formatCompact(amount: Double): String {
        val absAmount = abs(amount)
        val prefix = if (amount < 0) "-" else ""
        
        return when {
            absAmount >= 10000000 -> { // Crores (1,00,00,000)
                "${prefix}₹${String.format("%.2f", absAmount / 10000000)}Cr"
            }
            absAmount >= 100000 -> { // Lakhs (1,00,000)
                "${prefix}₹${String.format("%.2f", absAmount / 100000)}L"
            }
            absAmount >= 1000 -> { // Thousands
                "${prefix}₹${String.format("%.1f", absAmount / 1000)}K"
            }
            else -> {
                // For small amounts, use regular formatting
                formatRegular(amount)
            }
        }
    }
    
    /**
     * Formats currency with full Indian notation
     * Examples:
     * 123456.78 -> ₹1,23,456.78
     * -123456.78 -> -₹1,23,456.78
     */
    fun formatRegular(amount: Double): String {
        val formatted = currencyFormat.format(abs(amount))
        return if (amount < 0) "-$formatted" else formatted
    }
    
    /**
     * Formats with trend indicator
     * Examples:
     * 1234, 15.5 -> ₹1.23K ↑15.5%
     * 1234, -10 -> ₹1.23K ↓10%
     */
    fun formatWithTrend(amount: Double, trendPercent: Double): String {
        val amountStr = formatCompact(amount)
        val trendStr = when {
            trendPercent > 0 -> " ↑${String.format("%.1f", trendPercent)}%"
            trendPercent < 0 -> " ↓${String.format("%.1f", abs(trendPercent))}%"
            else -> ""
        }
        return "$amountStr$trendStr"
    }
    
    /**
     * Formats for display with color hint
     * Returns pair of (formattedText, isPositive)
     */
    fun formatWithColor(amount: Double): Pair<String, Boolean> {
        val formatted = formatCompact(amount)
        val isPositive = amount >= 0
        return Pair(formatted, isPositive)
    }
}