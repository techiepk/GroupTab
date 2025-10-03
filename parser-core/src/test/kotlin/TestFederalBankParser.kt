import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.FederalBankParser
import java.math.BigDecimal

data class FederalBankTestCase(
    val name: String,
    val message: String,
    val sender: String = "AD-FEDBNK",
    val expected: ExpectedTransaction,
    val shouldParse: Boolean = true
)

data class MandateTestCase(
    val name: String,
    val message: String,
    val sender: String = "AX-FEDBNK-S",
    val expectedMandate: FederalBankParser.EMandateInfo?,
    val shouldBeParsedAsTransaction: Boolean = false
)

fun main() {
    val parser = FederalBankParser()

    println("=" * 80)
    println("Federal Bank Parser Test Suite - Full Coverage")
    println("=" * 80)
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println("Can Handle 'AD-FEDBNK': ${parser.canHandle("AD-FEDBNK")}")
    println("Can Handle 'JM-FEDBNK': ${parser.canHandle("JM-FEDBNK")}")
    println("Can Handle 'AX-FEDBNK-S': ${parser.canHandle("AX-FEDBNK-S")}")
    println()

    // Test regular transactions
    println("=== REGULAR TRANSACTIONS ===")
    testRegularTransactions(parser)
    println()

    // Test mandate API
    println("=== MANDATE API TESTS ===")
    testMandateAPI(parser)
    println()

    // Test rejected messages
    println("=== REJECTED MESSAGES ===")
    testRejectedMessages(parser)
    println()

    // Summary
    println("=" * 80)
    println("Test Summary")
    println("=" * 80)
}

