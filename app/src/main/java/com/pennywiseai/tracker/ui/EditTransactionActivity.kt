package com.pennywiseai.tracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.databinding.ActivityEditTransactionBinding
import com.pennywiseai.tracker.viewmodel.EditTransactionViewModel
import com.pennywiseai.tracker.viewmodel.EditTransactionViewModelFactory
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class EditTransactionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditTransactionBinding
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var selectedDate: Long = System.currentTimeMillis()
    
    companion object {
        private const val EXTRA_TRANSACTION_ID = "transaction_id"
        
        fun start(context: Context, transactionId: String) {
            val intent = Intent(context, EditTransactionActivity::class.java).apply {
                putExtra(EXTRA_TRANSACTION_ID, transactionId)
            }
            context.startActivity(intent)
        }
    }
    
    private val transactionId by lazy {
        intent.getStringExtra(EXTRA_TRANSACTION_ID) ?: throw IllegalArgumentException("Transaction ID required")
    }
    
    private val viewModel: EditTransactionViewModel by viewModels {
        EditTransactionViewModelFactory(application, transactionId)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupCategoryDropdown()
        setupDatePicker()
        setupButtons()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupCategoryDropdown() {
        val categories = TransactionCategory.values().map { category ->
            category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
        val adapter = ArrayAdapter(this, R.layout.dropdown_menu_popup_item, categories)
        binding.categoryDropdown.setAdapter(adapter)
    }
    
    private fun setupDatePicker() {
        binding.dateInput.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select transaction date")
                .setSelection(selectedDate)
                .build()
                
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = selection
                binding.dateInput.setText(dateFormat.format(Date(selection)))
            }
            
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
        
        binding.dateLayout.setEndIconOnClickListener {
            binding.dateInput.performClick()
        }
    }
    
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener { finish() }
        
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveTransaction()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.transaction.observe(this) { transaction ->
            transaction?.let {
                populateFields(it)
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
        
        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                setResult(RESULT_OK)
                finish()
            }
        }
    }
    
    private fun populateFields(transaction: com.pennywiseai.tracker.data.Transaction) {
        // Amount
        binding.amountInput.setText(String.format("%.2f", abs(transaction.amount)))
        
        // Transaction type
        if (transaction.amount < 0) {
            binding.transactionTypeToggle.check(binding.btnDebit.id)
        } else {
            binding.transactionTypeToggle.check(binding.btnCredit.id)
        }
        
        // Merchant
        binding.merchantInput.setText(transaction.merchant)
        
        // Category
        val categoryName = transaction.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        binding.categoryDropdown.setText(categoryName, false)
        
        // Date
        selectedDate = transaction.date
        binding.dateInput.setText(dateFormat.format(Date(transaction.date)))
    }
    
    private fun validateInput(): Boolean {
        var isValid = true
        
        // Validate amount
        val amountText = binding.amountInput.text?.toString()?.trim()
        if (amountText.isNullOrEmpty()) {
            binding.amountLayout.error = "Amount is required"
            isValid = false
        } else {
            try {
                amountText.toDouble()
                binding.amountLayout.error = null
            } catch (e: NumberFormatException) {
                binding.amountLayout.error = "Invalid amount"
                isValid = false
            }
        }
        
        // Validate merchant
        val merchantText = binding.merchantInput.text?.toString()?.trim()
        if (merchantText.isNullOrEmpty()) {
            binding.merchantLayout.error = "Merchant name is required"
            isValid = false
        } else {
            binding.merchantLayout.error = null
        }
        
        // Validate category
        if (binding.categoryDropdown.text.isNullOrEmpty()) {
            binding.categoryLayout.error = "Please select a category"
            isValid = false
        } else {
            binding.categoryLayout.error = null
        }
        
        // Validate transaction type
        if (binding.transactionTypeToggle.checkedButtonId == View.NO_ID) {
            Snackbar.make(binding.root, "Please select transaction type", Snackbar.LENGTH_SHORT).show()
            isValid = false
        }
        
        return isValid
    }
    
    private fun saveTransaction() {
        val amount = binding.amountInput.text.toString().toDouble()
        val isDebit = binding.transactionTypeToggle.checkedButtonId == binding.btnDebit.id
        val finalAmount = if (isDebit) -amount else amount
        
        val merchant = binding.merchantInput.text.toString().trim()
        
        val categoryText = binding.categoryDropdown.text.toString()
        val category = TransactionCategory.values().find { 
            it.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } == categoryText
        } ?: TransactionCategory.OTHER
        
        val description = binding.descriptionInput.text?.toString()?.trim()
        
        viewModel.updateTransaction(
            amount = finalAmount,
            merchant = merchant,
            category = category,
            date = selectedDate,
            description = description
        )
    }
    
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_transaction, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTransaction()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}