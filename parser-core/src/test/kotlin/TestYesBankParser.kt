import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.YesBankParser
import java.math.BigDecimal

data class YesBankTestCase(
    val name: String,
    val message: String,
    val sender: String = "CP-YESBNK-S",
    val expected: ExpectedYesBankTransaction,
    val shouldParse: Boolean = true
)

data class ExpectedYesBankTransaction(
    val amount: BigDecimal,
    val currency: String = "INR",
    val type: TransactionType,
    val merchant: String? = null,
    val cardLast4: String? = null,
    val availableLimit: BigDecimal? = null,
    val reference: String? = null
)

fun main() {
    val parser = YesBankParser()

    println("=" * 80)
    println("Yes Bank Parser Test Suite")
    println("=" * 80)
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println("Can Handle 'CP-YESBNK-S': ${parser.canHandle("CP-YESBNK-S")}")
    println("Can Handle 'VM-YESBNK-S': ${parser.canHandle("VM-YESBNK-S")}")
    println("Can Handle 'JX-YESBNK-S': ${parser.canHandle("JX-YESBNK-S")}")
    println()

    val testCases = listOf(
        // Test Case 1: Fuel Station Transaction
        YesBankTestCase(
            name = "C N S FUEL PORT Transaction",
            message = "INR 404.36 spent on YES BANK Card X3349 @UPI_C N S FUEL PORT 24-08-2025 06:17:25 pm. Avl Lmt INR 211,476.24. SMS BLKCC 3349 to 9840909000 if not you",
            sender = "CP-YESBNK-S",
            expected = ExpectedYesBankTransaction(
                amount = BigDecimal("404.36"),
                type = TransactionType.CREDIT,
                merchant = "C N S FUEL PORT",
                cardLast4 = "3349",
                availableLimit = BigDecimal("211476.24")
            )
        ),

        // Test Case 2: S B ENTERPRISES Transaction
        YesBankTestCase(
            name = "S B ENTERPRISES Transaction",
            message = "INR 56.00 spent on YES BANK Card X3349 @UPI_S B ENTERPRISES 24-08-2025 06:03:40 am. Avl Lmt INR 211,880.60. SMS BLKCC 3349 to 9840909000 if not you",
            sender = "VM-YESBNK-S",
            expected = ExpectedYesBankTransaction(
                amount = BigDecimal("56.00"),
                type = TransactionType.CREDIT,
                merchant = "S B ENTERPRISES",
                cardLast4 = "3349",
                availableLimit = BigDecimal("211880.60")
            )
        ),

        // Test Case 3: Individual Name (MOHAMMED AKRAM)
        YesBankTestCase(
            name = "MOHAMMED AKRAM Transaction",
            message = "INR 24.00 spent on YES BANK Card X3349 @UPI_MOHAMMED AKRAM 23-08-2025 11:51:19 am. Avl Lmt INR 212,012.60. SMS BLKCC 3349 to 9840909000 if not you",
            sender = "JX-YESBNK-S",
            expected = ExpectedYesBankTransaction(
                amount = BigDecimal("24.00"),
                type = TransactionType.CREDIT,
                merchant = "MOHAMMED AKRAM",
                cardLast4 = "3349",
                availableLimit = BigDecimal("212012.60")
            )
        ),

        // Test Case 4: Healthcare Transaction (Truncated Name)
        YesBankTestCase(
            name = "SURAKSHAA HEALTHCA Transaction",
            message = "INR 250.00 spent on YES BANK Card X3349 @UPI_SURAKSHAA HEALTHCA 23-08-2025 10:02:59 am. Avl Lmt INR 212,036.60. SMS BLKCC 3349 to 9840909000 if not you",
            sender = "CP-YESBNK-S",
            expected = ExpectedYesBankTransaction(
                amount = BigDecimal("250.00"),
                type = TransactionType.CREDIT,
                merchant = "SURAKSHAA HEALTHCA",
                cardLast4 = "3349",
                availableLimit = BigDecimal("212036.60")
            )
        )
    )

    var passed = 0
    var failed = 0

    println("=== TRANSACTION TESTS ===")
    println()

    testCases.forEachIndexed { index, testCase ->
        println("Test ${index + 1}: ${testCase.name}")
        println("-" * 60)
        println("Sender: ${testCase.sender}")
        println("Message: ${testCase.message.take(80)}...")
        println()

        val result = parser.parse(testCase.message, testCase.sender, System.currentTimeMillis())
        val expected = testCase.expected

        if (result == null) {
            if (testCase.shouldParse) {
                println("✗ FAILED: Parser returned null but should have parsed")
                failed++
            } else {
                println("✓ PASSED: Correctly returned null")
                passed++
            }
        } else {
            if (!testCase.shouldParse) {
                println("✗ FAILED: Parser returned result but should have returned null")
                failed++
            } else {
                val failures = mutableListOf<String>()

                // Validate all fields
                if (result.amount != expected.amount) {
                    failures.add("Amount: expected ${expected.amount}, got ${result.amount}")
                }
                if (result.currency != expected.currency) {
                    failures.add("Currency: expected ${expected.currency}, got ${result.currency}")
                }
                if (result.type != expected.type) {
                    failures.add("Type: expected ${expected.type}, got ${result.type}")
                }
                if (expected.merchant != null && result.merchant != expected.merchant) {
                    failures.add("Merchant: expected '${expected.merchant}', got '${result.merchant}'")
                }
                if (expected.cardLast4 != null && result.accountLast4 != expected.cardLast4) {
                    failures.add("Card Last 4: expected '${expected.cardLast4}', got '${result.accountLast4}'")
                }
                if (expected.availableLimit != null && result.creditLimit != expected.availableLimit) {
                    failures.add("Available Limit: expected ${expected.availableLimit}, got ${result.creditLimit}")
                }

                if (failures.isEmpty()) {
                    println("✓ PASSED")
                    println("  Amount: ${result.amount} ${result.currency}")
                    println("  Type: ${result.type}")
                    println("  Merchant: ${result.merchant}")
                    println("  Card Last 4: ${result.accountLast4}")
                    println("  Available Limit: ${result.creditLimit}")
                    println("  Is Card Transaction: ${result.isFromCard}")
                    passed++
                } else {
                    println("✗ FAILED:")
                    failures.forEach { println("  - $it") }
                    failed++
                }
            }
        }
        println()
    }

    // Test rejection cases
    println("=== REJECTION TESTS ===")
    println()

    val rejectionTests = listOf(
        "Dear Customer, your OTP for login is 123456. Do not share with anyone. -Yes Bank",
        "Get exciting offers on Yes Bank Credit Cards. Apply now! Visit yesbank.in",
        "Payment request of INR 500.00 from merchant@upi. Ignore if already paid.",
        "Your Yes Bank Credit Card payment of INR 10,000 is due by 25-08-2025"
    )

    var correctlyRejected = 0
    var incorrectlyParsed = 0

    rejectionTests.forEachIndexed { index, message ->
        println("Rejection Test ${index + 1}:")
        println("-" * 30)
        println("Message: ${message.take(80)}...")

        val result = parser.parse(message, "CP-YESBNK-S", System.currentTimeMillis())
        if (result == null) {
            println("✓ PASSED: Correctly rejected")
            correctlyRejected++
        } else {
            println("✗ FAILED: Should have been rejected but got:")
            println("  Amount: ${result.amount} ${result.currency}")
            println("  Type: ${result.type}")
            println("  Merchant: ${result.merchant}")
            incorrectlyParsed++
        }
        println()
    }

    println("=" * 80)
    println("TEST SUMMARY")
    println("=" * 80)
    println("Transaction Tests: $passed passed, $failed failed")
    println("Rejection Tests: $correctlyRejected correctly rejected, $incorrectlyParsed incorrectly parsed")
    println("Total: ${passed + correctlyRejected} passed, ${failed + incorrectlyParsed} failed")
}

private operator fun String.times(count: Int): String = this.repeat(count)