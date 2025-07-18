package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.AppSettings
import com.pennywiseai.tracker.data.ScanProgress
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.llm.TransactionClassifier
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.sms.HistoricalSmsScanner
import com.pennywiseai.tracker.llm.LLMTransactionExtractor
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.util.Log
import java.util.*
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.io.File
import java.text.DecimalFormat
import com.pennywiseai.tracker.firebase.FirebaseHelper
import com.pennywiseai.tracker.data.TransactionGroup
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import com.pennywiseai.tracker.grouping.TransactionGroupingService
import com.pennywiseai.tracker.data.TimeRange
import java.text.SimpleDateFormat
import java.util.Date
import com.pennywiseai.tracker.utils.AIInsightsGenerator
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.SharedEventBus
import com.pennywiseai.tracker.background.ScanWorker
import com.pennywiseai.tracker.logging.LogStreamManager

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    private val groupRepository = TransactionGroupRepository(AppDatabase.getDatabase(application))
    private val llmExtractor = LLMTransactionExtractor(application)
    private val smsScanner = HistoricalSmsScanner(application, repository, llmExtractor)
    private val classifier = TransactionClassifier(application)
    private val aiInsightsGenerator = AIInsightsGenerator(repository, groupRepository)
    
    private val _monthlySpending = MutableLiveData<Double>()
    val monthlySpending: LiveData<Double> = _monthlySpending
    
    private val _monthlyIncome = MutableLiveData<Double>()
    val monthlyIncome: LiveData<Double> = _monthlyIncome
    
    private val _monthlyNet = MutableLiveData<Double>()
    val monthlyNet: LiveData<Double> = _monthlyNet
    
    private val _transactionCount = MutableLiveData<Int>()
    val transactionCount: LiveData<Int> = _transactionCount

    private val _previousTransactionCount = MutableLiveData<Int>()
    val previousTransactionCount: LiveData<Int> = _previousTransactionCount
    
    private val _topCategory = MutableLiveData<String?>()
    val topCategory: LiveData<String?> = _topCategory
    
    private val _scanProgress = MutableLiveData<ScanProgress?>()
    val scanProgress: LiveData<ScanProgress?> = _scanProgress
    
    private val _aiStatus = MutableLiveData<Boolean>()
    val aiStatus: LiveData<Boolean> = _aiStatus
    
    private val _todayClassifiedCount = MutableLiveData<Int>()
    val todayClassifiedCount: LiveData<Int> = _todayClassifiedCount
    
    private val _scanTimeframeDays = MutableLiveData<Int>()
    val scanTimeframeDays: LiveData<Int> = _scanTimeframeDays
    
    private val _llmDebugInfo = MutableLiveData<LLMDebugInfo>()
    val llmDebugInfo: LiveData<LLMDebugInfo> = _llmDebugInfo
    
    private val _topGroups = MutableLiveData<List<TransactionGroup>>()
    val topGroups: LiveData<List<TransactionGroup>> = _topGroups
    
    private val _selectedTimeRange = MutableLiveData<TimeRange>(TimeRange.THIRTY_DAYS)
    val selectedTimeRange: LiveData<TimeRange> = _selectedTimeRange
    
    private val _categorySpending = MutableLiveData<List<com.pennywiseai.tracker.database.CategorySpending>>()
    val categorySpending: LiveData<List<com.pennywiseai.tracker.database.CategorySpending>> = _categorySpending
    
    private val _currentMonthDisplay = MutableLiveData<String>()
    val currentMonthDisplay: LiveData<String> = _currentMonthDisplay
    
    private val _dateRangeDisplay = MutableLiveData<String>()
    val dateRangeDisplay: LiveData<String> = _dateRangeDisplay
    
    private val _aiInsights = MutableLiveData<List<AIInsightsGenerator.AIInsight>>()
    val aiInsights: LiveData<List<AIInsightsGenerator.AIInsight>> = _aiInsights
    
    private val _currentInsight = MutableLiveData<AIInsightsGenerator.AIInsight?>()
    val currentInsight: LiveData<AIInsightsGenerator.AIInsight?> = _currentInsight
    
    // New dashboard data
    private val _spendingComparison = MutableLiveData<SpendingComparison?>()
    val spendingComparison: LiveData<SpendingComparison?> = _spendingComparison
    
    private val _activeSubscriptions = MutableLiveData<List<com.pennywiseai.tracker.data.Subscription>>()
    val activeSubscriptions: LiveData<List<com.pennywiseai.tracker.data.Subscription>> = _activeSubscriptions
    
    private val _recentTransactions = MutableLiveData<List<Transaction>>()
    val recentTransactions: LiveData<List<Transaction>> = _recentTransactions
    
    private val _topCategoryData = MutableLiveData<TopCategoryData?>()
    val topCategoryData: LiveData<TopCategoryData?> = _topCategoryData
    
    val aiInsight: LiveData<AIInsightsGenerator.AIInsight?> = _currentInsight
    
    // Performance tracking
    private var lastProcessingTime = 0L
    private var totalProcessingTime = 0L
    private var processedCount = 0
    
    data class SpendingComparison(
        val lastMonthAmount: Double,
        val currentMonthAmount: Double,
        val percentageChange: Double,
        val lastMonthIncome: Double = 0.0,
        val currentMonthIncome: Double = 0.0,
        val incomePercentageChange: Double = 0.0
    )
    
    data class TopCategoryData(
        val category: TransactionCategory,
        val categoryName: String,
        val amount: Double,
        val transactionCount: Int
    )
    
    init {
        loadDashboardData()
        initializeClassifier()
        initializeLLMExtractor()
        loadTodayClassifiedCount()
        loadScanTimeframe()
        loadLLMDebugInfo()
        loadTopGroups()
        loadCategorySpending()
        loadAIInsights()
        observeTransactions()
    }
    
    private fun initializeClassifier() {
        viewModelScope.launch {
            _aiStatus.postValue(false)
            val isReady = classifier.initialize()
            _aiStatus.postValue(isReady)
        }
    }
    
    private fun initializeLLMExtractor() {
        viewModelScope.launch {
            try {
                Log.i("DashboardViewModel", "🔧 Initializing LLM Transaction Extractor...")
                val isReady = llmExtractor.initialize()
                if (isReady) {
                    Log.i("DashboardViewModel", "✅ LLM Extractor initialized successfully")
                } else {
                    Log.w("DashboardViewModel", "⚠️ LLM Extractor initialization failed")
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "❌ Error initializing LLM Extractor: ${e.message}")
            }
        }
    }
    
    fun reinitializeClassifier() {
        viewModelScope.launch {
            try {
                Log.i("DashboardViewModel", "🔄 Reinitializing classifier after model download...")
                _aiStatus.postValue(false)
                val isReady = classifier.initialize()
                _aiStatus.postValue(isReady)
                if (isReady) {
                    Log.i("DashboardViewModel", "✅ Classifier reinitialized successfully")
                } else {
                    Log.w("DashboardViewModel", "⚠️ Classifier reinitialization failed")
                }
                
                // Also reinitialize the LLM extractor
                Log.i("DashboardViewModel", "🔄 Reinitializing LLM Extractor after model download...")
                val extractorReady = llmExtractor.initialize()
                if (extractorReady) {
                    Log.i("DashboardViewModel", "✅ LLM Extractor reinitialized successfully")
                } else {
                    Log.w("DashboardViewModel", "⚠️ LLM Extractor reinitialization failed")
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "❌ Error reinitializing: ${e.message}")
                _aiStatus.postValue(false)
            }
        }
    }
    
    private fun observeTransactions() {
        // Observe transactions reactively for automatic updates
        viewModelScope.launch {
            repository.getAllTransactions().collect { transactions ->
                loadDashboardData()
            }
        }
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            _previousTransactionCount.postValue(_transactionCount.value ?: 0)
            val calendar = Calendar.getInstance()
            
            // Store current day for date range
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Set month display
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            _currentMonthDisplay.postValue(monthFormat.format(calendar.time))
            
            // Reset to start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis
            
            // Calculate month end consistently with TransactionsViewModel
            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis
            
            // Set date range display
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val startDate = dateFormat.format(Date(monthStart))
            // For display, use current date or last day of month, whichever is earlier
            val displayEndTime = minOf(System.currentTimeMillis(), monthEnd - 1)
            val endDate = dateFormat.format(Date(displayEndTime))
            _dateRangeDisplay.postValue("$startDate - $endDate")
            
            // Get monthly income and expenses separately
            val monthTransactions = repository.getTransactionsByDateRange(monthStart, monthEnd).firstOrNull() ?: emptyList()
            val expenseTransactions = monthTransactions.filter { it.amount < 0 }
            val incomeTransactions = monthTransactions.filter { it.amount > 0 }
            val actualExpenses = expenseTransactions.sumOf { kotlin.math.abs(it.amount) }
            val actualIncome = incomeTransactions.sumOf { it.amount }
            val netAmount = actualIncome - actualExpenses
            
            
            // Debug: Print first 10 transactions to verify
            monthTransactions.take(10).forEach { transaction ->
            }
            
            // Post all financial data
            _monthlySpending.postValue(actualExpenses)
            _monthlyIncome.postValue(actualIncome)
            _monthlyNet.postValue(netAmount)
            
            // Get transaction count
            val transactions = repository.getTransactionsByDateRange(monthStart, monthEnd).firstOrNull()
            _transactionCount.postValue(transactions?.size ?: 0)
            
            // Get top category
            val categorySpending = repository.getCategorySpending(monthStart, monthEnd)
            val topCategory = categorySpending.maxByOrNull { it.total }
            _topCategory.postValue(topCategory?.category?.name?.replace("_", " ")?.lowercase()?.replaceFirstChar { it.uppercase() })
            
            // Refresh AI insights when data changes
            loadAIInsights()
            
            // Load additional dashboard data
            loadSpendingComparison()
            loadActiveSubscriptions()
            loadRecentTransactions()
            loadTopCategoryData()
        }
    }
    
    private fun loadSpendingComparison() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Current month range
            calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val currentMonthStart = calendar.timeInMillis
            val currentMonthEnd = System.currentTimeMillis()
            
            // Last month range
            calendar.add(Calendar.MONTH, -1)
            val lastMonthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val lastMonthEnd = calendar.timeInMillis
            
            // Get actual expenses for both months
            val currentMonthTransactions = repository.getTransactionsByDateRange(currentMonthStart, currentMonthEnd).firstOrNull() ?: emptyList()
            val currentMonthSpending = currentMonthTransactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            
            val lastMonthTransactions = repository.getTransactionsByDateRange(lastMonthStart, lastMonthEnd).firstOrNull() ?: emptyList()
            val lastMonthSpending = lastMonthTransactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
            
            val percentageChange = if (lastMonthSpending != 0.0) {
                ((currentMonthSpending - lastMonthSpending) / kotlin.math.abs(lastMonthSpending)) * 100
            } else {
                0.0
            }
            
            // Also get income for comparison
            val currentMonthIncome = currentMonthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            val lastMonthIncome = lastMonthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            
            val incomePercentageChange = if (lastMonthIncome != 0.0) {
                ((currentMonthIncome - lastMonthIncome) / kotlin.math.abs(lastMonthIncome)) * 100
            } else {
                0.0
            }
            
            _spendingComparison.postValue(SpendingComparison(
                lastMonthAmount = lastMonthSpending,
                currentMonthAmount = currentMonthSpending,
                percentageChange = percentageChange,
                lastMonthIncome = lastMonthIncome,
                currentMonthIncome = currentMonthIncome,
                incomePercentageChange = incomePercentageChange
            ))
        }
    }
    
    private fun loadActiveSubscriptions() {
        viewModelScope.launch {
            val subscriptions = repository.getAllSubscriptions().firstOrNull() ?: emptyList()
            _activeSubscriptions.postValue(subscriptions)
        }
    }
    
    private fun loadRecentTransactions() {
        viewModelScope.launch {
            val transactions = repository.getAllTransactions().firstOrNull() ?: emptyList()
            val recentTransactions = transactions
                .sortedByDescending { it.date }
                .take(5)
            _recentTransactions.postValue(recentTransactions)
        }
    }
    
    private fun loadTopCategoryData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis
            val monthEnd = System.currentTimeMillis()
            
            val categorySpending = repository.getCategorySpending(monthStart, monthEnd)
            val topCategory = categorySpending.maxByOrNull { kotlin.math.abs(it.total) }
            
            if (topCategory != null) {
                val categoryName = topCategory.category.name
                    .replace("_", " ")
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
                
                _topCategoryData.postValue(TopCategoryData(
                    category = topCategory.category,
                    categoryName = categoryName,
                    amount = topCategory.total,
                    transactionCount = 0 // TODO: Get actual count from database
                ))
            } else {
                _topCategoryData.postValue(null)
            }
        }
    }
    
    suspend fun checkAndStartInitialScan() {
        val settings = repository.getSettingsSync()
        if (settings?.hasCompletedInitialScan != true) {
            // First time setup - just create default settings
            val defaultSettings = AppSettings(
                historicalScanDays = 30,
                hasCompletedInitialScan = true
            )
            repository.insertSettings(defaultSettings)
            
            // No automatic scanning - user has full control
            loadDashboardData()
        }
    }
    
    suspend fun startManualScan() {
        val settings = repository.getSettingsSync()
        val daysBack = settings?.historicalScanDays ?: 30
        
        // Start real SMS scanning
        startScan(daysBack)
    }
    
    fun cancelScan() {
        // Cancel any running WorkManager scan
        ScanWorker.cancel(getApplication())
        
        // Cancel the scan through LogStreamManager
        LogStreamManager.cancelScan()
        
        // Clear scan progress
        _scanProgress.postValue(null)
        
    }
    
    
    private fun startScan(daysBack: Int) {
        viewModelScope.launch {
            try {
                val scanStartTime = System.currentTimeMillis()
                Log.i("SMS_SCAN", "🔍 Starting SMS scan for $daysBack days")
                FirebaseHelper.logDebugInfo("SMS_SCAN", "Starting scan for $daysBack days")
                
                // Check memory before scan
                val runtime = Runtime.getRuntime()
                val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                
                smsScanner.scanHistoricalSms(daysBack).collect { progress ->
                _scanProgress.postValue(progress)
                
                if (progress.isComplete) {
                    val scanDuration = System.currentTimeMillis() - scanStartTime
                    
                    if (progress.errorMessage != null) {
                        FirebaseHelper.logException(RuntimeException("SMS scan failed: ${progress.errorMessage}"))
                    } else {
                        FirebaseHelper.logTransactionScanned(progress.transactionsFound, scanDuration)
                        FirebaseHelper.logSMSDebugInfo(
                            messagesFound = progress.transactionsFound,
                            transactionsExtracted = progress.transactionsFound,
                            scanDurationMs = scanDuration,
                            daysScanned = daysBack
                        )
                        
                        // Trigger automatic grouping after scan
                        if (progress.transactionsFound > 0) {
                            Log.i("SMS_SCAN", "🤖 Starting automatic grouping...")
                            _scanProgress.postValue(progress.copy(
                                currentMessage = progress.totalMessages,
                                isComplete = false,
                                errorMessage = "Grouping transactions..."
                            ))
                            
                            performAutoGrouping()
                        }
                    }
                    
                    // Update settings to mark scan as complete
                    val settings = repository.getSettingsSync()
                    settings?.let {
                        repository.updateSettings(
                            it.copy(
                                hasCompletedInitialScan = true,
                                lastScanTimestamp = System.currentTimeMillis()
                            )
                        )
                    }
                    
                    // Refresh dashboard data
                    loadDashboardData()
                    loadTopGroups() // Also refresh top groups after grouping
                    loadAIInsights() // Refresh insights with new data
                    
                    // Notify other ViewModels about scan completion
                    viewModelScope.launch {
                        SharedEventBus.emit(SharedEventBus.Event.SmsScanCompleted)
                    }
                    
                    // Force a UI refresh by re-observing the data
                    Log.i("DashboardViewModel", "🔄 Forcing UI refresh after scan completion")
                    
                    // Clear progress after a short delay
                    kotlinx.coroutines.delay(2000)
                    _scanProgress.postValue(null)
                } else {
                    // Log progress updates
                }
            }
            } catch (e: Exception) {
                Log.e("SMS_SCAN", "💥 SMS scan crashed", e)
                FirebaseHelper.logException(e)
                FirebaseHelper.logDebugInfo("SMS_SCAN_ERROR", "Scan failed: ${e.message}")
                
                // Update UI to show error
                _scanProgress.postValue(com.pennywiseai.tracker.data.ScanProgress(
                    currentMessage = 0,
                    totalMessages = 0,
                    transactionsFound = 0,
                    isComplete = true,
                    errorMessage = "Scan failed: ${e.message}"
                ))
            }
        }
    }
    
    private fun loadTodayClassifiedCount() {
        viewModelScope.launch {
            try {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val count = repository.getTransactionCountSince(startOfDay)
                _todayClassifiedCount.postValue(count)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading today's classified count", e)
                _todayClassifiedCount.postValue(0)
            }
        }
    }
    
    private fun loadScanTimeframe() {
        viewModelScope.launch {
            try {
                val settings = repository.getSettingsSync()
                val days = settings?.historicalScanDays ?: 30
                _scanTimeframeDays.postValue(days)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading scan timeframe", e)
                _scanTimeframeDays.postValue(30)
            }
        }
    }
    
    private fun loadLLMDebugInfo() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                
                val availableMemoryMB = memInfo.availMem / (1024 * 1024)
                val totalMemoryMB = memInfo.totalMem / (1024 * 1024)
                val memoryPercent = ((memInfo.availMem.toDouble() / memInfo.totalMem.toDouble()) * 100).toInt()
                
                val modelExists = classifier.isModelDownloaded()
                val modelSizeMB = if (modelExists) {
                    val modelFile = File(context.filesDir, "model")
                    if (modelFile.exists()) {
                        "✅ ${modelFile.length() / (1024 * 1024)}MB installed"
                    } else {
                        "✅ Installed"
                    }
                } else {
                    "❌ 2.7GB required"
                }
                
                val gpuStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    "Available"
                } else {
                    "Not supported"
                }
                
                val processingSpeed = if (classifier.isModelDownloaded()) {
                    if (processedCount > 0) {
                        val avgTime = totalProcessingTime / processedCount
                        "${avgTime}ms avg (${processedCount} processed)"
                    } else {
                        "Ready (no recent activity)"
                    }
                } else {
                    "Model not loaded"
                }
                
                val debugInfo = LLMDebugInfo(
                    availableMemory = "${availableMemoryMB}MB ($memoryPercent%)",
                    modelSize = modelSizeMB,
                    processingSpeed = processingSpeed,
                    gpuStatus = gpuStatus,
                    modelDetails = "MediaPipe Gemma-2B\nDevice: ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nAPI: ${Build.VERSION.SDK_INT}",
                    errorMessage = ""
                )
                
                // Log AI debug info to Firebase
                FirebaseHelper.logAIDebugInfo(
                    availableMemoryMB = availableMemoryMB,
                    modelSize = modelSizeMB,
                    processingSpeed = processingSpeed,
                    isModelLoaded = modelExists
                )
                
                _llmDebugInfo.postValue(debugInfo)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading LLM debug info", e)
                val errorInfo = LLMDebugInfo(
                    availableMemory = "Error loading",
                    modelSize = "Unknown",
                    processingSpeed = "--",
                    gpuStatus = "Unknown",
                    modelDetails = "Error: ${e.message}",
                    errorMessage = "Failed to load debug info: ${e.message}"
                )
                _llmDebugInfo.postValue(errorInfo)
            }
        }
    }
    
    fun refreshLLMDebugInfo() {
        loadLLMDebugInfo()
    }
    
    fun refreshAllData() {
        loadDashboardData()
        loadTodayClassifiedCount()
        loadScanTimeframe()
        loadLLMDebugInfo()
        loadTopGroups()
        loadCategorySpending()
        loadAIInsights()
    }
    
    fun loadCategorySpending() {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                
                val monthEnd = System.currentTimeMillis()
                
                // Get category spending data
                val spending = repository.getCategorySpending(monthStart, monthEnd)
                _categorySpending.postValue(spending)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading category spending", e)
                _categorySpending.postValue(emptyList())
            }
        }
    }
    
    private fun loadTopGroups() {
        val timeRange = _selectedTimeRange.value ?: TimeRange.THIRTY_DAYS
        loadTopGroups(timeRange)
    }
    
    fun setSelectedTimeRange(timeRange: TimeRange) {
        _selectedTimeRange.value = timeRange
        loadTopGroups(timeRange)
    }
    
    private fun loadTopGroups(timeRange: TimeRange) {
        viewModelScope.launch {
            try {
                // Calculate time bounds
                val (startTime, endTime) = getTimeRangeBounds(timeRange)
                
                // Get all active groups
                val allGroups = groupRepository.getAllActiveGroups().firstOrNull() ?: emptyList()
                
                // Calculate group stats for the selected time range
                val groupsWithTimeRangeStats = allGroups.mapNotNull { group ->
                    // Get transactions for this group within the time range
                    val groupTransactions = groupRepository.getAllTransactionsForGroup(group.id)
                        .firstOrNull()?.filter { transaction ->
                            transaction.date >= startTime && transaction.date <= endTime
                        } ?: emptyList()
                    
                    if (groupTransactions.isNotEmpty()) {
                        // Create a copy with time-range specific stats
                        group.copy(
                            transactionCount = groupTransactions.size,
                            totalAmount = groupTransactions.sumOf { it.amount },
                            averageAmount = groupTransactions.sumOf { it.amount } / groupTransactions.size,
                            lastTransactionDate = groupTransactions.maxOfOrNull { it.date } ?: 0
                        )
                    } else {
                        null
                    }
                }
                
                // Sort by transaction count (descending) and take top 3
                val sortedGroups = groupsWithTimeRangeStats
                    .sortedByDescending { it.transactionCount }
                    .take(3)
                
                _topGroups.postValue(sortedGroups)
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading top groups for time range", e)
                _topGroups.postValue(emptyList())
            }
        }
    }
    
    private fun getTimeRangeBounds(timeRange: TimeRange): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        return when (timeRange) {
            TimeRange.SEVEN_DAYS -> {
                val startTime = now - (7 * 24 * 60 * 60 * 1000L)
                Pair(startTime, now)
            }
            TimeRange.THIRTY_DAYS -> {
                val startTime = now - (30 * 24 * 60 * 60 * 1000L)
                Pair(startTime, now)
            }
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                Pair(startTime, now)
            }
            TimeRange.ALL_TIME -> {
                Pair(0L, now)
            }
        }
    }
    
    private suspend fun performAutoGrouping() {
        try {
            val database = AppDatabase.getDatabase(getApplication())
            val groupingService = TransactionGroupingService(groupRepository, repository, database)
            
            // Get initial ungrouped count
            val ungroupedBefore = groupRepository.getUngroupedTransactionCount()
            
            // Perform grouping
            groupingService.autoGroupTransactions()
            
            // Get final counts
            val ungroupedAfter = groupRepository.getUngroupedTransactionCount()
            val groupedCount = ungroupedBefore - ungroupedAfter
            val groupsCreated = groupRepository.getAllActiveGroups().first().size
            
            Log.i("DashboardViewModel", "✅ Grouping complete: $groupedCount transactions grouped into $groupsCreated groups")
            
            // Update progress with grouping result
            val currentProgress = _scanProgress.value
            if (currentProgress != null) {
                val message = if (groupedCount > 0) {
                    "✅ Scan complete! Found ${currentProgress.transactionsFound} transactions, created $groupsCreated groups"
                } else {
                    "✅ Scan complete! Found ${currentProgress.transactionsFound} transactions"
                }
                _scanProgress.postValue(currentProgress.copy(
                    isComplete = true,
                    errorMessage = null,
                    currentMessage = currentProgress.totalMessages
                ))
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "❌ Auto-grouping failed: ${e.message}")
            val currentProgress = _scanProgress.value
            if (currentProgress != null) {
                _scanProgress.postValue(currentProgress.copy(
                    isComplete = true,
                    errorMessage = "Scan complete (grouping failed)"
                ))
            }
        }
    }
    
    
    // Debug methods removed for production
    
    private fun loadAIInsights() {
        viewModelScope.launch {
            try {
                
                // Check if AI model is available
                val isModelDownloaded = llmExtractor.isAvailable()
                
                if (!isModelDownloaded) {
                    // Show basic insights when model not downloaded
                    val basicInsight = AIInsightsGenerator.AIInsight(
                        title = "Basic Analytics Available",
                        message = "Download AI model for smarter insights. Currently showing basic spending analysis",
                        type = AIInsightsGenerator.InsightType.RECOMMENDATION,
                        priority = AIInsightsGenerator.InsightPriority.MEDIUM,
                        icon = "📊"
                    )
                    _currentInsight.postValue(basicInsight)
                    
                    // Still generate basic statistical insights
                    val insights = aiInsightsGenerator.generateInsights()
                    _aiInsights.postValue(insights)
                } else {
                    // Full AI insights available
                    val insights = aiInsightsGenerator.generateInsights()
                    _aiInsights.postValue(insights)
                    
                    // Set current insight for display
                    if (insights.isNotEmpty()) {
                        _currentInsight.postValue(insights.first())
                    } else {
                        // Generate quick insight if no full insights available
                        val quickInsight = aiInsightsGenerator.generateQuickInsight(
                            monthlySpending = _monthlySpending.value ?: 0.0,
                            transactionCount = _transactionCount.value ?: 0,
                            topCategory = _topCategory.value
                        )
                        _currentInsight.postValue(quickInsight)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "❌ Error loading AI insights: ${e.message}", e)
                // Fallback insight
                val fallbackInsight = aiInsightsGenerator.generateQuickInsight(
                    monthlySpending = _monthlySpending.value ?: 0.0,
                    transactionCount = _transactionCount.value ?: 0,
                    topCategory = _topCategory.value
                )
                _currentInsight.postValue(fallbackInsight)
            }
        }
    }
    
    fun getNextInsight() {
        val insights = _aiInsights.value ?: return
        val currentInsight = _currentInsight.value
        
        if (insights.isEmpty()) return
        
        val currentIndex = insights.indexOf(currentInsight)
        val nextIndex = (currentIndex + 1) % insights.size
        _currentInsight.postValue(insights[nextIndex])
    }
    
    fun getAllInsightsForDisplay(): List<AIInsightsGenerator.AIInsight> {
        return _aiInsights.value ?: emptyList()
    }
}

data class LLMDebugInfo(
    val availableMemory: String,
    val modelSize: String,
    val processingSpeed: String,
    val gpuStatus: String,
    val modelDetails: String,
    val errorMessage: String
)