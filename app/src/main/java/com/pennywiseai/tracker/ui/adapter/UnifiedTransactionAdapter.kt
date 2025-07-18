package com.pennywiseai.tracker.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.databinding.ItemTransactionBinding
import com.pennywiseai.tracker.databinding.ItemTransactionGroupHeaderBinding
import com.pennywiseai.tracker.utils.RecyclerViewOptimizer
import com.pennywiseai.tracker.utils.ThemeColorUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unified adapter that shows transactions with group headers inline
 */
class UnifiedTransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit = {},
    private val onTransactionLongClick: (Transaction) -> Unit = {},
    private val onGroupHeaderClick: (String, String) -> Unit = { _, _ -> } // groupId, groupName
) : ListAdapter<TransactionItem, RecyclerView.ViewHolder>(TransactionItemDiffCallback()) {

    companion object {
        private const val TYPE_GROUP_HEADER = 0
        private const val TYPE_TRANSACTION = 1
    }
    
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        
        // Apply performance optimizations
        RecyclerViewOptimizer.optimizeRecyclerView(recyclerView)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionItem.GroupHeader -> TYPE_GROUP_HEADER
            is TransactionItem.TransactionData -> TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GROUP_HEADER -> {
                val binding = ItemTransactionGroupHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                GroupHeaderViewHolder(binding)
            }
            TYPE_TRANSACTION -> {
                val binding = ItemTransactionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                TransactionViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionItem.GroupHeader -> {
                (holder as GroupHeaderViewHolder).bind(item, onGroupHeaderClick)
            }
            is TransactionItem.TransactionData -> {
                (holder as TransactionViewHolder).bind(item, onTransactionClick, onTransactionLongClick)
            }
        }
    }
    

    class GroupHeaderViewHolder(
        private val binding: ItemTransactionGroupHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            groupHeader: TransactionItem.GroupHeader,
            onGroupClick: (String, String) -> Unit
        ) {
            binding.apply {
                groupName.text = groupHeader.groupName
                transactionCount.text = "${groupHeader.transactionCount} transactions"
                totalAmount.text = "₹${String.format("%.0f", kotlin.math.abs(groupHeader.totalAmount))}"
                
                // Color code the total amount
                totalAmount.setTextColor(ThemeColorUtils.getColorForAmount(root.context, groupHeader.totalAmount))
                
                // Hide group type badge entirely
                groupType.visibility = View.GONE
                
                // Set category color indicator
                val categoryColor = getCategoryColor(groupHeader.category)
                categoryIconCard.setCardBackgroundColor(categoryColor)
                
                // Show arrow for clickable groups (not ungrouped)
                if (groupHeader.groupId == "ungrouped") {
                    expandArrow.visibility = View.GONE
                    root.isClickable = false
                    root.isFocusable = false
                } else {
                    expandArrow.visibility = View.VISIBLE
                    expandArrow.rotation = -90f // Point right to indicate navigation
                    
                    root.setOnClickListener {
                        onGroupClick(groupHeader.groupId, groupHeader.groupName)
                    }
                }
            }
        }
        
        
        private fun getCategoryColor(category: String): Int {
            val context = binding.root.context
            return when (category) {
                "FOOD_DINING" -> context.getColor(android.R.color.holo_orange_dark)
                "TRANSPORTATION" -> context.getColor(android.R.color.holo_blue_dark)
                "SHOPPING" -> context.getColor(android.R.color.holo_purple)
                "ENTERTAINMENT" -> context.getColor(android.R.color.holo_red_dark)
                "BILLS_UTILITIES" -> context.getColor(android.R.color.holo_green_dark)
                else -> context.getColor(android.R.color.darker_gray)
            }
        }
    }

    class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        private fun formatCategoryName(category: com.pennywiseai.tracker.data.TransactionCategory): String {
            return category.name
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
        }
        
        private fun getCategoryColor(category: com.pennywiseai.tracker.data.TransactionCategory): Int {
            val context = binding.root.context
            return when (category) {
                com.pennywiseai.tracker.data.TransactionCategory.FOOD_DINING -> context.getColor(com.pennywiseai.tracker.R.color.category_food)
                com.pennywiseai.tracker.data.TransactionCategory.TRANSPORTATION -> context.getColor(com.pennywiseai.tracker.R.color.category_transport)
                com.pennywiseai.tracker.data.TransactionCategory.SHOPPING -> context.getColor(com.pennywiseai.tracker.R.color.category_shopping)
                com.pennywiseai.tracker.data.TransactionCategory.ENTERTAINMENT -> context.getColor(com.pennywiseai.tracker.R.color.category_entertainment)
                com.pennywiseai.tracker.data.TransactionCategory.BILLS_UTILITIES -> context.getColor(com.pennywiseai.tracker.R.color.category_bills)
                else -> context.getColor(com.pennywiseai.tracker.R.color.category_other)
            }
        }

        fun bind(
            transactionItem: TransactionItem.TransactionData,
            onTransactionClick: (Transaction) -> Unit,
            onTransactionLongClick: (Transaction) -> Unit
        ) {
            val transaction = transactionItem.transaction
            val isGrouped = transactionItem.isGrouped
            
            binding.apply {
                merchantName.text = transaction.merchant
                transactionCategory.text = formatCategoryName(transaction.category)
                
                // Format amount with sign and color
                val absAmount = kotlin.math.abs(transaction.amount)
                if (transaction.amount >= 0) {
                    transactionAmount.text = "+${currencyFormat.format(absAmount)}"
                } else {
                    transactionAmount.text = currencyFormat.format(transaction.amount)
                }
                transactionAmount.setTextColor(ThemeColorUtils.getColorForAmount(root.context, transaction.amount))
                
                transactionDate.text = dateFormat.format(Date(transaction.date))
                
                // Set category icon background color - disabled for simplified layout
                // val categoryColor = getCategoryColor(transaction.category)
                // categoryIconCard.setCardBackgroundColor(categoryColor)
                
                // Show visual indication if transaction is grouped
                if (isGrouped) {
                    // Add slight transparency for grouped transactions
                    root.alpha = 0.95f
                } else {
                    // Full opacity for ungrouped transactions
                    root.alpha = 1.0f
                }
                
                root.setOnClickListener {
                    onTransactionClick(transaction)
                }
                
                root.setOnLongClickListener {
                    onTransactionLongClick(transaction)
                    true
                }
            }
        }
    }
}

/**
 * Sealed class representing items in the unified list
 */
sealed class TransactionItem {
    data class GroupHeader(
        val groupId: String,
        val groupName: String,
        val transactionCount: Int,
        val totalAmount: Double,
        val groupType: String,
        val category: String
    ) : TransactionItem()
    
    data class TransactionData(
        val transaction: Transaction,
        val isGrouped: Boolean
    ) : TransactionItem()
}

class TransactionItemDiffCallback : DiffUtil.ItemCallback<TransactionItem>() {
    override fun areItemsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
        return when {
            oldItem is TransactionItem.GroupHeader && newItem is TransactionItem.GroupHeader -> {
                oldItem.groupId == newItem.groupId
            }
            oldItem is TransactionItem.TransactionData && newItem is TransactionItem.TransactionData -> {
                oldItem.transaction.id == newItem.transaction.id
            }
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: TransactionItem, newItem: TransactionItem): Boolean {
        return oldItem == newItem
    }
}