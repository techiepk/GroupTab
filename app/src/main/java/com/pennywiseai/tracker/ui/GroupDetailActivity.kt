package com.pennywiseai.tracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.pennywiseai.tracker.databinding.ActivityGroupDetailBinding
import com.pennywiseai.tracker.ui.adapter.TransactionAdapter
import com.pennywiseai.tracker.viewmodel.GroupDetailViewModel
import com.pennywiseai.tracker.viewmodel.GroupDetailViewModelFactory
import android.util.Log
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pennywiseai.tracker.utils.ThemeColorUtils
import com.pennywiseai.tracker.data.TransactionSortOrder
import com.pennywiseai.tracker.ui.dialog.EditGroupDialog

class GroupDetailActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "GroupDetailActivity"
        private const val EXTRA_GROUP_ID = "extra_group_id"
        private const val EXTRA_GROUP_NAME = "extra_group_name"
        
        fun start(context: Context, groupId: String, groupName: String) {
            val intent = Intent(context, GroupDetailActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_GROUP_NAME, groupName)
            }
            context.startActivity(intent)
        }
    }
    
    private lateinit var binding: ActivityGroupDetailBinding
    private lateinit var transactionAdapter: TransactionAdapter
    
    private val groupId by lazy { intent.getStringExtra(EXTRA_GROUP_ID) ?: "" }
    private val groupName by lazy { intent.getStringExtra(EXTRA_GROUP_NAME) ?: "Group Details" }
    
    private val viewModel: GroupDetailViewModel by viewModels {
        GroupDetailViewModelFactory(application, groupId)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Edge-to-edge display is handled by theme
        
        setupToolbar()
        setupRecyclerView()
        observeData()
        setupFab()
        setupSortButton()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = groupName
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onTransactionClick = { transaction ->
                TransactionDetailActivity.start(this, transaction.id)
            }
        )
        
        binding.transactionsRecyclerView.apply {
            adapter = transactionAdapter
            layoutManager = LinearLayoutManager(this@GroupDetailActivity)
        }
    }
    
    private fun observeData() {
        // Observe group details
        viewModel.groupDetails.observe(this) { group ->
            group?.let {
                updateGroupStats(it)
            }
        }
        
        // Observe transactions in group
        viewModel.transactions.observe(this) { transactions ->
            if (transactions.isEmpty()) {
                Log.w(TAG, "⚠️ No transactions found for group $groupId despite cached count")
            }
            transactionAdapter.submitList(transactions)
            
            binding.emptyStateText.visibility = if (transactions.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            
            // Update date range if we have transactions
            if (transactions.isNotEmpty()) {
                updateDateRange(transactions)
            }
            
            // Update the actual count in the UI
            binding.transactionCountText.text = transactions.size.toString()
        }
    }
    
    private fun updateGroupStats(group: com.pennywiseai.tracker.data.TransactionGroup) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        
        binding.apply {
            // Update group name
            groupNameText.text = group.name
            
            // Note: We'll update the actual count from transactions later
            
            val absAmount = kotlin.math.abs(group.totalAmount)
            totalAmountText.text = if (group.totalAmount >= 0) {
                "+${currencyFormat.format(absAmount)}"
            } else {
                currencyFormat.format(group.totalAmount)
            }
            
            // Color code the amount
            totalAmountText.setTextColor(ThemeColorUtils.getColorForAmount(this@GroupDetailActivity, group.totalAmount))
            
            // Calculate and show average
            if (group.transactionCount > 0) {
                val average = group.totalAmount / group.transactionCount
                averageAmountText.text = currencyFormat.format(kotlin.math.abs(average))
            } else {
                averageAmountText.text = currencyFormat.format(0)
            }
            
            // Show group type
            groupTypeChip.text = when (group.groupingType) {
                com.pennywiseai.tracker.data.GroupingType.MERCHANT_EXACT -> "Exact Match"
                com.pennywiseai.tracker.data.GroupingType.MERCHANT_FUZZY -> "Similar Merchant"
                com.pennywiseai.tracker.data.GroupingType.UPI_ID -> "UPI ID"
                com.pennywiseai.tracker.data.GroupingType.CATEGORY_AMOUNT -> "Category & Amount"
                com.pennywiseai.tracker.data.GroupingType.RECURRING_PATTERN -> "Recurring"
                com.pennywiseai.tracker.data.GroupingType.MANUAL -> "Manual"
            }
            
            // Show category
            categoryChip.text = group.category.name.replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun updateDateRange(transactions: List<com.pennywiseai.tracker.data.Transaction>) {
        if (transactions.isEmpty()) return
        
        val sortedTransactions = transactions.sortedBy { it.date }
        val firstDate = Date(sortedTransactions.first().date)
        val lastDate = Date(sortedTransactions.last().date)
        
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        
        binding.dateRangeText.text = "${dateFormat.format(firstDate)} - ${dateFormat.format(lastDate)}"
    }
    
    private fun setupFab() {
        binding.fabGroupAction.setOnClickListener {
            // TODO: Show group actions menu (edit, delete, etc.)
            showGroupActionsMenu()
        }
    }
    
    private fun showGroupActionsMenu() {
        val options = arrayOf("Edit Group", "Delete Group", "Export Transactions")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Group Actions")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditGroupDialog()
                    1 -> showDeleteConfirmation()
                    2 -> { /* TODO: Export transactions */ }
                }
            }
            .show()
    }
    
    private fun showEditGroupDialog() {
        viewModel.groupDetails.value?.let { group ->
            val dialog = EditGroupDialog.newInstance(group)
            dialog.show(supportFragmentManager, "EditGroupDialog")
        }
    }
    
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete this group? Transactions will not be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                // TODO: Delete group
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun setupSortButton() {
        // Observe sort order changes
        viewModel.sortOrder.observe(this) { sortOrder ->
            binding.btnSort.text = sortOrder.displayName.substringBefore(" (")
        }
        
        // Setup sort button click
        binding.btnSort.setOnClickListener {
            showSortOptionsDialog()
        }
    }
    
    private fun showSortOptionsDialog() {
        val currentSort = viewModel.sortOrder.value ?: TransactionSortOrder.getDefault()
        // Only show date and amount sort options for group detail
        val sortOptions = listOf(
            TransactionSortOrder.DATE_DESC,
            TransactionSortOrder.DATE_ASC,
            TransactionSortOrder.AMOUNT_DESC,
            TransactionSortOrder.AMOUNT_ASC
        )
        val sortOptionNames = sortOptions.map { it.displayName }.toTypedArray()
        val currentIndex = sortOptions.indexOf(currentSort).takeIf { it >= 0 } ?: 0
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Sort Transactions")
            .setSingleChoiceItems(sortOptionNames, currentIndex) { dialog, which ->
                viewModel.setSortOrder(sortOptions[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}