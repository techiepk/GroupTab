package com.pennywiseai.tracker.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionWithGroup
import com.pennywiseai.tracker.databinding.ItemTransactionBinding
import com.pennywiseai.tracker.utils.ColorUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit = {},
    private val onTransactionLongClick: (Transaction) -> Unit = {}
) : ListAdapter<TransactionWithGroup, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position), onTransactionClick, onTransactionLongClick)
    }
    
    class TransactionViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun bind(
            transactionWithGroup: TransactionWithGroup,
            onTransactionClick: (Transaction) -> Unit,
            onTransactionLongClick: (Transaction) -> Unit
        ) {
            val transaction = transactionWithGroup.transaction
            binding.apply {
                // Show group name as merchant if grouped, otherwise show original merchant
                merchantName.text = transactionWithGroup.groupName ?: transaction.merchant
                
                // Format amount with sign and color
                val absAmount = kotlin.math.abs(transaction.amount)
                if (transaction.amount >= 0) {
                    transactionAmount.text = "+${currencyFormat.format(absAmount)}"
                } else {
                    transactionAmount.text = currencyFormat.format(transaction.amount)
                }
                transactionAmount.setTextColor(ColorUtils.getTransactionAmountColor(root.context, transaction.amount))
                
                transactionDate.text = dateFormat.format(Date(transaction.date))
                
                // Show transaction type instead of just category
                val typeText = transaction.transactionType.name.replace("_", " ")
                    .lowercase().replaceFirstChar { it.uppercase() }
                val categoryText = transaction.category.name.replace("_", " ")
                    .lowercase().replaceFirstChar { it.uppercase() }
                transactionCategory.text = "$typeText • $categoryText"
                
                // Set category color or icon based on type
                val categoryColor = getCategoryColor(transaction.category.name)
                // categoryIndicator.setBackgroundColor(categoryColor) // TODO: Update for new layout
                
                // Show group badge if transaction is grouped and merchant is unknown
                if (transactionWithGroup.groupName != null && transaction.merchant == "Unknown Merchant") {
                    subscriptionBadge.text = "Pattern"
                    subscriptionBadge.visibility = android.view.View.VISIBLE
                } else {
                    subscriptionBadge.visibility = android.view.View.GONE
                }
                
                // Add click listeners
                root.setOnClickListener { onTransactionClick(transaction) }
                root.setOnLongClickListener { 
                    onTransactionLongClick(transaction)
                    true
                }
            }
        }
        
        private fun getCategoryColor(category: String): Int {
            return when (category) {
                "FOOD_DINING" -> 0xFFFF6B6B.toInt()
                "TRANSPORTATION" -> 0xFF4ECDC4.toInt()
                "SHOPPING" -> 0xFF45B7D1.toInt()
                "ENTERTAINMENT" -> 0xFF96CEB4.toInt()
                "BILLS_UTILITIES" -> 0xFFFECA57.toInt()
                "GROCERIES" -> 0xFF6C5CE7.toInt()
                "HEALTHCARE" -> 0xFFFF7675.toInt()
                "SUBSCRIPTION" -> 0xFFA29BFE.toInt()
                else -> 0xFF74B9FF.toInt()
            }
        }
    }
    
    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionWithGroup>() {
        override fun areItemsTheSame(oldItem: TransactionWithGroup, newItem: TransactionWithGroup): Boolean {
            return oldItem.transaction.id == newItem.transaction.id
        }
        
        override fun areContentsTheSame(oldItem: TransactionWithGroup, newItem: TransactionWithGroup): Boolean {
            return oldItem == newItem
        }
    }
}