package com.pennywiseai.tracker.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.databinding.ItemTransactionPreviewBinding
import com.pennywiseai.tracker.utils.ColorUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionPreviewAdapter : ListAdapter<Transaction, TransactionPreviewAdapter.PreviewViewHolder>(TransactionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val binding = ItemTransactionPreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PreviewViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class PreviewViewHolder(private val binding: ItemTransactionPreviewBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun bind(transaction: Transaction) {
            binding.apply {
                // Show SMS content
                smsContent.text = transaction.rawSms
                
                // Show merchant (highlight if it's Unknown)
                if (transaction.merchant == "Unknown" || transaction.merchant.isEmpty()) {
                    merchantName.text = "⚠️ Unknown Merchant"
                    merchantName.setTextColor(root.context.getColor(android.R.color.holo_orange_dark))
                } else {
                    merchantName.text = transaction.merchant
                    merchantName.setTextColor(root.context.getColor(android.R.color.darker_gray))
                }
                
                // Format amount with sign and color
                val absAmount = kotlin.math.abs(transaction.amount)
                if (transaction.amount >= 0) {
                    transactionAmount.text = "+${currencyFormat.format(absAmount)}"
                } else {
                    transactionAmount.text = currencyFormat.format(transaction.amount)
                }
                transactionAmount.setTextColor(ColorUtils.getTransactionAmountColor(root.context, transaction.amount))
                
                // Date
                transactionDate.text = dateFormat.format(Date(transaction.date))
            }
        }
    }
    
    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}