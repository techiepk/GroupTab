package com.pennywiseai.tracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.database.CategorySpending
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class DataExporter(
    private val context: Context,
    private val repository: TransactionRepository
) {
    
    companion object {
        private const val EXPORT_FOLDER = "exports"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
    
    data class ExportResult(
        val success: Boolean,
        val filePath: String? = null,
        val error: String? = null
    )
    
    suspend fun exportTransactionsToCSV(
        startDate: Long? = null,
        endDate: Long? = null,
        category: TransactionCategory? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Get transactions
            val allTransactions = repository.getAllTransactionsSync()
            var transactions = allTransactions
            
            // Apply filters
            if (startDate != null && endDate != null) {
                transactions = transactions.filter { it.date >= startDate && it.date <= endDate }
            }
            category?.let {
                transactions = transactions.filter { it.category == category }
            }
            
            // Sort by date descending
            transactions = transactions.sortedByDescending { it.date }
            
            // Create export directory in Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportDir = File(downloadsDir, "PennyWise_Exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // Create CSV file with readable name
            val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "PennyWise_Transactions_$dateStr.csv"
            val file = File(exportDir, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.appendLine("Date,Time,Merchant,Category,Amount,Type,UPI ID,Confidence")
                
                // Write transactions
                transactions.forEach { transaction ->
                    val date = dateFormat.format(Date(transaction.date))
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(transaction.date))
                    val merchant = escapeCSV(transaction.merchant)
                    val category = transaction.category.name.replace("_", " ")
                    val amount = String.format("%.2f", abs(transaction.amount))
                    val type = if (transaction.amount < 0) "Debit" else "Credit"
                    val upiId = transaction.upiId ?: ""
                    val confidence = String.format("%.2f%%", transaction.confidence * 100)
                    
                    writer.appendLine("$date,$time,$merchant,$category,$amount,$type,$upiId,$confidence")
                }
            }
            
            ExportResult(true, file.absolutePath)
            
        } catch (e: Exception) {
            ExportResult(false, error = e.message ?: "Unknown error occurred")
        }
    }
    
    suspend fun exportSubscriptionsToCSV(): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Get subscriptions
            val subscriptions = repository.getActiveSubscriptionsSync()
            
            // Create export directory in Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportDir = File(downloadsDir, "PennyWise_Exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // Create CSV file with readable name
            val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "PennyWise_Subscriptions_$dateStr.csv"
            val file = File(exportDir, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.appendLine("Merchant,Amount,Frequency,Status,Next Payment,Last Payment,Category,Created Date")
                
                // Write subscriptions
                subscriptions.forEach { subscription ->
                    val merchant = escapeCSV(subscription.merchantName)
                    val amount = String.format("%.2f", subscription.amount)
                    val frequency = subscription.frequency.name.replace("_", " ")
                    val status = subscription.status.name.replace("_", " ")
                    val nextPayment = dateFormat.format(Date(subscription.nextPaymentDate))
                    val lastPayment = subscription.lastPaymentDate?.let { dateFormat.format(Date(it)) } ?: "N/A"
                    val category = subscription.category.name.replace("_", " ")
                    val createdDate = dateFormat.format(Date(subscription.startDate))
                    
                    writer.appendLine("$merchant,$amount,$frequency,$status,$nextPayment,$lastPayment,$category,$createdDate")
                }
            }
            
            ExportResult(true, file.absolutePath)
            
        } catch (e: Exception) {
            ExportResult(false, error = e.message ?: "Unknown error occurred")
        }
    }
    
    suspend fun exportAnalyticsReport(
        startDate: Long,
        endDate: Long
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Get data for analytics
            val transactions = repository.getAllTransactionsSync()
                .filter { it.date >= startDate && it.date <= endDate }
            
            val categorySpending = repository.getCategorySpending(startDate, endDate)
            val totalSpending = transactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
            val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
            
            // Create export directory in Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportDir = File(downloadsDir, "PennyWise_Exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // Create text report file with readable name
            val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "PennyWise_Analytics_Report_$dateStr.txt"
            val file = File(exportDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.appendLine("PennyWise AI - Financial Analytics Report")
                writer.appendLine("=" * 50)
                writer.appendLine()
                writer.appendLine("Report Period: ${dateFormat.format(Date(startDate))} to ${dateFormat.format(Date(endDate))}")
                writer.appendLine("Generated on: ${dateTimeFormat.format(Date())}")
                writer.appendLine()
                writer.appendLine("SUMMARY")
                writer.appendLine("-" * 30)
                writer.appendLine("Total Transactions: ${transactions.size}")
                writer.appendLine("Total Spending: ₹${String.format("%,.2f", totalSpending)}")
                writer.appendLine("Total Income: ₹${String.format("%,.2f", totalIncome)}")
                writer.appendLine("Net: ₹${String.format("%,.2f", totalIncome - totalSpending)}")
                writer.appendLine()
                writer.appendLine("SPENDING BY CATEGORY")
                writer.appendLine("-" * 30)
                
                categorySpending.sortedByDescending { abs(it.total) }.forEach { category ->
                    val percentage = (abs(category.total) / totalSpending) * 100
                    writer.appendLine("${category.category.name.replace("_", " ").padEnd(20)} ₹${String.format("%,10.2f", abs(category.total))} (${String.format("%5.1f%%", percentage)})")
                }
                
                writer.appendLine()
                writer.appendLine("TOP MERCHANTS")
                writer.appendLine("-" * 30)
                
                // Get top merchants
                val merchantSpending = transactions
                    .filter { it.amount < 0 }
                    .groupBy { it.merchant }
                    .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(10)
                
                merchantSpending.forEach { (merchant, amount) ->
                    writer.appendLine("${merchant.padEnd(30)} ₹${String.format("%,10.2f", amount)}")
                }
                
                writer.appendLine()
                writer.appendLine("DAILY AVERAGE")
                writer.appendLine("-" * 30)
                val days = ((endDate - startDate) / (24 * 60 * 60 * 1000)).toInt() + 1
                writer.appendLine("Average spending per day: ₹${String.format("%,.2f", totalSpending / days)}")
                writer.appendLine("Average transactions per day: ${String.format("%.1f", transactions.size.toDouble() / days)}")
            }
            
            ExportResult(true, file.absolutePath)
            
        } catch (e: Exception) {
            ExportResult(false, error = e.message ?: "Unknown error occurred")
        }
    }
    
    fun shareFile(filePath: String, mimeType: String = "text/csv") {
        val file = File(filePath)
        
        // For files in Downloads folder, we can use the file URI directly
        val uri = if (file.absolutePath.contains(Environment.DIRECTORY_DOWNLOADS)) {
            Uri.fromFile(file)
        } else {
            // For app internal files, use FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PennyWise AI Export - ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Export"))
    }
    
    fun getDownloadNotificationMessage(fileName: String): String {
        return "File saved to Downloads/PennyWise_Exports/$fileName"
    }
    
    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
    
    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}