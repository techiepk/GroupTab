package com.pennywiseai.tracker.parser

/**
 * Comprehensive list of Indian bank SMS sender IDs
 * Source: TRAI registered sender IDs (June 2020) + additional updates
 * 
 * This includes:
 * - Major commercial banks
 * - Cooperative banks
 * - Small finance banks
 * - Payment banks
 * - Regional rural banks
 * - State cooperative banks
 */
object BankSenderIds {
    
    /**
     * Set of all known bank sender IDs for fast lookup
     * Includes variations like AD-HDFCBK, VM-HDFCBK etc.
     */
    val BANK_SENDERS = setOf(
        // Major Commercial Banks
        "HDFCBK", "ICICIT", "SBIBNK", "AXISBK", "KOTAKB", "PNBSMS",
        "IDBIBK", "YESBK", "INDUSB", "SCBANK", "HSBCIN", "CITIBK",
        "RBLBNK", "BOIIND", "UNIONB", "CANBNK", "IOBCHN", "BOBBSR",
        "CENTBK", "UCOBNK", "PSBBNK", "OBCBNK", "CORPBK", "ANDBMB",
        "SYNDBK", "VIJBNK", "DENABN", "ALBBNK", "MSHBNK", "KVBLNK",
        
        // Axis Bank variations
        "AXISBK", "AXISB", "AXISHR", "AXISIN", "AXISMR", "AXISPR", "AXISSR",
        "AXSFI", "AXSFIN",
        
        // HDFC variations
        "HDFCBK", "HDFCB", "HDFCBN", "HDFC",
        
        // SBI variations
        "SBIBNK", "SBISMS", "SBIMSG", "CBSSBI", "ATMSBI", "SBIACC",
        "SBIINB", "SBICRD", "SBIPSG", "ONLSBI", "STBANK",
        
        // ICICI variations
        "ICICIT", "ICICIB", "ICICIS", "ICICIM", "ICICI",
        
        // Kotak variations
        "KOTAKB", "KOTMSG", "KOTBNK", "KOTAKM",
        
        // Small Finance Banks
        "AUBANK", "AUBMSG", "AUBSMS", "AUDOST", "AUITSM", // AU Bank
        "EQBANK", "EQUITS", "EQSMSG", // Equitas
        "UJJIVN", "UJJSFB", // Ujjivan
        "SURBNK", "SURSMS", // Suryoday
        "FINSFB", "FINCRE", // Fincare
        "UTKARB", "UTKBNK", // Utkarsh
        "ESAFBN", "ESAFSB", // ESAF
        "JNLBFL", "JANASB", // Jana
        "NBFCAP", "CAPITAL", // Capital
        
        // Payment Banks
        "PPBLNK", "PAYTMB", "AIRBPB", "FINOPB", "JIOPPB", "NSDLPB",
        "BNDNBK", "BNDNHL", // Bandhan Bank
        
        // Regional Rural Banks
        "APGBBK", "APGBHO", "APGBIT", "APGBNK", "APGECM", // Andhra Pradesh
        "KGBBNK", "KKBBNK", "PKGBNK", "BGBBNK", "MGBBNK", // Karnataka
        "TGBBNK", "DGBBNK", "CGBBNK", // Telangana
        "KERBGB", "KGRBNK", // Kerala
        "TNSCBK", "PSCBNK", // Tamil Nadu
        
        // Cooperative Banks
        "PMCBNK", "MUCBNK", "SVCBNK", "COSBNK", "TJSBNK", "KARADB",
        "BHARAT", "SARBNK", "DCCBNK", "GSCBNK", "NAGRIK", "JSBBNK",
        
        // Foreign Banks
        "SCBANK", "HSBCIN", "CITIBK", "DBSBNK", "BOFAAS", "JPBANK",
        "DEUTBN", "BNYBNK", "ABNABN", "RBSBNK", "SOCGEN", "BNPBNK",
        
        // Others
        "FEDBNK", "FEDSMS", // Federal Bank
        "TMBNET", "TMBBNK", // Tamilnad Mercantile Bank
        "KVBANK", "KVBNET", // Karur Vysya Bank
        "LAKSVI", "LVBANK", // Lakshmi Vilas Bank
        "CITYUB", "CITYUN", // City Union Bank
        "DHANBN", "DHANBK", // Dhanlaxmi Bank
        "JKBANK", "JKBSMS", // J&K Bank
        "NAINBN", "NAINIB", // Nainital Bank
        "SOUTHB", "SOUTHI", // South Indian Bank
        "CSBBNK", "CSBNET", // CSB Bank
        "DCBANK", "DCBSMS", // DCB Bank
        
        // Additional sender IDs from CSV
        "ANDBNK", "BHRDBK", "BSCBNK", "CCBANK", "CORPBN", "DENABN",
        "IDFCFB", "INDBNK", "OBCBMB", "PNJBMB", "SYNBMB", "UCOBMB",
        "UNIONB", "VJYBMB", "ALBMBN", "MSHBMB", "KVBLMB"
    )
    
