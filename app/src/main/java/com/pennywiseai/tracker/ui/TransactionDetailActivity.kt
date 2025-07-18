package com.pennywiseai.tracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.databinding.ActivityTransactionDetailBinding
import com.pennywiseai.tracker.viewmodel.TransactionDetailViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.pennywiseai.tracker.utils.ThemeColorUtils

class TransactionDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTransactionDetailBinding
    private val viewModel: TransactionDetailViewModel by viewModels()
    private lateinit var transaction: Transaction
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    
    companion object {
        private const val EXTRA_TRANSACTION_ID = "transaction_id"
        
        fun start(context: Context, transactionId: String) {
            val intent = Intent(context, TransactionDetailActivity::class.java).apply {
                putExtra(EXTRA_TRANSACTION_ID, transactionId)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        
        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)
        if (transactionId == null) {
            finish()
            return
        }
        
        observeData(transactionId)
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Transaction Details"
        }
    }
    
    private fun observeData(transactionId: String) {
        viewModel.getTransaction(transactionId).observe(this) { transaction ->
            if (transaction != null) {
                this.transaction = transaction
                displayTransactionDetails(transaction)
            } else {
                finish()
            }
        }
    }
    
    private fun displayTransactionDetails(transaction: Transaction) {
        binding.apply {
            // Hero section
            merchantName.text = transaction.merchant
            
            // Format amount with sign and color
            val absAmount = kotlin.math.abs(transaction.amount)
            if (transaction.amount >= 0) {
                transactionAmount.text = "+₹${String.format("%.2f", absAmount)}"
            } else {
                transactionAmount.text = "-₹${String.format("%.2f", absAmount)}"
            }
            transactionAmount.setTextColor(ThemeColorUtils.getColorForAmount(this@TransactionDetailActivity, transaction.amount))
            
            transactionDate.text = SimpleDateFormat("MMMM dd, yyyy • h:mm a", Locale.getDefault())
                .format(Date(transaction.date))
            
            // Category and Type chips
            categoryChip.text = transaction.category.name.replace("_", " ")
                .lowercase().replaceFirstChar { it.uppercase() }
            typeChip.text = transaction.transactionType.name.replace("_", " ")
                .lowercase().replaceFirstChar { it.uppercase() }
            
            // Confidence indicator
            val confidencePercent = (transaction.confidence * 100).toInt()
            confidenceIndicator.progress = confidencePercent
            confidenceScore.text = "$confidencePercent% confidence"
            
            // Transaction details
            transactionId.text = transaction.id
            paymentMethod.text = if (!transaction.upiId.isNullOrEmpty()) "UPI" else "Unknown"
            
            // UPI ID visibility
            if (transaction.upiId.isNullOrEmpty()) {
                upiIdContainer.visibility = android.view.View.GONE
            } else {
                upiIdContainer.visibility = android.view.View.VISIBLE
                upiId.text = transaction.upiId
            }
            
            // Balance - if available in SMS
            availableBalance.text = extractBalanceFromSms(transaction.rawSms) ?: "Not available"
            
            // Raw SMS
            rawSmsContent.text = transaction.rawSms
            
            // Dynamic colors will automatically handle appropriate colors
            // No need to manually set colors - Material You handles this!
        }
    }
    
    private fun extractBalanceFromSms(sms: String): String? {
        // Extract balance from SMS patterns like "Avl Bal Rs.5000.00" or "Available balance: Rs.15,230.50"
        val balanceRegex = """(?:Avl Bal|Available balance)[:\s]*Rs\.?(\d{1,3}(?:,\d{3})*(?:\.\d{2})?)""".toRegex(RegexOption.IGNORE_CASE)
        return balanceRegex.find(sms)?.let { "₹${it.groupValues[1]}" }
    }
    
    private fun setupClickListeners() {
        binding.editButton.setOnClickListener {
            EditTransactionActivity.start(this, transaction.id)
        }
        
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
        
        binding.fabActions.setOnClickListener {
            showQuickActionsMenu()
        }
    }
    
    private fun showQuickActionsMenu() {
        // Create a bottom sheet or popup menu for quick actions
        val items = if (transaction.transactionType != com.pennywiseai.tracker.data.TransactionType.SUBSCRIPTION) {
            arrayOf("Edit Transaction", "Convert to Subscription", "Delete Transaction", "Share Details", "Report Issue")
        } else {
            arrayOf("Edit Transaction", "Delete Transaction", "Share Details", "Report Issue")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Quick Actions")
            .setItems(items) { _, which ->
                if (transaction.transactionType != com.pennywiseai.tracker.data.TransactionType.SUBSCRIPTION) {
                    when (which) {
                        0 -> EditTransactionActivity.start(this, transaction.id)
                        1 -> showConvertToSubscriptionDialog()
                        2 -> showDeleteConfirmation()
                        3 -> shareTransactionDetails()
                        4 -> reportIssue()
                    }
                } else {
                    when (which) {
                        0 -> EditTransactionActivity.start(this, transaction.id)
                        1 -> showDeleteConfirmation()
                        2 -> shareTransactionDetails()
                        3 -> reportIssue()
                    }
                }
            }
            .show()
    }
    
    private fun shareTransactionDetails() {
        val shareText = """
            Transaction Details:
            
            Merchant: ${transaction.merchant}
            Amount: ₹${String.format("%.2f", transaction.amount)}
            Date: ${SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(transaction.date))}
            Category: ${transaction.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}
            Type: ${transaction.transactionType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}
            
            Shared from Transaction Tracker
        """.trimIndent()
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share Transaction Details"))
    }
    
    private fun reportIssue() {
        // Simple toast for now - could be expanded to email or bug reporting
        android.widget.Toast.makeText(this, "Feature coming soon!", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun showConvertToSubscriptionDialog() {
        val frequencies = arrayOf("Weekly", "Monthly", "Quarterly", "Yearly")
        var selectedFrequency = com.pennywiseai.tracker.data.SubscriptionFrequency.MONTHLY
        
        AlertDialog.Builder(this)
            .setTitle("Convert to Subscription")
            .setMessage("Convert this transaction to a recurring subscription?")
            .setSingleChoiceItems(frequencies, 1) { _, which ->
                selectedFrequency = when (which) {
                    0 -> com.pennywiseai.tracker.data.SubscriptionFrequency.WEEKLY
                    1 -> com.pennywiseai.tracker.data.SubscriptionFrequency.MONTHLY
                    2 -> com.pennywiseai.tracker.data.SubscriptionFrequency.QUARTERLY
                    3 -> com.pennywiseai.tracker.data.SubscriptionFrequency.YEARLY
                    else -> com.pennywiseai.tracker.data.SubscriptionFrequency.MONTHLY
                }
            }
            .setPositiveButton("Convert") { _, _ ->
                viewModel.convertToSubscription(transaction, selectedFrequency)
                android.widget.Toast.makeText(this, "Converted to subscription", android.widget.Toast.LENGTH_SHORT).show()
                finish() // Close and refresh
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction(transaction)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun getCategoryColor(category: String): Int {
        return when (category) {
            "FOOD_DINING" -> 0xFFFF6B6B.toInt()
            "TRANSPORTATION" -> 0xFF4ECDC4.toInt()
            "SHOPPING" -> 0xFF45B7D1.toInt()
            "ENTERTAINMENT" -> 0xFF96CEB4.toInt()
            "BILLS_UTILITIES" -> 0xFFFECA57.toInt()
            "GROCERIES" -> 0xFF6C5CE7.toInt()
            "HEALTHCARE" -> 0xFFFF7675.toInt()
            "SUBSCRIPTION" -> 0xFFA29BFE.toInt()
            else -> 0xFF74B9FF.toInt()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.transaction_detail_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_edit -> {
                EditTransactionActivity.start(this, transaction.id)
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}