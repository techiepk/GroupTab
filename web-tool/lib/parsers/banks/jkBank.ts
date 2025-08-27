import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const JKBankConfig: BankConfig = {
  bankName: 'JK Bank',
  bankCode: 'jkbank',
  senders: {
    exact: ['JKBANK', 'JKB', 'JKBANKL', 'JKBNK'],
    contains: ['JKBANK', 'JKB'],
    patterns: [
      /^[A-Z]{2}-JKBANK.*$/,
      /^[A-Z]{2}-JKB.*$/,
      /^[A-Z]{2}-JKBNK.*$/,
      /^JKBANK-[A-Z]+$/,
      /^JKB-[A-Z]+$/
    ]
  },
  transactionType: {
    expense: ['has been debited', 'debited', 'withdrawn', 'spent', 'charged', 'paid', 'purchase', 'transferred'],
    income: ['has been credited', 'credited', 'deposited', 'received', 'refund', 'cashback']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount',
    'cashback offer',
    'win ',
    'has requested',
    'payment request',
    'collect request',
    'requesting payment'
  ]
}

export class JKBankParser extends BankParser {
  constructor() {
    super(JKBankConfig)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 1: "via UPI from SENDER NAME on" (for credits)
    if (message.toLowerCase().includes('via upi from')) {
      const fromPattern = /via\s+UPI\s+from\s+([^.\n]+?)\s+on/i
      const fromMatch = message.match(fromPattern)
      if (fromMatch) {
        const merchant = fromMatch[1].trim()
        if (this.isValidMerchantName(merchant)) {
          return this.cleanMerchantName(merchant)
        }
      }
    }

    // Pattern 2: "by mTFR/962211111/SENDER NAME" (mPay transfer)
    const mtfrPattern = /mTFR\/\d+\/([^.\n]+?)(?:\.|A\/c|$)/i
    const mtfrMatch = message.match(mtfrPattern)
    if (mtfrMatch) {
      const merchant = mtfrMatch[1].trim()
      if (this.isValidMerchantName(merchant)) {
        return this.cleanMerchantName(merchant)
      }
    }

    // Pattern 3: UPI transactions to merchant
    if (message.toLowerCase().includes('via upi')) {
      // Look for UPI VPA pattern
      const vpaPattern = /to\s+([^@\s]+@[^\s]+)/i
      const vpaMatch = message.match(vpaPattern)
      if (vpaMatch) {
        const vpa = vpaMatch[1].trim()
        // Extract the part before @ as merchant name
        const merchantName = vpa.split('@')[0]
        if (merchantName && merchantName !== 'upi') {
          return this.cleanMerchantName(merchantName)
        }
      }

      // Look for merchant after "to" but before "via UPI"
      const toMerchantPattern = /to\s+([^.\n]+?)\s+via\s+UPI/i
      const toMerchantMatch = message.match(toMerchantPattern)
      if (toMerchantMatch) {
        const merchant = toMerchantMatch[1].trim()
        if (this.isValidMerchantName(merchant)) {
          return this.cleanMerchantName(merchant)
        }
      }

      // Default to "UPI" if no specific merchant found
      return 'UPI'
    }

    // Check for ATM withdrawals
    if (message.toLowerCase().includes('atm') || message.toLowerCase().includes('withdrawn')) {
      return 'ATM'
    }

    // Standard patterns for merchant extraction
    const merchantPatterns = [
      // Pattern for "to MERCHANT via"
      /to\s+([^.\n]+?)\s+via/i,
      // Pattern for "from MERCHANT"
      /from\s+([^.\n]+?)(?:\s+on|\s+Ref|$)/i,
      // Pattern for "at MERCHANT"
      /at\s+([^.\n]+?)(?:\s+on|\s+Ref|$)/i,
      // Pattern for "for MERCHANT"
      /for\s+([^.\n]+?)(?:\s+on|\s+Ref|$)/i
    ]

    for (const pattern of merchantPatterns) {
      const match = message.match(pattern)
      if (match) {
        const merchant = this.cleanMerchantName(match[1].trim())
        if (this.isValidMerchantName(merchant)) {
          return merchant
        }
      }
    }

    // Fall back to base extraction
    return super.extractMerchant(message, sender)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    // JK Bank specific patterns
    if (lowerMessage.includes('has been debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('has been credited')) return TransactionType.INCOME

    // Standard expense keywords
    if (lowerMessage.includes('debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('withdrawn')) return TransactionType.EXPENSE
    if (lowerMessage.includes('spent')) return TransactionType.EXPENSE
    if (lowerMessage.includes('charged')) return TransactionType.EXPENSE
    if (lowerMessage.includes('paid')) return TransactionType.EXPENSE
    if (lowerMessage.includes('purchase')) return TransactionType.EXPENSE
    if (lowerMessage.includes('transferred')) return TransactionType.EXPENSE

    // Income keywords
    if (lowerMessage.includes('credited')) return TransactionType.INCOME
    if (lowerMessage.includes('deposited')) return TransactionType.INCOME
    if (lowerMessage.includes('received')) return TransactionType.INCOME
    if (lowerMessage.includes('refund')) return TransactionType.INCOME
    if (lowerMessage.includes('cashback') && !lowerMessage.includes('earn cashback')) return TransactionType.INCOME

    return null
  }

  protected extractReference(message: string): string | null {
    // JK Bank specific reference patterns
    const jkBankPatterns = [
      // UPI Ref: 115458170728
      /UPI\s+Ref[:\s]+(\d+)/i,
      // txn Ref: XXXXX
      /txn\s+Ref[:\s]+([A-Z0-9]+)/i,
      // Reference: XXXXX
      /Reference[:\s]+([A-Z0-9]+)/i,
      // Ref No: XXXXX
      /Ref\s+No[:\s]+([A-Z0-9]+)/i
    ]

    for (const pattern of jkBankPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1].trim()
      }
    }

    // Fall back to base extraction
    return super.extractReference(message)
  }

  protected extractAccountLast4(message: string): string | null {
    // JK Bank specific account patterns
    const jkBankPatterns = [
      // Your A/c XXXXXXXX1111
      /Your\s+A\/c\s+[X]+(\d{4})/i,
      // A/c XXXXXXXX1111 or A/c XX1111
      /A\/c\s+[X]*(\d{4})/i,
      // Account XXXXXXXX1111
      /Account\s+[X]+(\d{4})/i,
      // from A/c ending 1111
      /A\/c\s+ending\s+(\d{4})/i
    ]

    for (const pattern of jkBankPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }

    // Fall back to base extraction
    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // JK Bank specific balance patterns
    const balancePatterns = [
      // Avl Bal: Rs.1234.56
      /Avl\s+Bal[:\s]+Rs\.?\s*([\d,]+(?:\.\d{2})?)/i,
      // Balance: Rs.1234.56
      /Balance[:\s]+Rs\.?\s*([\d,]+(?:\.\d{2})?)/i,
      // Bal Rs.1234.56
      /Bal\s+Rs\.?\s*([\d,]+(?:\.\d{2})?)/i
    ]

    for (const pattern of balancePatterns) {
      const match = message.match(pattern)
      if (match) {
        const balanceStr = match[1].replace(/,/g, '')
        const balance = parseFloat(balanceStr)
        if (!isNaN(balance)) {
          return balance
        }
      }
    }

    // Fall back to base extraction
    return super.extractBalance(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip OTP and verification messages
    if (
      lowerMessage.includes('otp') ||
      lowerMessage.includes('one time password') ||
      lowerMessage.includes('verification code')
    ) {
      return false
    }

    // Skip promotional messages
    if (
      lowerMessage.includes('offer') ||
      lowerMessage.includes('discount') ||
      lowerMessage.includes('cashback offer') ||
      lowerMessage.includes('win ')
    ) {
      return false
    }

    // Skip payment request messages
    if (
      lowerMessage.includes('has requested') ||
      lowerMessage.includes('payment request') ||
      lowerMessage.includes('collect request') ||
      lowerMessage.includes('requesting payment')
    ) {
      return false
    }

    // Skip messages asking to report fraud
    // But make sure the transaction keywords are present
    if (lowerMessage.includes('if not done by you') || lowerMessage.includes('report immediately')) {
      // These are usually part of transaction messages, so check for transaction keywords
      const transactionKeywords = [
        'debited',
        'credited',
        'withdrawn',
        'deposited',
        'spent',
        'received',
        'transferred',
        'paid'
      ]
      return transactionKeywords.some(keyword => lowerMessage.includes(keyword))
    }

    // JK Bank specific transaction keywords
    const jkBankTransactionKeywords = [
      'has been debited',
      'has been credited',
      'debited',
      'credited',
      'withdrawn',
      'deposited',
      'spent',
      'received',
      'transferred',
      'paid'
    ]

    return jkBankTransactionKeywords.some(keyword => lowerMessage.includes(keyword))
  }
}