import com.pennywiseai.parser.core.bank.DiscoverCardParser

fun main() {
    val parser = DiscoverCardParser()

    println("=== Discover Card Parser Tests ===")
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println()

    // Test 1: Standard transaction with date
    println("=== Test 1: Standard Transaction with Date ===")
    val message1 = """Discover Card Alert: A transaction of $25.00 at WWW.XXX.ORG on February 21, 2025. No Action needed. See it at https://app.discover.com/ACTVT. Text STOP to end"""
    val sender1 = "347268"

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

    // Test 2: PayPal transaction
    println("\n=== Test 2: PayPal Transaction ===")
    val message2 = """Discover Card Alert: A transaction of $5.36 at PAYPAL *SParkXXX on July 20, 2025. No Action needed. See it at https://app.discover.com/ACTVT. Text STOP to end"""
    val sender2 = "DISCOVER"

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
    val message3 = """Discover Card Alert: A transaction of $99.99 at NETFLIX.COM on March 15, 2025. No Action needed. See it at https://app.discover.com/ACTVT. Text STOP to end"""
    val sender3 = "US-DISCOVER-A"

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

    // Test 4: Different merchant format
    println("\n=== Test 4: Different Merchant Format ===")
    val message4 = """Discover Card Alert: A transaction of $42.50 at STARBUCKS STORE #1234 on April 10, 2025. No Action needed. See it at https://app.discover.com/ACTVT. Text STOP to end"""
    val sender4 = "347268"

    println("Can handle sender '${sender4}': ${parser.canHandle(sender4)}")
    val result4 = parser.parse(message4, sender4, System.currentTimeMillis())
    if (result4 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result4.amount}")
        println("  Currency: ${result4.currency}")
        println("  Type: ${result4.type}")
        println("  Merchant: ${result4.merchant}")
        println("  Account: ${result4.accountLast4}")
        println("  Reference: ${result4.reference}")
    } else {
        println("✗ Failed to parse")
    }
}