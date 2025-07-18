package com.pennywiseai.tracker.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.TransactionCategory

object ColorUtils {
    
    /**
     * Get the color resource for a transaction category.
     * These colors are defined in both light and dark mode variants.
     */
    fun getCategoryColorRes(category: TransactionCategory): Int {
        return when (category) {
            TransactionCategory.FOOD_DINING -> R.color.category_food_dining
            TransactionCategory.TRANSPORTATION -> R.color.category_transportation
            TransactionCategory.SHOPPING -> R.color.category_shopping
            TransactionCategory.ENTERTAINMENT -> R.color.category_entertainment
            TransactionCategory.BILLS_UTILITIES -> R.color.category_bills_utilities
            TransactionCategory.HEALTHCARE -> R.color.category_healthcare
            TransactionCategory.EDUCATION -> R.color.category_education
            TransactionCategory.TRAVEL -> R.color.category_travel
            TransactionCategory.GROCERIES -> R.color.category_groceries
            TransactionCategory.SUBSCRIPTION -> R.color.category_subscription
            TransactionCategory.INVESTMENT -> R.color.category_investment
            TransactionCategory.TRANSFER -> R.color.category_transfer
            else -> R.color.category_other
        }
    }
    
    /**
     * Get the actual color int for a transaction category.
     */
    fun getCategoryColor(context: Context, category: TransactionCategory): Int {
        return ContextCompat.getColor(context, getCategoryColorRes(category))
    }
    
    /**
     * Get the color for transaction amounts.
     * Returns appropriate color for income (positive) or expense (negative).
     */
    fun getTransactionAmountColorRes(amount: Double): Int {
        return if (amount >= 0) {
            R.color.transaction_income
        } else {
            R.color.transaction_expense
        }
    }
    
    /**
     * Get the actual color int for transaction amounts.
     */
    fun getTransactionAmountColor(context: Context, amount: Double): Int {
        return ContextCompat.getColor(context, getTransactionAmountColorRes(amount))
    }
}