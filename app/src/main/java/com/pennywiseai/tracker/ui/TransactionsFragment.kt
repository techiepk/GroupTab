package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.databinding.FragmentTransactionsBinding
import com.pennywiseai.tracker.ui.adapter.TransactionsPagerAdapter
import com.pennywiseai.tracker.ui.adapter.TransactionAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.pennywiseai.tracker.ui.TransactionDetailActivity
import com.pennywiseai.tracker.ui.EditTransactionActivity
import com.pennywiseai.tracker.viewmodel.TransactionsViewModel
import com.pennywiseai.tracker.viewmodel.TransactionGroupViewModel
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.pennywiseai.tracker.ui.dialog.CreateGroupDialog
import com.pennywiseai.tracker.ui.dialog.CreateSubscriptionDialog
import com.pennywiseai.tracker.data.TimeRange
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.TransactionSortOrder
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TransactionsFragment : Fragment() {
    
    companion object {
        private const val TAG = "TransactionsFragment"
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_MERCHANT = "arg_merchant"
        private const val ARG_START_DATE = "arg_start_date"
        private const val ARG_END_DATE = "arg_end_date"
        
        fun newInstance(
            category: com.pennywiseai.tracker.data.TransactionCategory? = null,
            merchant: String? = null,
            startDate: Long? = null,
            endDate: Long? = null
        ): TransactionsFragment {
            return TransactionsFragment().apply {
                arguments = Bundle().apply {
                    category?.let { putString(ARG_CATEGORY, it.name) }
                    merchant?.let { putString(ARG_MERCHANT, it) }
                    startDate?.let { putLong(ARG_START_DATE, it) }
                    endDate?.let { putLong(ARG_END_DATE, it) }
                }
            }
        }
    }
    
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TransactionsViewModel by viewModels()
    private val groupViewModel: TransactionGroupViewModel by viewModels()
    private lateinit var pagerAdapter: TransactionsPagerAdapter
    private lateinit var filteredTransactionAdapter: TransactionAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        applyFilters()
        if (!isFiltered) {
            setupViewPager()
            autoTriggerGroupingIfNeeded()
            setupTimeRangeSelector()
        }
        setupSortButton()
        observeData()
    }
    
    private var isFiltered = false
    
    private fun applyFilters() {
        arguments?.let { args ->
            val categoryName = args.getString(ARG_CATEGORY)
            val category = categoryName?.let { 
                com.pennywiseai.tracker.data.TransactionCategory.valueOf(it) 
            }
            val merchant = args.getString(ARG_MERCHANT)
            val startDate = if (args.containsKey(ARG_START_DATE)) args.getLong(ARG_START_DATE) else null
            val endDate = if (args.containsKey(ARG_END_DATE)) args.getLong(ARG_END_DATE) else null
            
            val dateRange = if (startDate != null && endDate != null) {
                Pair(startDate, endDate)
            } else null
            
            if (category != null || merchant != null || dateRange != null) {
                isFiltered = true
                viewModel.setFilters(category, merchant, dateRange)
                
                // Also update group view model with date filter
                if (dateRange != null) {
                    groupViewModel.setDateRangeFilter(dateRange.first, dateRange.second)
                }
                
                // Hide tab layout and view pager when filtering
                binding.tabLayout.visibility = View.GONE
                binding.viewPager.visibility = View.GONE
                
                // Show filtered transactions directly
                setupFilteredTransactionsList()
            }
        }
    }
    
    private fun setupFilteredTransactionsList() {
        // Show filtered recycler view
        binding.filteredRecyclerView.visibility = View.VISIBLE
        
        // Setup adapter
        filteredTransactionAdapter = TransactionAdapter { transaction ->
            TransactionDetailActivity.start(requireContext(), transaction.id)
        }
        
        binding.filteredRecyclerView.apply {
            adapter = filteredTransactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        
        // Observe filtered transactions
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            filteredTransactionAdapter.submitList(transactions)
            
            // Show/hide empty state
            if (transactions.isEmpty()) {
                showEmptyState(hasFilters = true)
                binding.filteredRecyclerView.visibility = View.GONE
            } else {
                hideEmptyState()
                binding.filteredRecyclerView.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupViewPager() {
        pagerAdapter = TransactionsPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.getPageTitle(position)
            
            // Add badges to show counts
            when (position) {
                0 -> {
                    // Groups tab - will update badge in observeData
                    tab.tag = "groups_tab"
                }
                1 -> {
                    // Unknown tab - will update badge in observeData
                    tab.tag = "unknown_tab"
                }
            }
        }.attach()
    }
    
    private fun autoTriggerGroupingIfNeeded() {
        // Auto-trigger grouping when transactions are available but no groups exist
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            if (transactions.isNotEmpty()) {
                groupViewModel.transactionGroups.observe(viewLifecycleOwner) { groups ->
                    if (groups.isEmpty()) {
                        groupViewModel.ungroupedCount.observe(viewLifecycleOwner) { count ->
                            if (count > 1) { // Only group if we have at least 2 transactions
                                groupViewModel.startAutoGrouping()
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    private fun observeData() {
        // The tab fragments will observe their own data
        // We just need to update tab badges here
        
        // Observe filter info
        viewModel.filterInfo.observe(viewLifecycleOwner) { filterInfo ->
            if (!filterInfo.isNullOrEmpty()) {
                binding.filterInfoText.text = "Filtered by: $filterInfo"
                binding.filterInfoText.visibility = View.VISIBLE
            } else {
                binding.filterInfoText.visibility = View.GONE
            }
        }
        
        // Only observe tab-related data if not filtered
        if (!isFiltered) {
            // Observe groups count for tab badge
            groupViewModel.transactionGroups.observe(viewLifecycleOwner) { groups ->
                updateTabBadge(0, groups.size)
            }
            
            // Observe ungrouped count for tab badge
            groupViewModel.ungroupedCount.observe(viewLifecycleOwner) { count ->
                updateTabBadge(1, count)
            }
        }
    }
    
    private fun updateTabBadge(position: Int, count: Int) {
        val tab = binding.tabLayout.getTabAt(position)
        tab?.text = when (position) {
            0 -> if (count > 0) "Groups ($count)" else "Groups"
            1 -> if (count > 0) "Unknown ($count)" else "Unknown"
            else -> ""
        }
    }
    
    fun showQuickActionsDialog(transaction: Transaction) {
        // Check if transaction is grouped
        viewLifecycleOwner.lifecycleScope.launch {
            val isGrouped = isTransactionGrouped(transaction.id)
            
            val options = if (isGrouped) {
                arrayOf("View Details", "Edit", "Remove from Group", "Convert to Subscription", "Delete")
            } else {
                arrayOf("View Details", "Edit", "Add to Group", "Create Group from This", "Convert to Subscription", "Delete")
            }
            
            AlertDialog.Builder(requireContext())
                .setTitle(transaction.merchant)
                .setItems(options) { _, which ->
                    when {
                        isGrouped -> {
                            when (which) {
                                0 -> TransactionDetailActivity.start(requireContext(), transaction.id)
                                1 -> EditTransactionActivity.start(requireContext(), transaction.id)
                                2 -> removeFromGroup(transaction)
                                3 -> showCreateSubscriptionDialog(transaction)
                                4 -> showDeleteConfirmation(transaction)
                            }
                        }
                        else -> {
                            when (which) {
                                0 -> TransactionDetailActivity.start(requireContext(), transaction.id)
                                1 -> EditTransactionActivity.start(requireContext(), transaction.id)
                                2 -> showGroupSelectionDialog(transaction)
                                3 -> showCreateGroupDialog(transaction)
                                4 -> showCreateSubscriptionDialog(transaction)
                                5 -> showDeleteConfirmation(transaction)
                            }
                        }
                    }
                }
                .show()
        }
    }
    
    private suspend fun isTransactionGrouped(transactionId: String): Boolean {
        // Check if transaction is in any of the grouped transactions
        val groupedTransactions = groupViewModel.groupedTransactions.value ?: emptyList()
        return groupedTransactions.any { group ->
            group.transactions.any { it.id == transactionId }
        }
    }
    
    private fun showGroupSelectionDialog(transaction: Transaction) {
        // Get available groups
        val groups = groupViewModel.transactionGroups.value ?: emptyList()
        
        if (groups.isEmpty()) {
            android.widget.Toast.makeText(
                requireContext(),
                "No groups available. Run auto-grouping first.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Create list of group names
        val groupNames = groups.map { "${it.name} (${it.transactionCount} transactions)" }.toTypedArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Add to Group")
            .setItems(groupNames) { _, which ->
                val selectedGroup = groups[which]
                groupViewModel.assignTransactionToGroup(transaction.id, selectedGroup.id)
                android.widget.Toast.makeText(
                    requireContext(),
                    "Added to ${selectedGroup.name}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun removeFromGroup(transaction: Transaction) {
        groupViewModel.removeTransactionFromAllGroups(transaction.id)
        android.widget.Toast.makeText(
            requireContext(),
            "Transaction removed from group",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Delete transaction from ${transaction.merchant}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showCreateGroupDialog(transaction: Transaction) {
        // For now, show a simple dialog. Will be replaced with advanced CreateGroupDialog
        val dialog = CreateGroupDialog.newInstance(transaction)
        dialog.show(childFragmentManager, "CreateGroupDialog")
    }
    
    private fun showCreateSubscriptionDialog(transaction: Transaction) {
        val dialog = CreateSubscriptionDialog.newInstance(transaction)
        dialog.show(childFragmentManager, "CreateSubscriptionDialog")
    }
    
    private fun setupTimeRangeSelector() {
        // Set default selection to "This Month"
        binding.chipThisMonth.isChecked = true
        viewModel.setSelectedTimeRange(TimeRange.THIS_MONTH)
        
        // Set initial date filter on group view model
        val initialDateRange = viewModel.getTimeRangeBounds(TimeRange.THIS_MONTH)
        groupViewModel.setDateRangeFilter(initialDateRange.first, initialDateRange.second)
        
        // Setup chip selection handling
        binding.transactionsTimeRangeChips.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val selectedTimeRange = when (checkedIds.first()) {
                    R.id.chip_this_month -> TimeRange.THIS_MONTH
                    R.id.chip_all_time -> TimeRange.ALL_TIME
                    else -> TimeRange.THIS_MONTH
                }
                viewModel.setSelectedTimeRange(selectedTimeRange)
                
                // Also update the group view model with the date filter
                val dateRange = viewModel.getTimeRangeBounds(selectedTimeRange)
                groupViewModel.setDateRangeFilter(dateRange.first, dateRange.second)
            }
        }
    }
    
    private fun setupSortButton() {
        // Observe sort order changes
        viewModel.sortOrder.observe(viewLifecycleOwner) { sortOrder ->
            binding.btnSort.text = sortOrder.displayName.substringBefore(" (")
        }
        
        // Setup sort button click
        binding.btnSort.setOnClickListener {
            showSortOptionsDialog()
        }
    }
    
    private fun showSortOptionsDialog() {
        val currentSort = viewModel.sortOrder.value ?: TransactionSortOrder.getDefault()
        val sortOptions = TransactionSortOrder.values()
        val sortOptionNames = sortOptions.map { it.displayName }.toTypedArray()
        val currentIndex = sortOptions.indexOf(currentSort)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort Transactions")
            .setSingleChoiceItems(sortOptionNames, currentIndex) { dialog, which ->
                viewModel.setSortOrder(sortOptions[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showEmptyState(hasFilters: Boolean = false) {
        binding.apply {
            emptyStateView.visibility = View.VISIBLE
            
            // Configure empty state based on whether filters are active
            val config = EmptyStateView.getTransactionsEmptyConfig(
                context = requireContext(),
                hasFilters = hasFilters,
                onClearFilters = {
                    // Clear all filters
                    viewModel.clearFilters()
                    // Reset time range to default
                    transactionsTimeRangeChips.check(R.id.chip_this_month)
                    // Clear group date filter
                    val defaultDateRange = viewModel.getTimeRangeBounds(TimeRange.THIS_MONTH)
                    groupViewModel.setDateRangeFilter(defaultDateRange.first, defaultDateRange.second)
                },
                onScanClick = {
                    // Navigate to dashboard
                    // Since we can't access the bottom navigation directly, we should 
                    // just go back to let the user navigate manually
                    activity?.onBackPressed()
                }
            )
            emptyStateView.configure(config)
        }
    }
    
    private fun hideEmptyState() {
        binding.emptyStateView.visibility = View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}