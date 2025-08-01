package com.pennywiseai.tracker.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.repository.SubscriptionRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.worker.SmsReaderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadHomeData()
    }
    
    private fun loadHomeData() {
        viewModelScope.launch {
            // Load current month total
            transactionRepository.getCurrentMonthTotal().collect { currentTotal ->
                _uiState.value = _uiState.value.copy(currentMonthTotal = currentTotal)
                calculateMonthlyChange()
            }
        }
        
        viewModelScope.launch {
            // Load last month total
            transactionRepository.getLastMonthTotal().collect { lastTotal ->
                _uiState.value = _uiState.value.copy(lastMonthTotal = lastTotal)
                calculateMonthlyChange()
            }
        }
        
        viewModelScope.launch {
            // Load recent transactions (last 5)
            transactionRepository.getRecentTransactions(limit = 5).collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    recentTransactions = transactions,
                    isLoading = false
                )
            }
        }
        
        viewModelScope.launch {
            // Load all active subscriptions
            subscriptionRepository.getActiveSubscriptions().collect { subscriptions ->
                val totalAmount = subscriptions.sumOf { it.amount }
                _uiState.value = _uiState.value.copy(
                    upcomingSubscriptions = subscriptions,
                    upcomingSubscriptionsTotal = totalAmount
                )
            }
        }
    }
    
    private fun calculateMonthlyChange() {
        val current = _uiState.value.currentMonthTotal
        val last = _uiState.value.lastMonthTotal
        
        val change = current - last
        val percentChange = if (last > BigDecimal.ZERO) {
            ((change.toDouble() / last.toDouble()) * 100).toInt()
        } else if (current > BigDecimal.ZERO) {
            100 // If last month was 0 but current month has expenses
        } else {
            0
        }
        
        _uiState.value = _uiState.value.copy(
            monthlyChange = change,
            monthlyChangePercent = percentChange
        )
    }
    
    fun scanSmsMessages() {
        val workRequest = OneTimeWorkRequestBuilder<SmsReaderWorker>()
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            SmsReaderWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        // Update UI to show scanning
        _uiState.value = _uiState.value.copy(isScanning = true)
        
        // Reset scanning state after a delay (WorkManager doesn't provide easy progress)
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }
}

data class HomeUiState(
    val currentMonthTotal: BigDecimal = BigDecimal.ZERO,
    val lastMonthTotal: BigDecimal = BigDecimal.ZERO,
    val monthlyChange: BigDecimal = BigDecimal.ZERO,
    val monthlyChangePercent: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val upcomingSubscriptions: List<SubscriptionEntity> = emptyList(),
    val upcomingSubscriptionsTotal: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = true,
    val isScanning: Boolean = false
)