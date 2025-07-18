package com.pennywiseai.tracker.parser

import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.TransactionType

/**
 * Centralized repository of regex patterns for SMS transaction parsing
 */
object TransactionPatterns {
    
    // Amount patterns - matches various formats like Rs.500, INR 1,234.56, ₹123, etc.
    val AMOUNT_PATTERNS = listOf(
        // Standard formats: Rs.500, Rs 500, Rs500, INR 500
        Regex("""(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        // Amount with currency after: 500 INR, 500Rs
        Regex("""([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:Rs\.?|INR|₹)""", RegexOption.IGNORE_CASE),
        // Just numbers in transaction context (must be followed/preceded by transaction keywords)
        Regex("""(?:amount|paid|received|debited|credited|of)\s*:?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    )
    
    // Direction indicators
    val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "paid", "payment", "withdrawn", "spent", 
        "charged", "deducted", "purchase", "bought", "withdrawal"
    )
    
    val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "deposited", "added", 
        "refund", "cashback", "reversed", "deposit"
    )
    
    // Merchant extraction patterns
    val MERCHANT_PATTERNS = listOf(
        // "Payment of Rs.X to merchant" - HIGHEST PRIORITY
        Regex("""Payment\s+of\s+(?:Rs\.?|INR|₹)\s*[0-9,]+(?:\.[0-9]{1,2})?\s+to\s+([a-zA-Z][a-zA-Z0-9\s\-_\.]*?)(?:\s+is\s+|\s+has\s+|\s+was\s+|\s+\.|,|$)""", RegexOption.IGNORE_CASE),
        // "to VPA merchant@bank" or "to merchant via"
        Regex("""to\s+(?:VPA\s+)?([a-zA-Z0-9\s\-_\.]+?)(?:@|(?:\s+(?:via|using|through|on|dated|dt|is))\s+|\s+\(|,|$)""", RegexOption.IGNORE_CASE),
        // "from VPA merchant@bank" or "from merchant"
        Regex("""from\s+(?:VPA\s+)?([a-zA-Z0-9\s\-_\.]+?)(?:@|(?:\s+(?:via|using|through|on|dated|dt))\s+|\s+\(|,|$)""", RegexOption.IGNORE_CASE),
        // "at merchant" (for POS transactions)
        Regex("""at\s+([a-zA-Z0-9\s\-_\.]+?)(?:(?:\s+(?:using|via|through|on|dated|dt))\s+|\.|,|;|$)""", RegexOption.IGNORE_CASE),
        // "towards merchant" (for bills)
        Regex("""towards\s+([a-zA-Z0-9\s\-_\.]+?)(?:\s+(?:subscription|bill)|(?:\s+(?:using|via|through|on|dated))\s+|\.|,|$)""", RegexOption.IGNORE_CASE),
        // "paid to merchant" pattern
        Regex("""paid\s+to\s+([a-zA-Z0-9\s\-_\.]+?)(?:(?:\s+(?:using|via|through|on|dated|is))\s+|\.|,|$)""", RegexOption.IGNORE_CASE),
        // "spent at merchant" pattern
        Regex("""spent\s+at\s+([a-zA-Z0-9\s\-_\.]+?)(?:(?:\s+(?:using|via|through|on|dated))\s+|\.|,|$)""", RegexOption.IGNORE_CASE),
        // Generic merchant after amount - more restrictive
        Regex("""(?:Rs\.?|INR|₹)\s*[0-9,]+(?:\.[0-9]{1,2})?\s+(?:to|at|for)\s+([a-zA-Z][a-zA-Z0-9\s\-_\.]*?)(?:(?:\s+(?:using|via|through|on|dated|is))\s+|$)""", RegexOption.IGNORE_CASE)
    )
    
