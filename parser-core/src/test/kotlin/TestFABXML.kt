package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.SMSData
import com.pennywiseai.parser.core.test.XMLTestUtils
import java.math.BigDecimal


fun main() {
    val parser = FABParser()

    println("=== Testing FAB canHandle ===")
    testCanHandle(parser)
    println()

    println("=== Testing FAB Parser with XML Data ===")
    testFABParserWithXMLData(parser)
}

fun testCanHandle(parser: FABParser) {
    val testCases = mapOf(
        "FAB" to true,
        "FABBANK" to true,
        "ADFAB" to true,
        "AD-FAB-A" to true,
        "HDFC" to false,
        "SBI" to false
    )

    testCases.forEach { (sender, expected) ->
        val result = parser.canHandle(sender)
        val status = if (result == expected) "✅" else "❌"
        println("$status canHandle('$sender'): $result (expected: $expected)")
    }
}

fun testFABParserWithXMLData(parser: FABParser) {
    val smsMessages = XMLTestUtils.loadSMSDataFromResource("fab_sms_test_data_anonymized.xml")

    if (smsMessages.isEmpty()) {
        println("❌ No SMS messages found in XML file")
        return
    }

    println("Loaded ${smsMessages.size} SMS messages from XML file")
    println()

    var passedTests = 0
    var failedTests = 0
    val failureDetails = mutableListOf<String>()

    smsMessages.forEachIndexed { index, smsData ->
        try {
            val result = parser.parse(smsData.body, smsData.sender, smsData.timestamp)

            if (result != null) {
                println("✅ PARSED: Message ${index + 1} - ${smsData.description}")
                println("   Amount: ${result.amount} ${result.currency}")
                println("   Type: ${result.type}")
                println("   Merchant: ${result.merchant}")
                println("   Account: ${result.accountLast4}")
                println()

                val (isValid, errors) = validateResult(result, smsData.body)

                if (isValid) {
                    passedTests++
                } else {
                    failedTests++
                    val errorDetails = """
                        ❌ VALIDATION FAILED: Message ${index + 1}
                          Message: ${smsData.body.take(100)}...
                          Parsed: Amount=${result.amount}, Currency=${result.currency}, Type=${result.type}
                          Errors: ${errors.joinToString(", ")}
                    """.trimIndent()
                    failureDetails.add(errorDetails)
                    println(errorDetails)
                    println()
                }
            } else {
                val shouldBeParsed = parser.shouldParseTransactionMessage(smsData.body)

                if (shouldBeParsed) {
                    failedTests++
                    val errorDetails = """
                        ❌ FAILED TO PARSE: Message ${index + 1} - Should have been parsed
                          Message: ${smsData.body.take(150)}...
                    """.trimIndent()
                    failureDetails.add(errorDetails)
                    println(errorDetails)
                    println()
                } else {
                    passedTests++
                    println("✅ CORRECTLY REJECTED: Message ${index + 1} - Non-transaction message")
                    println("   Message: ${smsData.body.take(100)}...")
                    println()
                }
            }
        } catch (e: Exception) {
            failedTests++
            val errorDetails = """
                ❌ EXCEPTION: Message ${index + 1} - ${e.message}
                  Message: ${smsData.body.take(100)}...
            """.trimIndent()
            failureDetails.add(errorDetails)
            println(errorDetails)
            println()
        }
    }

    printSummary(smsMessages.size, passedTests, failedTests, failureDetails)
}

fun validateResult(result: com.pennywiseai.parser.core.ParsedTransaction, body: String): Pair<Boolean, List<String>> {
    val errors = mutableListOf<String>()

    if (result.amount <= BigDecimal.ZERO) {
        errors.add("Amount should be positive: ${result.amount}")
    }

//    if (result.currency !in listOf("AED", "USD", "THB","MYR","SGD","KWD","EUR","INR")) {
//        errors.add("Unexpected currency: ${result.currency}")
//    }

    if (result.type == TransactionType.INCOME && body.contains("Debit", ignoreCase = true)) {
        errors.add("Debit transaction marked as INCOME")
    }

    if (result.type == TransactionType.EXPENSE && body.contains("credit", ignoreCase = true)) {
        errors.add("Credit transaction marked as EXPENSE")
    }

    return Pair(errors.isEmpty(), errors)
}

fun printSummary(total: Int, passed: Int, failed: Int, failureDetails: List<String>) {
    println("=".repeat(80))
    println("FAB PARSER XML TEST SUMMARY")
    println("=".repeat(80))
    println("Total SMS messages tested: $total")
    println("Successfully parsed/validated: $passed")
    println("Failed to parse or validate: $failed")
    println("Success rate: ${"%.2f".format(passed.toDouble() / total * 100)}%")

    if (failed > 0) {
        println("\n" + "=".repeat(80))
        println("DETAILED FAILURE LOG")
        println("=".repeat(80))
        failureDetails.forEach { detail ->
            println(detail)
            println("-".repeat(80))
        }
    }

    val minimumSuccessRate = 0.7
    val actualSuccessRate = passed.toDouble() / total

    if (actualSuccessRate >= minimumSuccessRate) {
        println("\n✅ Test passed with success rate: ${"%.2f".format(actualSuccessRate * 100)}%")
    } else {
        println("\n❌ Test failed: Success rate ${"%.2f".format(actualSuccessRate * 100)}% is below minimum ${"%.2f".format(minimumSuccessRate * 100)}%")
    }
}

