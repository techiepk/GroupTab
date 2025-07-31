package com.example.pennywisecompose.learning

import java.time.Instant
import java.util.UUID

// 1. Variables and Types
fun variablesAndTypes() {
    val immutableName: String = "PennyWise"  // Can't be changed
    var mutableAmount: Double = 250.50      // Can be changed
    
    var nullable: String? = null           // Can be null
    val nonNull: String = "Always has value"
    
    val inferredType = "Kotlin infers this is String"
    val number = 42                        // Int
    val decimal = 3.14                     // Double
    val isActive = true                    // Boolean
}

// 2. Functions
fun calculateTotal(amount: Double, tax: Double = 0.18): Double {
    return amount + (amount * tax)
}

fun greet(name: String) = "Hello, $name!"

// 3. Classes and Data Classes
class Transaction(
    val id: String,
    val amount: Double,
    val merchant: String
) {
    fun display() = "$merchant: ₹$amount"
}

data class TransactionData(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val merchant: String,
    val date: Instant = Instant.now()
)

// 4. Enums
enum class Category {
    FOOD, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT
}

// 5. Collections
fun collectionsExample() {
    val categories = listOf("Food", "Transport", "Shopping")
    val mutableList = mutableListOf<String>()
    
    val transactionMap = mapOf(
        "id1" to TransactionData(amount = 250.0, merchant = "Swiggy"),
        "id2" to TransactionData(amount = 180.0, merchant = "Uber")
    )
    
    val amounts = setOf(100.0, 200.0, 100.0)  // Duplicates removed
}

// 6. Control Flow
fun categorizeAmount(amount: Double): String {
    return when {
        amount < 100 -> "Small"
        amount < 1000 -> "Medium"
        else -> "Large"
    }
}

fun processTransactions(transactions: List<TransactionData>) {
    for (transaction in transactions) {
        println(transaction.display())
    }
    
    transactions.forEach { transaction ->
        println("Processing ${transaction.id}")
    }
    
    val foodTransactions = transactions.filter { it.merchant.contains("Swiggy") }
    val totalAmount = transactions.sumOf { it.amount }
}

// 7. Null Safety
fun handleNullable(text: String?) {
    val length = text?.length ?: 0
    
    text?.let {
        println("Text is not null: $it")
    }
    
    val guaranteed = text ?: "Default Value"
}

// 8. Extension Functions
fun TransactionData.display(): String {
    return "$merchant: ₹$amount on $date"
}

fun Double.toRupeeString(): String {
    return "₹${String.format("%.2f", this)}"
}

// 9. Higher-Order Functions and Lambdas
fun processTransaction(
    transaction: TransactionData,
    onSuccess: (TransactionData) -> Unit,
    onError: (String) -> Unit
) {
    if (transaction.amount > 0) {
        onSuccess(transaction)
    } else {
        onError("Invalid amount")
    }
}

// 10. Coroutines Preview (for async operations)
suspend fun fetchTransactions(): List<TransactionData> {
    // Simulated async operation
    return listOf(
        TransactionData(amount = 250.0, merchant = "Swiggy"),
        TransactionData(amount = 180.0, merchant = "Uber")
    )
}

// Practice Examples
fun main() {
    // Creating objects
    val transaction = TransactionData(
        amount = 299.99,
        merchant = "Amazon"
    )
    
    // Using extension functions
    println(transaction.display())
    println(250.50.toRupeeString())
    
    // Lambda usage
    processTransaction(
        transaction,
        onSuccess = { trans ->
            println("Success: ${trans.merchant}")
        },
        onError = { error ->
            println("Error: $error")
        }
    )
    
    // Collection operations
    val transactions = listOf(
        TransactionData(amount = 250.0, merchant = "Swiggy"),
        TransactionData(amount = 180.0, merchant = "Uber"),
        TransactionData(amount = 1500.0, merchant = "Amazon")
    )
    
    val totalSpent = transactions.sumOf { it.amount }
    val expensiveOnes = transactions.filter { it.amount > 200 }
    val merchantNames = transactions.map { it.merchant }
}