    // UPI ID patterns
    val UPI_PATTERNS = listOf(
        Regex("""([a-zA-Z0-9\.\-_]+@[a-zA-Z0-9]+)"""),
        Regex("""UPI\s*ID\s*:?\s*([a-zA-Z0-9\.\-_]+@[a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE),
        Regex("""VPA\s*:?\s*([a-zA-Z0-9\.\-_]+@[a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE)
    )
    
    // Transaction reference patterns
    val REFERENCE_PATTERNS = listOf(
        Regex("""(?:Ref|Reference|Trans|Transaction|ID|UPI)\s*(?:No\.?|Number|#)?\s*:?\s*([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
        Regex("""\((?:UPI|Ref)\s+([0-9]+)\)""", RegexOption.IGNORE_CASE)
    )
    
    // Account number patterns (last 4 digits)
    val ACCOUNT_PATTERNS = listOf(
        Regex("""A/c\s*(?:XX|xx|\*\*)?(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""Account\s*(?:ending|XX|xx|\*\*)?(\d{4})""", RegexOption.IGNORE_CASE),
        Regex("""card\s*(?:ending|XX|xx|\*\*)?(\d{4})""", RegexOption.IGNORE_CASE)
    )
    
    // Category keywords mapping
    val CATEGORY_KEYWORDS = mapOf(
        TransactionCategory.FOOD_DINING to listOf(
            "zomato", "swiggy", "uber eats", "dominos", "pizza", "mcdonalds", 
            "burger", "kfc", "starbucks", "cafe", "restaurant", "food"
        ),
        TransactionCategory.TRANSPORTATION to listOf(
            "uber", "ola", "rapido", "metro", "bus", "taxi", "cab", 
            "fuel", "petrol", "diesel", "parking"
        ),
        TransactionCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "shopping", "mall", 
            "store", "mart", "shop", "purchase", "mani square", "reliance"
        ),
        TransactionCategory.ENTERTAINMENT to listOf(
            "netflix", "spotify", "youtube", "hotstar", "jiohotstar", "prime", "movie", 
            "cinema", "pvr", "inox", "bookmyshow", "game", "disney", "zee5", "sonyliv"
        ),
        TransactionCategory.BILLS_UTILITIES to listOf(
            "electricity", "water", "gas", "internet", "broadband", "wifi", 
            "mobile", "postpaid", "prepaid", "dth", "bill"
        ),
        TransactionCategory.HEALTHCARE to listOf(
            "pharmacy", "medical", "doctor", "hospital", "clinic", "apollo", 
            "medicine", "health", "diagnostic", "lab"
        ),
        TransactionCategory.GROCERIES to listOf(
            "bigbasket", "grofers", "blinkit", "zepto", "grocery", "vegetables", 
            "fruits", "supermarket", "kirana"
        ),
        TransactionCategory.SUBSCRIPTION to listOf(
            "subscription", "premium", "membership", "renewal", "monthly", 
            "yearly", "annual"
        ),
        TransactionCategory.INVESTMENT to listOf(
            "mutual fund", "sip", "stock", "trading", "investment", "zerodha", 
            "groww", "upstox", "paytm money"
        ),
        TransactionCategory.TRANSFER to listOf(
            "transfer", "sent to", "received from", "p2p", "imps", "neft", "rtgs"
        )
    )
    
    // Transaction type keywords
    val TYPE_KEYWORDS = mapOf(
        TransactionType.SUBSCRIPTION to listOf(
            "subscription", "recurring", "auto debit", "renewal", "monthly payment",
            "annual payment", "membership"
        ),
        TransactionType.RECURRING_BILL to listOf(
            "bill payment", "utility", "electricity bill", "water bill", "gas bill",
            "internet bill", "mobile bill"
        ),
        TransactionType.REFUND to listOf(
            "refund", "reversed", "cashback", "money back", "return"
        ),
        TransactionType.INVESTMENT to listOf(
            "sip", "mutual fund", "investment", "trading", "stocks"
        ),
        TransactionType.TRANSFER to listOf(
            "transfer", "sent", "received", "p2p", "person to person"
        )
    )
    
    // SMS sender to category mapping
    val SENDER_CATEGORY_MAP = mapOf(
        "HDFCBK" to TransactionCategory.TRANSFER,
        "ICICIB" to TransactionCategory.TRANSFER,
        "SBIINB" to TransactionCategory.TRANSFER,
        "PAYTM" to TransactionCategory.TRANSFER,
        "PHONEPE" to TransactionCategory.TRANSFER,
        "NETFLIX" to TransactionCategory.ENTERTAINMENT,
        "SPOTIFY" to TransactionCategory.ENTERTAINMENT,
        "HOTSTAR" to TransactionCategory.ENTERTAINMENT,
        "JIOHOTSTAR" to TransactionCategory.ENTERTAINMENT,
        "AMAZON" to TransactionCategory.SHOPPING,
        "FLIPKART" to TransactionCategory.SHOPPING,
        "UBER" to TransactionCategory.TRANSPORTATION,
        "OLA" to TransactionCategory.TRANSPORTATION,
        "SWIGGY" to TransactionCategory.FOOD_DINING,
        "ZOMATO" to TransactionCategory.FOOD_DINING
    )
}