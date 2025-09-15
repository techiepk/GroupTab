import com.pennywiseai.parser.core.bank.SBIBankParser

fun main() {
    val parser = SBIBankParser()

    // Test 1: The problematic message
    println("=== Test 1: ATM Transaction ===")
    val message1 = "Dear Customer, transaction number 1234 for Rs.383.00 by SBI Debit Card 0000 done at -string of number redacted- on 13Sep25 at 21:38:26. Your updated available balance is Rs.999999999  -obfuscated-. If not done by you, forward this SMS to 7400165218/ call 1800111109/9449112211 to block card. GOI helpline for cyber fraud 1930."
    val sender1 = "ATMSBI"

    println("Message: ${message1.take(100)}...")
    println("Can handle sender: ${parser.canHandle(sender1)}")
    println("Contains 'by sbi debit card': ${message1.lowercase().contains("by sbi debit card")}")

    val result1 = parser.parse(message1, sender1, System.currentTimeMillis())
    if (result1 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result1.amount}")
        println("  Type: ${result1.type}")
        println("  Merchant: ${result1.merchant}")
        println("  Balance: ${result1.balance}")
        println("  Card Last 4: ${result1.accountLast4}")
        println("  Reference: ${result1.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 2: A standard debit message to verify parser works
    println("\n=== Test 2: Standard Debit ===")
    val message2 = "Rs.500 debited from A/c X1234 on 13Sep25. Avl Bal Rs.999999999"
    val sender2 = "ATMSBI"

    println("Message: $message2")
    val result2 = parser.parse(message2, sender2, System.currentTimeMillis())
    if (result2 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result2.amount}")
        println("  Type: ${result2.type}")
    } else {
        println("✗ Failed to parse")
    }
}