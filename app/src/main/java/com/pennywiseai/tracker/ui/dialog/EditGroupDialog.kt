package com.pennywiseai.tracker.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.TransactionGroup
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.databinding.DialogEditGroupBinding
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import android.app.Application
import android.util.Log

class EditGroupDialog : DialogFragment() {
    
    companion object {
        private const val TAG = "EditGroupDialog"
        private const val ARG_GROUP = "arg_group"
        
        fun newInstance(group: TransactionGroup): EditGroupDialog {
            return EditGroupDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_GROUP, group)
                }
            }
        }
    }
    
    private var _binding: DialogEditGroupBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EditGroupViewModel by viewModels()
    
    private val group by lazy {
        arguments?.getParcelable<TransactionGroup>(ARG_GROUP)
            ?: throw IllegalArgumentException("Group required")
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditGroupBinding.inflate(layoutInflater)
        
        setupViews()
        setupButtons()
        
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Group")
            .setView(binding.root)
            .create()
    }
    
    private fun setupViews() {
        // Pre-fill current values
        binding.groupNameInput.setText(group.name)
        binding.merchantPatternInput.setText(group.merchantPattern)
        
        // Setup category dropdown
        val categories = TransactionCategory.values().map { category ->
            category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        }
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_menu_popup_item, categories)
        binding.categoryDropdown.setAdapter(adapter)
        
        // Set current category
        val currentCategory = group.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
        binding.categoryDropdown.setText(currentCategory, false)
    }
    
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                saveGroup()
            }
        }
        
        // Observe save result
        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                dismiss()
            }
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                binding.groupNameLayout.error = it
            }
        }
    }
    
    private fun validateInput(): Boolean {
        var isValid = true
        
        val name = binding.groupNameInput.text?.toString()?.trim()
        if (name.isNullOrEmpty()) {
            binding.groupNameLayout.error = "Group name is required"
            isValid = false
        } else {
            binding.groupNameLayout.error = null
        }
        
        val pattern = binding.merchantPatternInput.text?.toString()?.trim()
        if (pattern.isNullOrEmpty()) {
            binding.merchantPatternLayout.error = "Merchant pattern is required"
            isValid = false
        } else {
            binding.merchantPatternLayout.error = null
        }
        
        if (binding.categoryDropdown.text.isNullOrEmpty()) {
            binding.categoryLayout.error = "Please select a category"
            isValid = false
        } else {
            binding.categoryLayout.error = null
        }
        
        return isValid
    }
    
    private fun saveGroup() {
        val name = binding.groupNameInput.text.toString().trim()
        val pattern = binding.merchantPatternInput.text.toString().trim()
        
        val categoryText = binding.categoryDropdown.text.toString()
        val category = TransactionCategory.values().find { 
            it.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } == categoryText
        } ?: TransactionCategory.OTHER
        
        viewModel.updateGroup(group, name, pattern, category)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class EditGroupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val groupRepository = TransactionGroupRepository(AppDatabase.getDatabase(application))
    
    private val _saveSuccess = androidx.lifecycle.MutableLiveData<Boolean>()
    val saveSuccess: androidx.lifecycle.LiveData<Boolean> = _saveSuccess
    
    private val _error = androidx.lifecycle.MutableLiveData<String?>()
    val error: androidx.lifecycle.LiveData<String?> = _error
    
    fun updateGroup(
        group: TransactionGroup,
        name: String,
        merchantPattern: String,
        category: TransactionCategory
    ) {
        viewModelScope.launch {
            try {
                val updatedGroup = group.copy(
                    name = name,
                    merchantPattern = merchantPattern,
                    category = category,
                    lastUpdated = System.currentTimeMillis()
                )
                
                groupRepository.updateGroup(updatedGroup)
                _saveSuccess.value = true
                
            } catch (e: Exception) {
                Log.e("EditGroupViewModel", "Error updating group", e)
                _error.value = "Failed to update group: ${e.message}"
            }
        }
    }
}