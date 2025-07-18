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

class GroupedTransactionsTabFragment : Fragment() {
    
    companion object {
        private const val TAG = "GroupedTransactionsTab"
        
        fun newInstance() = GroupedTransactionsTabFragment()
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
            onGroupHeaderClick = { groupId, groupName ->
                if (groupId != "ungrouped") {
                    GroupDetailActivity.start(requireContext(), groupId, groupName)
                }
            }
        )
        
        binding.recyclerView.apply {
            this.adapter = this@GroupedTransactionsTabFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun observeData() {
        // Observe grouped transactions
        groupViewModel.groupedTransactions.observe(viewLifecycleOwner) { groupedTransactions ->
            
            val transactions = transactionsViewModel.transactions.value ?: emptyList()
            val items = buildGroupedTransactionItems(groupedTransactions, transactions)
            
            adapter.submitList(items)
            updateEmptyState(items.isEmpty())
        }
    }
    
    private fun buildGroupedTransactionItems(
        groupedTransactions: List<com.pennywiseai.tracker.data.GroupedTransaction>,
        allTransactions: List<Transaction>
    ): List<TransactionItem> {
        val items = mutableListOf<TransactionItem>()
        
        // Build items for each group
        groupedTransactions.forEach { groupedTransaction ->
            val group = groupedTransaction.group
            
            // Skip ungrouped transactions in this tab
            if (group.id != "ungrouped") {
                // Calculate actual values from transactions
                val actualTransactionCount = groupedTransaction.transactions.size
                val actualTotalAmount = groupedTransaction.transactions.sumOf { it.amount }
                
                // Add group header
                items.add(
                    TransactionItem.GroupHeader(
                        groupId = group.id,
                        groupName = group.name,
                        transactionCount = actualTransactionCount,
                        totalAmount = actualTotalAmount,
                        groupType = group.groupingType.name,
                        category = group.category.name
                    )
                )
            }
        }
        
        return items
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateText.apply {
            visibility = if (isEmpty) View.VISIBLE else View.GONE
            text = "No grouped transactions yet.\nTransactions will be automatically grouped based on patterns."
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}