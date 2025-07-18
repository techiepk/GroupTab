package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.databinding.FragmentAnalyticsChartBinding
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import android.graphics.Color
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.ui.charts.SimpleLineChartView
import com.pennywiseai.tracker.ui.charts.SimplePieChartView
import com.pennywiseai.tracker.viewmodel.AnalyticsViewModel
import java.text.NumberFormat
import java.util.*
import android.content.Intent
import com.pennywiseai.tracker.utils.ColorUtils

class AnalyticsFragment : Fragment() {
    
    private var _binding: FragmentAnalyticsChartBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    // Chart views will be created programmatically
    private var lineChart: View? = null
    private var pieChart: View? = null
    private var barChart: View? = null
    private var horizontalBarChart: View? = null
    
    private val selectedCategories = mutableSetOf<TransactionCategory>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsChartBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupTimePeriodSelector()
        setupTransactionTypeToggle()
        setupCategoryChips()
        observeData()
    }
    
    private fun setupViews() {
        // Set up info button
        binding.filterInfo.setOnClickListener {
            showFilterInfoDialog()
        }
    }
    
    private fun setupTimePeriodSelector() {
        binding.timePeriodChips.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            
            val period = when (checkedIds.first()) {
                binding.chipWeek.id -> AnalyticsViewModel.TimePeriod.LAST_7_DAYS
                binding.chipMonth.id -> AnalyticsViewModel.TimePeriod.THIS_MONTH
                binding.chipQuarter.id -> AnalyticsViewModel.TimePeriod.LAST_3_MONTHS
                binding.chipYear.id -> AnalyticsViewModel.TimePeriod.LAST_YEAR
                binding.chipAll.id -> AnalyticsViewModel.TimePeriod.ALL_TIME
                else -> AnalyticsViewModel.TimePeriod.THIS_MONTH
            }
            viewModel.setTimePeriod(period)
        }
        
        // Select default
        binding.chipMonth.isChecked = true
    }
    
    private fun setupTransactionTypeToggle() {
        binding.toggleTransactionType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            
            val transactionType = when (checkedId) {
                binding.btnExpenses.id -> AnalyticsViewModel.TransactionTypeFilter.EXPENSE
                binding.btnIncome.id -> AnalyticsViewModel.TransactionTypeFilter.INCOME
                binding.btnBoth.id -> AnalyticsViewModel.TransactionTypeFilter.BOTH
                else -> AnalyticsViewModel.TransactionTypeFilter.EXPENSE
            }
            
            viewModel.setTransactionTypeFilter(transactionType)
        }
        
        // Select expense by default
        binding.btnExpenses.isChecked = true
    }
    
    private fun setupCategoryChips() {
        binding.categoryChipsContainer.removeAllViews()
        
        // Add "All" chip
        val allChip = Chip(requireContext()).apply {
            text = "All"
            isCheckable = true
            isChecked = true
            setChipBackgroundColorResource(R.color.chip_background_color)
            setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color))
        }
        
        allChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Clear all other selections
                selectedCategories.clear()
                binding.categoryChipsContainer.children.forEach { view ->
                    if (view is Chip && view != allChip) {
                        view.isChecked = false
                    }
                }
                viewModel.setMultipleCategoryFilter(emptySet())
            }
        }
        
        binding.categoryChipsContainer.addView(allChip)
        
        // Add category chips
        TransactionCategory.values().forEach { category ->
            if (category != TransactionCategory.OTHER) {
                val chip = Chip(requireContext()).apply {
                    text = category.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    isCheckable = true
                    setChipBackgroundColorResource(R.color.chip_background_color)
                    setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_color))
                }
                
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategories.add(category)
                        allChip.isChecked = false
                    } else {
                        selectedCategories.remove(category)
                        if (selectedCategories.isEmpty()) {
                            allChip.isChecked = true
                        }
                    }
                    viewModel.setMultipleCategoryFilter(selectedCategories.toSet())
                }
                
                binding.categoryChipsContainer.addView(chip)
            }
        }
    }
    
    private fun observeData() {
        viewModel.overviewData.observe(viewLifecycleOwner) { overview ->
            if (overview == null) return@observe
            
            // Update summary cards
            binding.totalSpentAmount.text = currencyFormat.format(kotlin.math.abs(overview.totalExpense))
            binding.transactionCountValue.text = overview.transactionCount.toString()
            binding.avgTransactionValue.text = currencyFormat.format(
                if (overview.transactionCount > 0) kotlin.math.abs(overview.totalExpense) / overview.transactionCount else 0.0
            )
            
            // Update spending trend
            if (overview.expenseChangePercent != 0.0) {
                binding.spendingTrend.visibility = View.VISIBLE
                val trendIcon = if (overview.expenseChangePercent > 0) "↑" else "↓"
                val trendColor = if (overview.expenseChangePercent > 0) {
                    ContextCompat.getColor(requireContext(), R.color.pennywise_error)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.pennywise_success)
                }
                binding.spendingTrend.text = "$trendIcon ${kotlin.math.abs(overview.expenseChangePercent).toInt()}% vs last period"
                binding.spendingTrend.setTextColor(trendColor)
            } else {
                binding.spendingTrend.visibility = View.GONE
            }
            
            // Show insights
            showInsights(overview)
            
            // Update charts
            updateCharts()
        }
        
        viewModel.dailyTrend.observe(viewLifecycleOwner) { dailyData ->
            updateLineChart(dailyData)
        }
        
        viewModel.categoryData.observe(viewLifecycleOwner) { categoryData ->
            updatePieChart(categoryData)
        }
    }
    
    private fun updateCharts() {
        // Charts will update automatically via LiveData observers
        // All filtering is now handled in the ViewModel
    }
    
    private fun updateLineChart(dailyData: List<AnalyticsViewModel.DayData>) {
        binding.spendingTrendChartContainer.removeAllViews()
        
        if (dailyData.isEmpty()) {
            binding.spendingTrendChartContainer.visibility = View.GONE
            return
        }
        
        binding.spendingTrendChartContainer.visibility = View.VISIBLE
        
        val lineChart = SimpleLineChartView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        val chartData = dailyData.takeLast(7).map { day ->
            SimpleLineChartView.LineData(
                label = day.dayLabel,
                value = kotlin.math.abs(day.amount.toFloat())
            )
        }
        
        lineChart.setData(chartData)
        binding.spendingTrendChartContainer.addView(lineChart)
    }
    
    private fun updatePieChart(categoryData: List<AnalyticsViewModel.CategoryData>) {
        binding.categoryPieChartContainer.removeAllViews()
        binding.pieLegendContainer.removeAllViews()
        
        if (categoryData.isEmpty()) {
            binding.categoryPieChartContainer.visibility = View.GONE
            binding.pieLegendContainer.visibility = View.GONE
            return
        }
        
        binding.categoryPieChartContainer.visibility = View.VISIBLE
        binding.pieLegendContainer.visibility = View.VISIBLE
        
        val pieChart = SimplePieChartView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        val total = categoryData.sumOf { kotlin.math.abs(it.amount) }
        val chartData = categoryData.map { category ->
            SimplePieChartView.PieData(
                label = category.category.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                value = kotlin.math.abs(category.amount.toFloat()),
                color = getCategoryColor(category.category)
            )
        }
        
        pieChart.setData(chartData)
        binding.categoryPieChartContainer.addView(pieChart)
        
        // Add legend items
        categoryData.forEach { category ->
            val legendItem = createLegendItem(
                category.category,
                kotlin.math.abs(category.amount),
                total
            )
            binding.pieLegendContainer.addView(legendItem)
        }
    }
    
    private fun createLegendItem(category: TransactionCategory, amount: Double, total: Double): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(requireContext(), R.drawable.clickable_category_background)
            setOnClickListener {
                navigateToFilteredTransactions(category)
            }
        }
        
        // Color box
        val colorBox = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                marginEnd = 8.dpToPx()
            }
            setBackgroundColor(getCategoryColor(category))
        }
        
        // Category name and amount
        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val categoryText = TextView(requireContext()).apply {
            text = category.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.pennywise_text_primary))
        }
        
        val amountText = TextView(requireContext()).apply {
            val percentage = (amount / total * 100).toInt()
            text = "${currencyFormat.format(amount)} ($percentage%)"
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.pennywise_text_secondary))
        }
        
        textContainer.addView(categoryText)
        textContainer.addView(amountText)
        
        // Add arrow icon to indicate clickable
        val arrowIcon = TextView(requireContext()).apply {
            text = "›"
            textSize = 20f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.pennywise_text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 8.dpToPx()
            }
        }
        
        container.addView(colorBox)
        container.addView(textContainer)
        container.addView(arrowIcon)
        
        return container
    }
    
    
    private fun getCategoryColor(category: TransactionCategory): Int {
        return ColorUtils.getCategoryColor(requireContext(), category)
    }
    
    private fun showInsights(overview: AnalyticsViewModel.OverviewData) {
        binding.insightsContainer.removeAllViews()
        val insights = mutableListOf<String>()
        
        // Highest spending day insight
        overview.highestSpendingDay?.let { (day, amount) ->
            insights.add("📅 Highest spending: ${currencyFormat.format(amount)} on $day")
        }
        
        // Most frequent category insight
        overview.mostFrequentCategory?.let { category ->
            val categoryName = category.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
            insights.add("🏷️ Most transactions in: $categoryName")
        }
        
        // Average daily spending insight
        if (overview.averageDaily > 0) {
            insights.add("📊 Average daily spending: ${currencyFormat.format(overview.averageDaily)}")
        }
        
        // Savings rate insight
        if (overview.totalIncome > 0) {
            val savingsEmoji = when {
                overview.savingsRate >= 30 -> "🌟"
                overview.savingsRate >= 20 -> "✨"
                overview.savingsRate >= 10 -> "👍"
                else -> "⚠️"
            }
            insights.add("$savingsEmoji Savings rate: ${overview.savingsRate}%")
        }
        
        if (insights.isNotEmpty()) {
            binding.insightsCard.visibility = View.VISIBLE
            insights.forEach { insight ->
                val textView = TextView(requireContext()).apply {
                    text = insight
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.pennywise_text_primary))
                    setPadding(0, 4.dpToPx(), 0, 4.dpToPx())
                }
                binding.insightsContainer.addView(textView)
            }
        } else {
            binding.insightsCard.visibility = View.GONE
        }
    }
    
    private fun showFilterInfoDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Analytics Filters")
            .setMessage("Use the filters to customize your analytics view:\n\n" +
                    "• Time Period: Select the date range for analysis\n" +
                    "• Transaction Type: View expenses, income, or both\n" +
                    "• Categories: Filter by specific spending categories\n\n" +
                    "Charts will update automatically based on your selections.")
            .setPositiveButton("Got it", null)
            .show()
    }
    
    // Helper extension function
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // Helper extension to iterate over ViewGroup children
    private val ViewGroup.children: Sequence<View>
        get() = sequence {
            for (i in 0 until childCount) {
                yield(getChildAt(i))
            }
        }
    
    private fun navigateToFilteredTransactions(category: TransactionCategory) {
        val currentTimePeriod = when {
            binding.chipWeek.isChecked -> AnalyticsViewModel.TimePeriod.LAST_7_DAYS
            binding.chipMonth.isChecked -> AnalyticsViewModel.TimePeriod.THIS_MONTH
            binding.chipQuarter.isChecked -> AnalyticsViewModel.TimePeriod.LAST_3_MONTHS
            binding.chipYear.isChecked -> AnalyticsViewModel.TimePeriod.LAST_YEAR
            binding.chipAll.isChecked -> AnalyticsViewModel.TimePeriod.ALL_TIME
            else -> AnalyticsViewModel.TimePeriod.THIS_MONTH
        }
        
        val intent = Intent(requireContext(), FilteredTransactionsActivity::class.java).apply {
            putExtra("extra_category", category.name)
            putExtra("extra_time_period", currentTimePeriod.name)
        }
        startActivity(intent)
    }
}