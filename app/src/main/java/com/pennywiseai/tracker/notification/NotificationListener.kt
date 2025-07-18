package com.pennywiseai.tracker.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.llm.TransactionClassifier
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.sms.UpiSmsParser
import com.pennywiseai.tracker.subscription.SubscriptionDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {
    
    private lateinit var repository: TransactionRepository
    private lateinit var upiParser: UpiSmsParser
    private lateinit var classifier: TransactionClassifier
    private lateinit var subscriptionDetector: SubscriptionDetector
    
    companion object {
        // Banking and payment app package names
        private val BANKING_APPS = setOf(
            "com.phonepe.app",
            "net.one97.paytm",
            "com.google.android.apps.nbu.paisa.user",
            "in.amazon.mShop.android.shopping",
            "com.whatsapp",
            "com.sbi.upi",
            "com.axis.mobile",
            "com.icicibank.mobile",
            "com.hdfc.banking",
            "com.kotak.mobile",
            "com.yesbank.yesmobile"
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        repository = TransactionRepository(AppDatabase.getDatabase(this))
        upiParser = UpiSmsParser()
        classifier = TransactionClassifier(this)
        subscriptionDetector = SubscriptionDetector()
        
        // Initialize classifier in background
        CoroutineScope(Dispatchers.IO).launch {
            classifier.initialize()
        }
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName
        
        // Only process notifications from banking/payment apps
        if (!BANKING_APPS.contains(packageName)) return
        
        val notification = sbn.notification
        val extras = notification.extras
        
        val title = extras.getString("android.title") ?: ""
        val text = extras.getString("android.text") ?: ""
        val bigText = extras.getString("android.bigText") ?: ""
        
        // Combine all text content
        val fullText = "$title $text $bigText".trim()
        
        if (fullText.isNotEmpty() && looksLikeTransaction(fullText)) {
            processNotification(fullText, packageName, sbn.postTime)
        }
    }
    
    private fun looksLikeTransaction(text: String): Boolean {
        val lowerText = text.lowercase()
        
        // Check for transaction keywords
        val hasTransactionKeywords = lowerText.contains("debited") ||
                lowerText.contains("credited") ||
                lowerText.contains("paid") ||
                lowerText.contains("payment") ||
                lowerText.contains("upi") ||
                lowerText.contains("transaction")
        
        // Check for amount patterns
        val hasAmount = text.contains("Rs.", ignoreCase = true) ||
                text.contains("₹") ||
                text.contains("INR", ignoreCase = true)
        
        return hasTransactionKeywords && hasAmount
    }
    
    private fun processNotification(text: String, packageName: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Parse transaction from notification text
                val transaction = upiParser.parseUpiTransaction(text, packageName, timestamp)
                
                if (transaction != null) {
                    // Classify the transaction
                    val classifiedCategory = classifier.classifyTransaction(transaction)
                    val classifiedTransaction = transaction.copy(category = classifiedCategory)
                    
                    // Check if it's a subscription
                    val subscription = subscriptionDetector.isLikelySubscription(
                        classifiedTransaction,
                        emptyList() // For simplicity in real-time processing
                    )
                    
                    val finalTransaction = classifiedTransaction.copy(subscription = subscription)
                    
                    // Save to database
                    repository.insertTransaction(finalTransaction)
                }
                
            } catch (e: Exception) {
                // Log error but don't crash the service
                e.printStackTrace()
            }
        }
    }
}