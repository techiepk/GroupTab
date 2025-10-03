package com.pennywiseai.parser.core.test

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.bank.BankParser
import org.junit.jupiter.api.Assertions.assertTrue
import java.math.BigDecimal

/**
 * Common test data structures and utilities for bank parser tests.
 * This reduces code duplication across different parser test files.
 */

data class ExpectedTransaction(
    val amount: BigDecimal,
    val currency: String,
    val type: com.pennywiseai.parser.core.TransactionType,
    val merchant: String? = null,
    val reference: String? = null,
    val accountLast4: String? = null,
    val balance: BigDecimal? = null
)

data class ParserTestCase(
    val name: String,
    val message: String,
    val sender: String,
    val expected: ExpectedTransaction? = null,
    val shouldParse: Boolean = true,
    val description: String = ""
)

data class SimpleTestCase(
    val bankName: String,
    val sender: String,
    val currency: String,
    val message: String,
    val description: String = ""
)

/**
 * Common test runner utilities for bank parser tests.
 */
object ParserTestUtils {

    /**
     * Run a single parser test case and return the result.
     */
    fun runSingleTest(
        parser: BankParser,
        testCase: ParserTestCase
    ): TestResult {
        val result = parser.parse(testCase.message, testCase.sender, System.currentTimeMillis())

        return when {
            result == null && testCase.shouldParse -> TestResult(
                name = testCase.name,
                passed = false,
                error = "Parser returned null but expected to parse: ${testCase.message.take(100)}..."
            )

            result != null && !testCase.shouldParse -> TestResult(
                name = testCase.name,
                passed = false,
                error = "Parser parsed message but should have rejected: ${result.amount} ${result.currency}"
            )

            result == null && !testCase.shouldParse -> TestResult(
                name = testCase.name,
                passed = true,
                details = "Correctly rejected non-transaction message"
            )

            else -> {
                val errors = validateResult(result!!, testCase.expected!!)
                TestResult(
                    name = testCase.name,
                    passed = errors.isEmpty(),
                    error = if (errors.isNotEmpty()) errors.joinToString(", ") else null,
                    details = if (errors.isEmpty()) "Successfully parsed: ${result.amount} ${result.currency}" else null
                )
            }
        }
    }

    /**
     * Validate parsed transaction against expected values.
     */
    fun validateResult(
        result: ParsedTransaction,
        expected: ExpectedTransaction
    ): List<String> {
        val errors = mutableListOf<String>()

        if (result.amount != expected.amount) {
            errors.add("Amount mismatch: expected ${expected.amount}, got ${result.amount}")
        }

        if (result.currency != expected.currency) {
            errors.add("Currency mismatch: expected ${expected.currency}, got ${result.currency}")
        }

        if (result.type != expected.type) {
            errors.add("Transaction type mismatch: expected ${expected.type}, got ${result.type}")
        }

        if (expected.merchant != null && result.merchant != expected.merchant) {
            errors.add("Merchant mismatch: expected '${expected.merchant}', got '${result.merchant}'")
        }

        if (expected.reference != null && result.reference != expected.reference) {
            errors.add("Reference mismatch: expected '${expected.reference}', got '${result.reference}'")
        }

        if (expected.accountLast4 != null && result.accountLast4 != expected.accountLast4) {
            errors.add("Account mismatch: expected '${expected.accountLast4}', got '${result.accountLast4}'")
        }

        if (expected.balance != null && result.balance != expected.balance) {
            errors.add("Balance mismatch: expected ${expected.balance}, got ${result.balance}")
        }

        return errors
    }

    /**
     * Print test header with bank information.
     */
    fun printTestHeader(
        parserName: String,
        bankName: String,
        currency: String,
        additionalInfo: String = ""
    ) {
        println("=" * 80)
        println("$parserName Test Suite$additionalInfo")
        println("=" * 80)
        println("Bank Name: $bankName")
        println("Currency: $currency")
        println()
    }

    /**
     * Print section header.
     */
    fun printSectionHeader(sectionName: String) {
        println("=== $sectionName ===")
        println()
    }

    /**
     * Print test result with appropriate formatting.
     */
    fun printTestResult(result: TestResult, showDetails: Boolean = true) {
        val status = if (result.passed) "✓ PASSED" else "✗ FAILED"
        println("$status: ${result.name}")

        if (result.passed && showDetails && result.details != null) {
            println("  ${result.details}")
        }

        if (!result.passed && result.error != null) {
            println("  Error: ${result.error}")
        }

        println()
    }

    /**
     * Print test summary.
     */
    fun printTestSummary(
        totalTests: Int,
        passedTests: Int,
        failedTests: Int,
        failureDetails: List<String> = emptyList()
    ) {
        println("=" * 80)
        println("Test Summary")
        println("=" * 80)
        println("Total tests: $totalTests")
        println("Passed: $passedTests")
        println("Failed: $failedTests")
        println("Success rate: ${"%.2f".format(passedTests.toDouble() / totalTests * 100)}%")

        if (failedTests > 0 && failureDetails.isNotEmpty()) {
            println("\n" + "=" * 80)
            println("Failure Details")
            println("=" * 80)
            failureDetails.forEach { detail ->
                println("  - $detail")
            }
        }

        val successRate = passedTests.toDouble() / totalTests
        if (successRate >=1) {
            println("\n✅ Overall test passed with success rate: ${"%.2f".format(successRate * 100)}%")
        } else {
            println("\n❌ Overall test failed: Success rate ${"%.2f".format(successRate * 100)}% is below 80%")
            assertTrue(false)
        }
    }

    /**
     * Run multiple test cases and return summary.
     */
    fun runTestSuite(
        parser: BankParser,
        testCases: List<ParserTestCase>,
        suiteName: String = ""
    ): TestSuiteResult {
        if (suiteName.isNotEmpty()) {
            printSectionHeader(suiteName)
        }

        val results = mutableListOf<TestResult>()
        val failureDetails = mutableListOf<String>()

        testCases.forEach { testCase ->
            val result = runSingleTest(parser, testCase)
            results.add(result)
            printTestResult(result)

            if (!result.passed && result.error != null) {
                failureDetails.add("${testCase.name}: ${result.error}")
            }
        }

        val passedTests = results.count { it.passed }
        val failedTests = results.size - passedTests

        return TestSuiteResult(
            totalTests = results.size,
            passedTests = passedTests,
            failedTests = failedTests,
            results = results,
            failureDetails = failureDetails
        )
    }
}

/**
 * Data class representing a single test result.
 */
data class TestResult(
    val name: String,
    val passed: Boolean,
    val error: String? = null,
    val details: String? = null
)

/**
 * Data class representing test suite results.
 */
data class TestSuiteResult(
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val results: List<TestResult>,
    val failureDetails: List<String>
)

/**
 * Extension function for string repetition (since some test files use "=" * 80)
 */
private operator fun String.times(count: Int): String = this.repeat(count)