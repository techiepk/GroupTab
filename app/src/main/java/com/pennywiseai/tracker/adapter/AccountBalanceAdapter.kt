package com.pennywiseai.tracker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pennywiseai.tracker.data.AccountBalance
import com.pennywiseai.tracker.databinding.ItemAccountBalanceBinding
import com.pennywiseai.tracker.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AccountBalanceAdapter : ListAdapter<AccountBalance, AccountBalanceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAccountBalanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemAccountBalanceBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(balance: AccountBalance) {
            // Set bank initial
            binding.bankInitial.text = balance.bankName.firstOrNull()?.toString() ?: "?"
            
            // Set bank name
            binding.bankName.text = balance.bankName
            
            // Set account number
            binding.accountNumber.text = "****${balance.last4Digits}"
            
            // Set balance
            binding.balanceAmount.text = CurrencyFormatter.formatCompact(balance.balance)
            
            // Set last updated
            binding.lastUpdated.text = getLastUpdatedText(balance.lastUpdated)
        }
        
        private fun getLastUpdatedText(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < TimeUnit.HOURS.toMillis(1) -> "Updated just now"
                diff < TimeUnit.HOURS.toMillis(24) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "Updated $hours hour${if (hours > 1) "s" else ""} ago"
                }
                diff < TimeUnit.DAYS.toMillis(2) -> "Updated yesterday"
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "Updated $days days ago"
                }
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    "Updated ${dateFormat.format(Date(timestamp))}"
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AccountBalance>() {
        override fun areItemsTheSame(oldItem: AccountBalance, newItem: AccountBalance): Boolean {
            return oldItem.accountId == newItem.accountId
        }

        override fun areContentsTheSame(oldItem: AccountBalance, newItem: AccountBalance): Boolean {
            return oldItem == newItem
        }
    }
}