    /**
     * Regex patterns to match bank sender variations
     * These handle prefix variations like AD-HDFCBK, VM-HDFCBK, etc.
     */
    val BANK_SENDER_PATTERNS = listOf(
        // Standard bank codes with prefixes (AD-HDFCBK, VM-ICICIT, AX-HDFCBK-S, etc)
        Regex("^[A-Z]{2}-(HDFCBK|HDFCB|HDFC|ICICIT|ICICIB|SBIBNK|SBISMS|AXISBK|AXISB|KOTAKB|KOTMSG|PNBSMS|YESBK|INDUSB|SCBANK|HSBCIN|CITIBK|RBLBNK).*"),
        Regex("^[A-Z]{2}-(BOIIND|UNIONB|CANBNK|IOBCHN|BOBBSR|CENTBK|UCOBNK|PSBBNK|OBCBNK|CORPBK).*"),
        Regex("^[A-Z]{2}-(FEDBNK|TMBNET|KVBANK|LAKSVI|CITYUB|DHANBN|JKBANK|NAINBN|SOUTHB|CSBBNK|DCBANK).*"),
        Regex("^[A-Z]{2}-(AUBANK|EQBANK|UJJIVN|SURBNK|FINSFB|UTKARB|ESAFBN|JNLBFL|NBFCAP).*"),
        
        // Generic bank patterns - REMOVED, too broad
        // These were matching non-banks like "BANK OF BOOKS" etc.
        
        // More flexible bank sender patterns
        Regex(".*-(HDFCBK|ICICIT|SBIBNK|AXISBK|KOTAKB|HDFC|ICICI|SBI|AXIS|KOTAK).*"),
        
        // UPI/Payment providers that handle bank transactions
        Regex("^(PAYTM|PayTM|PHONPE|PhonePe|PHONEPE|GPAY|BHIM|BHIMUPI|PYTM|PHONEPE-S)$")
    )
    
    /**
     * Check if a sender ID belongs to a bank or financial institution
     */
    fun isBankSender(senderId: String): Boolean {
        // Direct match
        if (senderId in BANK_SENDERS) {
            android.util.Log.d("BankSenderIds", "Direct match for $senderId: true")
            return true
        }
        
        // Pattern match
        val patternMatch = BANK_SENDER_PATTERNS.any { pattern ->
            pattern.matches(senderId)
        }
        android.util.Log.d("BankSenderIds", "Pattern match for $senderId: $patternMatch")
        return patternMatch
    }
    
    /**
     * Check if SMS content indicates a bank transaction
     * Used as additional validation for unknown senders
     */
    fun hasStrongBankingIndicators(smsBody: String): Boolean {
        val lowerBody = smsBody.lowercase()
        
        // Must have account/balance indicators
        val hasAccountInfo = listOf(
            "a/c", "account", "acct", "acc no",
            "available balance", "avl bal", "avbl bal", "bal:",
            "current balance", "total balance", "remaining balance"
        ).any { lowerBody.contains(it) }
        
        // Or transaction reference
        val hasTransactionRef = listOf(
            "txn id", "transaction id", "ref no", "reference no",
            "rrn:", "utr:", "imps ref", "neft ref", "rtgs ref"
        ).any { lowerBody.contains(it) }
        
        // Or card indicators
        val hasCardInfo = listOf(
            "card ending", "card no", "xxxx", "****"
        ).any { lowerBody.contains(it) } && 
        listOf("debited", "credited", "withdrawn").any { lowerBody.contains(it) }
        
        return hasAccountInfo || hasTransactionRef || hasCardInfo
    }
    
    /**
     * Non-bank senders to explicitly exclude
     * These are known to send messages with amounts but aren't transactions
     */
    val EXCLUDED_SENDERS = setOf(
        // Telecom
        "AIRTEL", "VODAFONE", "JIOINF", "BSNLIN", "MTSNLI", "AIRTLM",
        
        // E-commerce
        "AMAZON", "FLIPKART", "MYNTRA", "SNAPDEAL", "AJIOAX", "MEESHO",
        
        // Travel/Transport
        "REDBUS", "MAKEMT", "GOIBIB", "IRCTCS", "OLACAB", "UBERIN",
        
        // Food/Delivery
        "SWIGGY", "ZOMATO", "DOMINOS", "PIZZAH", "KFCIND", "MCDIND",
        
        // Others
        "JUSTDL", "POLICYX", "TIMESJ", "NDTVPR"
    )
    
    /**
     * Check if sender should be excluded
     */
    fun isExcludedSender(senderId: String): Boolean {
        return senderId in EXCLUDED_SENDERS
    }
}