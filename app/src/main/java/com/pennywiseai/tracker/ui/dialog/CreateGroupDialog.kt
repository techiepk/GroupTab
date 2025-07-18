package com.pennywiseai.tracker.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.GroupingType
import com.pennywiseai.tracker.databinding.DialogCreateGroupBinding
import com.pennywiseai.tracker.viewmodel.CreateGroupViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CreateGroupDialog : DialogFragment() {
    
    private var _binding: DialogCreateGroupBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CreateGroupViewModel by viewModels()
    
    private var transaction: Transaction? = null
    
    companion object {
        private const val ARG_TRANSACTION_ID = "transaction_id"
        
        fun newInstance(transaction: Transaction? = null): CreateGroupDialog {
            return CreateGroupDialog().apply {
                arguments = Bundle().apply {
                    transaction?.let { putString(ARG_TRANSACTION_ID, it.id) }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog)
        
        // Load transaction if provided
        arguments?.getString(ARG_TRANSACTION_ID)?.let { transactionId ->
            viewModel.loadTransaction(transactionId)
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create Transaction Group")
            .setView(createView())
            .setPositiveButton("Create") { _, _ ->
                createGroup()
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Test Pattern") { _, _ ->
                testPattern()
            }
            .create()
    }
    
    private fun createView(): View {
        _binding = DialogCreateGroupBinding.inflate(layoutInflater)
        
        setupUI()
        observeData()
        
        return binding.root
    }
    
    private fun setupUI() {
        // Setup pattern type spinner
        val patternTypes = arrayOf(
            "Contains",
            "Exact Match", 
            "Starts With",
            "Ends With",
            "Multiple Keywords"
        )
        binding.patternTypeSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            patternTypes
        )
        
        // Setup category spinner
        val categories = TransactionCategory.values().map { category ->
            category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }.toTypedArray()
        
        binding.categorySpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        
        // Pattern text change listener
        binding.patternInput.addTextChangedListener { text ->
            if (text?.isNotEmpty() == true) {
                updatePreviewCount()
            }
        }
        
        // Advanced options toggle
        binding.advancedOptionsToggle.setOnCheckedChangeListener { _, isChecked ->
            binding.advancedOptionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Preview button
        binding.showPreviewButton.setOnClickListener {
            showPreviewTransactions()
        }
    }
    
    private fun observeData() {
        // Observe loaded transaction
        viewModel.transaction.observe(this) { transaction ->
            this.transaction = transaction
            transaction?.let {
                // Pre-fill from transaction
                binding.groupNameInput.setText(extractGroupName(it.merchant))
                binding.patternInput.setText(extractPattern(it.merchant))
                
                // Set category
                val categoryIndex = TransactionCategory.values().indexOf(it.category)
                if (categoryIndex >= 0) {
                    binding.categorySpinner.setSelection(categoryIndex)
                }
                
                // Show example transaction
                binding.exampleTransactionCard.visibility = View.VISIBLE
                binding.exampleMerchant.text = it.merchant
                binding.exampleAmount.text = "₹${String.format("%.0f", Math.abs(it.amount))}"
                binding.exampleDate.text = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date(it.date))
            }
        }
        
        // Observe preview count
        viewModel.previewCount.observe(this) { count ->
            binding.previewText.text = "$count matching transactions"
            binding.showPreviewButton.isEnabled = count > 0
        }
    }
    
    private fun extractGroupName(merchant: String): String {
        // Extract first significant word as group name
        val words = merchant.split(" ", "-", "_")
            .filter { it.length > 2 }
            .map { it.trim() }
        
        return words.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: merchant
    }
    
    private fun extractPattern(merchant: String): String {
        // Extract pattern - for now, just lowercase first word
        // TODO: Use PatternExtractor for smarter extraction
        val words = merchant.split(" ", "-", "_")
            .filter { it.length > 2 }
            .map { it.trim().lowercase() }
        
        return words.firstOrNull() ?: merchant.lowercase()
    }
    
    private fun updatePreviewCount() {
        val pattern = binding.patternInput.text.toString()
        val patternType = binding.patternTypeSpinner.selectedItemPosition
        
        if (pattern.isNotEmpty()) {
            viewModel.updatePreviewCount(pattern, patternType)
        }
    }
    
    private fun testPattern() {
        // TODO: Show pattern test results
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), "Pattern testing coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showPreviewTransactions() {
        // TODO: Show preview dialog with matching transactions
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), "Preview coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createGroup() {
        val name = binding.groupNameInput.text.toString().trim()
        val pattern = binding.patternInput.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.groupNameLayout.error = "Group name required"
            return
        }
        
        if (pattern.isEmpty()) {
            binding.patternLayout.error = "Pattern required"
            return
        }
        
        val patternType = when (binding.patternTypeSpinner.selectedItemPosition) {
            0 -> GroupingType.MERCHANT_FUZZY // Contains
            1 -> GroupingType.MERCHANT_EXACT // Exact
            else -> GroupingType.MERCHANT_FUZZY
        }
        
        val categoryIndex = binding.categorySpinner.selectedItemPosition
        val category = TransactionCategory.values()[categoryIndex]
        
        val applyToExisting = binding.applyToExistingCheckbox.isChecked
        val learnFromPattern = binding.learnFromPatternCheckbox.isChecked
        
        // Additional filters if advanced options are enabled
        val amountMin = if (binding.advancedOptionsToggle.isChecked && binding.amountMinInput.text?.isNotEmpty() == true) {
            binding.amountMinInput.text.toString().toDoubleOrNull()
        } else null
        
        val amountMax = if (binding.advancedOptionsToggle.isChecked && binding.amountMaxInput.text?.isNotEmpty() == true) {
            binding.amountMaxInput.text.toString().toDoubleOrNull()
        } else null
        
        lifecycleScope.launch {
            val success = viewModel.createGroup(
                name = name,
                pattern = pattern,
                patternType = patternType,
                category = category,
                applyToExisting = applyToExisting,
                learnFromPattern = learnFromPattern,
                amountMin = amountMin,
                amountMax = amountMax,
                exampleTransactionId = transaction?.id
            )
            
            // Check if fragment is still attached before showing Toast
            if (isAdded && context != null) {
                if (success) {
                    Toast.makeText(requireContext(), "Group '$name' created successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to create group", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}