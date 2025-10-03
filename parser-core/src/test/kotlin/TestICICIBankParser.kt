import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.ICICIBankParser
import java.math.BigDecimal

fun main() {
    val parser = ICICIBankParser()

    println("=" * 80)
    println("ICICI Bank Parser Test - Multi-Currency & Future Debit Fix")
    println("=" * 80)
    println()

    // Test Multi-Currency Support
    println("=== MULTI-CURRENCY TESTS ===")
    println()

    println("Test 1: USD Transaction (JetBrains)")
    println("-" * 60)
    val usdMessage = "USD 11.80 spent using ICICI Bank Card XX7004 on 03-Sep-25 on 1xJetBrains AI . Avl Limit: INR 17,95,899.53. If not you, call 1800 2662/SMS BLOCK 7004 to 9215676766."
    val sender = "JM-ICICIT-S"

    println("Message: ${usdMessage.take(80)}...")
    println("Sender: $sender")
    println()

    val usdResult = parser.parse(usdMessage, sender, System.currentTimeMillis())

    if (usdResult != null) {
        if (usdResult.amount == BigDecimal("11.80") && usdResult.currency == "USD") {
            println("✓ PASSED: Correctly parsed USD transaction")
            println("  Amount: ${usdResult.amount} ${usdResult.currency}")
            println("  Type: ${usdResult.type}")
            println("  Merchant: ${usdResult.merchant}")
            println("  Available Limit: ${usdResult.creditLimit}")
        } else {
            println("✗ FAILED: Incorrect parsing")
            println("  Expected: 11.80 USD")
            println("  Got: ${usdResult.amount} ${usdResult.currency}")
        }
    } else {
        println("✗ FAILED: Parser returned null")
    }

    println()
    println("Test 2: EUR Transaction")
    println("-" * 60)
    val eurMessage = "EUR 50.00 spent using ICICI Bank Card XX1234 on 05-Sep-25 on Amazon DE. Avl Limit: INR 2,00,000.00. SMS BLOCK 1234 to 9215676766"

    println("Message: ${eurMessage.take(80)}...")
    println()

    val eurResult = parser.parse(eurMessage, sender, System.currentTimeMillis())

    if (eurResult != null) {
        if (eurResult.amount == BigDecimal("50.00") && eurResult.currency == "EUR") {
            println("✓ PASSED: Correctly parsed EUR transaction")
            println("  Amount: ${eurResult.amount} ${eurResult.currency}")
            println("  Type: ${eurResult.type}")
            println("  Merchant: ${eurResult.merchant}")
        } else {
            println("✗ FAILED: Incorrect parsing")
            println("  Expected: 50.00 EUR")
            println("  Got: ${eurResult.amount} ${eurResult.currency}")
        }
    } else {
        println("✗ FAILED: Parser returned null")
    }

    println()
    println("Test 3: Regular INR Transaction (Should still work)")
    println("-" * 60)
    val inrMessage = "INR 500.00 spent using ICICI Bank Card XX5678 on 06-Sep-25 on Swiggy. Avl Limit: INR 1,50,000.00."

    println("Message: ${inrMessage.take(80)}...")
    println()

    val inrResult = parser.parse(inrMessage, sender, System.currentTimeMillis())

    if (inrResult != null) {
        if (inrResult.amount == BigDecimal("500.00") && inrResult.currency == "INR") {
            println("✓ PASSED: INR transactions still work")
            println("  Amount: ${inrResult.amount} ${inrResult.currency}")
            println("  Type: ${inrResult.type}")
            println("  Merchant: ${inrResult.merchant}")
        } else {
            println("✗ FAILED: INR parsing broken")
            println("  Got: ${inrResult.amount} ${inrResult.currency}")
        }
    } else {
        println("✗ FAILED: Parser returned null")
    }

    println()
    println()

    // Test the specific case from user feedback
    println("Test 1: Future AutoPay Notification (Should be rejected)")
    println("-" * 60)
    val futureDebitMessage = "Your account will be debited with Rs 649.00 on 03-Oct-25 towards Netflix Entertainment Ser for AutoPay MERCHANTMANDATE, RRN 421723106963-ICICI Bank."
    val futureDebitSender = "AX-ICICIT-S"

    println("Message: $futureDebitMessage")
    println("Sender: $futureDebitSender")
    println()

    val result1 = parser.parse(futureDebitMessage, futureDebitSender, System.currentTimeMillis())

    if (result1 == null) {
        println("✓ PASSED: Correctly rejected future debit notification")
    } else {
        println("✗ FAILED: Should have rejected but got:")
        println("  Amount: ${result1.amount}")
        println("  Type: ${result1.type}")
        println("  Merchant: ${result1.merchant}")
    }

    println()
    println("Test 2: Actual AutoPay Transaction (Should be parsed)")
    println("-" * 60)
    val actualDebitMessage = "Your account has been debited with Rs 649.00 towards Netflix Entertainment Ser for AutoPay MERCHANTMANDATE. RRN 421723106963. Avl Bal Rs 10,000.00-ICICI Bank"

    println("Message: $actualDebitMessage")
    println("Sender: $futureDebitSender")
    println()

    val result2 = parser.parse(actualDebitMessage, futureDebitSender, System.currentTimeMillis())

    if (result2 != null) {
        println("✓ PASSED: Correctly parsed actual transaction")
        println("  Amount: ${result2.amount}")
        println("  Type: ${result2.type}")
        println("  Merchant: ${result2.merchant}")
        println("  Balance: ${result2.balance}")
    } else {
        println("✗ FAILED: Should have parsed the actual transaction")
    }

    println()
    println("Test 3: Other Future Debit Variations (All should be rejected)")
    println("-" * 60)

    val futureVariations = listOf(
        "Rs. 500.00 will be debited from your account on 05-Oct-25 for EMI payment",
        "Your ICICI Bank Account will be debited with Rs 1,000.00 on 10-Oct-25",
        "AutoPay: Rs 299.00 will be debited on 15-Oct-25 for Spotify subscription"
    )

    var rejectedCount = 0
    futureVariations.forEachIndexed { index, message ->
        println("Variation ${index + 1}: ${message.take(60)}...")
        val result = parser.parse(message, "AX-ICICIT-S", System.currentTimeMillis())
        if (result == null) {
            println("  ✓ Correctly rejected")
            rejectedCount++
        } else {
            println("  ✗ Should have been rejected")
        }
    }

    println()
    println("Summary: $rejectedCount/${futureVariations.size} future debit variations correctly rejected")

    println()
    println("Test 4: Regular Debit Messages (Should still work)")
    println("-" * 60)

    val regularDebits = listOf(
        "ICICI Bank Acct XX123 debited for Rs 500.00 on 01-Oct-25; merchant credited. UPI: 543210987654. Call 18002662 for dispute. Updated Bal: Rs 5,000.00",
        "Rs. 1,000.00 has been debited from your account XX456 for bill payment. Avl Bal: Rs 3,000.00",
        "Your account has been successfully debited with Rs 250.00. Reference: TXN123456789"
    )

    var parsedCount = 0
    regularDebits.forEachIndexed { index, message ->
        println("Message ${index + 1}: ${message.take(60)}...")
        val result = parser.parse(message, "AX-ICICIT-S", System.currentTimeMillis())
        if (result != null) {
            println("  ✓ Parsed: Amount=${result.amount}, Type=${result.type}")
            parsedCount++
        } else {
            println("  ✗ Failed to parse")
        }
    }

    println()
    println("Summary: $parsedCount/${regularDebits.size} regular debits correctly parsed")

    println()
    println("=" * 80)
    println("TEST COMPLETE")
    println("=" * 80)
}

private operator fun String.times(count: Int): String = this.repeat(count)