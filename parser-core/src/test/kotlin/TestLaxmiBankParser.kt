import com.pennywiseai.parser.core.bank.LaxmiBankParser

fun main() {
    val parser = LaxmiBankParser()

    println("=== Laxmi Sunrise Bank Parser Tests ===")
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println()

    // Test 1: Debit Transaction (ESEWA Load)
    println("=== Test 1: Debit Transaction (ESEWA Load) ===")
    val message1 = """Dear Customer, Your #12344560 has been debited by NPR 720.00 on 05/09/25. Remarks:ESEWA LOAD/9763698550,127847587
-Laxmi Sunrise"""
    val sender1 = "LAXMI_ALERT"

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

    // Test 2: Credit Transaction (Stipend Payment)
    println("\n=== Test 2: Credit Transaction (Stipend Payment) ===")
    val message2 = """Dear Customer, Your #12344560 has been credited by NPR 60,892.00 on 02/09/25. Remarks:(STIPEND PMT DM/MCH-SHRAWAN82).
-Laxmi Sunrise"""
    val sender2 = "LAXMI_ALERT"

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
    val message3 = """Dear Customer, Your #98765432 has been debited by NPR 1,500.00 on 10/09/25. Remarks:ATM WITHDRAWAL/KATHMANDU
-Laxmi Sunrise"""
    val sender3 = "LAXMI"

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

    // Test 4: Different account number length
    println("\n=== Test 4: Short Account Number ===")
    val message4 = """Dear Customer, Your #1234 has been credited by NPR 5,000.00 on 15/09/25. Remarks:SALARY CREDIT
-Laxmi Sunrise"""
    val sender4 = "LAXMI_ALERT"

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