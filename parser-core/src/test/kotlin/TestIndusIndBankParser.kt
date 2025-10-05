import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.IndusIndBankParser
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import com.pennywiseai.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class IndusIndBankParserTest {

    @Test
    fun `indusind parser basic flows`() {
        val parser = IndusIndBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "IndusInd Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Debit with merchant and balance",
                message = "Rs. 1,234.00 debited from A/c XX1234 at ZOMATO Ref 998877. Avl Bal: Rs 10,000.00",
                sender = "VM-INDUSB-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1234.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "ZOMATO",
                    accountLast4 = "1234",
                    balance = BigDecimal("10000.00"),
                    reference = "998877"
                )
            ),
            ParserTestCase(
                name = "UPI debit with RRN and VPA",
                message = "A/c *XX1234 debited by Rs 1234.00 towards xxxx.yyyy@icici. RRN: 510048508040. Not You? call 18602677777- IndusInd Bank.",
                sender = "AD-INDUSIND-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1234.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "xxxx.yyyy",
                    accountLast4 = "1234",
                    reference = "510048508040"
                )
            ),
            ParserTestCase(
                name = "UPI credit with RRN and VPA",
                message = "A/C *XX1234 credited by Rs 25000.00 from xxxx.yyyy@ybl. RRN:510048508040. Avl Bal:105502.12. Not you? Call 18602677777 - IndusInd bank.",
                sender = "AD-INDUSIND-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("25000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "xxxx.yyyy",
                    accountLast4 = "1234",
                    reference = "510048508040",
                    balance = BigDecimal("105502.12")
                )
            ),
            ParserTestCase(
                name = "Deposit interest - should not parse",
                message = "Net interest INR 248.07 paid on your IndusInd Deposit No 300***123456 on 17/09/25. Call 18602677777 for assistance - IndusInd Bank",
                sender = "AD-INDUSIND-S",
                shouldParse = false
            ),
            ParserTestCase(
                name = "IMPS debit - should parse",
                message = "Your IndusInd Account 20XXXXX1234 has been debited for INR 6440 towards IMPS/12345678901. Call 18602677777 to report issue-IndusInd Bank.",
                sender = "AD-INDUSIND-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("6440"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "IMPS",
                    accountLast4 = "1234"
                )
            ),
            ParserTestCase(
                name = "Balance-only - should not parse",
                message = "Your A/C 2134***12345 has Avl BAL of INR 1,234.56 as on 05/10/25 04:10 AM. Download IndusMobile from PlayStore - IndusInd Bank",
                sender = "AD-INDUSIND-S",
                shouldParse = false
            ),
            ParserTestCase(
                name = "ACH debit with Ref and trailing Bal",
                message = "IndusInd A/C  Debited; INR 4,500.00 Ref-ACH DR INW PAY/0000WD2CEFDT2Z58B2202320321456/Grow.Bal INR 141,999.93.Dispute-Call 18602677777-IndusInd Bank.",
                sender = "AD-INDUSIND-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4500.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "Grow",
                    balance = BigDecimal("141999.93"),
                    isFromCard = false,
                    accountLast4 = null
                )
            )
        )

        val handleChecks = listOf(
            "AD-INDUSB-S" to true,
            "VM-INDUSIND-S" to true,
            "INDUSB" to true,
            "INDUSIND" to true,
            "AX-HDFC-S" to false
        )

        val result = ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "IndusInd Bank Parser"
        )

        ParserTestUtils.printTestSummary(
            totalTests = result.totalTests,
            passedTests = result.passedTests,
            failedTests = result.failedTests,
            failureDetails = result.failureDetails
        )
    }

    @Test
    fun `factory resolves indusind`() {
        val cases = listOf(
            SimpleTestCase(
                bankName = "IndusInd Bank",
                sender = "AD-INDUSB-S",
                currency = "INR",
                message = "Rs. 250.00 debited from A/c XX9876 at SWIGGY Ref TXN123",
                expected = ExpectedTransaction(
                    amount = BigDecimal("250.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE
                ),
                shouldHandle = true
            )
        )

        val suite = ParserTestUtils.runFactoryTestSuite(cases, "Factory smoke tests - IndusInd")
        ParserTestUtils.printTestSummary(
            totalTests = suite.totalTests,
            passedTests = suite.passedTests,
            failedTests = suite.failedTests,
            failureDetails = suite.failureDetails
        )
    }

}
