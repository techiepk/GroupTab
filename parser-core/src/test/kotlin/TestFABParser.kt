import com.pennywiseai.parser.core.bank.FABParser

data class FABTestCase(
    val name: String,
    val message: String,
    val sender: String
)

fun main() {
    val parser = FABParser()

    println("=== First Abu Dhabi Bank Parser Tests ===")
    println("Bank Name: ${parser.getBankName()}")
    println("Currency: ${parser.getCurrency()}")
    println()

    val testCases = listOf(
        FABTestCase(
            name = "Credit Card Purchase",
            sender = "FAB",
            message = """
                Credit Card Purchase
                Card No XXXX
                AED 8.00
                T*** R** DUBAI ARE
                23/09/25 16:17
                Available Balance AED **30.16
                Your September statement payment due date is 26/09/2025
                Pay school fees in 12 instalments at 0% interest with no fee. bit.ly/47nWGYG Conditions apply.
            """.trimIndent()
        ),
        FABTestCase(
            name = "Inward Remittance",
            sender = "FAB",
            message = """
                Inward Remittance
                Credit
                Account XXXX**
                AED 444.00
                Value Date 18/09/2025
                Available Balance AED ***0.00
            """.trimIndent()
        ),
        FABTestCase(
            name = "Payment Instructions",
            sender = "FAB",
            message = "Dear Customer, Your payment instructions of AED 250.00 to 5xxx**1xxx has been processed on 10/09/2025 15:28"
        ),
        FABTestCase(
            name = "Global Currency (THB) Transaction",
            sender = "FAB",
            message = """
                Debit Card Purchase
                Debit
                Account XXXX####
                Card XXXX####
                THB 1500.50
                WWW.GRAB.COM          BANGKOK         TH
                26/07/25 17:55
                Available Balance AED 8500.25
            """.trimIndent()
        ),
        FABTestCase(
            name = "ATM Cash Withdrawal (THB)",
            sender = "FAB",
            message = """
                ATM Cash withdrawal
                Debit
                Account XXXX####
                Card XXXX####
                THB 5000.00
                18/06/25 13:06
                Available Balance AED 15000.00
            """.trimIndent()
        ),
        FABTestCase(
            name = "Debit Card payment in thb",
            sender = "FAB",
            message =
"""
Debit Card Purchase 
Debit 
Account XXXX0002 
Card XXXX2865
THB 283.00
WWW.GRAB.COM          BANGKOK         TH 
26/06/25 11:51 
Available Balance AED 9999
"""
        )
    )

    testCases.forEachIndexed { index, testCase ->
        println("=== Test ${index + 1}: ${testCase.name} ===")

        if (index == 0) {
            println("Can handle sender '${testCase.sender}': ${parser.canHandle(testCase.sender)}")
        }

        val result = parser.parse(testCase.message, testCase.sender, System.currentTimeMillis())

        if (result != null) {
            println("✓ Parsed successfully!")
            println("  Amount: ${result.amount}")
            println("  Currency: ${result.currency}")
            println("  Type: ${result.type}")
            println("  Merchant: ${result.merchant}")
            result.accountLast4?.let { println("  Account: $it") }
            result.balance?.let { println("  Balance: $it") }
            result.reference?.let { println("  Reference: $it") }
        } else {
            println("✗ Failed to parse")
        }

        println()
    }
}