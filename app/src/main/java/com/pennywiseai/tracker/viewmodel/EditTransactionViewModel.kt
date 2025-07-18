package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import android.util.Log

class EditTransactionViewModel(
    application: Application,
    private val transactionId: String
) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "EditTransactionViewModel"
    }
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    
    private val _transaction = MutableLiveData<Transaction?>()
    val transaction: LiveData<Transaction?> = _transaction
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    init {
        loadTransaction()
    }
    
    private fun loadTransaction() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val transactionData = repository.getTransactionByIdSync(transactionId)
                _transaction.value = transactionData
            } catch (e: Exception) {
                Log.e(TAG, "Error loading transaction", e)
                _error.value = "Failed to load transaction"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateTransaction(
        amount: Double,
        merchant: String,
        category: TransactionCategory,
        date: Long,
        description: String?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val currentTransaction = _transaction.value ?: return@launch
                val updatedTransaction = currentTransaction.copy(
                    amount = amount,
                    merchant = merchant,
                    category = category,
                    date = date,
                    // Note: We don't have a description field in Transaction data class
                    // You might want to add it if needed
                )
                
                repository.updateTransaction(updatedTransaction)
                _saveSuccess.value = true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating transaction", e)
                _error.value = "Failed to update transaction: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteTransaction() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _transaction.value?.let { transaction ->
                    repository.deleteTransaction(transaction)
                    _saveSuccess.value = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting transaction", e)
                _error.value = "Failed to delete transaction: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class EditTransactionViewModelFactory(
    private val application: Application,
    private val transactionId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditTransactionViewModel(application, transactionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}