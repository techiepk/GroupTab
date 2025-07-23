package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TimeRange
import com.pennywiseai.tracker.data.TransactionSortOrder
import com.pennywiseai.tracker.databinding.FragmentTransactionsSimpleBinding
import com.pennywiseai.tracker.ui.adapter.TransactionAdapter
import com.pennywiseai.tracker.ui.dialog.CreateSubscriptionDialog
import com.pennywiseai.tracker.viewmodel.TransactionsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.util.Log

class TransactionsSimpleFragment : Fragment() {
    
    companion object {
        private const val TAG = "TransactionsSimpleFragment"
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_MERCHANT = "arg_merchant"
        private const val ARG_START_DATE = "arg_start_date"
        private const val ARG_END_DATE = "arg_end_date"
        
        fun newInstance(
            category: com.pennywiseai.tracker.data.TransactionCategory? = null,
            merchant: String? = null,
            startDate: Long? = null,
            endDate: Long? = null
        ): TransactionsSimpleFragment {
            return TransactionsSimpleFragment().apply {
                arguments = Bundle().apply {
                    category?.let { putString(ARG_CATEGORY, it.name) }
                    merchant?.let { putString(ARG_MERCHANT, it) }
                    startDate?.let { putLong(ARG_START_DATE, it) }
                    endDate?.let { putLong(ARG_END_DATE, it) }
                }
            }
        }
    }
    
    private var _binding: FragmentTransactionsSimpleBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter
    private var isFiltered = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsSimpleBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        applyFilters()
        setupTimeRangeSelector()
        setupSortButton()
        observeData()
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                TransactionDetailActivity.start(requireContext(), transaction.id)
            },
            onTransactionLongClick = { transaction ->
                showQuickActionsDialog(transaction)
            }
        )
        
        binding.transactionsRecyclerView.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = null // Disable animations for smoother updates
        }
    }
    
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
                updateFilterInfo(category, merchant, dateRange)
            }
        }
    }
    
    private fun setupTimeRangeSelector() {
        // Hide time range selector if already filtered by date
        if (isFiltered && viewModel.dateRange.value != null) {
            binding.transactionsTimeRangeChips.visibility = View.GONE
            return
        }
        
        binding.chipThisMonth.isChecked = true
        
        viewModel.setSelectedTimeRange(TimeRange.THIRTY_DAYS)
        
        binding.transactionsTimeRangeChips.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                binding.chipThisMonth.id in checkedIds -> {
                    viewModel.setSelectedTimeRange(TimeRange.THIRTY_DAYS)
                }
                binding.chipAllTime.id in checkedIds -> {
                    viewModel.setSelectedTimeRange(TimeRange.ALL_TIME)
                }
            }
        }
    }
    
    private fun setupSortButton() {
        updateSortButtonText(viewModel.sortOrder.value ?: TransactionSortOrder.DATE_DESC)
        
        binding.btnSort.setOnClickListener {
            showSortDialog()
        }
        
        viewModel.sortOrder.observe(viewLifecycleOwner) { sortOrder ->
            updateSortButtonText(sortOrder)
        }
    }
    
    private fun updateSortButtonText(sortOrder: TransactionSortOrder) {
        // Update chip text with arrow indicator
        val arrow = when (sortOrder) {
            TransactionSortOrder.DATE_DESC,
            TransactionSortOrder.AMOUNT_DESC,
            TransactionSortOrder.MERCHANT_DESC -> "↓"
            else -> "↑"
        }
        
        val sortText = when (sortOrder) {
            TransactionSortOrder.DATE_DESC, TransactionSortOrder.DATE_ASC -> "Date"
            TransactionSortOrder.AMOUNT_DESC, TransactionSortOrder.AMOUNT_ASC -> "Amount"
            TransactionSortOrder.MERCHANT_ASC, TransactionSortOrder.MERCHANT_DESC -> "Merchant"
            else -> "Sort"
        }
        
        binding.btnSort.text = "$sortText $arrow"
    }
    
    private fun showSortDialog() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogBinding = com.pennywiseai.tracker.databinding.DialogSortTransactionsBinding.inflate(
            layoutInflater
        )
        bottomSheet.setContentView(dialogBinding.root)
        
        // Set current selection
        val currentRadioId = when (viewModel.sortOrder.value) {
            TransactionSortOrder.DATE_DESC -> dialogBinding.radioDateNewest.id
            TransactionSortOrder.DATE_ASC -> dialogBinding.radioDateOldest.id
            TransactionSortOrder.AMOUNT_DESC -> dialogBinding.radioAmountHighest.id
            TransactionSortOrder.AMOUNT_ASC -> dialogBinding.radioAmountLowest.id
            TransactionSortOrder.MERCHANT_ASC -> dialogBinding.radioMerchantAz.id
            TransactionSortOrder.MERCHANT_DESC -> dialogBinding.radioMerchantZa.id
            else -> dialogBinding.radioDateNewest.id
        }
        dialogBinding.sortRadioGroup.check(currentRadioId)
        
        // Handle selection
        dialogBinding.sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val sortOrder = when (checkedId) {
                dialogBinding.radioDateNewest.id -> TransactionSortOrder.DATE_DESC
                dialogBinding.radioDateOldest.id -> TransactionSortOrder.DATE_ASC
                dialogBinding.radioAmountHighest.id -> TransactionSortOrder.AMOUNT_DESC
                dialogBinding.radioAmountLowest.id -> TransactionSortOrder.AMOUNT_ASC
                dialogBinding.radioMerchantAz.id -> TransactionSortOrder.MERCHANT_ASC
                dialogBinding.radioMerchantZa.id -> TransactionSortOrder.MERCHANT_DESC
                else -> TransactionSortOrder.DATE_DESC
            }
            viewModel.setSortOrder(sortOrder)
            bottomSheet.dismiss()
        }
        
        bottomSheet.show()
    }
    
    private fun observeData() {
        // Observe transactions
        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
            updateEmptyState(transactions.isEmpty())
        }
        
        // Observe filter info
        viewModel.filterInfo.observe(viewLifecycleOwner) { filterInfo ->
            if (!filterInfo.isNullOrEmpty()) {
                binding.filterInfoText.text = "Filtered by: $filterInfo"
                binding.filterInfoText.visibility = View.VISIBLE
            } else {
                binding.filterInfoText.visibility = View.GONE
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateView.visibility = View.VISIBLE
            binding.transactionsRecyclerView.visibility = View.GONE
            
            if (isFiltered) {
                binding.emptyStateView.configure(
                    EmptyStateView.getSearchEmptyConfig(
                        requireContext(),
                        searchQuery = "transactions",
                        onClearSearch = { 
                            viewModel.clearFilters()
                        }
                    )
                )
            } else {
                binding.emptyStateView.configure(
                    EmptyStateView.getTransactionsEmptyConfig(
                        requireContext(),
                        hasFilters = false,
                        onClearFilters = {},
                        onScanClick = {
                            // TODO: Implement scan functionality
                        }
                    )
                )
            }
        } else {
            binding.emptyStateView.visibility = View.GONE
            binding.transactionsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun updateFilterInfo(
        category: com.pennywiseai.tracker.data.TransactionCategory?,
        merchant: String?,
        dateRange: Pair<Long, Long>?
    ) {
        val filters = mutableListOf<String>()
        
        category?.let { filters.add(it.name.replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() }) }
        merchant?.let { filters.add(it) }
        dateRange?.let {
            filters.add("Custom date range")
        }
        
        if (filters.isNotEmpty()) {
            binding.filterInfoText.text = "Filtered by: ${filters.joinToString(", ")}"
            binding.filterInfoText.visibility = View.VISIBLE
        }
    }
    
    fun showQuickActionsDialog(transaction: Transaction) {
        lifecycleScope.launch {
            val options = arrayOf(
                "View Details", 
                "Edit", 
                "Convert to Subscription", 
                "Delete"
            )
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(transaction.merchant ?: transaction.rawSms?.take(50) ?: "Transaction")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> TransactionDetailActivity.start(requireContext(), transaction.id)
                        1 -> EditTransactionActivity.start(requireContext(), transaction.id)
                        2 -> showConvertToSubscriptionDialog(transaction)
                        3 -> confirmDeleteTransaction(transaction)
                    }
                }
                .show()
        }
    }
    
    private fun showConvertToSubscriptionDialog(transaction: Transaction) {
        CreateSubscriptionDialog.newInstance(transaction).show(childFragmentManager, "CreateSubscriptionDialog")
    }
    
    private fun confirmDeleteTransaction(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}