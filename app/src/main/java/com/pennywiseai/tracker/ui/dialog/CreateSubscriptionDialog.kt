package com.pennywiseai.tracker.ui.dialog

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.SubscriptionFrequency
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.databinding.DialogCreateSubscriptionBinding
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.viewmodel.CreateSubscriptionViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class CreateSubscriptionDialog : DialogFragment() {
    
    private var _binding: DialogCreateSubscriptionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CreateSubscriptionViewModel by viewModels()
    private val repository by lazy { TransactionRepository(AppDatabase.getDatabase(requireContext())) }
    
    private var transaction: Transaction? = null
    private var selectedFrequency = SubscriptionFrequency.MONTHLY
    private var selectedStartDate = System.currentTimeMillis()
    
    companion object {
        private const val ARG_TRANSACTION_ID = "arg_transaction_id"
        
        fun newInstance(transaction: Transaction): CreateSubscriptionDialog {
            return CreateSubscriptionDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TRANSACTION_ID, transaction.id)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Dialog)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Load transaction from database
        arguments?.getString(ARG_TRANSACTION_ID)?.let { transactionId ->
            lifecycleScope.launch {
                transaction = repository.getTransactionByIdSync(transactionId)
                transaction?.let { txn ->
                    setupUI()
                    setupListeners()
                    observeViewModel()
                    
                    // Check for duplicates and suggest frequency
                    viewModel.checkForDuplicateSubscription(txn.merchant)
                    viewModel.suggestFrequency(txn)
                }
            }
        }
    }
    
    private fun setupUI() {
        val transaction = this.transaction ?: return
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        
        binding.apply {
            // Populate merchant info
            merchantName.text = transaction.merchant
            transactionAmount.text = currencyFormat.format(kotlin.math.abs(transaction.amount))
            
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            transactionDate.text = "From transaction on ${dateFormat.format(Date(transaction.date))}"
            
            // Set default start date to transaction date
            selectedStartDate = transaction.date
            startDateInput.setText(dateFormat.format(Date(selectedStartDate)))
            
            // Default frequency selection (Monthly)
            chipMonthly.isChecked = true
        }
    }
    
    private fun setupListeners() {
        binding.apply {
            // Frequency selection
            frequencyChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    selectedFrequency = when (checkedIds.first()) {
                        R.id.chip_weekly -> SubscriptionFrequency.WEEKLY
                        R.id.chip_monthly -> SubscriptionFrequency.MONTHLY
                        R.id.chip_quarterly -> SubscriptionFrequency.QUARTERLY
                        R.id.chip_yearly -> SubscriptionFrequency.YEARLY
                        else -> SubscriptionFrequency.MONTHLY
                    }
                }
            }
            
            // Start date picker
            startDateInput.setOnClickListener {
                showDatePicker()
            }
            
            // Action buttons
            cancelButton.setOnClickListener {
                dismiss()
            }
            
            createButton.setOnClickListener {
                createSubscription()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.suggestedFrequency.observe(viewLifecycleOwner) { suggestedFrequency ->
            suggestedFrequency?.let { frequency ->
                // Update UI to show suggested frequency
                binding.apply {
                    when (frequency) {
                        SubscriptionFrequency.WEEKLY -> chipWeekly.isChecked = true
                        SubscriptionFrequency.MONTHLY -> chipMonthly.isChecked = true
                        SubscriptionFrequency.QUARTERLY -> chipQuarterly.isChecked = true
                        SubscriptionFrequency.YEARLY -> chipYearly.isChecked = true
                    }
                }
                selectedFrequency = frequency
            }
        }
        
        viewModel.isDuplicateWarning.observe(viewLifecycleOwner) { showWarning ->
            binding.duplicateWarningCard.visibility = if (showWarning) View.VISIBLE else View.GONE
        }
        
        viewModel.isCreating.observe(viewLifecycleOwner) { isCreating ->
            binding.apply {
                createButton.isEnabled = !isCreating
                cancelButton.isEnabled = !isCreating
                
                if (isCreating) {
                    createButton.text = "Creating..."
                } else {
                    createButton.text = "Create Subscription"
                }
            }
        }
        
        viewModel.creationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is CreateSubscriptionViewModel.CreationResult.Success -> {
                        Toast.makeText(
                            requireContext(),
                            "Subscription created for ${it.subscription.merchantName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }
                    is CreateSubscriptionViewModel.CreationResult.Error -> {
                        Toast.makeText(
                            requireContext(),
                            it.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                viewModel.clearResult()
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedStartDate
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedStartDate = calendar.timeInMillis
                
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                binding.startDateInput.setText(dateFormat.format(Date(selectedStartDate)))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun createSubscription() {
        val transaction = this.transaction ?: return
        val customAmount = binding.amountInput.text.toString().toDoubleOrNull()
        val description = binding.descriptionInput.text.toString().takeIf { it.isNotBlank() }
        
        viewModel.createSubscriptionFromTransaction(
            transaction = transaction,
            frequency = selectedFrequency,
            startDate = selectedStartDate,
            customAmount = customAmount,
            description = description
        )
    }
    
    override fun onStart() {
        super.onStart()
        
        // Make dialog take up most of the screen width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}