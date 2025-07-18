package com.pennywiseai.tracker.utils

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.pennywiseai.tracker.R

object ThemeColorUtils {
    
    /**
     * Get theme color for income (positive amounts)
     */
    @ColorInt
    fun getIncomeColor(context: Context): Int {
        return getThemeColor(context, R.attr.colorIncome)
    }
    
    /**
     * Get theme color for expenses (negative amounts)
     */
    @ColorInt
    fun getExpenseColor(context: Context): Int {
        return getThemeColor(context, R.attr.colorExpense)
    }
    
    /**
     * Get theme color for income container/background
     */
    @ColorInt
    fun getIncomeContainerColor(context: Context): Int {
        return getThemeColor(context, R.attr.colorIncomeContainer)
    }
    
    /**
     * Get theme color for expense container/background
     */
    @ColorInt
    fun getExpenseContainerColor(context: Context): Int {
        return getThemeColor(context, R.attr.colorExpenseContainer)
    }
    
    /**
     * Get color based on amount (positive = income, negative = expense)
     */
    @ColorInt
    fun getColorForAmount(context: Context, amount: Double): Int {
        return if (amount >= 0) getIncomeColor(context) else getExpenseColor(context)
    }
    
    /**
     * Get container color based on amount
     */
    @ColorInt
    fun getContainerColorForAmount(context: Context, amount: Double): Int {
        return if (amount >= 0) getIncomeContainerColor(context) else getExpenseContainerColor(context)
    }
    
    /**
     * Get theme color for active status
     */
    @ColorInt
    fun getStatusActiveColor(context: Context): Int {
        return getThemeColor(context, R.attr.colorStatusActive)
    }
    
    /**
     * Get theme color for pending status
     */
    @ColorInt
    fun getStatusPendingColor(context: Context): Int {
        return getThemeColor(context, R.attr.colorStatusPending)
    }
    
    /**
     * Get theme color for inactive status
     */
    @ColorInt
    fun getStatusInactiveColor(context: Context): Int {
        return getThemeColor(context, R.attr.colorStatusInactive)
    }
    
    /**
     * Get theme color for error status
     */
    @ColorInt
    fun getStatusErrorColor(context: Context): Int {
        return getThemeColor(context, R.attr.colorStatusError)
    }
    
    /**
     * Generic method to get theme color from attribute
     */
    @ColorInt
    private fun getThemeColor(context: Context, @AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}