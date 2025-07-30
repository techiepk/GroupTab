package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.databinding.FragmentDashboardSimplifiedBinding
import com.pennywiseai.tracker.viewmodel.DashboardViewModel
import com.pennywiseai.tracker.firebase.FirebaseHelper
import com.pennywiseai.tracker.background.ScanWorker
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import com.pennywiseai.tracker.logging.LogStreamDialog
import com.pennywiseai.tracker.logging.LogStreamManager
import android.util.Log
import androidx.transition.TransitionManager
import androidx.transition.AutoTransition
import com.google.android.material.snackbar.Snackbar
import com.pennywiseai.tracker.MainActivity
import com.pennywiseai.tracker.utils.CurrencyFormatter
import com.pennywiseai.tracker.utils.AIInsightsGenerator
import com.pennywiseai.tracker.ui.UserMenuBottomSheetFragment
import android.content.Intent
import com.pennywiseai.tracker.adapter.AccountBalanceAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.pennywiseai.tracker.utils.DataExporter
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.ui.OrganizedSettingsFragment
import com.pennywiseai.tracker.utils.ThemeColorUtils
import java.io.File
import com.pennywiseai.tracker.data.FinancialInsight
import com.pennywiseai.tracker.viewmodel.ChatViewModel
import com.pennywiseai.tracker.databinding.ItemDashboardInsightBinding
import com.pennywiseai.tracker.ui.dialogs.ModelDownloadDialog
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SimplifiedDashboardFragment : Fragment() {
    
    private var _binding: FragmentDashboardSimplifiedBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DashboardViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var accountBalanceAdapter: AccountBalanceAdapter
    
    private var isCategoriesExpanded = false
    private var currentInsightsPeriod = 7 // days
    
    companion object {
        private const val TAG = "SimplifiedDashboard"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardSimplifiedBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Log screen view
        FirebaseHelper.logScreenView("SimplifiedDashboard", "SimplifiedDashboardFragment")
        
        setupUI()
        observeData()
        setupAIInsights()
        
        // Start initial scan if needed
        lifecycleScope.launch {
            viewModel.checkAndStartInitialScan()
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshAllData()
    }
    
    private fun setupUI() {
        binding.apply {
            // Set initial values
            totalSpentValue.text = CurrencyFormatter.formatCompact(0.0)
            transactionCountValue.text = "0 transactions"
            activeSubscriptionsCount.text = "0 active"
            subscriptionsTotal.text = "₹0/month"
            topCategoryName.text = "None"
            topCategoryAmount.text = "₹0"
            
            // Initialize account balance adapter
            accountBalanceAdapter = AccountBalanceAdapter()
            accountBalancesRecycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = accountBalanceAdapter
            }
            
            // Always use pattern parser now
            val prefs = requireContext().getSharedPreferences("settings", 0)
            prefs.edit().putBoolean("use_pattern_parser", true).apply()
            
            // Primary actions
            scanActionButton.setOnClickListener {
                if (LogStreamManager.isScanRunning.value) {
                    LogStreamDialog.show(childFragmentManager)
                } else {
                    showScanConfirmationDialog()
                }
            }
            
            // Secondary actions
            viewAnalyticsButton.setOnClickListener {
                (activity as? MainActivity)?.navigateToAnalytics()
            }
            
            manageSubscriptionsButton.setOnClickListener {
                (activity as? MainActivity)?.navigateToSubscriptions()
            }
            
            // Quick insight cards
            subscriptionsCard.setOnClickListener {
                (activity as? MainActivity)?.navigateToSubscriptions()
            }
            
            topCategoryCard.setOnClickListener {
                (activity as? MainActivity)?.navigateToAnalytics()
            }
            
            // View all transactions
            viewAllTransactions.setOnClickListener {
                (activity as? MainActivity)?.navigateToTransactions()
            }
            
            // Settings button - navigate directly to settings page
            settingsButton.setOnClickListener {
                navigateToSettings()
            }
            
            // Expandable categories section
            categoriesHeader.setOnClickListener {
                toggleCategoriesSection()
            }
            
            // View all categories button
            viewAllCategoriesButton.setOnClickListener {
                (activity as? MainActivity)?.navigateToAnalytics()
            }
            
            // Touch target for spending value (shows trend on tap)
            monthlySpendingTouchTarget.setOnClickListener {
                (activity as? MainActivity)?.navigateToAnalytics()
            }
            
            // AI Alert close button
            aiAlertClose.setOnClickListener {
                aiAlertBanner.visibility = View.GONE
            }
        }
    }
    
    private fun observeData() {
        // Primary metrics
        viewModel.monthlySpending.observe(viewLifecycleOwner) { amount ->
            updateSpendingDisplay(amount)
        }
        
        viewModel.transactionCount.observe(viewLifecycleOwner) { count ->
            binding.transactionCountValue.text = if (count == 1) {
                "1 transaction"
            } else {
                "$count transactions"
            }
        }
        
        // Income and Net displays
        viewModel.monthlyIncome.observe(viewLifecycleOwner) { amount ->
            updateIncomeDisplay(amount)
        }
        
        viewModel.monthlyNet.observe(viewLifecycleOwner) { amount ->
            updateNetDisplay(amount)
        }
        
        // Spending comparison
        viewModel.spendingComparison.observe(viewLifecycleOwner) { comparison ->
            updateSpendingComparison(comparison)
        }
        
        // Subscriptions data
        viewModel.activeSubscriptions.observe(viewLifecycleOwner) { subscriptions ->
            updateSubscriptionsCard(subscriptions)
        }
        
        // Recent transactions
        viewModel.recentTransactions.observe(viewLifecycleOwner) { transactions ->
            updateRecentTransactions(transactions)
        }
        
        // Account balances
        viewModel.accountBalances.observe(viewLifecycleOwner) { balances ->
            updateAccountBalances(balances)
        }
        
        // Total balance
        viewModel.totalBalance.observe(viewLifecycleOwner) { total ->
            updateTotalBalance(total)
        }
        
        // Top category
        viewModel.topCategoryData.observe(viewLifecycleOwner) { categoryData ->
            updateTopCategory(categoryData)
        }
        
        // AI insights
        viewModel.aiInsight.observe(viewLifecycleOwner) { insight ->
            showAiInsight(insight)
        }
        
        // Date range display
        viewModel.dateRangeDisplay.observe(viewLifecycleOwner) { dateRange ->
            binding.dateRangeLabel.apply {
                text = dateRange
                visibility = View.VISIBLE
            }
        }
        
        // Scan state
        viewLifecycleOwner.lifecycleScope.launch {
            LogStreamManager.isScanRunning.collect { isRunning ->
                updateScanButtonState(isRunning)
            }
        }
        
        // Dashboard data is loaded automatically by ViewModel
        
        // Load category spending data
        viewModel.loadCategorySpending()
        
        // Observe category spending
        viewModel.categorySpending.observe(viewLifecycleOwner) { categories ->
            updateCategoriesSection(categories)
        }
    }
    
    private fun updateSpendingDisplay(amount: Double) {
        binding.totalSpentValue.apply {
            text = CurrencyFormatter.formatCompact(amount)
            // Color is already set in XML using theme attribute
        }
    }
    
    private fun updateIncomeDisplay(amount: Double) {
        binding.totalIncomeValue.apply {
            text = "+${CurrencyFormatter.formatCompact(amount)}"
            // Color is already set in XML using theme attribute
        }
    }
    
    private fun updateNetDisplay(amount: Double) {
        binding.totalNetValue.apply {
            text = if (amount >= 0) {
                "+${CurrencyFormatter.formatCompact(amount)}"
            } else {
                CurrencyFormatter.formatCompact(amount)
            }
            
            // Set color based on positive/negative using theme attributes
            val colorAttr = if (amount >= 0) R.attr.colorIncome else R.attr.colorExpense
            val typedArray = context.obtainStyledAttributes(intArrayOf(colorAttr))
            setTextColor(typedArray.getColor(0, 0))
            typedArray.recycle()
        }
    }
    
    private fun updateScanButtonState(isRunning: Boolean) {
        binding.scanActionButton.apply {
            text = if (isRunning) "View Progress" else "Scan SMS Messages"
            isEnabled = true
        }
    }
    
    private fun toggleCategoriesSection() {
        isCategoriesExpanded = !isCategoriesExpanded
        
        // Animate the transition
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition())
        
        binding.apply {
            categoriesContent.visibility = if (isCategoriesExpanded) View.VISIBLE else View.GONE
            categoriesExpandIcon.rotation = if (isCategoriesExpanded) 180f else 0f
        }
    }
    
    private fun updateCategoriesSection(categories: List<Any>) {
        binding.apply {
            categoriesContent.removeAllViews()
            
            if (categories.isEmpty()) {
                binding.noCategoriesLayout.visibility = View.VISIBLE
            } else {
                binding.noCategoriesLayout.visibility = View.GONE
                binding.viewAllCategoriesButton.visibility = View.VISIBLE
                
                // Show top 3 categories
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                categories.take(3).forEach { categoryData ->
                    val categoryView = layoutInflater.inflate(
                        R.layout.item_category_simple, 
                        categoriesContent, 
                        false
                    )
                    
                    val nameView = categoryView.findViewById<TextView>(R.id.category_name)
                    val amountView = categoryView.findViewById<TextView>(R.id.category_amount)
                    
                    // Extract category name and amount based on the data structure
                    when (categoryData) {
                        is com.pennywiseai.tracker.database.CategorySpending -> {
                            // Format category name
                            val categoryName = categoryData.category.name
                                .replace("_", " ")
                                .lowercase()
                                .replaceFirstChar { it.uppercase() }
                            nameView.text = categoryName
                            
                            // Format amount
                            amountView.text = currencyFormat.format(kotlin.math.abs(categoryData.total))
                            
                            // Make category clickable
                            categoryView.setOnClickListener {
                                navigateToAnalyticsWithCategory(categoryData.category)
                            }
                        }
                    }
                    
                    categoriesContent.addView(categoryView)
                    
                    // Add divider except for last item
                    if (categories.indexOf(categoryData) < minOf(2, categories.size - 1)) {
                        val divider = View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                1
                            )
                            setBackgroundColor(
                                com.google.android.material.color.MaterialColors.getColor(
                                    this,
                                    com.google.android.material.R.attr.colorOutlineVariant
                                )
                            )
                            alpha = 0.3f
                        }
                        categoriesContent.addView(divider)
                    }
                }
            }
        }
    }
    
    private fun updateSpendingComparison(comparison: DashboardViewModel.SpendingComparison?) {
        if (comparison == null) return
        
        // Update expense comparison
        if (comparison.percentageChange != 0.0) {
            val isIncrease = comparison.percentageChange > 0
            val arrow = if (isIncrease) "↑" else "↓"
            val colorAttr = if (isIncrease) R.attr.colorExpense else R.attr.colorIncome
            val typedArray = requireContext().obtainStyledAttributes(intArrayOf(colorAttr))
            val color = typedArray.getColor(0, 0)
            typedArray.recycle()
            
            binding.expenseComparison.apply {
                text = "$arrow ${kotlin.math.abs(comparison.percentageChange).toInt()}%"
                setTextColor(color)
                visibility = View.VISIBLE
            }
            
            // Also update the main spending comparison (legacy)
            binding.spendingComparison.apply {
                text = "$arrow ${kotlin.math.abs(comparison.percentageChange).toInt()}% vs last month"
                setTextColor(color)
                visibility = View.VISIBLE
            }
        } else {
            binding.expenseComparison.visibility = View.GONE
            binding.spendingComparison.visibility = View.GONE
        }
        
        // Update income comparison
        if (comparison.incomePercentageChange != 0.0) {
            val isIncrease = comparison.incomePercentageChange > 0
            val arrow = if (isIncrease) "↑" else "↓"
            val colorAttr = if (isIncrease) R.attr.colorIncome else R.attr.colorExpense
            val typedArray = requireContext().obtainStyledAttributes(intArrayOf(colorAttr))
            val color = typedArray.getColor(0, 0)
            typedArray.recycle()
            
            binding.incomeComparison.apply {
                text = "$arrow ${kotlin.math.abs(comparison.incomePercentageChange).toInt()}%"
                setTextColor(color)
                visibility = View.VISIBLE
            }
        } else {
            binding.incomeComparison.visibility = View.GONE
        }
        
        // Update net comparison
        val currentNet = comparison.currentMonthIncome - comparison.currentMonthAmount
        val lastNet = comparison.lastMonthIncome - comparison.lastMonthAmount
        
        if (lastNet != 0.0) {
            val netChange = ((currentNet - lastNet) / kotlin.math.abs(lastNet)) * 100
            if (netChange != 0.0) {
                val isIncrease = netChange > 0
                val arrow = if (isIncrease) "↑" else "↓"
                val colorAttr = if (isIncrease) R.attr.colorIncome else R.attr.colorExpense
                val typedArray = requireContext().obtainStyledAttributes(intArrayOf(colorAttr))
                val color = typedArray.getColor(0, 0)
                typedArray.recycle()
                
                binding.netComparison.apply {
                    text = "$arrow ${kotlin.math.abs(netChange).toInt()}%"
                    setTextColor(color)
                    visibility = View.VISIBLE
                }
            } else {
                binding.netComparison.visibility = View.GONE
            }
        } else {
            binding.netComparison.visibility = View.GONE
        }
    }
    
    private fun updateSubscriptionsCard(subscriptions: List<com.pennywiseai.tracker.data.Subscription>) {
        val activeCount = subscriptions.count { it.status == com.pennywiseai.tracker.data.SubscriptionStatus.ACTIVE }
        val totalMonthly = subscriptions.filter { it.status == com.pennywiseai.tracker.data.SubscriptionStatus.ACTIVE }.sumOf { it.amount }
        
        binding.activeSubscriptionsCount.text = "$activeCount active"
        binding.subscriptionsTotal.text = CurrencyFormatter.formatCompact(totalMonthly) + "/month"
    }
    
    private fun updateTopCategory(categoryData: DashboardViewModel.TopCategoryData?) {
        if (categoryData != null) {
            binding.topCategoryName.text = categoryData.categoryName
            binding.topCategoryAmount.text = CurrencyFormatter.formatCompact(kotlin.math.abs(categoryData.amount))
        } else {
            binding.topCategoryName.text = "None"
            binding.topCategoryAmount.text = "₹0"
        }
    }
    
    private fun updateRecentTransactions(transactions: List<com.pennywiseai.tracker.data.Transaction>) {
        binding.recentTransactionsList.removeAllViews()
        
        if (transactions.isEmpty()) {
            binding.noRecentTransactions.visibility = View.VISIBLE
            binding.recentTransactionsList.visibility = View.GONE
        } else {
            binding.noRecentTransactions.visibility = View.GONE
            binding.recentTransactionsList.visibility = View.VISIBLE
            
            transactions.take(5).forEach { transaction ->
                val itemView = layoutInflater.inflate(R.layout.item_recent_transaction, binding.recentTransactionsList, false)
                
                val categoryIcon = itemView.findViewById<TextView>(R.id.category_icon)
                val merchantName = itemView.findViewById<TextView>(R.id.merchant_name)
                val transactionTime = itemView.findViewById<TextView>(R.id.transaction_time)
                val transactionAmount = itemView.findViewById<TextView>(R.id.transaction_amount)
                
                // Set category emoji
                categoryIcon.text = getCategoryEmoji(transaction.category)
                
                // Set merchant name
                merchantName.text = transaction.merchant
                
                // Set time
                transactionTime.text = getRelativeTime(transaction.date)
                
                // Set amount
                val (formattedAmount, isPositive) = CurrencyFormatter.formatWithColor(transaction.amount)
                transactionAmount.text = formattedAmount
                transactionAmount.setTextColor(ThemeColorUtils.getColorForAmount(requireContext(), transaction.amount))
                
                itemView.setOnClickListener {
                    // Navigate to transaction details or transactions list
                    (activity as? MainActivity)?.navigateToTransactions()
                }
                
                binding.recentTransactionsList.addView(itemView)
            }
        }
    }
    
    private fun getCategoryEmoji(category: TransactionCategory): String {
        return when (category) {
            TransactionCategory.FOOD_DINING -> "🍔"
            TransactionCategory.TRANSPORTATION -> "🚗"
            TransactionCategory.SHOPPING -> "🛒"
            TransactionCategory.ENTERTAINMENT -> "🎬"
            TransactionCategory.BILLS_UTILITIES -> "💡"
            TransactionCategory.HEALTHCARE -> "🏥"
            TransactionCategory.EDUCATION -> "📚"
            TransactionCategory.TRAVEL -> "✈️"
            TransactionCategory.GROCERIES -> "🛍️"
            TransactionCategory.SUBSCRIPTION -> "📱"
            TransactionCategory.INVESTMENT -> "📈"
            TransactionCategory.TRANSFER -> "💸"
            TransactionCategory.OTHER -> "📌"
        }
    }
    
    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60 * 60 * 1000 -> "Just now"
            diff < 24 * 60 * 60 * 1000 -> "Today"
            diff < 2 * 24 * 60 * 60 * 1000 -> "Yesterday"
            else -> {
                val days = (diff / (24 * 60 * 60 * 1000)).toInt()
                "$days days ago"
            }
        }
    }
    
    private fun showAiInsight(insight: AIInsightsGenerator.AIInsight?) {
        if (insight != null) {
            binding.aiAlertBanner.visibility = View.VISIBLE
            binding.aiAlertTitle.text = insight.title
            binding.aiAlertMessage.text = insight.message
        } else {
            binding.aiAlertBanner.visibility = View.GONE
        }
    }
    
    private fun showScanConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Scan Messages")
            .setMessage("Find transactions in your SMS inbox")
            .setPositiveButton("Start") { _, _ ->
                LogStreamDialog.show(childFragmentManager)
                val scanDays = viewModel.scanTimeframeDays.value ?: 30
                ScanWorker.enqueue(requireContext(), scanDays)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showQuickSettings(selectAiManagement: Boolean = false) {
        val prefs = requireContext().getSharedPreferences("settings", 0)
        val usePatternParser = prefs.getBoolean("use_pattern_parser", false)
        val parserType = if (usePatternParser) "Pattern-based" else "AI-powered"
        
        val options = arrayOf(
            "Scan period: ${viewModel.scanTimeframeDays.value ?: 30} days",
            "Parser info",
            "AI Model Management",
            "Export Data",
            "About"
        )
        
        if (selectAiManagement) {
            // Directly show model management when coming from banner
            showModelManagement()
        } else {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Settings")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showScanPeriodDialog()
                        1 -> showParserInfoDialog()
                        2 -> showModelManagement()
                        3 -> exportData()
                        4 -> showAbout()
                    }
                }
                .show()
        }
    }
    
    private fun showScanPeriodDialog() {
        val periods = arrayOf("7 days", "14 days", "30 days", "60 days")
        val values = intArrayOf(7, 14, 30, 60)
        val currentValue = viewModel.scanTimeframeDays.value ?: 30
        val currentIndex = values.indexOf(currentValue).takeIf { it >= 0 } ?: 2
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Scan Period")
            .setSingleChoiceItems(periods, currentIndex) { dialog, which ->
                // Save to preferences
                requireContext().getSharedPreferences("settings", 0)
                    .edit()
                    .putInt("scan_timeframe_days", values[which])
                    .apply()
                dialog.dismiss()
                Snackbar.make(binding.root, "Scan period updated to ${periods[which]}", Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun showParserInfoDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("About Parser Types")
            .setMessage("""
                AI-Powered Parser:
                • Uses Google's Gemma LLM model
                • Accurately extracts merchant names, categories, and amounts
                • Works with complex and varied SMS formats
                • Requires ~1.5GB storage for the AI model
                • Slightly slower processing
                
                Pattern-Based Parser:
                • Uses pre-defined regex patterns
                • Fast processing speed
                • No additional storage required
                • May miss transactions with unusual formats
                • Less accurate merchant name extraction
                
                Recommendation: Use AI-powered parser for best results.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showModelManagement() {
        // Check model status and show appropriate dialog
        val modelDownloader = com.pennywiseai.tracker.llm.PersistentModelDownloader(requireContext())
        val isDownloaded = modelDownloader.isModelDownloaded()
        val modelSize = modelDownloader.getModelSize()
        val expectedSize = modelDownloader.getExpectedModelSize()
        
        val statusText = if (isDownloaded) "Ready" else "Not Downloaded"
        val sizeText = if (modelSize > 0) {
            formatFileSize(modelSize)
        } else {
            formatFileSize(expectedSize) + " (expected)"
        }
        
        val message = "🤖 AI Model Information\n\n" +
                "Status: $statusText\n" +
                "Size: $sizeText\n" +
                "Type: Gemma 2B-IT\n" +
                "Processing: On-device only\n\n" +
                "🔒 Privacy: All analysis happens locally on your device. No data is sent to external servers."
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("AI Model Management")
            .setMessage(message)
            .setNegativeButton("Close", null)
        
        if (!isDownloaded) {
            builder.setPositiveButton("Download") { _, _ ->
                startModelDownload()
            }
        } else {
            builder.setPositiveButton("Re-download") { _, _ ->
                startModelDownload()
            }
            builder.setNeutralButton("Delete Model") { _, _ ->
                showDeleteModelConfirmation()
            }
        }
        
        builder.show()
    }
    
    private fun startModelDownload() {
        // Use the settings view model to start download
        val bottomSheet = UserMenuBottomSheetFragment()
        bottomSheet.show(childFragmentManager, "user_menu")
        Snackbar.make(binding.root, "Navigate to Settings > AI Model to download", Snackbar.LENGTH_LONG).show()
    }
    
    private fun showDeleteModelConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete AI Model")
            .setMessage("Are you sure you want to delete the AI model?\n\n" +
                    "• You'll need to download it again to use AI features\n" +
                    "• All AI analysis will be disabled")
            .setPositiveButton("Delete") { _, _ ->
                com.pennywiseai.tracker.llm.PersistentModelDownloader(requireContext()).deleteModel()
                Snackbar.make(binding.root, "Model deleted", Snackbar.LENGTH_SHORT).show()
                // Refresh AI status
                viewModel.refreshAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    private fun exportData() {
        val options = arrayOf("Export Transactions (CSV)", "Export Subscriptions (CSV)", "Export Analytics Report")
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Export Data")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportTransactions()
                    1 -> exportSubscriptions()
                    2 -> exportAnalyticsReport()
                }
            }
            .show()
    }
    
    private fun exportTransactions() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val repository = TransactionRepository(database)
            val exporter = DataExporter(requireContext(), repository)
            val result = exporter.exportTransactionsToCSV()
            
            if (result.success && result.filePath != null) {
                val fileName = File(result.filePath).name
                val message = exporter.getDownloadNotificationMessage(fileName)
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .setAction("Share") {
                        exporter.shareFile(result.filePath)
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, "Export failed: ${result.error}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportSubscriptions() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val repository = TransactionRepository(database)
            val exporter = DataExporter(requireContext(), repository)
            val result = exporter.exportSubscriptionsToCSV()
            
            if (result.success && result.filePath != null) {
                val fileName = File(result.filePath).name
                val message = exporter.getDownloadNotificationMessage(fileName)
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .setAction("Share") {
                        exporter.shareFile(result.filePath)
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, "Export failed: ${result.error}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun exportAnalyticsReport() {
        // Export last 30 days by default
        val endDate = System.currentTimeMillis()
        val startDate = endDate - (30L * 24 * 60 * 60 * 1000) // 30 days ago
        
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val repository = TransactionRepository(database)
            val exporter = DataExporter(requireContext(), repository)
            val result = exporter.exportAnalyticsReport(startDate, endDate)
            
            if (result.success && result.filePath != null) {
                val fileName = File(result.filePath).name
                val message = exporter.getDownloadNotificationMessage(fileName)
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                    .setAction("Share") {
                        exporter.shareFile(result.filePath, "text/plain")
                    }
                    .show()
            } else {
                Snackbar.make(binding.root, "Export failed: ${result.error}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAbout() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("About PennyWise AI")
            .setMessage("Version 1.0\n\nAI-powered transaction tracking that respects your privacy. All processing happens on your device.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showMoreInsights() {
        // Feature removed - navigate to analytics for insights
        (activity as? MainActivity)?.navigateToAnalytics()
    }
    
    private fun navigateToAnalyticsWithCategory(category: com.pennywiseai.tracker.data.TransactionCategory) {
        // Navigate to analytics tab with category filter
        (activity as? MainActivity)?.navigateToAnalytics()
        // TODO: Add functionality to pre-select category in AnalyticsFragment
    }
    
    private fun navigateToSettings() {
        // Navigate to organized settings page
        val settingsFragment = OrganizedSettingsFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, settingsFragment)
            .addToBackStack("settings")
            .commit()
    }
    
    private fun updateAccountBalances(balances: List<com.pennywiseai.tracker.data.AccountBalance>) {
        if (balances.isEmpty()) {
            binding.noAccountBalances.visibility = View.VISIBLE
            binding.accountBalancesRecycler.visibility = View.GONE
        } else {
            binding.noAccountBalances.visibility = View.GONE
            binding.accountBalancesRecycler.visibility = View.VISIBLE
            accountBalanceAdapter.submitList(balances)
        }
    }
    
    private fun updateTotalBalance(total: Double) {
        if (total > 0) {
            binding.totalBalanceValue.visibility = View.VISIBLE
            binding.totalBalanceValue.text = "Total: ${CurrencyFormatter.formatCompact(total)}"
        } else {
            binding.totalBalanceValue.visibility = View.GONE
        }
    }
    
    private fun setupAIInsights() {
        // Setup download button
        binding.downloadModelButton.setOnClickListener {
            showModelDownloadDialog()
        }
        
        // Load insights on startup
        loadAIInsights()
    }
    
    private fun loadAIInsights() {
        Log.d(TAG, "📊 Starting AI insights loading...")
        
        // Show loading state
        binding.insightsLoadingCard.visibility = View.VISIBLE
        binding.insightsContainer.removeAllViews()
        binding.downloadModelCard.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                // Check if model is downloaded
                if (!chatViewModel.isModelDownloaded()) {
                    Log.i(TAG, "🔄 AI model not downloaded, showing download prompt")
                    // Show download prompt
                    binding.insightsLoadingCard.visibility = View.GONE
                    binding.downloadModelCard.visibility = View.VISIBLE
                    return@launch
                }
                
                Log.i(TAG, "🤖 Generating insights for $currentInsightsPeriod days...")
                
                // Generate insights
                val insights = withContext(Dispatchers.IO) {
                    chatViewModel.generateFinancialInsights(currentInsightsPeriod)
                }
                
                Log.i(TAG, "✅ Generated ${insights.size} insights")
                
                // Hide loading
                binding.insightsLoadingCard.visibility = View.GONE
                
                // Display insights
                displayInsights(insights)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load AI insights", e)
                binding.insightsLoadingCard.visibility = View.GONE
                showInsightError()
            }
        }
    }
    
    private fun displayInsights(insights: List<FinancialInsight>) {
        binding.insightsContainer.removeAllViews()
        
        if (insights.isEmpty()) {
            showNoInsights()
            return
        }
        
        insights.forEach { insight ->
            val insightView = createInsightView(insight)
            binding.insightsContainer.addView(insightView)
        }
    }
    
    private fun createInsightView(insight: FinancialInsight): View {
        val insightBinding = ItemDashboardInsightBinding.inflate(
            layoutInflater,
            binding.insightsContainer,
            false
        )
        
        // Set icon
        insightBinding.insightIcon.text = when (insight.type) {
            FinancialInsight.Type.SPENDING_ALERT -> "⚠️"
            FinancialInsight.Type.SAVING_TIP -> "💰"
            FinancialInsight.Type.SUBSCRIPTION_ALERT -> "🔄"
            FinancialInsight.Type.TREND_ANALYSIS -> "📊"
            FinancialInsight.Type.BUDGET_RECOMMENDATION -> "🎯"
        }
        
        // Set priority indicator color
        val indicatorColor = when (insight.priority) {
            FinancialInsight.Priority.HIGH -> requireContext().getColor(R.color.expense_red)
            FinancialInsight.Priority.MEDIUM -> requireContext().getColor(R.color.md_theme_light_primary)
            FinancialInsight.Priority.LOW -> requireContext().getColor(R.color.income_green)
        }
        insightBinding.priorityIndicator.setBackgroundColor(indicatorColor)
        
        // Set content
        insightBinding.insightTitle.text = insight.title
        insightBinding.insightDescription.text = insight.description
        
        // Set action button
        if (insight.actionText != null) {
            insightBinding.actionButton.visibility = View.VISIBLE
            insightBinding.actionButton.text = insight.actionText
            insightBinding.actionButton.setOnClickListener {
                handleInsightAction(insight)
            }
        }
        
        // Add click listener for the whole card
        insightBinding.root.setOnClickListener {
            if (insight.actionQuery != null) {
                navigateToChat(insight.actionQuery)
            }
        }
        
        return insightBinding.root
    }
    
    private fun handleInsightAction(insight: FinancialInsight) {
        when {
            insight.actionQuery != null -> navigateToChat(insight.actionQuery)
            insight.actionText == "Download Model" -> showModelDownloadDialog()
            else -> {
                // Handle other actions
            }
        }
    }
    
    private fun navigateToChat(query: String) {
        // Navigate to chat with the query
        val mainActivity = activity as? MainActivity
        mainActivity?.switchToChat(query)
    }
    
    private fun showModelDownloadDialog() {
        val dialog = ModelDownloadDialog.newInstance()
        dialog.setOnDownloadCompleteListener {
            // Reload insights after download
            loadAIInsights()
            Toast.makeText(requireContext(), "AI model downloaded successfully!", Toast.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "model_download")
    }
    
    private fun showNoInsights() {
        val emptyView = TextView(requireContext()).apply {
            text = "No insights available. Try scanning more transactions."
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.light_gray))
            gravity = android.view.Gravity.CENTER
            setPadding(16, 24, 16, 24)
        }
        binding.insightsContainer.addView(emptyView)
    }
    
    private fun showInsightError() {
        val errorView = TextView(requireContext()).apply {
            text = "Unable to generate insights. Please try again later."
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.light_gray))
            gravity = android.view.Gravity.CENTER
            setPadding(16, 24, 16, 24)
        }
        binding.insightsContainer.addView(errorView)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
