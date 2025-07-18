package com.pennywiseai.tracker.ui.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.GroupedTransaction
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionGroup
import com.pennywiseai.tracker.data.GroupingType
import com.pennywiseai.tracker.databinding.ItemTransactionGroupBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class GroupedTransactionAdapter(
    private val onGroupClick: (TransactionGroup) -> Unit = {},
    private val onTransactionClick: (Transaction) -> Unit = {}
) : ListAdapter<GroupedTransaction, GroupedTransactionAdapter.GroupViewHolder>(GroupedTransactionDiffCallback()) {

    private val expandedGroups = mutableSetOf<String>()
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemTransactionGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: ItemTransactionGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        private val transactionAdapter = TransactionInGroupAdapter { transaction ->
            onTransactionClick(transaction)
        }

        init {
            // Setup nested RecyclerView
            binding.transactionsRecycler.apply {
                adapter = transactionAdapter
                layoutManager = LinearLayoutManager(binding.root.context)
                isNestedScrollingEnabled = false
            }

            // Setup click listeners
            binding.groupHeader.setOnClickListener {
                val groupedTransaction = getItem(adapterPosition)
                toggleExpansion(groupedTransaction.group.id)
            }

            binding.viewAllButton.setOnClickListener {
                val groupedTransaction = getItem(adapterPosition)
                // Instead of calling onGroupClick, let's expand the group to show all transactions
                toggleExpansion(groupedTransaction.group.id)
            }
        }

        fun bind(groupedTransaction: GroupedTransaction) {
            val group = groupedTransaction.group
            val transactions = groupedTransaction.transactions

            // Group header information
            binding.groupName.text = group.name
            binding.transactionCount.text = "${group.transactionCount} transactions"
            binding.averageAmount.text = "Avg ${formatCurrency(group.averageAmount)}"
            binding.totalAmount.text = formatCurrency(group.totalAmount)
            
            // Format grouping type
            binding.groupingType.text = when (group.groupingType) {
                GroupingType.MERCHANT_EXACT -> "EXACT"
                GroupingType.MERCHANT_FUZZY -> "FUZZY"
                GroupingType.UPI_ID -> "UPI"
                GroupingType.CATEGORY_AMOUNT -> "AMOUNT"
                GroupingType.RECURRING_PATTERN -> "RECURRING"
                GroupingType.MANUAL -> "MANUAL"
            }

            // Last transaction date
            if (group.lastTransactionDate > 0) {
                binding.lastTransactionDate.text = dateFormat.format(Date(group.lastTransactionDate))
            } else {
                binding.lastTransactionDate.text = ""
            }

            // Category indicator color
            val categoryColor = getCategoryColor(group.category)
            binding.categoryIndicator.backgroundTintList = 
                android.content.res.ColorStateList.valueOf(categoryColor)

            // Handle expansion state
            val isExpanded = expandedGroups.contains(group.id)
            updateExpansionState(isExpanded, animate = false)

            // Load transactions in nested RecyclerView
            if (isExpanded) {
                transactionAdapter.submitList(transactions)
            }
        }

        private fun toggleExpansion(groupId: String) {
            val isCurrentlyExpanded = expandedGroups.contains(groupId)
            
            if (isCurrentlyExpanded) {
                expandedGroups.remove(groupId)
            } else {
                expandedGroups.add(groupId)
                // Load transactions when expanding
                val groupedTransaction = getItem(adapterPosition)
                transactionAdapter.submitList(groupedTransaction.transactions)
            }
            
            updateExpansionState(!isCurrentlyExpanded, animate = true)
        }

        private fun updateExpansionState(isExpanded: Boolean, animate: Boolean) {
            val duration = if (animate) 300L else 0L
            
            if (isExpanded) {
                // Show transactions, hide view all button
                binding.transactionsRecycler.visibility = View.VISIBLE
                binding.viewAllButton.visibility = View.GONE
                
                // Rotate arrow
                if (animate) {
                    ObjectAnimator.ofFloat(binding.expandArrow, "rotation", 0f, 180f)
                        .setDuration(duration)
                        .start()
                } else {
                    binding.expandArrow.rotation = 180f
                }
            } else {
                // Hide transactions, show view all button
                binding.transactionsRecycler.visibility = View.GONE
                binding.viewAllButton.visibility = View.VISIBLE
                
                // Rotate arrow back
                if (animate) {
                    ObjectAnimator.ofFloat(binding.expandArrow, "rotation", 180f, 0f)
                        .setDuration(duration)
                        .start()
                } else {
                    binding.expandArrow.rotation = 0f
                }
            }
        }

        private fun getCategoryColor(category: com.pennywiseai.tracker.data.TransactionCategory): Int {
            val context = binding.root.context
            return when (category) {
                com.pennywiseai.tracker.data.TransactionCategory.FOOD_DINING -> 
                    context.getColor(android.R.color.holo_orange_dark)
                com.pennywiseai.tracker.data.TransactionCategory.TRANSPORTATION -> 
                    context.getColor(android.R.color.holo_blue_dark)
                com.pennywiseai.tracker.data.TransactionCategory.SHOPPING -> 
                    context.getColor(android.R.color.holo_purple)
                com.pennywiseai.tracker.data.TransactionCategory.ENTERTAINMENT -> 
                    context.getColor(android.R.color.holo_red_dark)
                com.pennywiseai.tracker.data.TransactionCategory.BILLS_UTILITIES -> 
                    context.getColor(android.R.color.holo_green_dark)
                else -> context.getColor(android.R.color.darker_gray)
            }
        }

        private fun formatCurrency(amount: Double): String {
            return "₹${String.format("%.0f", amount)}"
        }
    }

    // Nested adapter for transactions within groups
    private class TransactionInGroupAdapter(
        private val onTransactionClick: (Transaction) -> Unit
    ) : ListAdapter<Transaction, TransactionInGroupAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            val binding = com.pennywiseai.tracker.databinding.ItemTransactionInGroupBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TransactionViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            holder.bind(getItem(position), onTransactionClick)
        }

        class TransactionViewHolder(
            private val binding: com.pennywiseai.tracker.databinding.ItemTransactionInGroupBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            private val dateTimeFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

            fun bind(transaction: Transaction, onTransactionClick: (Transaction) -> Unit) {
                binding.merchantName.text = transaction.merchant
                binding.transactionDate.text = dateTimeFormat.format(Date(transaction.date))
                binding.transactionAmount.text = "₹${String.format("%.0f", transaction.amount)}"

                binding.root.setOnClickListener {
                    onTransactionClick(transaction)
                }
            }
        }

        private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
                return oldItem == newItem
            }
        }
    }

    private class GroupedTransactionDiffCallback : DiffUtil.ItemCallback<GroupedTransaction>() {
        override fun areItemsTheSame(oldItem: GroupedTransaction, newItem: GroupedTransaction): Boolean {
            return oldItem.group.id == newItem.group.id
        }

        override fun areContentsTheSame(oldItem: GroupedTransaction, newItem: GroupedTransaction): Boolean {
            return oldItem.group == newItem.group && 
                   oldItem.transactions.size == newItem.transactions.size &&
                   oldItem.isExpanded == newItem.isExpanded
        }
    }
}