import com.pennywiseai.parser.core.bank.BankParserFactory

fun main() {
    println("=== Testing US Bank Parsers ===")

    // Test Citi Bank
    println("\n--- Citi Bank Tests ---")
    val citiSender = "692484"
    val citiMessage = "Citi Alert: A \$3.01 transaction was made at BP#1234E on card ending in 1234. View details at citi.com/citimobileapp"

    val citiParser = BankParserFactory.getParser(citiSender)
    if (citiParser != null) {
        println("✓ Found parser: ${citiParser.getBankName()}")
        println("✓ Currency: ${citiParser.getCurrency()}")

        val citiResult = citiParser.parse(citiMessage, citiSender, System.currentTimeMillis())
        if (citiResult != null) {
            println("✓ Parsed amount: ${citiResult.amount}")
            println("✓ Parsed currency: ${citiResult.currency}")
            println("✓ Parsed merchant: ${citiResult.merchant}")
            println("✓ Parsed account: ${citiResult.accountLast4}")
            println("✓ Parsed type: ${citiResult.type}")
        } else {
            println("✗ Failed to parse Citi message")
        }
    } else {
        println("✗ No parser found for Citi sender: $citiSender")
    }

    // Test Discover Card
    println("\n--- Discover Card Tests ---")
    val discoverSender = "347268"
    val discoverMessage = "Discover Card Alert: A transaction of \$25.00 at WWW.XXX.ORG on February 21, 2025. No Action needed. See it at https://app.discover.com/ACTVT. Text STOP to end"

    val discoverParser = BankParserFactory.getParser(discoverSender)
    if (discoverParser != null) {
        println("✓ Found parser: ${discoverParser.getBankName()}")
        println("✓ Currency: ${discoverParser.getCurrency()}")

        val discoverResult = discoverParser.parse(discoverMessage, discoverSender, System.currentTimeMillis())
        if (discoverResult != null) {
            println("✓ Parsed amount: ${discoverResult.amount}")
            println("✓ Parsed currency: ${discoverResult.currency}")
            println("✓ Parsed merchant: ${discoverResult.merchant}")
            println("✓ Parsed account: ${discoverResult.accountLast4}")
            println("✓ Parsed type: ${discoverResult.type}")
            println("✓ Parsed reference: ${discoverResult.reference}")
        } else {
            println("✗ Failed to parse Discover message")
        }
    } else {
        println("✗ No parser found for Discover sender: $discoverSender")
    }

    // Test alternative senders
    println("\n--- Alternative Sender Tests ---")
    val altSenders = listOf("CITI", "DISCOVER")
    for (sender in altSenders) {
        val parser = BankParserFactory.getParser(sender)
        if (parser != null) {
            println("✓ $sender -> ${parser.getBankName()} (${parser.getCurrency()})")
        } else {
            println("✗ No parser for $sender")
        }
    }
}