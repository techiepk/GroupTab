import com.pennywiseai.parser.core.bank.EverestBankParser

fun main() {
    val parser = EverestBankParser()

    println("=== Everest Bank Parser Tests ===")
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println()

    // Test 1: Debit Transaction (Numeric Sender)
    println("=== Test 1: Debit Transaction (Numeric Sender) ===")
    val message1 = """Dear Customer, Your A/c 12345678 is debited by NPR 520.00 For: 9843368/Mobile Recharge,Ncell. Never Share Password/OTP With Anyone"""
    val sender1 = "9843368"

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

    // Test 2: Credit Transaction
    println("\n=== Test 2: Credit Transaction ===")
    val message2 = """Dear Customer, Your A/c 87654321 is credited by NPR 15,000.00 For: Salary Payment/Monthly Salary,UJJ SH. Never Share Password/OTP With Anyone"""
    val sender2 = "UJJ SH"

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

    // Test 3: ATM Withdrawal
    println("\n=== Test 3: ATM Withdrawal ===")
    val message3 = """Dear Customer, Your A/c 11223344 is debited by NPR 6,000.00 For: CWDR/521708008016/202508050854. Never Share Password/OTP With Anyone"""
    val sender3 = "CWRD"

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

    // Test 4: Different numeric sender
    println("\n=== Test 4: Different Numeric Sender ===")
    val message4 = """Dear Customer, Your A/c 99887766 is debited by NPR 1,250.00 For: 9801234567/Food Delivery,Foodmandu. Never Share Password/OTP With Anyone"""
    val sender4 = "9801234567"

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

    // Test 5: Large amount with complex for field
    println("\n=== Test 5: Large Amount Transfer ===")
    val message5 = """Dear Customer, Your A/c 55443322 is credited by NPR 50,000.00 For: Business Payment/Invoice Settlement,ABC Company. Never Share Password/OTP With Anyone"""
    val sender5 = "9843001"

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

    // Test 6: Alternative sender format
    println("\n=== Test 6: Alternative Sender Format ===")
    val message6 = """Dear Customer, Your A/c 33445566 is debited by NPR 750.00 For: Online Shopping/E-commerce,Daraz. Never Share Password/OTP With Anyone"""
    val sender6 = "EVEREST"

    println("Can handle sender '${sender6}': ${parser.canHandle(sender6)}")
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