fun testRegularTransactions(parser: FederalBankParser) {
    val transactionTestCases = listOf(
        // UPI Debit Transactions
        FederalBankTestCase(
            name = "UPI Debit to Individual VPA",
            message = "Rs 150.00 debited via UPI on 15-08-2024 10:30:25 to VPA john.doe123@okbank.Ref No 987654321098.Small txns?Use UPI Lite!-Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("150.00"),
                currency = "INR",
                type = TransactionType.EXPENSE,
                merchant = "john.doe123@okbank",
                reference = "987654321098"
            )
        ),

        FederalBankTestCase(
            name = "UPI Debit to Merchant VPA",
            message = "Rs 450.75 debited via UPI on 16-08-2024 14:22:10 to VPA swiggy.food@paytm.Ref No 876543210987.Small txns?Use UPI Lite!-Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("450.75"),
                currency = "INR",
                type = TransactionType.EXPENSE,
                merchant = "Swiggy",
                reference = "876543210987"
            )
        ),

        FederalBankTestCase(
            name = "UPI Payment to Indigo via Paytm",
            message = "Rs 3500.00 debited via UPI on 20-08-2024 12:30:45 to VPA indigo.paytm@hdfcbank.Ref No 987654321099.Small txns?Use UPI Lite!-Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("3500.00"),
                currency = "INR",
                type = TransactionType.EXPENSE,
                merchant = "Indigo",
                reference = "987654321099"
            )
        ),

        FederalBankTestCase(
            name = "UPI Debit with Complex VPA",
            message = "Rs 1250.00 debited via UPI on 17-08-2024 09:15:45 to VPA merchant.store.98765@hdfcbank.Ref No 765432109876.Small txns?Use UPI Lite!-Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("1250.00"),
                currency = "INR",
                type = TransactionType.EXPENSE,
                merchant = "merchant.store.98765@hdfcbank",
                reference = "765432109876"
            )
        ),

        // IMPS Credit Transactions
        FederalBankTestCase(
            name = "IMPS Credit to Account",
            message = "Rs 3500.50 credited to your A/c XX4567 via IMPS on 18AUG2024 11:45:30 IMPS Ref no 654321098765 Bal:Rs 25000.75 -Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("3500.50"),
                currency = "INR",
                type = TransactionType.INCOME,
                merchant = "IMPS Credit",
                accountLast4 = "4567",
                balance = BigDecimal("25000.75"),
                reference = "654321098765"
            )
        ),

        // "You've received" Credit Transactions
        FederalBankTestCase(
            name = "You've Received from Individual",
            message = "John, you've received INR 10,509.09 in your Account XXXXXXXX1896. Woohoo! It was sent by TESTUSER on March 19, 2025. -Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("10509.09"),
                currency = "INR",
                type = TransactionType.INCOME,
                merchant = "TESTUSER",
                accountLast4 = "1896"
            )
        ),

        FederalBankTestCase(
            name = "You've Received from Person",
            message = "Jane, you've received INR 50,000.00 in your Account XXXXXXXX1896. Woohoo! It was sent by SAMPLE PERSON on July 24, 2024. -Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("50000.00"),
                currency = "INR",
                type = TransactionType.INCOME,
                merchant = "SAMPLE PERSON",
                accountLast4 = "1896"
            )
        ),

        FederalBankTestCase(
            name = "You've Received from 0000 - Bank Transfer",
            message = "John, you've received INR 17,179.95 in your Account XXXXXXXX1896. Woohoo! It was sent by 0000 on July 25, 2024. -Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("17179.95"),
                currency = "INR",
                type = TransactionType.INCOME,
                merchant = "Bank Transfer",
                accountLast4 = "1896"
            )
        ),

        FederalBankTestCase(
            name = "IMPS Credit Large Amount",
            message = "Rs 15000.00 credited to your A/c XX7890 via IMPS on 19AUG2024 16:20:15 IMPS Ref no 543210987654 Bal:Rs 42500.80 -Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("15000.00"),
                currency = "INR",
                type = TransactionType.INCOME,
                merchant = "IMPS Credit",
                accountLast4 = "7890",
                balance = BigDecimal("42500.80"),
                reference = "543210987654"
            )
        ),

        // Successful E-mandate Payments
        FederalBankTestCase(
            name = "Successful E-mandate Payment - Netflix",
            message = "Hi, payment of INR 199.00 for Netflix via e-mandate ID: NX789XYZABC on Federal Bank Debit Card 3456 is processed successfully. To manage, visit: https://www.sihub.in/managesi/federal T&CA - Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("199.00"),
                currency = "INR",
                type = TransactionType.EXPENSE,
                merchant = "Netflix via e-mandate ID: NX789XYZABC",
                accountLast4 = "3456"
            )
        ),

        FederalBankTestCase(
            name = "Successful E-mandate Payment - Spotify",
            message = "Hi, payment of INR 119.00 for Spotify via e-mandate ID: SP456DEF123 on Federal Bank Debit Card 7890 is processed successfully. To manage, visit: https://www.sihub.in/managesi/federal T&CA - Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("119.00"),
                currency = "INR",
                type = TransactionType.EXPENSE,
                merchant = "Spotify via e-mandate ID: SP456DEF123",
                accountLast4 = "7890"
            )
        ),

        FederalBankTestCase(
            name = "Successful E-mandate Payment - Insurance",
            message = "Hi, payment of INR 2500.00 for LifeInsurance via e-mandate ID: LI789GHI456 on Federal Bank Debit Card 1234 is processed successfully. To manage, visit: https://www.sihub.in/managesi/federal T&CA - Federal Bank",
            expected = ExpectedTransaction(
                amount = BigDecimal("2500.00"),
                currency = "INR",
                type = TransactionType.EXPENSE,
                merchant = "LifeInsurance via e-mandate ID: LI789GHI456",
                accountLast4 = "1234"
            )
        )
    )

    var passed = 0
    var failed = 0

    transactionTestCases.forEachIndexed { index, testCase ->
        println("Test ${index + 1}: ${testCase.name}")
        println("-" * 60)

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
                if (expected.accountLast4 != null && result.accountLast4 != expected.accountLast4) {
                    failures.add("Account: expected '${expected.accountLast4}', got '${result.accountLast4}'")
                }
                if (expected.balance != null && result.balance != expected.balance) {
                    failures.add("Balance: expected ${expected.balance}, got ${result.balance}")
                }
                if (expected.reference != null && result.reference != expected.reference) {
                    failures.add("Reference: expected '${expected.reference}', got '${result.reference}'")
                }

                if (failures.isEmpty()) {
                    println("✓ PASSED")
                    println("  Amount: ${result.amount} ${result.currency}")
                    println("  Type: ${result.type}")
                    println("  Merchant: ${result.merchant}")
                    if (result.accountLast4 != null) println("  Account: ****${result.accountLast4}")
                    if (result.balance != null) println("  Balance: ${result.balance}")
                    if (result.reference != null) println("  Reference: ${result.reference}")
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

    println("Transaction Tests: $passed passed, $failed failed")
}

fun testMandateAPI(parser: FederalBankParser) {
    // Test mandate creation notifications (should parse via API but not as transactions)
    val mandateCreationTestCases = listOf(
        MandateTestCase(
            name = "Mandate Creation - Netflix",
            message = "Dear Customer, You have successfully created a mandate on Netflix India for a MONTHLY frequency starting from 05-09-2024 for a maximum amount of Rs 199.00 Mandate Ref No- abc123def456@fifederal - Federal Bank",
            expectedMandate = FederalBankParser.EMandateInfo(
                amount = BigDecimal("199.00"),
                nextDeductionDate = "05-09-2024",
                merchant = "Netflix India",
                umn = "abc123def456@fifederal"
            )
        ),

        MandateTestCase(
            name = "Mandate Creation - Spotify Premium",
            message = "Dear Customer, You have successfully created a mandate on Spotify Premium for a QUARTERLY frequency starting from 10-09-2024 for a maximum amount of Rs 359.00 Mandate Ref No- spot789xyz012@fifederal - Federal Bank",
            expectedMandate = FederalBankParser.EMandateInfo(
                amount = BigDecimal("359.00"),
                nextDeductionDate = "10-09-2024",
                merchant = "Spotify Premium",
                umn = "spot789xyz012@fifederal"
            )
        ),

        MandateTestCase(
            name = "Mandate Creation - Insurance Premium",
            message = "Dear Customer, You have successfully created a mandate on Life Insurance Corp for a YEARLY frequency starting from 01-09-2024 for a maximum amount of Rs 12000.00 Mandate Ref No- lic456mno789@fifederal - Federal Bank",
            expectedMandate = FederalBankParser.EMandateInfo(
                amount = BigDecimal("12000.00"),
                nextDeductionDate = "01-09-2024",
                merchant = "Life Insurance Corp",
                umn = "lic456mno789@fifederal"
            )
        )
    )

    // Test payment due notifications (should parse via API but not as transactions)
    val paymentDueTestCases = listOf(
        MandateTestCase(
            name = "Payment Due - Netflix",
            message = "Hi, payment due for Netflix,INR 199.00 on 05/09/2024 will be processed using Federal Bank Debit Card 3456. To cancel, visit https://www.sihub.in/managesi/federal T&CA - Federal Bank",
            expectedMandate = FederalBankParser.EMandateInfo(
                amount = BigDecimal("199.00"),
                nextDeductionDate = "05/09/24",
                merchant = "Netflix",
                umn = null
            )
        ),

        MandateTestCase(
            name = "Payment Due - Spotify",
            message = "Hi, payment due for Spotify Premium,INR 359.00 on 10/09/2024 will be processed using Federal Bank Debit Card 7890. To cancel, visit https://www.sihub.in/managesi/federal T&CA - Federal Bank",
            expectedMandate = FederalBankParser.EMandateInfo(
                amount = BigDecimal("359.00"),
                nextDeductionDate = "10/09/24",
                merchant = "Spotify Premium",
                umn = null
            )
        ),

        MandateTestCase(
            name = "Payment Due - Insurance",
            message = "Hi, payment due for Life Insurance Corp,INR 12000.00 on 01/09/2024 will be processed using Federal Bank Debit Card 1234. To cancel, visit https://www.sihub.in/managesi/federal T&CA - Federal Bank",
            expectedMandate = FederalBankParser.EMandateInfo(
                amount = BigDecimal("12000.00"),
                nextDeductionDate = "01/09/24",
                merchant = "Life Insurance Corp",
                umn = null
            )
        )
    )

    // Test mandate creation API
    println("--- Mandate Creation API ---")
    var mandateCreationPassed = 0
    var mandateCreationFailed = 0

    mandateCreationTestCases.forEachIndexed { index, testCase ->
        println("Mandate Creation Test ${index + 1}: ${testCase.name}")
        println("-" * 40)

        // Should not parse as regular transaction
        val transactionResult = parser.parse(testCase.message, testCase.sender, System.currentTimeMillis())
        if (transactionResult != null) {
            println("✗ FAILED: Should not parse mandate creation as regular transaction")
            mandateCreationFailed++
        } else {
            println("✓ Correctly rejected as regular transaction")
        }

        // Should parse via mandate API
        val mandateResult = parser.parseEMandateSubscription(testCase.message)
        if (mandateResult == null) {
            println("✗ FAILED: parseEMandateSubscription returned null")
            mandateCreationFailed++
        } else {
            val failures = mutableListOf<String>()

            if (mandateResult.amount != testCase.expectedMandate?.amount) {
                failures.add("Amount: expected ${testCase.expectedMandate?.amount}, got ${mandateResult.amount}")
            }
            if (mandateResult.nextDeductionDate != testCase.expectedMandate?.nextDeductionDate) {
                failures.add("Date: expected '${testCase.expectedMandate?.nextDeductionDate}', got '${mandateResult.nextDeductionDate}'")
            }
            if (mandateResult.merchant != testCase.expectedMandate?.merchant) {
                failures.add("Merchant: expected '${testCase.expectedMandate?.merchant}', got '${mandateResult.merchant}'")
            }
            if (mandateResult.umn != testCase.expectedMandate?.umn) {
                failures.add("UMN: expected '${testCase.expectedMandate?.umn}', got '${mandateResult.umn}'")
            }

            if (failures.isEmpty()) {
                println("✓ PASSED: parseEMandateSubscription")
                println("  Amount: ${mandateResult.amount}")
                println("  Date: ${mandateResult.nextDeductionDate}")
                println("  Merchant: ${mandateResult.merchant}")
                println("  UMN: ${mandateResult.umn}")
                mandateCreationPassed++
            } else {
                println("✗ FAILED: parseEMandateSubscription")
                failures.forEach { println("  - $it") }
                mandateCreationFailed++
            }
        }
        println()
    }

    // Test payment due API
    println("--- Payment Due API ---")
    var paymentDuePassed = 0
    var paymentDueFailed = 0

    paymentDueTestCases.forEachIndexed { index, testCase ->
        println("Payment Due Test ${index + 1}: ${testCase.name}")
        println("-" * 40)

        // Should not parse as regular transaction
        val transactionResult = parser.parse(testCase.message, testCase.sender, System.currentTimeMillis())
        if (transactionResult != null) {
            println("✗ FAILED: Should not parse payment due as regular transaction")
            paymentDueFailed++
        } else {
            println("✓ Correctly rejected as regular transaction")
        }

        // Should parse via future debit API
        val futureDebitResult = parser.parseFutureDebit(testCase.message)
        if (futureDebitResult == null) {
            println("✗ FAILED: parseFutureDebit returned null")
            paymentDueFailed++
        } else {
            val failures = mutableListOf<String>()

            if (futureDebitResult.amount != testCase.expectedMandate?.amount) {
                failures.add("Amount: expected ${testCase.expectedMandate?.amount}, got ${futureDebitResult.amount}")
            }
            if (futureDebitResult.nextDeductionDate != testCase.expectedMandate?.nextDeductionDate) {
                failures.add("Date: expected '${testCase.expectedMandate?.nextDeductionDate}', got '${futureDebitResult.nextDeductionDate}'")
            }
            if (futureDebitResult.merchant != testCase.expectedMandate?.merchant) {
                failures.add("Merchant: expected '${testCase.expectedMandate?.merchant}', got '${futureDebitResult.merchant}'")
            }
            if (futureDebitResult.umn != testCase.expectedMandate?.umn) {
                failures.add("UMN: expected '${testCase.expectedMandate?.umn}', got '${futureDebitResult.umn}'")
            }

            if (failures.isEmpty()) {
                println("✓ PASSED: parseFutureDebit")
                println("  Amount: ${futureDebitResult.amount}")
                println("  Date: ${futureDebitResult.nextDeductionDate}")
                println("  Merchant: ${futureDebitResult.merchant}")
                paymentDuePassed++
            } else {
                println("✗ FAILED: parseFutureDebit")
                failures.forEach { println("  - $it") }
                paymentDueFailed++
            }
        }
        println()
    }

    println("Mandate API Tests: Creation ($mandateCreationPassed/$(${mandateCreationTestCases.size}), Payment Due ($paymentDuePassed/${paymentDueTestCases.size})")
}

fun testRejectedMessages(parser: FederalBankParser) {
    val rejectedTestCases = listOf(
        // Failed transactions
        "Hi, txn of Rs. 1500.00 using card XX**3456 failed due to insufficient funds. Current Bal: Rs.250.75. Call 18004251199 if txn not initiated by you -Federal Bank",

        // Declined e-mandate payments
        "Hi, payment of INR 199.00 via e-mandate declined for ID: NX789XYZABC on Federal Bank Debit Card 3456. To manage, visit: https://www.sihub.in/managesi/federal T&CA - Federal Bank",

        // Account notifications
        "Hi, your Federal Bank Savings Account is currently debit frozen due to incomplete Video KYC. Please complete the VKYC by 30/09/2024 to avoid account closure.",
        "Hi, your Federal Bank Savings Account on Fi will be closed if Video KYC is not completed before 31-10-2024. Complete it now: Visit the Fi App - Federal Bank",

        // OTP messages
        "Dear Customer, your FedMobile registration has been initiated. If not initiated by you, please call 18004201199. Please do not share your card details/OTP/CVV to anyone -Federal Bank",

        // General notifications
        "Hi, your Federal Bank Debit Card XX**3456 is being used for an online transaction. If not you, please block your card immediately."
    )

    var correctlyRejected = 0
    var incorrectlyParsed = 0

    rejectedTestCases.forEachIndexed { index, message ->
        println("Rejection Test ${index + 1}:")
        println("-" * 30)
        println("Message: ${message.take(80)}...")

        val result = parser.parse(message, "AD-FEDBNK", System.currentTimeMillis())
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

    println("Rejection Tests: $correctlyRejected correctly rejected, $incorrectlyParsed incorrectly parsed")
}

private operator fun String.times(count: Int): String = this.repeat(count)