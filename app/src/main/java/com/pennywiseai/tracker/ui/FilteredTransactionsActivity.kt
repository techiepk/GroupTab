package com.pennywiseai.tracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.databinding.ActivityFilteredTransactionsBinding
import com.pennywiseai.tracker.viewmodel.AnalyticsViewModel

class FilteredTransactionsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFilteredTransactionsBinding
    
    companion object {
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_MERCHANT = "extra_merchant"
        private const val EXTRA_START_DATE = "extra_start_date"
        private const val EXTRA_END_DATE = "extra_end_date"
        private const val EXTRA_TIME_PERIOD = "extra_time_period"
        
        fun startWithCategory(
            context: Context, 
            category: TransactionCategory,
            timePeriod: AnalyticsViewModel.TimePeriod? = null
        ) {
            val intent = Intent(context, FilteredTransactionsActivity::class.java).apply {
                putExtra(EXTRA_CATEGORY, category.name)
                timePeriod?.let { putExtra(EXTRA_TIME_PERIOD, it.name) }
            }
            context.startActivity(intent)
        }
        
        fun startWithMerchant(
            context: Context,
            merchant: String,
            timePeriod: AnalyticsViewModel.TimePeriod? = null
        ) {
            val intent = Intent(context, FilteredTransactionsActivity::class.java).apply {
                putExtra(EXTRA_MERCHANT, merchant)
                timePeriod?.let { putExtra(EXTRA_TIME_PERIOD, it.name) }
            }
            context.startActivity(intent)
        }
        
        fun startWithDateRange(
            context: Context,
            startDate: Long,
            endDate: Long
        ) {
            val intent = Intent(context, FilteredTransactionsActivity::class.java).apply {
                putExtra(EXTRA_START_DATE, startDate)
                putExtra(EXTRA_END_DATE, endDate)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilteredTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupFragment()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            
            // Set title based on filter
            title = when {
                intent.hasExtra(EXTRA_CATEGORY) -> {
                    val categoryName = intent.getStringExtra(EXTRA_CATEGORY) ?: "Transactions"
                    categoryName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                }
                intent.hasExtra(EXTRA_MERCHANT) -> {
                    intent.getStringExtra(EXTRA_MERCHANT) ?: "Transactions"
                }
                else -> "Filtered Transactions"
            }
        }
    }
    
    private fun setupFragment() {
        // Get filter parameters
        val categoryName = intent.getStringExtra(EXTRA_CATEGORY)
        val category = categoryName?.let { TransactionCategory.valueOf(it) }
        val merchant = intent.getStringExtra(EXTRA_MERCHANT)
        var startDate = if (intent.hasExtra(EXTRA_START_DATE)) intent.getLongExtra(EXTRA_START_DATE, 0) else null
        var endDate = if (intent.hasExtra(EXTRA_END_DATE)) intent.getLongExtra(EXTRA_END_DATE, 0) else null
        
        // If time period is provided, calculate date range
        val timePeriodName = intent.getStringExtra(EXTRA_TIME_PERIOD)
        if (timePeriodName != null && startDate == null && endDate == null) {
            val timePeriod = AnalyticsViewModel.TimePeriod.valueOf(timePeriodName)
            val (start, end) = getDateRange(timePeriod)
            startDate = start
            endDate = end
        }
        
        // Create fragment with filters
        val fragment = TransactionsSimpleFragment.newInstance(
            category = category,
            merchant = merchant,
            startDate = startDate,
            endDate = endDate
        )
        
        // Add fragment
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }
    
    private fun getDateRange(timePeriod: AnalyticsViewModel.TimePeriod): Pair<Long, Long> {
        val calendar = java.util.Calendar.getInstance()
        val endDate = System.currentTimeMillis()
        
        val startDate = when (timePeriod) {
            AnalyticsViewModel.TimePeriod.THIS_MONTH -> {
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            AnalyticsViewModel.TimePeriod.LAST_30_DAYS -> {
                endDate - (30L * 24 * 60 * 60 * 1000)
            }
            AnalyticsViewModel.TimePeriod.LAST_3_MONTHS -> {
                calendar.add(java.util.Calendar.MONTH, -3)
                calendar.timeInMillis
            }
            AnalyticsViewModel.TimePeriod.LAST_YEAR -> {
                calendar.add(java.util.Calendar.YEAR, -1)
                calendar.timeInMillis
            }
            AnalyticsViewModel.TimePeriod.ALL_TIME -> {
                0L
            }
            AnalyticsViewModel.TimePeriod.LAST_7_DAYS -> {
                endDate - (7L * 24 * 60 * 60 * 1000)
            }
        }
        
        return Pair(startDate, endDate)
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