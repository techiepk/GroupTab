package com.pennywiseai.tracker.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.SubscriptionStatus
import com.pennywiseai.tracker.databinding.ItemSubscriptionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionAdapter(
    private val onSubscriptionClick: (Subscription) -> Unit = {},
    private val onToggleActive: (Subscription) -> Unit = {}
) : ListAdapter<Subscription, SubscriptionAdapter.SubscriptionViewHolder>(SubscriptionDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val binding = ItemSubscriptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubscriptionViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        holder.bind(getItem(position), onSubscriptionClick, onToggleActive)
    }
    
    class SubscriptionViewHolder(private val binding: ItemSubscriptionBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        
        fun bind(
            subscription: Subscription,
            onSubscriptionClick: (Subscription) -> Unit,
            onToggleActive: (Subscription) -> Unit
        ) {
            binding.apply {
                merchantName.text = subscription.merchantName
                subscriptionAmount.text = currencyFormat.format(subscription.amount)
                
                // Frequency text
                val frequencyText = when (subscription.frequency.days) {
                    7 -> "Weekly"
                    30 -> "Monthly"
                    90 -> "Quarterly"
                    365 -> "Yearly"
                    else -> "Every ${subscription.frequency.days} days"
                }
                subscriptionFrequency.text = frequencyText
                
                // Next payment date
                if (subscription.status == SubscriptionStatus.ACTIVE) {
                    nextPaymentDate.text = "Next: ${dateFormat.format(Date(subscription.nextPaymentDate))}"
                } else {
                    nextPaymentDate.text = subscription.status.name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                }
                
                // Status indicator
                val statusColor = when (subscription.status) {
                    SubscriptionStatus.ACTIVE -> 0xFF4CAF50.toInt() // Green
                    SubscriptionStatus.PAUSED -> 0xFFFF9800.toInt() // Orange
                    SubscriptionStatus.CANCELLED -> 0xFFF44336.toInt() // Red
                    SubscriptionStatus.EXPIRED -> 0xFF9E9E9E.toInt() // Gray
                    SubscriptionStatus.FAILED -> 0xFFE91E63.toInt() // Pink
                    SubscriptionStatus.TRIAL -> 0xFF2196F3.toInt() // Blue
                }
                statusIndicator.setBackgroundColor(statusColor)
                
                // Payment count
                paymentCount.text = "${subscription.paymentCount} payments"
                
                // Click listeners
                root.setOnClickListener { onSubscriptionClick(subscription) }
                toggleButton.setOnClickListener { onToggleActive(subscription) }
                
                // Toggle button text
                toggleButton.text = if (subscription.status == SubscriptionStatus.ACTIVE) {
                    "Pause"
                } else {
                    "Resume"
                }
            }
        }
    }
    
    class SubscriptionDiffCallback : DiffUtil.ItemCallback<Subscription>() {
        override fun areItemsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Subscription, newItem: Subscription): Boolean {
            return oldItem == newItem
        }
    }
}