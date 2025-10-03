import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.ADCBParser
import java.math.BigDecimal

data class ADCBTestCase(
    val name: String,
    val message: String,
    val sender: String = "ADCBAlert",
    val expected: ExpectedTransaction
)

fun main() {
    val parser = ADCBParser()

    println("=" * 80)
    println("Abu Dhabi Commercial Bank (ADCB) Parser Test Suite")
    println("=" * 80)
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println("Can Handle 'ADCBAlert': ${parser.canHandle("ADCBAlert")}")
    println()

    val testCases = listOf(
        ADCBTestCase(
            name = "Debit Card Purchase (AED)",
            message = "Your debit card XXX1234 linked to acc. XXX810001 was used for AED100.50 on Jul 10 2024  5:49PM at MERCHANT123,AE. Avl.Bal AED 200.75.",
            expected = ExpectedTransaction(
                amount = BigDecimal("100.50"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                merchant = "MERCHANT123",
                accountLast4 = "810001",
                balance = BigDecimal("200.75")
            )
        ),

        ADCBTestCase(
            name = "Debit Card Purchase (Foreign Currency)",
            message = "Your debit card XXX1234 linked to acc. XXX810001 was used for USD50.25 on Jul 11 2024  1:00PM at ONLINEPLATFORM,GE. Avl.Bal AED 150.50.",
            expected = ExpectedTransaction(
                amount = BigDecimal("50.25"),
                currency = "USD",
                type = TransactionType.EXPENSE,
                merchant = "ONLINEPLATFORM",
                accountLast4 = "810001",
                balance = BigDecimal("150.50")
            )
        ),

        ADCBTestCase(
            name = "ATM Deposit",
            message = "AED5000.00 has been deposited via ATM in your account XXX810001 on Jan 16 2025 16:56 at LOCATION123. Available Balance is AED5200.25.",
            expected = ExpectedTransaction(
                amount = BigDecimal("5000.00"),
                currency = "AED",
                type = TransactionType.INCOME,
                merchant = "ATM Deposit: LOCATION123",
                accountLast4 = "810001",
                balance = BigDecimal("5200.25")
            )
        ),

        ADCBTestCase(
            name = "Bank Transfer",
            message = "AED750.50 transferred via ADCB Personal Internet Banking / Mobile App from acc. no. XXX810001 on Feb  4 2025 12:49PM. Avl. bal. AED 2000.00.",
            expected = ExpectedTransaction(
                amount = BigDecimal("750.50"),
                currency = "AED",
                type = TransactionType.TRANSFER,
                merchant = "Transfer via ADCB Banking",
                accountLast4 = "810001",
                balance = BigDecimal("2000.00")
            )
        ),

        ADCBTestCase(
            name = "Account Credit",
            message = "A Cr. transaction of AED 200.00 on your account number XXX810001 was successful.Available balance is AED 220.25.",
            expected = ExpectedTransaction(
                amount = BigDecimal("200.00"),
                currency = "AED",
                type = TransactionType.INCOME,
                merchant = "Account Credit",
                accountLast4 = "810001",
                balance = BigDecimal("220.25")
            )
        ),

        ADCBTestCase(
            name = "Account Debit",
            message = "A Dr. transaction of AED 2.10 on your account number XXX810001 was successful.Available balance is 13697.16.",
            expected = ExpectedTransaction(
                amount = BigDecimal("2.10"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                merchant = "Account Debit",
                accountLast4 = "810001",
                balance = BigDecimal("13697.16")
            )
        ),

        ADCBTestCase(
            name = "ATM Withdrawal (AED)",
            message = "AED1500.50 withdrawn from acc. XXX810001 on Jan 8 2025 3:07PM at ATM-BANK123. Avl.Bal.AED1200.75. Be cautious with large amt. of cash.",
            expected = ExpectedTransaction(
                amount = BigDecimal("1500.50"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                merchant = "ATM Withdrawal: BANK123",
                accountLast4 = "810001",
                balance = BigDecimal("1200.75")
            )
        ),

        ADCBTestCase(
            name = "ATM Withdrawal (Foreign Currency)",
            message = "EUR350.25 withdrawn from acc. XXX810001 on Jun 16 2025 5:07PM at ATM-SHOPPINGMALL. Avl.Bal.AED150.50. Be cautious with large amt. of cash.",
            expected = ExpectedTransaction(
                amount = BigDecimal("350.25"),
                currency = "EUR",
                type = TransactionType.EXPENSE,
                merchant = "ATM Withdrawal: SHOPPINGMALL",
                accountLast4 = "810001",
                balance = BigDecimal("150.50")
            )
        ),

        ADCBTestCase(
            name = "ATM Withdrawal with Numeric ID",
            message = "GBP4500.75 withdrawn from acc. XXX810001 on Dec 24 2024 11:14PM at ATM-123456LOCATION123. Avl.Bal.AED250.25. Be cautious with large amt. of cash.",
            expected = ExpectedTransaction(
                amount = BigDecimal("4500.75"),
                currency = "GBP",
                type = TransactionType.EXPENSE,
                merchant = "ATM Withdrawal: LOCATION123",
                accountLast4 = "810001",
                balance = BigDecimal("250.25")
            )
        ),

        ADCBTestCase(
            name = "Debit Card Purchase (THB - No Space)",
            message = "Your debit card XXX0830 linked to acc. XXX810001 was used for THB28.25 on Jun 16 2025  5:02PM at SHOPPING MALL,TH. Avl.Bal AED 321.56.",
            expected = ExpectedTransaction(
                amount = BigDecimal("28.25"),
                currency = "THB",
                type = TransactionType.EXPENSE,
                merchant = "SHOPPING MALL",
                accountLast4 = "810001",
                balance = BigDecimal("321.56")
            )
        ),

        ADCBTestCase(
            name = "Debit Card Purchase (AED - No Space)",
            message = "Your debit card XXX0830 linked to acc. XXX810001 was used for AED26.80 on Jun 13 2025  4:28PM at TRANSPORT SERVICE,AE. Avl.Bal AED 2928.77.",
            expected = ExpectedTransaction(
                amount = BigDecimal("26.80"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                merchant = "TRANSPORT SERVICE",
                accountLast4 = "810001",
                balance = BigDecimal("2928.77")
            )
        ),

        ADCBTestCase(
            name = "TouchPoints Redemption",
            message = "TouchPoints Redemption Request registered successfully on 11-06-2025 15:58:58 Amount Paid: AED 100.00 TouchPoints Remaining: 344.",
            expected = ExpectedTransaction(
                amount = BigDecimal("100.00"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                merchant = "TouchPoints Redemption",
                accountLast4 = null,
                balance = null
            )
        ),

        // Test cases that should NOT be parsed (non-transaction messages)
        ADCBTestCase(
            name = "Failed Transaction (Should Not Parse)",
            message = "Transaction of USD 75.50 made on Jul 13, 2024 12:30AM on your Debit Card XXX1234 could not be completed due to insufficient funds. For assistance, please call BANK_HOTLINE",
            expected = ExpectedTransaction(
                amount = BigDecimal.ZERO,
                currency = "",
                type = TransactionType.EXPENSE
            )
        ),

        ADCBTestCase(
            name = "OTP Message (Should Not Parse)",
            message = "Do not share your OTP with anyone. If not initiated by you, please call BANK_HOTLINE. OTP for transaction at RETAILER for THB 25.00 on your ADCB Debit Car...",
            expected = ExpectedTransaction(
                amount = BigDecimal.ZERO,
                currency = "",
                type = TransactionType.EXPENSE
            )
        ),

        ADCBTestCase(
            name = "Card Management (Should Not Parse)",
            message = "Your digital card assigned to ADCB Debit Card XXX1234 for PAYMENT_SERVICE has been de-activated. Please call BANK_HOTLINE if you have not initiated this request.",
            expected = ExpectedTransaction(
                amount = BigDecimal.ZERO,
                currency = "",
                type = TransactionType.EXPENSE
            )
        ),

        ADCBTestCase(
            name = "Activation Message (Should Not Parse)",
            message = "Activation Key for your ADCB App is ACTIVATION_CODE Valid for 24 hours. Do not share with anyone. If not initiated by you, please call BANK_HOTLINE",
            expected = ExpectedTransaction(
                amount = BigDecimal.ZERO,
                currency = "",
                type = TransactionType.EXPENSE
            )
        )
    )

    var passed = 0
    var failed = 0

    testCases.forEachIndexed { index, testCase ->
        println("Test ${index + 1}: ${testCase.name}")
        println("-" * 80)

        val result = parser.parse(testCase.message, testCase.sender, System.currentTimeMillis())
        val expected = testCase.expected

        // For tests that should not parse, expected amount will be ZERO
        val shouldNotParse = expected.amount == BigDecimal.ZERO && expected.currency.isEmpty()

        if (shouldNotParse) {
            if (result == null) {
                println("✓ PASSED: Correctly rejected non-transaction message")
                passed++
            } else {
                println("✗ FAILED: Should have rejected this message but parsed: ${result.amount} ${result.currency} - ${result.type}")
                failed++
            }
            println()
            return@forEachIndexed
        }

        if (result == null) {
            println("✗ FAILED: Parser returned null")
            failed++
            println()
            return@forEachIndexed
        }

        val failures = mutableListOf<String>()

        // Validate amount
        if (result.amount != expected.amount) {
            failures.add("Amount: expected ${expected.amount}, got ${result.amount}")
        }

        // Validate currency
        if (result.currency != expected.currency) {
            failures.add("Currency: expected ${expected.currency}, got ${result.currency}")
        }

        // Validate type
        if (result.type != expected.type) {
            failures.add("Type: expected ${expected.type}, got ${result.type}")
        }

        // Validate merchant (if expected)
        if (expected.merchant != null && result.merchant != expected.merchant) {
            failures.add("Merchant: expected '${expected.merchant}', got '${result.merchant}'")
        }

        // Validate account (if expected)
        if (expected.accountLast4 != null && result.accountLast4 != expected.accountLast4) {
            failures.add("Account: expected '${expected.accountLast4}', got '${result.accountLast4}'")
        }

        // Validate balance (if expected)
        if (expected.balance != null && result.balance != expected.balance) {
            failures.add("Balance: expected ${expected.balance}, got ${result.balance}")
        }

        // Validate reference (if expected)
        if (expected.reference != null && result.reference != expected.reference) {
            failures.add("Reference: expected '${expected.reference}', got '${result.reference}'")
        }

        if (failures.isEmpty()) {
            println("✓ PASSED")
            passed++
        } else {
            println("✗ FAILED:")
            failures.forEach { println("  - $it") }
            failed++
        }

        println()
    }

    // Summary
    println("=" * 80)
    println("Test Summary")
    println("=" * 80)
    println("Total: ${testCases.size}")
    println("Passed: $passed ✓")
    println("Failed: $failed ✗")
    println("Success Rate: ${(passed * 100.0 / testCases.size).toInt()}%")
    println("=" * 80)

    // Success criteria
    val minimumSuccessRate = 80
    val actualSuccessRate = (passed * 100.0 / testCases.size).toInt()

    if (actualSuccessRate >= minimumSuccessRate) {
        println("✅ TEST PASSED: Success rate $actualSuccessRate% meets minimum requirement of $minimumSuccessRate%")
    } else {
        println("❌ TEST FAILED: Success rate $actualSuccessRate% is below minimum requirement of $minimumSuccessRate%")
    }
}

private operator fun String.times(count: Int): String = this.repeat(count)