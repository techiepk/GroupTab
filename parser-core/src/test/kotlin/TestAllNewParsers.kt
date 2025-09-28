import com.pennywiseai.parser.core.bank.BankParserFactory

fun main() {
    println("=== Testing All New Session Parsers ===")
    println()

    // Test data for each new parser
    val testCases = listOf(
        // FAB Parser (UAE - AED)
        TestCase(
            "First Abu Dhabi Bank",
            "FAB",
            "AED",
            """Credit Card Purchase
Card No XXXX
AED 8.00
T*** R** DUBAI ARE
23/09/25 16:17
Available Balance AED **30.16"""
        ),

        // Citi Bank Parser (USA - USD)
        TestCase(
            "Citi Bank",
            "692484",
            "USD",
            "Citi Alert: A \$3.01 transaction was made at BP#1234E on card ending in 1234. View details at citi.com/citimobileapp"
        ),

        // Discover Card Parser (USA - USD)
        TestCase(
            "Discover Card",
            "347268",
            "USD",
            "Discover Card Alert: A transaction of \$25.00 at WWW.XXX.ORG on February 21, 2025. No Action needed. See it at https://app.discover.com/ACTVT. Text STOP to end"
        ),

        // Laxmi Bank Parser (Nepal - NPR)
        TestCase(
            "Laxmi Sunrise Bank",
            "LAXMI_ALERT",
            "NPR",
            "Dear Customer, Your #12344560 has been debited by NPR 720.00 on 05/09/25. Remarks:ESEWA LOAD/9763698550,127847587\n-Laxmi Sunrise"
        ),

        // CBE Bank Parser (Ethiopia - ETB)
        TestCase(
            "Commercial Bank of Ethiopia",
            "CBE",
            "ETB",
            "Dear [Name] your Account 1*********9388 has been Credited with ETB 3,000.00 from Be***, on 13/09/2025 at 12:37:24 with Ref No ********* Your Current Balance is ETB 3,104.87. Thank you for Banking with CBE!"
        ),

        // Everest Bank Parser (Nepal - NPR)
        TestCase(
            "Everest Bank",
            "9843368",
            "NPR",
            "Dear Customer, Your A/c 12345678 is debited by NPR 520.00 For: 9843368/Mobile Recharge,Ncell. Never Share Password/OTP With Anyone"
        ),

        // Old Hickory Parser (USA - USD)
        TestCase(
            "Old Hickory Credit Union",
            "(877) 590-7589",
            "USD",
            "A transaction for \$27.00 has posted to ACCOUNT NAME (part of ACCOUNT#), which is above the \$0.00 value you set."
        )
    )

    var successCount = 0
    var totalTests = 0

    for (testCase in testCases) {
        totalTests++
        println("--- Testing ${testCase.bankName} ---")

        val parser = BankParserFactory.getParser(testCase.sender)
        if (parser != null) {
            println("✓ Parser found: ${parser.getBankName()}")
            println("✓ Currency: ${parser.getCurrency()}")

            if (parser.getBankName() == testCase.bankName && parser.getCurrency() == testCase.currency) {
                println("✓ Bank name and currency match expected values")

                val result = parser.parse(testCase.message, testCase.sender, System.currentTimeMillis())
                if (result != null) {
                    println("✓ Message parsed successfully!")
                    println("  Amount: ${result.amount}")
                    println("  Currency: ${result.currency}")
                    println("  Type: ${result.type}")
                    println("  Merchant: ${result.merchant}")
                    successCount++
                } else {
                    println("✗ Failed to parse message")
                }
            } else {
                println("✗ Bank name or currency mismatch")
                println("  Expected: ${testCase.bankName} (${testCase.currency})")
                println("  Got: ${parser.getBankName()} (${parser.getCurrency()})")
            }
        } else {
            println("✗ No parser found for sender: ${testCase.sender}")
        }
        println()
    }

    println("=== Test Summary ===")
    println("Total tests: $totalTests")
    println("Successful: $successCount")
    println("Failed: ${totalTests - successCount}")
    println("Success rate: ${(successCount * 100 / totalTests)}%")

    if (successCount == totalTests) {
        println("🎉 All new parsers working correctly!")
    } else {
        println("⚠️ Some parsers need attention")
    }
}

data class TestCase(
    val bankName: String,
    val sender: String,
    val currency: String,
    val message: String
)