import com.pennywiseai.parser.core.bank.CBEBankParser

fun main() {
    val parser = CBEBankParser()

    println("=== Commercial Bank of Ethiopia Parser Tests ===")
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println()

    // Test 1: Credit Transaction
    println("=== Test 1: Credit Transaction ===")
    val message1 = """Dear [Name] your Account 1*********9388 has been Credited with ETB 3,000.00 from Be***, on 13/09/2025 at 12:37:24 with Ref No ********* Your Current Balance is ETB 3,104.87. Thank you for Banking with CBE!"""
    val sender1 = "CBE"

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

    // Test 2: Debit Transaction
    println("\n=== Test 2: Debit Transaction ===")
    val message2 = """Dear [Name] your Account 1*********9388 has been debited with ETB 25.00. Your Current Balance is ETB 3,079.87 Thank you for Banking with CBE! https://apps.cbe.com.et:100/?id=FT25256RP1FK27799388"""
    val sender2 = "CBE"

    val result2 = parser.parse(message2, sender2, System.currentTimeMillis())
    if (result2 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result2.amount}")
        println("  Currency: ${result2.currency}")
        println("  Type: ${result2.type}")
        println("  Merchant: ${result2.merchant}")
        println("  Account: ${result2.accountLast4}")
        println("  Balance: ${result2.balance}")
        println("  Reference: ${result2.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 3: Transfer Transaction
    println("\n=== Test 3: Transfer Transaction ===")
    val message3 = """Dear [Name], You have transfered ETB 250.00 to Se***** on 14/09/2025 at 12:28:56 from your account 1*********9388. Your account has been debited with a S.charge of ETB 0 and  15% VAT of ETB0.00, with a total of ETB250. Your Current Balance is ETB 2,829.87. Thank you for Banking with CBE!"""
    val sender3 = "CBE"

    val result3 = parser.parse(message3, sender3, System.currentTimeMillis())
    if (result3 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result3.amount}")
        println("  Currency: ${result3.currency}")
        println("  Type: ${result3.type}")
        println("  Merchant: ${result3.merchant}")
        println("  Account: ${result3.accountLast4}")
        println("  Balance: ${result3.balance}")
        println("  Reference: ${result3.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 4: Alternative sender format
    println("\n=== Test 4: Alternative Sender Format ===")
    val message4 = """Dear Customer your Account 1*********1234 has been Credited with ETB 5,000.00 from Salary Payment, on 15/09/2025 at 09:00:00 with Ref No ABC123456 Your Current Balance is ETB 8,000.00. Thank you for Banking with CBE!"""
    val sender4 = "CBEBANK"

    println("Can handle sender '${sender4}': ${parser.canHandle(sender4)}")
    val result4 = parser.parse(message4, sender4, System.currentTimeMillis())
    if (result4 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result4.amount}")
        println("  Currency: ${result4.currency}")
        println("  Type: ${result4.type}")
        println("  Merchant: ${result4.merchant}")
        println("  Account: ${result4.accountLast4}")
        println("  Balance: ${result4.balance}")
        println("  Reference: ${result4.reference}")
    } else {
        println("✗ Failed to parse")
    }

    // Test 5: Large amount with commas
    println("\n=== Test 5: Large Amount with Commas ===")
    val message5 = """Dear [Name] your Account 1*********5678 has been Credited with ETB 125,500.50 from Business Payment, on 16/09/2025 at 14:30:00 Your Current Balance is ETB 130,000.75. Thank you for Banking with CBE!"""
    val sender5 = "CBE"

    val result5 = parser.parse(message5, sender5, System.currentTimeMillis())
    if (result5 != null) {
        println("✓ Parsed successfully!")
        println("  Amount: ${result5.amount}")
        println("  Currency: ${result5.currency}")
        println("  Type: ${result5.type}")
        println("  Merchant: ${result5.merchant}")
        println("  Account: ${result5.accountLast4}")
        println("  Balance: ${result5.balance}")
        println("  Reference: ${result5.reference}")
    } else {
        println("✗ Failed to parse")
    }
}