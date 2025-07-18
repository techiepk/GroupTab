package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.databinding.FragmentTransactionTabBinding
import com.pennywiseai.tracker.ui.adapter.UnifiedTransactionAdapter
import com.pennywiseai.tracker.ui.adapter.TransactionItem
import com.pennywiseai.tracker.viewmodel.TransactionGroupViewModel
import com.pennywiseai.tracker.viewmodel.TransactionsViewModel
import android.util.Log

class UnknownTransactionsTabFragment : Fragment() {
    
    companion object {
        private const val TAG = "UnknownTransactionsTab"
        
        fun newInstance() = UnknownTransactionsTabFragment()
    }
    
    private var _binding: FragmentTransactionTabBinding? = null
    private val binding get() = _binding!!
    
    private val groupViewModel: TransactionGroupViewModel by viewModels({ requireParentFragment() })
    private val transactionsViewModel: TransactionsViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: UnifiedTransactionAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionTabBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeData()
    }
    
    private fun setupRecyclerView() {
        adapter = UnifiedTransactionAdapter(
            onTransactionClick = { transaction ->
                TransactionDetailActivity.start(requireContext(), transaction.id)
            },
            onTransactionLongClick = { transaction ->
                (parentFragment as? TransactionsFragment)?.showQuickActionsDialog(transaction)
            },
            onGroupHeaderClick = { _, _ ->
                // No group headers in unknown transactions
            }
        )
        
        binding.recyclerView.apply {
            this.adapter = this@UnknownTransactionsTabFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun observeData() {
        // Observe both transactions and grouped transactions to find ungrouped ones
        transactionsViewModel.transactions.observe(viewLifecycleOwner) { updateUngroupedTransactions() }
        groupViewModel.groupedTransactions.observe(viewLifecycleOwner) { updateUngroupedTransactions() }
    }
    
    private fun updateUngroupedTransactions() {
        val allTransactions = transactionsViewModel.transactions.value ?: emptyList()
        val groupedTransactions = groupViewModel.groupedTransactions.value ?: emptyList()
        
        // Get IDs of all grouped transactions
        val groupedTransactionIds = mutableSetOf<String>()
        groupedTransactions.forEach { groupedTransaction ->
            groupedTransaction.transactions.forEach { transaction ->
                groupedTransactionIds.add(transaction.id)
            }
        }
        
        // Filter to get only ungrouped transactions
        val ungroupedTransactions = allTransactions.filter { transaction ->
            transaction.id !in groupedTransactionIds
        }
        
        
        // Convert to TransactionItems
        val items = ungroupedTransactions.map { transaction ->
            TransactionItem.TransactionData(
                transaction = transaction,
                isGrouped = false
            )
        }
        
        adapter.submitList(items)
        updateEmptyState(items.isEmpty())
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateText.apply {
            visibility = if (isEmpty) View.VISIBLE else View.GONE
            text = "No unknown transactions!\nAll your transactions have been grouped."
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}