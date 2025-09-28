import com.pennywiseai.parser.core.bank.FABParser

fun main() {
    val parser = FABParser()

    println("=== First Abu Dhabi Bank Parser Tests ===")
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println()

    // Test 1: Credit Card Purchase
    println("=== Test 1: Credit Card Purchase ===")
    val message1 = """Credit Card Purchase
Card No XXXX
AED 8.00
T*** R** DUBAI ARE
23/09/25 16:17
Available Balance AED **30.16
Your September statement payment due date is 26/09/2025
Pay school fees in 12 instalments at 0% interest with no fee. bit.ly/47nWGYG Conditions apply."""
    val sender1 = "FAB"

    println("Can handle sender '${sender1}': ${parser.canHandle(sender1)}")
    val result1 = parser.parse(message1, sender1, System.currentTimeMillis())
    if (result1 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result1.amount}")
        println("  Currency: ${result1.currency}")
        println("  Type: ${result1.type}")
        println("  Merchant: ${result1.merchant}")
        println("  Account: ${result1.accountLast4}")
        println("  Balance: ${result1.balance}")
        println("  Reference: ${result1.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 2: Inward Remittance
    println("\n=== Test 2: Inward Remittance ===")
    val message2 = """Inward Remittance
Credit
Account XXXX**
AED *0.00
Value Date 18/09/2025
Available Balance AED ***0.00"""
    val sender2 = "FAB"

    val result2 = parser.parse(message2, sender2, System.currentTimeMillis())
    if (result2 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result2.amount}")
        println("  Currency: ${result2.currency}")
        println("  Type: ${result2.type}")
        println("  Merchant: ${result2.merchant}")
        println("  Balance: ${result2.balance}")
        println("  Reference: ${result2.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 3: Payment Instructions
    println("\n=== Test 3: Payment Instructions ===")
    val message3 = "Dear Customer, Your payment instructions of AED *.00 to 5xxx**1xxx has been processed on 10/09/2025 15:28"
    val sender3 = "FAB"

    val result3 = parser.parse(message3, sender3, System.currentTimeMillis())
    if (result3 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result3.amount}")
        println("  Currency: ${result3.currency}")
        println("  Type: ${result3.type}")
        println("  Merchant: ${result3.merchant}")
        println("  Reference: ${result3.reference}")
    } else {
        println("✗ Failed to parse")
    }
}