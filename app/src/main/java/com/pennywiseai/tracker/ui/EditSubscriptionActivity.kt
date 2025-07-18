package com.pennywiseai.tracker.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.SubscriptionFrequency
import com.pennywiseai.tracker.data.SubscriptionStatus
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.databinding.ActivityEditSubscriptionBinding
import com.pennywiseai.tracker.viewmodel.EditSubscriptionViewModel
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import android.view.View
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import com.pennywiseai.tracker.utils.ThemeColorUtils

class EditSubscriptionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditSubscriptionBinding
    private val viewModel: EditSubscriptionViewModel by viewModels()
    private lateinit var subscription: Subscription
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    companion object {
        private const val EXTRA_SUBSCRIPTION_ID = "subscription_id"
        
        fun start(context: Context, subscriptionId: String) {
            val intent = Intent(context, EditSubscriptionActivity::class.java).apply {
                putExtra(EXTRA_SUBSCRIPTION_ID, subscriptionId)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupSpinners()
        
        val subscriptionId = intent.getStringExtra(EXTRA_SUBSCRIPTION_ID)
        if (subscriptionId == null) {
            finish()
            return
        }
        
        observeData(subscriptionId)
        setupClickListeners()
        loadPaymentHistory(subscriptionId)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Edit Subscription"
        }
    }
    
    private fun setupSpinners() {
        // Frequency spinner
        val frequencies = SubscriptionFrequency.values().map { frequency ->
            when (frequency) {
                SubscriptionFrequency.WEEKLY -> "Weekly"
                SubscriptionFrequency.MONTHLY -> "Monthly"
                SubscriptionFrequency.QUARTERLY -> "Quarterly"
                SubscriptionFrequency.YEARLY -> "Yearly"
            }
        }
        binding.frequencySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            frequencies
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // Status spinner
        val statuses = SubscriptionStatus.values().map { status ->
            status.name.lowercase().replaceFirstChar { it.uppercase() }
        }
        binding.statusSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statuses
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        
        // Category spinner
        val categories = TransactionCategory.values().map { category ->
            category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
        binding.categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
    
    private fun observeData(subscriptionId: String) {
        viewModel.getSubscription(subscriptionId).observe(this) { subscription ->
            if (subscription != null) {
                this.subscription = subscription
                populateFields(subscription)
            } else {
                finish()
            }
        }
    }
    
    private fun populateFields(subscription: Subscription) {
        binding.apply {
            merchantNameEdit.setText(subscription.merchantName)
            amountEdit.setText(subscription.amount.toString())
            descriptionEdit.setText(subscription.description ?: "")
            
            // Set spinners
            frequencySpinner.setSelection(subscription.frequency.ordinal)
            statusSpinner.setSelection(subscription.status.ordinal)
            categorySpinner.setSelection(subscription.category.ordinal)
            
            // Set dates
            startDateText.text = dateFormat.format(Date(subscription.startDate))
            
            if (subscription.endDate != null) {
                endDateText.text = dateFormat.format(Date(subscription.endDate!!))
                endDateEnabled.isChecked = true
                endDateText.isEnabled = true
            } else {
                endDateText.text = "Not set"
                endDateEnabled.isChecked = false
                endDateText.isEnabled = false
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.apply {
            // Date pickers - use the container IDs
            startDateContainer.setOnClickListener {
                showDatePicker(subscription.startDate) { selectedDate ->
                    startDateText.text = dateFormat.format(Date(selectedDate))
                    subscription = subscription.copy(startDate = selectedDate)
                }
            }
            
            endDateContainer.setOnClickListener {
                if (endDateEnabled.isChecked) {
                    val currentEndDate = subscription.endDate ?: System.currentTimeMillis()
                    showDatePicker(currentEndDate) { selectedDate ->
                        endDateText.text = dateFormat.format(Date(selectedDate))
                        subscription = subscription.copy(endDate = selectedDate)
                    }
                }
            }
            
            endDateEnabled.setOnCheckedChangeListener { _, isChecked ->
                endDateText.isEnabled = isChecked
                if (!isChecked) {
                    endDateText.text = "Not set"
                    subscription = subscription.copy(endDate = null)
                } else if (subscription.endDate == null) {
                    // Set default end date to 1 year from start
                    val defaultEndDate = subscription.startDate + (365 * 24 * 60 * 60 * 1000L)
                    endDateText.text = dateFormat.format(Date(defaultEndDate))
                    subscription = subscription.copy(endDate = defaultEndDate)
                }
            }
            
            // Save button
            saveButton.setOnClickListener {
                saveSubscription()
            }
            
            // Cancel button
            cancelButton.setOnClickListener {
                finish()
            }
            
            // Delete button
            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        }
    }
    
    private fun showDatePicker(currentDate: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDate
        }
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                onDateSelected(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun saveSubscription() {
        try {
            val merchantName = binding.merchantNameEdit.text.toString().trim()
            val amount = binding.amountEdit.text.toString().toDoubleOrNull()
            val description = binding.descriptionEdit.text.toString().trim()
            
            if (merchantName.isEmpty()) {
                showError("Merchant name is required")
                return
            }
            
            if (amount == null || amount <= 0) {
                showError("Valid amount is required")
                return
            }
            
            val frequency = SubscriptionFrequency.values()[binding.frequencySpinner.selectedItemPosition]
            val status = SubscriptionStatus.values()[binding.statusSpinner.selectedItemPosition]
            val category = TransactionCategory.values()[binding.categorySpinner.selectedItemPosition]
            
            val updatedSubscription = subscription.copy(
                merchantName = merchantName,
                amount = amount,
                description = description.takeIf { it.isNotEmpty() },
                frequency = frequency,
                status = status,
                category = category,
                active = status == SubscriptionStatus.ACTIVE
            )
            
            viewModel.updateSubscription(updatedSubscription)
            
            Snackbar.make(binding.root, "Subscription updated", Snackbar.LENGTH_SHORT).show()
            finish()
            
        } catch (e: Exception) {
            showError("Error saving subscription: ${e.message}")
        }
    }
    
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
    
    private fun loadPaymentHistory(subscriptionId: String) {
        // Get recent transactions for this subscription
        viewModel.getRecentPayments(subscriptionId, 5).observe(this) { payments ->
            val container = binding.paymentHistoryContainer
            container.removeAllViews()
            
            if (payments.isEmpty()) {
                binding.noPaymentHistory.visibility = android.view.View.VISIBLE
            } else {
                binding.noPaymentHistory.visibility = android.view.View.GONE
                
                payments.forEach { transaction ->
                    val itemView = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(
                            resources.getDimensionPixelSize(R.dimen.spacing_large),
                            resources.getDimensionPixelSize(R.dimen.spacing_medium),
                            resources.getDimensionPixelSize(R.dimen.spacing_large),
                            resources.getDimensionPixelSize(R.dimen.spacing_medium)
                        )
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        isClickable = true
                        isFocusable = true
                        val outValue = android.util.TypedValue()
                        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                        setBackgroundResource(outValue.resourceId)
                    }
                    
                    val textContainer = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }
                    
                    val dateText = TextView(this).apply {
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(transaction.date))
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                        textSize = 14f
                    }
                    
                    val categoryText = TextView(this).apply {
                        text = transaction.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                        setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
                        textSize = 12f
                    }
                    
                    val amountText = TextView(this).apply {
                        text = "-₹${String.format("%.0f", kotlin.math.abs(transaction.amount))}"
                        setTextColor(ThemeColorUtils.getExpenseColor(context))
                        textSize = 16f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    
                    textContainer.addView(dateText)
                    textContainer.addView(categoryText)
                    itemView.addView(textContainer)
                    itemView.addView(amountText)
                    
                    container.addView(itemView)
                    
                    // Add divider
                    val divider = View(this@EditSubscriptionActivity)
                    divider.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        marginStart = resources.getDimensionPixelSize(R.dimen.spacing_large)
                        marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_large)
                    }
                    divider.setBackgroundColor(ContextCompat.getColor(this@EditSubscriptionActivity, com.google.android.material.R.color.material_on_surface_stroke))
                    divider.alpha = 0.2f
                    container.addView(divider)
                }
            }
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Subscription")
            .setMessage("Are you sure you want to delete this subscription?\n\nThis will:\n• Remove the subscription from your list\n• Stop tracking future payments\n• Keep past transaction history")
            .setPositiveButton("Delete") { _, _ ->
                deleteSubscription()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteSubscription() {
        viewModel.deleteSubscription(subscription)
        Snackbar.make(binding.root, "Subscription deleted", Snackbar.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}