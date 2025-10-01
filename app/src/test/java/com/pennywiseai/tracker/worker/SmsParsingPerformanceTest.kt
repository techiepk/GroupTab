package com.pennywiseai.tracker.worker


import com.pennywiseai.parser.core.bank.BankParserFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.measureTimeMillis
data class SMSData(
    val sender: String,
    val body: String,
    val timestamp: Long,
    val readableDate: String,
    val description: String
)

fun loadSMSDataFromXML(): List<SMSData> {
    val smsList = mutableListOf<SMSData>()

    try {
        val inputStream: InputStream = object {}.javaClass.classLoader?.getResourceAsStream("fab_sms_test_data_anonymized.xml")
            ?: throw RuntimeException("XML file not found: fab_sms_test_data_anonymized.xml")

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc: Document = dBuilder.parse(inputStream)
        doc.documentElement.normalize()

        // Parse regular SMS messages
        val smsNodes: NodeList = doc.getElementsByTagName("sms")
        for (i in 0 until smsNodes.length) {
            val smsNode = smsNodes.item(i)
            if (smsNode.nodeType == Node.ELEMENT_NODE) {
                val smsElement = smsNode as Element
                smsList.add(parseSMSElement(smsElement))
            }
        }

        // Parse MMS messages
        val mmsNodes: NodeList = doc.getElementsByTagName("mms")
        for (i in 0 until mmsNodes.length) {
            val mmsNode = mmsNodes.item(i)
            if (mmsNode.nodeType == Node.ELEMENT_NODE) {
                val mmsElement = mmsNode as Element
                smsList.add(parseMMSElement(mmsElement))
            }
        }

    } catch (e: Exception) {
        throw RuntimeException("Failed to load SMS data from XML file", e)
    }

    return smsList
}

fun parseSMSElement(smsElement: Element): SMSData {
    val address = smsElement.getAttribute("address")
    val body = smsElement.getAttribute("body")
        .replace("&#10;", "\n")
        .replace("&amp;", "&")
    val date = smsElement.getAttribute("date").toLongOrNull() ?: System.currentTimeMillis()
    val readableDate = smsElement.getAttribute("readable_date")

    return SMSData(
        sender = address,
        body = body,
        timestamp = date,
        readableDate = readableDate,
        description = generateDescription(body, readableDate)
    )
}

fun parseMMSElement(mmsElement: Element): SMSData {
    val address = mmsElement.getAttribute("address")
    val date = mmsElement.getAttribute("date").toLongOrNull() ?: System.currentTimeMillis()
    val readableDate = mmsElement.getAttribute("readable_date")

    var body = ""
    val parts = mmsElement.getElementsByTagName("part")
    for (i in 0 until parts.length) {
        val part = parts.item(i) as Element
        if (part.getAttribute("ct") == "text/plain") {
            body = part.getAttribute("text")
                .replace("&#10;", "\n")
                .replace("&amp;", "&")
            break
        }
    }

    return SMSData(
        sender = address,
        body = body,
        timestamp = date,
        readableDate = readableDate,
        description = "MMS: ${generateDescription(body, readableDate)}"
    )
}

fun generateDescription(body: String, readableDate: String): String {
    return when {
        body.contains("Debit Card Purchase") -> "Debit Card Purchase"
        body.contains("ATM Cash withdrawal") -> "ATM Cash Withdrawal"
        body.contains("Outward Remittance") -> "Outward Remittance"
        body.contains("funds transfer") -> "Funds Transfer"
        body.contains("OTP") -> "OTP Message"
        body.contains("Samsung Pay") -> "Samsung Pay Notification"
        else -> "General SMS - $readableDate"
    }
}
/**
 * Benchmarks parsing of real SMS dataset loaded via loadSMSDataFromXML().
 * Compares sequential vs optimized (parallel batched) processing.
 * Pure JVM test – does not require Android device.
 */
class SmsParsingBenchmarkTest {

    @Test
    fun benchmarkSequentialVsOptimized() = runBlocking {
        // Load real test data using the provided loader
        val smsList: List<SMSData> = loadSMSDataFromXML()
        println("Loaded ${smsList.size} SMS messages from XML file")

        // --- Sequential parsing ---
        var parsedSeq = 0
        val seqTime = measureTimeMillis {
            parsedSeq = parseSequential(smsList)
        }
        println("Sequential parsing: $parsedSeq messages in ${seqTime} ms")

        // --- Optimized parallel parsing ---
        var parsedOpt = 0
        val optTime = measureTimeMillis {
            parsedOpt = parseOptimized(smsList, batchSize = 200, parallelism = 8)
        }
        println("Optimized parsing:  $parsedOpt messages in ${optTime} ms")

        // --- Benchmark summary ---
        val improvement = if (seqTime > 0)
            ((seqTime - optTime) / seqTime.toFloat() * 100).toInt() else 0
        val speedup = if (optTime > 0)
            seqTime.toFloat() / optTime else 0f

        println("===================================")
        println("Benchmark Results:")
        println("Sequential time : ${seqTime} ms")
        println("Optimized time  : ${optTime} ms")
        println("Speedup factor  : ${"%.2f".format(speedup)}x")
        println("Improvement     : ${improvement}%")
        println("Msgs/sec (seq)  : ${
            "%.2f".format(smsList.size / (seqTime.coerceAtLeast(1) / 1000f))
        }")
        println("Msgs/sec (opt)  : ${
            "%.2f".format(smsList.size / (optTime.coerceAtLeast(1) / 1000f))
        }")
        println("===================================")

        // --- Basic sanity checks ---
        assertTrue("Optimized should parse at least as many messages",
            parsedOpt >= parsedSeq)

        // If dataset is reasonably large, expect optimized to be at least as fast
        if (smsList.size > 500) {
            assertTrue(
                "Optimized should be at least as fast (within 10% tolerance)",
                optTime <= seqTime * 1.10f
            )
        }
    }

    private fun parseSequential(messages: List<SMSData>): Int {
        var parsed = 0
        for (sms in messages) {
            val parser = BankParserFactory.getParser(sms.sender)
            if (parser != null && parser.parse(sms.body, sms.sender, sms.timestamp) != null) {
                parsed++
            }
        }
        return parsed
    }

    private suspend fun parseOptimized(
        messages: List<SMSData>,
        batchSize: Int,
        parallelism: Int
    ): Int = kotlinx.coroutines.coroutineScope {
        val batches = messages.chunked(batchSize)
        val chunkSize = maxOf(1, batches.size / parallelism)
        val chunks = batches.chunked(chunkSize)

        val deferred = chunks.map { chunk ->
            async(Dispatchers.Default) {
                var c = 0
                for (batch in chunk) {
                    for (sms in batch) {
                        val p = BankParserFactory.getParser(sms.sender)
                        if (p != null && p.parse(sms.body, sms.sender, sms.timestamp) != null) {
                            c++
                        }
                    }
                }
                c
            }
        }

        deferred.awaitAll().sum()
    }

}
