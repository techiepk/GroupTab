import com.pennywiseai.parser.core.bank.FederalBankParser
import java.math.BigDecimal

fun main() {
    val parser = FederalBankParser()

    // Test actual SMS messages from the XML file
    val testMessages = mapOf(
        "mandate_creation" to "Dear Customer, You have successfully created a mandate on Streaming Service for a MONTHLY frequency starting from 11-06-2025 for a maximum amount of Rs 299.00 Mandate Ref No- abc123def456@fifederal - Federal Bank",
        "payment_due" to "Hi, payment due for Streaming Service,INR 299.00 on 06/08/2024 will be processed using Federal Bank Debit Card ****. To cancel, visit https://www.sihub.in/managesi/federal T&CA -Federal Bank",
        "payment_successful" to "Hi, payment of INR 299.00 for Streaming Service via e-mandate ID: xyz789abc on Federal Bank Debit Card **** is processed successfully. To manage, visit: https://www.sihub.in/managesi/federal T&CA - Federal Bank"
    )

    println("=== Testing Federal Bank Mandate Parsing ===\n")

    // Test mandate creation
    println("1. Testing Mandate Creation Detection:")
    val mandateMessage = testMessages["mandate_creation"]!!
    val isMandateCreation = parser.isMandateCreationNotification(mandateMessage)
    println("   Message: $mandateMessage")
    println("   Is Mandate Creation: $isMandateCreation")

    if (isMandateCreation) {
        val mandateInfo = parser.parseEMandateSubscription(mandateMessage)
        if (mandateInfo != null) {
            println("   ✓ Parsed successfully:")
            println("     - Amount: ${mandateInfo.amount}")
            println("     - Merchant: ${mandateInfo.merchant}")
            println("     - Next Deduction: ${mandateInfo.nextDeductionDate}")
            println("     - UMN: ${mandateInfo.umn}")
            println("     - Date Format: ${mandateInfo.dateFormat}")
        } else {
            println("   ✗ Failed to parse mandate info")
        }
    }

    println("\n2. Testing Payment Due Detection:")
    val paymentDueMessage = testMessages["payment_due"]!!
    val futureDebitInfo = parser.parseFutureDebit(paymentDueMessage)
    println("   Message: $paymentDueMessage")

    if (futureDebitInfo != null) {
        println("   ✓ Parsed successfully:")
        println("     - Amount: ${futureDebitInfo.amount}")
        println("     - Merchant: ${futureDebitInfo.merchant}")
        println("     - Next Deduction: ${futureDebitInfo.nextDeductionDate}")
        println("     - UMN: ${futureDebitInfo.umn}")
        println("     - Date Format: ${futureDebitInfo.dateFormat}")
    } else {
        println("   ✗ Failed to parse future debit info")
    }

    println("\n3. Testing Successful Payment Detection:")
    val successMessage = testMessages["payment_successful"]!!
    val isDeclined = parser.isDeclinedMandatePayment(successMessage)
    val isTransaction = parser.isTransactionMessageForTesting(successMessage)
    println("   Message: $successMessage")
    println("   Is Declined: $isDeclined")
    println("   Is Transaction: $isTransaction")

    if (isTransaction) {
        val parsedTransaction = parser.parse(successMessage, "AD-FEDBNK", System.currentTimeMillis())
        println("   ✓ Parsed as transaction:")
        if (parsedTransaction != null) {
            println("     - Amount: ${parsedTransaction.amount}")
            println("     - Merchant: ${parsedTransaction.merchant}")
            println("     - Type: ${parsedTransaction.type}")
        } else {
            println("     ✗ Failed to parse transaction")
        }
    }

    println("\n=== Test Complete ===")
}