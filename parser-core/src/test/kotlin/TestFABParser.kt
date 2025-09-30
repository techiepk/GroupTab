import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.FABParser
import java.math.BigDecimal

data class FABTestCase(
    val name: String,
    val message: String,
    val sender: String = "FAB",
    val expected: ExpectedTransaction
)

data class ExpectedTransaction(
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val merchant: String? = null,
    val accountLast4: String? = null,
    val balance: BigDecimal? = null,
    val reference: String? = null
)

fun main() {
    val parser = FABParser()

    println("=" * 80)
    println("First Abu Dhabi Bank (FAB) Parser Test Suite")
    println("=" * 80)
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println("Can Handle 'FAB': ${parser.canHandle("FAB")}")
    println()

    val testCases = listOf(
        FABTestCase(
            name = "Credit Card Purchase",
            message = """
                Credit Card Purchase
                Debit
                Account XXXX0002
                Card XXXX1234
                AED 8.00
                TR              DUBAI           ARE
                23/09/25 16:17
                Available Balance AED 4530.16
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("8.00"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                merchant = "TR              DUBAI           ARE",
                accountLast4 = "0002",
                balance = BigDecimal("4530.16")
            )
        ),

        FABTestCase(
            name = "Inward Remittance",
            message = """
                Inward Remittance
                Credit
                Account XXXX5678
                AED 444.00
                Value Date 18/09/2025
                Available Balance AED 5444.00
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("444.00"),
                currency = "AED",
                type = TransactionType.INCOME,
                accountLast4 = "5678",
                balance = BigDecimal("5444.00")
            )
        ),

        FABTestCase(
            name = "Payment Instructions",
            message = "Dear Customer, Your payment instructions of AED 250.00 to 5xxx**1xxx has been processed on 10/09/2025 15:28",
            expected = ExpectedTransaction(
                amount = BigDecimal("250.00"),
                currency = "AED",
                type = TransactionType.EXPENSE
                // Note: Parser doesn't extract reference from this message format
            )
        ),

        FABTestCase(
            name = "Debit Card Purchase (THB)",
            message = """
                Debit Card Purchase
                Debit
                Account XXXX9876
                Card XXXX2865
                THB 1500.50
                WWW.GRAB.COM          BANGKOK         TH
                26/07/25 17:55
                Available Balance AED 8500.25
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("1500.50"),
                currency = "THB",
                type = TransactionType.EXPENSE,
                merchant = "WWW.GRAB.COM          BANGKOK         TH",
                accountLast4 = "9876",
                balance = BigDecimal("8500.25")
            )
        ),

        FABTestCase(
            name = "ATM Cash Withdrawal (THB)",
            message = """
                ATM Cash withdrawal
                Debit
                Account XXXX4321
                Card XXXX8765
                THB 5000.00
                18/06/25 13:06
                Available Balance AED 15000.00
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("5000.00"),
                currency = "THB",
                type = TransactionType.EXPENSE,
                merchant = "ATM Withdrawal",
                accountLast4 = "4321",
                balance = BigDecimal("15000.00")
            )
        ),

        FABTestCase(
            name = "Grab Payment (THB)",
            message = """
                Debit Card Purchase 
                Debit 
                Account XXXX0002 
                Card XXXX2865
                THB 283.00
                WWW.GRAB.COM          BANGKOK         TH 
                26/06/25 11:51 
                Available Balance AED 9999.00
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("283.00"),
                currency = "THB",
                type = TransactionType.EXPENSE,
                merchant = "WWW.GRAB.COM          BANGKOK         TH",
                accountLast4 = "0002",
                balance = BigDecimal("9999.00")
            )
        ),

        FABTestCase(
            name = "Outward Remittance",
            message = """
                Outward Remittance
                Debit
                Account XXXX0002
                AED 150.00
                Value Date 10/07/24
                Available Balance AED 6337.92
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("150.00"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                accountLast4 = "0002",
                balance = BigDecimal("6337.92")
            )
        ),

        FABTestCase(
            name = "USD Payment",
            message = """
                Debit Card Purchase
                Debit
                Account XXXX0002
                Card XXXX9879
                USD 20.00
                CLAUDE.AI SUBSCRIPTION+14152360599 CA US
                10/07/24 20:07
                Available Balance AED 5978.78
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("20.00"),
                currency = "USD",
                type = TransactionType.EXPENSE,
                merchant = "CLAUDE.AI SUBSCRIPTION+14152360599 CA US",
                accountLast4 = "0002",
                balance = BigDecimal("5978.78")
            )
        ),


        FABTestCase(
            name = "Grab Payment in Thailand (THB)",
            message = """
                Debit Card Purchase 
                Debit 
                Account XXXX1234
                Card XXXX5678
                THB 636.00
                WWW.GRAB.COM          BANGKOK         TH 
                26/07/25 17:55 
                Available Balance AED 8888.30
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("636.00"),
                currency = "THB",
                type = TransactionType.EXPENSE,
                merchant = "WWW.GRAB.COM          BANGKOK         TH",
                accountLast4 = "1234",
                balance = BigDecimal("8888.30")
            )
        ),

        FABTestCase(
            name = "Outward Remittance without Merchant",
            message = """
                Outward Remittance 
                Debit 
                Account XXXX9876
                AED 500.00
                Value Date 20/06/2025  
                Available Balance AED 12345.67
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("500.00"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                accountLast4 = "9876",
                balance = BigDecimal("12345.67")
            )
        ),

        FABTestCase(
            name = "Cash Deposit (Income)",
            message = """
                Cash Deposit 
                Credit 
                Account XXXX4321
                AED 10000.00
                Date 20/06/25
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("10000.00"),
                currency = "AED",
                type = TransactionType.INCOME,
                accountLast4 = "4321"
            )
        ),

        FABTestCase(
            name = "ATM Withdrawal in Thailand (THB)",
            message = """
                ATM Cash withdrawal 
                Debit 
                Account XXXX8888
                Card XXXX9999
                THB 3000.00
                18/06/25 13:06 
                Available Balance AED 5500.50
            """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("3000.00"),
                currency = "THB",
                type = TransactionType.EXPENSE,
                merchant = "ATM Withdrawal",
                accountLast4 = "8888",
                balance = BigDecimal("5500.50")
            )
        ),
        FABTestCase(
            name = "Cheque Credited",
            message = """
        Cheque Credited
        Cheque No 000020 for AED 9999.00 deposited in your account XXXX0002 has been credited on 01/10/2024 
        Your available balance is AED 7777.62.
    """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("9999.00"),
                currency = "AED",
                type = TransactionType.INCOME,
                merchant = "Cheque Credited",
                accountLast4 = "0002",
                balance = BigDecimal("7777.62")
            )
        ),

        FABTestCase(
            name = "Cheque Returned",
            message = """
        Cheque Returned
        Cheque No 000020 for AED 8888.00 deposited in your account XXXX0002  has been returned unpaid.
        Please contact the branch.
    """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("8888.00"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                merchant = "Cheque Returned",
                accountLast4 = "0002"
            )
        ),
        FABTestCase(
            name = "Unsuccessful Transaction Refund",
            message = """
        Dear customer, unsuccessful transaction of AED 42.13 has been credited to your account XXXX0002 Card XXXX2865 on 19/06/25 02:50 and your current available balance is AED 4444.13
    """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("42.13"),
                currency = "AED",
                type = TransactionType.INCOME,
                merchant = "Refund",
                accountLast4 = "0002",
                balance = BigDecimal("4444.13")
            )
        ),

        FABTestCase(
            name = "Funds Transfer with Comma Amount",
            message = """
        Dear Customer, your funds transfer request of  AED 3,555.00 to IBAN/Account/Card XXXX0001  has been processed successfully from your account/card XXXX0002 on 12/06/2025 16:43
    """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("3555.00"),
                currency = "AED",
                type = TransactionType.EXPENSE,
                merchant = "Transfer to XXXX0001",
                accountLast4 = "0001"
            )
        ),
        FABTestCase(
            name = "Generic Account Credit",
            message = """
        An amount of AED 555.00 has been credited to your FAB account XXXX0002 on 08/06/25 .Your Available Balance is AED 5555.43
    """.trimIndent(),
            expected = ExpectedTransaction(
                amount = BigDecimal("555.00"),
                currency = "AED",
                type = TransactionType.INCOME,
                merchant = "Account Credited",
                accountLast4 = "0002",
                balance = BigDecimal("5555.43")
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
}

private operator fun String.times(count: Int): String = this.repeat(count)