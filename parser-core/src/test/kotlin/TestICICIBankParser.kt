import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.bank.ICICIBankParser
import java.math.BigDecimal

fun main() {
    val parser = ICICIBankParser()

    println("=" * 80)
    println("ICICI Bank Parser Test - Future Debit Notification Fix")
    println("=" * 80)
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