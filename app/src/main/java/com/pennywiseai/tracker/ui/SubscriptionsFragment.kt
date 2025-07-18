package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.pennywiseai.tracker.databinding.FragmentSubscriptionsBinding
import com.pennywiseai.tracker.ui.adapter.SubscriptionAdapter
import com.pennywiseai.tracker.viewmodel.SubscriptionsViewModel
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.SubscriptionStatus
import com.google.android.material.chip.Chip
import androidx.appcompat.app.AlertDialog
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.pennywiseai.tracker.utils.ThemeColorUtils

class SubscriptionsFragment : Fragment() {
    
    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SubscriptionsViewModel by viewModels()
    private lateinit var subscriptionAdapter: SubscriptionAdapter
    private var currentFilter = "all"
    private var currentSort = "name" // name, amount, nextPayment
    private var isUpcomingExpanded = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupDashboard()
        setupFilters()
        setupSorting()
        setupUpcomingCard()
        observeData()
    }
    
    private fun setupRecyclerView() {
        subscriptionAdapter = SubscriptionAdapter(
            onSubscriptionClick = { subscription ->
                EditSubscriptionActivity.start(requireContext(), subscription.id)
            },
            onToggleActive = { subscription ->
                if (subscription.status == SubscriptionStatus.ACTIVE) {
                    // Show warning when pausing
                    showPauseWarningDialog(subscription)
                } else {
                    // Resume without warning
                    viewModel.toggleSubscriptionStatus(subscription)
                }
            }
        )
        binding.subscriptionsRecyclerView.apply {
            adapter = subscriptionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupDashboard() {
        // Dashboard will be updated via observeData
    }
    
    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chip_all -> {
                        currentFilter = "all"
                        viewModel.setFilter(null)
                    }
                    R.id.chip_active -> {
                        currentFilter = "active"
                        viewModel.setFilter(SubscriptionStatus.ACTIVE)
                    }
                    R.id.chip_paused -> {
                        currentFilter = "paused"
                        viewModel.setFilter(SubscriptionStatus.PAUSED)
                    }
                    R.id.chip_upcoming -> {
                        currentFilter = "upcoming"
                        viewModel.setUpcomingFilter(7) // Next 7 days
                    }
                }
            }
        }
    }
    
    private fun setupSorting() {
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }
    
    private fun setupUpcomingCard() {
        binding.upcomingPaymentsCard.setOnClickListener {
            toggleUpcomingExpansion()
        }
    }
    
    private fun toggleUpcomingExpansion() {
        isUpcomingExpanded = !isUpcomingExpanded
        
        // Animate rotation of expand icon
        val rotation = if (isUpcomingExpanded) 180f else 0f
        binding.expandUpcomingIcon.animate()
            .rotation(rotation)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        // Animate visibility of upcoming list
        if (isUpcomingExpanded) {
            binding.upcomingPaymentsList.visibility = View.VISIBLE
            binding.upcomingPaymentsList.alpha = 0f
            binding.upcomingPaymentsList.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        } else {
            binding.upcomingPaymentsList.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    binding.upcomingPaymentsList.visibility = View.GONE
                }
                .start()
        }
    }
    
    private fun showSortDialog() {
        val options = arrayOf("Name", "Amount (High to Low)", "Amount (Low to High)", "Next Payment")
        val currentIndex = when (currentSort) {
            "name" -> 0
            "amount_desc" -> 1
            "amount_asc" -> 2
            "nextPayment" -> 3
            else -> 0
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Sort Subscriptions")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                currentSort = when (which) {
                    0 -> "name"
                    1 -> "amount_desc"
                    2 -> "amount_asc"
                    3 -> "nextPayment"
                    else -> "name"
                }
                viewModel.setSortOrder(currentSort)
                updateSortButtonText()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateSortButtonText() {
        val sortText = when (currentSort) {
            "name" -> "Sort ↓"
            "amount_desc" -> "Amount ↓"
            "amount_asc" -> "Amount ↑"
            "nextPayment" -> "Due Date ↓"
            else -> "Sort ↓"
        }
        binding.sortButton.text = sortText
    }
    
    private fun observeData() {
        // Active subscriptions
        viewModel.activeSubscriptions.observe(viewLifecycleOwner) { subscriptions ->
            subscriptionAdapter.submitList(subscriptions)
            
            binding.emptyStateText.visibility = if (subscriptions.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
        
        // Dashboard data
        viewModel.monthlyTotal.observe(viewLifecycleOwner) { total ->
            binding.monthlyTotalAmount.text = "₹${String.format("%.0f", total)}"
        }
        
        viewModel.activeCount.observe(viewLifecycleOwner) { count ->
            binding.activeSubscriptionsCount.text = "$count active"
        }
        
        viewModel.upcomingPayments.observe(viewLifecycleOwner) { upcoming ->
            binding.upcomingPaymentsCount.text = "${upcoming.size} upcoming"
            updateUpcomingPaymentsList(upcoming)
        }
    }
    
    private fun showPauseWarningDialog(subscription: Subscription) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Pause Subscription")
            .setMessage("Are you sure you want to pause ${subscription.merchantName}?\n\nThis will stop tracking future payments until you resume it.")
            .setPositiveButton("Pause") { _, _ ->
                viewModel.toggleSubscriptionStatus(subscription)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateUpcomingPaymentsList(upcomingPayments: List<Subscription>) {
        binding.upcomingPaymentsList.removeAllViews()
        
        if (upcomingPayments.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "No upcoming payments in the next 7 days"
                setTextColor(ContextCompat.getColor(requireContext(), com.google.android.material.R.color.material_on_surface_emphasis_medium))
                textSize = 14f
            }
            binding.upcomingPaymentsList.addView(emptyText)
        } else {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            
            upcomingPayments.take(5).forEach { subscription ->
                val itemView = layoutInflater.inflate(android.R.layout.simple_list_item_2, null)
                val title = itemView.findViewById<TextView>(android.R.id.text1)
                val subtitle = itemView.findViewById<TextView>(android.R.id.text2)
                
                title.text = subscription.merchantName
                title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                
                val daysUntil = ((subscription.nextPaymentDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                val dueText = when {
                    daysUntil < 0 -> "Overdue"
                    daysUntil == 0 -> "Due today"
                    daysUntil == 1 -> "Due tomorrow"
                    else -> "Due in $daysUntil days"
                }
                
                subtitle.text = "₹${String.format("%.0f", subscription.amount)} • $dueText • ${dateFormat.format(Date(subscription.nextPaymentDate))}"
                subtitle.setTextColor(
                    if (daysUntil <= 1) ThemeColorUtils.getExpenseColor(requireContext()) 
                    else ContextCompat.getColor(requireContext(), com.google.android.material.R.color.material_on_surface_emphasis_medium)
                )
                
                itemView.setPadding(0, 8, 0, 8)
                binding.upcomingPaymentsList.addView(itemView)
            }
            
            if (upcomingPayments.size > 5) {
                val moreText = TextView(requireContext()).apply {
                    text = "+ ${upcomingPayments.size - 5} more"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.pennywise_primary))
                    textSize = 14f
                    setPadding(0, 16, 0, 0)
                }
                binding.upcomingPaymentsList.addView(moreText)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}