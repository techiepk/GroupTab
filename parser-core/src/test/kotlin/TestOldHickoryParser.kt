import com.pennywiseai.parser.core.bank.OldHickoryParser

fun main() {
    val parser = OldHickoryParser()

    println("=== Old Hickory Credit Union Parser Tests ===")
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println()

    // Test 1: Standard transaction alert
    println("=== Test 1: Standard Transaction Alert ===")
    val message1 = """A transaction for $27.00 has posted to ACCOUNT NAME (part of ACCOUNT#), which is above the $0.00 value you set."""
    val sender1 = "(877) 590-7589"

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

    // Test 2: Numeric sender format
    println("\n=== Test 2: Numeric Sender Format ===")
    val message2 = """A transaction for $150.50 has posted to SAVINGS ACCOUNT (part of SAV1234), which is above the $100.00 value you set."""
    val sender2 = "8775907589"

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

    // Test 3: Text sender format
    println("\n=== Test 3: Text Sender Format ===")
    val message3 = """A transaction for $75.25 has posted to CHECKING ACCOUNT (part of CHK5678), which is above the $50.00 value you set."""
    val sender3 = "OLDHICKORY"

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

    // Test 4: Large amount with commas
    println("\n=== Test 4: Large Amount with Commas ===")
    val message4 = """A transaction for $1,250.00 has posted to BUSINESS CHECKING (part of BUS9999), which is above the $1,000.00 value you set."""
    val sender4 = "OHCU"

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

    // Test 5: Alternative sender format
    println("\n=== Test 5: Alternative Sender Format ===")
    val message5 = """A transaction for $42.99 has posted to CREDIT CARD (part of CC1111), which is above the $25.00 value you set."""
    val sender5 = "US-HICKORY-A"

    println("Can handle sender '${sender5}': ${parser.canHandle(sender5)}")
    val result5 = parser.parse(message5, sender5, System.currentTimeMillis())
    if (result5 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result5.amount}")
        println("  Currency: ${result5.currency}")
        println("  Type: ${result5.type}")
        println("  Merchant: ${result5.merchant}")
        println("  Account: ${result5.accountLast4}")
        println("  Reference: ${result5.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 6: Zero threshold
    println("\n=== Test 6: Zero Threshold ===")
    val message6 = """A transaction for $5.00 has posted to YOUTH SAVINGS (part of YS2222), which is above the $0.00 value you set."""
    val sender6 = "(877) 590-7589"

    val result6 = parser.parse(message6, sender6, System.currentTimeMillis())
    if (result6 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result6.amount}")
        println("  Currency: ${result6.currency}")
        println("  Type: ${result6.type}")
        println("  Merchant: ${result6.merchant}")
        println("  Account: ${result6.accountLast4}")
        println("  Reference: ${result6.reference}")
    } else {
        println("✗ Failed to parse")
    }
}