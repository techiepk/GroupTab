import com.pennywiseai.parser.core.bank.CitiBankParser

fun main() {
    val parser = CitiBankParser()

    println("=== Citi Bank Parser Tests ===")
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println()

    // Test 1: Standard transaction
    println("=== Test 1: Standard Transaction ===")
    val message1 = """Citi Alert: A $3.01 transaction was made at BP#1234E  on card ending in 1234. View details at citi.com/citimobileapp"""
    val sender1 = "692484"

    println("Can handle sender '${sender1}': ${parser.canHandle(sender1)}")
    val result1 = parser.parse(message1, sender1, System.currentTimeMillis())
    if (result1 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result1.amount}")
        println("  Currency: ${result1.currency}")
        println("  Type: ${result1.type}")
        println("  Merchant: ${result1.merchant}")
        println("  Account: ${result1.accountLast4}")
        println("  Reference: ${result1.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 2: Card not present transaction
    println("\n=== Test 2: Card Not Present Transaction ===")
    val message2 = """Citi Alert: Card ending in 1234 was not present for a $506.39 transaction at WWW Google C. View at citi.com/citimobileapp"""
    val sender2 = "CITI"

    println("Can handle sender '${sender2}': ${parser.canHandle(sender2)}")
    val result2 = parser.parse(message2, sender2, System.currentTimeMillis())
    if (result2 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result2.amount}")
        println("  Currency: ${result2.currency}")
        println("  Type: ${result2.type}")
        println("  Merchant: ${result2.merchant}")
        println("  Account: ${result2.accountLast4}")
        println("  Reference: ${result2.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 3: Alternative sender format
    println("\n=== Test 3: Alternative Sender Format ===")
    val message3 = """Citi Alert: A $150.00 transaction was made at AMAZON.COM on card ending in 5678. View details at citi.com/citimobileapp"""
    val sender3 = "US-CITI-A"

    println("Can handle sender '${sender3}': ${parser.canHandle(sender3)}")
    val result3 = parser.parse(message3, sender3, System.currentTimeMillis())
    if (result3 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result3.amount}")
        println("  Currency: ${result3.currency}")
        println("  Type: ${result3.type}")
        println("  Merchant: ${result3.merchant}")
        println("  Account: ${result3.accountLast4}")
        println("  Reference: ${result3.reference}")
    } else {
        println("✗ Failed to parse")
    }
}