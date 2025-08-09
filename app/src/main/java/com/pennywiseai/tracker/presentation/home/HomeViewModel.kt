package com.pennywiseai.tracker.presentation.home

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.manager.InAppUpdateManager
import com.pennywiseai.tracker.data.repository.LlmRepository
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
    private val llmRepository: LlmRepository,
    private val inAppUpdateManager: InAppUpdateManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadHomeData()
    }
    
    private fun loadHomeData() {
        viewModelScope.launch {
            // Load current month breakdown
            transactionRepository.getCurrentMonthBreakdown().collect { breakdown ->
                _uiState.value = _uiState.value.copy(
                    currentMonthTotal = breakdown.total,
                    currentMonthIncome = breakdown.income,
                    currentMonthExpenses = breakdown.expenses
                )
                calculateMonthlyChange()
            }
        }
        
        viewModelScope.launch {
            // Load last month breakdown
            transactionRepository.getLastMonthBreakdown().collect { breakdown ->
                _uiState.value = _uiState.value.copy(
                    lastMonthTotal = breakdown.total,
                    lastMonthIncome = breakdown.income,
                    lastMonthExpenses = breakdown.expenses
                )
                calculateMonthlyChange()
            }
        }
        
        viewModelScope.launch {
            // Load recent transactions (last 3)
            transactionRepository.getRecentTransactions(limit = 3).collect { transactions ->
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
        
        val percentChange = when {
            // Both are zero - no change
            last == BigDecimal.ZERO && current == BigDecimal.ZERO -> 0
            
            // Last month was zero - new activity
            last == BigDecimal.ZERO -> {
                if (current > BigDecimal.ZERO) 100 else -100
            }
            
            // Sign change (deficit to surplus or vice versa)
            (last < BigDecimal.ZERO && current > BigDecimal.ZERO) ||
            (last > BigDecimal.ZERO && current < BigDecimal.ZERO) -> {
                // Use special value to indicate sign change in UI
                Int.MAX_VALUE
            }
            
            // Both negative (both deficits) - use absolute values
            last < BigDecimal.ZERO && current < BigDecimal.ZERO -> {
                val lastAbs = last.abs()
                val currentAbs = current.abs()
                val changeAbs = currentAbs - lastAbs
                ((changeAbs.toDouble() / lastAbs.toDouble()) * 100).toInt()
            }
            
            // Normal case (both positive)
            else -> {
                ((change.toDouble() / last.toDouble()) * 100).toInt()
            }
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
    
    fun updateSystemPrompt() {
        viewModelScope.launch {
            try {
                llmRepository.updateSystemPrompt()
            } catch (e: Exception) {
                // Handle error silently or add error state if needed
            }
        }
    }
    
    fun showBreakdownDialog() {
        _uiState.value = _uiState.value.copy(showBreakdownDialog = true)
    }
    
    fun hideBreakdownDialog() {
        _uiState.value = _uiState.value.copy(showBreakdownDialog = false)
    }
    
    /**
     * Checks for app updates using Google Play In-App Updates.
     * Should be called with the current activity context.
     */
    fun checkForAppUpdate(activity: ComponentActivity) {
        inAppUpdateManager.checkForUpdate(activity)
    }
    
    override fun onCleared() {
        super.onCleared()
        inAppUpdateManager.cleanup()
    }
}

data class HomeUiState(
    val currentMonthTotal: BigDecimal = BigDecimal.ZERO,
    val currentMonthIncome: BigDecimal = BigDecimal.ZERO,
    val currentMonthExpenses: BigDecimal = BigDecimal.ZERO,
    val lastMonthTotal: BigDecimal = BigDecimal.ZERO,
    val lastMonthIncome: BigDecimal = BigDecimal.ZERO,
    val lastMonthExpenses: BigDecimal = BigDecimal.ZERO,
    val monthlyChange: BigDecimal = BigDecimal.ZERO,
    val monthlyChangePercent: Int = 0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val upcomingSubscriptions: List<SubscriptionEntity> = emptyList(),
    val upcomingSubscriptionsTotal: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val showBreakdownDialog: Boolean = false
)