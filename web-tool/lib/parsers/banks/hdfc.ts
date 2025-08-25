import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const HDFCConfig: BankConfig = {
  bankName: 'HDFC Bank',
  bankCode: 'hdfc',
  senders: {
    exact: ['HDFCBK', 'HDFCBANK', 'HDFC', 'HDFCB'],
    contains: ['HDFC'],
    patterns: [
      /^[A-Z]{2}-HDFCBK.*$/,
      /^[A-Z]{2}-HDFC.*$/,
      /^HDFC-[A-Z]+$/,
      /^[A-Z]{2}-HDFCB.*$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'spent', 'charged', 'paid', 'purchase', 'sent', 'deducted', 'txn'],
    income: ['credited', 'deposited', 'received', 'refund', 'cashback']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount',
    'cashback offer',
    'win ',
    'e-mandate!',
    'block cc',
    'received towards your credit card',
    'credited to your card'
  ]
}

export class HDFCBankParser extends BankParser {
  constructor() {
    super(HDFCConfig)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Try HDFC specific patterns first

    // Pattern for: "Spent Rs.XXX On HDFC Bank Card XXXX At MERCHANT On DATE"
    // Handles merchants like RSP*INSTAMART (Swiggy Instamart)
    const spentOnCardPattern = /Spent\s+Rs\.?\d+(?:\.\d{2})?\s+On\s+HDFC\s+Bank\s+Card\s+\d+\s+At\s+([^\s]+)\s+On/i
    const spentMatch = message.match(spentOnCardPattern)
    if (spentMatch) {
      let merchant = spentMatch[1].trim()
      
      // Map known merchant codes to full names
      if (merchant.toUpperCase() === 'RSP*INSTAMART') {
        merchant = 'Swiggy Instamart'
      } else if (merchant.startsWith('RSP*')) {
        // General RSP prefix handling for other Swiggy services
        merchant = 'Swiggy ' + merchant.substring(4)
      }
      
      return this.cleanMerchantName(merchant)
    }

    // Pattern 1: Salary credit - "for XXXXX-ABC-XYZ MONTH SALARY-COMPANY NAME"
    if (message.toLowerCase().includes('salary') && message.toLowerCase().includes('deposited')) {
      const salaryPattern = /for\s+[^-]+-[^-]+-[^-]+\s+[A-Z]+\s+SALARY-([^.\n]+)/i
      const match = message.match(salaryPattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }

      // Simpler salary pattern
      const simpleSalaryPattern = /SALARY[- ]([^.\n]+?)(?:\s+Info|$)/i
      const simpleMatch = message.match(simpleSalaryPattern)
      if (simpleMatch) {
        const merchant = simpleMatch[1].trim()
        if (merchant && !/^\d+$/.test(merchant)) {
          return this.cleanMerchantName(merchant)
        }
      }
    }

    // Pattern 2: "Info: UPI/merchant/category" format
    if (message.toLowerCase().includes('info:')) {
      const infoPattern = /Info:\s*(?:UPI\/)?([^\/.\n]+?)(?:\/|$)/i
      const match = message.match(infoPattern)
      if (match) {
        const merchant = match[1].trim()
        if (merchant && merchant.toUpperCase() !== 'UPI') {
          return this.cleanMerchantName(merchant)
        }
      }
    }

    // Pattern 3: "VPA merchant@bank (Merchant Name)" format
    if (message.toLowerCase().includes('vpa')) {
      // First try to get name in parentheses
      const vpaWithNamePattern = /VPA\s+[^@\s]+@[^\s]+\s*\(([^)]+)\)/i
      const nameMatch = message.match(vpaWithNamePattern)
      if (nameMatch) {
        return this.cleanMerchantName(nameMatch[1].trim())
      }

      // Then try just the VPA username part
      const vpaPattern = /VPA\s+([^@\s]+)@/i
      const vpaMatch = message.match(vpaPattern)
      if (vpaMatch) {
        const vpaName = vpaMatch[1].trim()
        if (vpaName.length > 3 && !/^\d+$/.test(vpaName)) {
          return this.cleanMerchantName(vpaName)
        }
      }
    }

    // Pattern 4: "spent on Card XX1234 at merchant on date" or "Spent Rs.X On HDFC Bank Card X At merchant On date"
    if (message.toLowerCase().includes('spent') && message.toLowerCase().includes('card')) {
      // More flexible pattern to capture merchant between "At" and "On"
      const spentAtPattern = /At\s+([^\s]+(?:\*[^\s]+)?)\s+On\s+/i
      const spentAtMatch = message.match(spentAtPattern)
      if (spentAtMatch) {
        let merchant = spentAtMatch[1].trim()
        // Map known merchant codes
        if (merchant.toUpperCase() === 'RSP*INSTAMART') {
          merchant = 'Swiggy Instamart'
        } else if (merchant.toUpperCase().startsWith('RSP*')) {
          merchant = 'Swiggy ' + merchant.substring(4)
        }
        return this.cleanMerchantName(merchant)
      }
      
      // Original pattern for older format
      const spentPattern = /at\s+([^.\n]+?)\s+on\s+\d{2}/i
      const match = message.match(spentPattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }
    }

    // Pattern 5: "debited for merchant on date"
    if (message.toLowerCase().includes('debited for')) {
      const debitForPattern = /debited\s+for\s+([^.\n]+?)\s+on\s+\d{2}/i
      const match = message.match(debitForPattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }
    }

    // Pattern 6: "To merchant name" (for UPI mandate)
    if (message.toLowerCase().includes('upi mandate')) {
      const mandatePattern = /To\s+([^\n]+?)\s*(?:\n|\d{2}\/\d{2})/i
      const match = message.match(mandatePattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }
    }

    // Fall back to base extraction
    return super.extractMerchant(message, sender)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    // Use base class investment detection
    if (this.isInvestmentTransaction(lowerMessage)) {
      return TransactionType.INVESTMENT
    }

    // Credit card transactions - ONLY if message contains CC or PCC indicators
    // Any transaction with BLOCK CC or BLOCK PCC is a credit card transaction
    if (lowerMessage.includes('block cc') || lowerMessage.includes('block pcc')) {
      return TransactionType.CREDIT
    }

    // Legacy pattern for older format that explicitly says "spent on card"
    if (lowerMessage.includes('spent on card') && !lowerMessage.includes('block dc')) {
      return TransactionType.CREDIT
    }

    // Credit card bill payments (these are regular expenses from bank account)
    if (lowerMessage.includes('payment') && lowerMessage.includes('credit card')) {
      return TransactionType.EXPENSE
    }
    if (lowerMessage.includes('towards') && lowerMessage.includes('credit card')) {
      return TransactionType.EXPENSE
    }

    // HDFC specific: "Sent Rs.X From HDFC Bank"
    if (lowerMessage.includes('sent') && lowerMessage.includes('from hdfc')) {
      return TransactionType.EXPENSE
    }

    // HDFC specific: "Spent Rs.X From HDFC Bank Card" (debit card transactions)
    if (lowerMessage.includes('spent') && lowerMessage.includes('from hdfc bank card')) {
      return TransactionType.EXPENSE
    }

    // Standard expense keywords
    if (lowerMessage.includes('debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('withdrawn') && !lowerMessage.includes('block cc')) return TransactionType.EXPENSE
    if (lowerMessage.includes('spent') && !lowerMessage.includes('card')) return TransactionType.EXPENSE
    if (lowerMessage.includes('charged')) return TransactionType.EXPENSE
    if (lowerMessage.includes('paid')) return TransactionType.EXPENSE
    if (lowerMessage.includes('purchase')) return TransactionType.EXPENSE

    // Income keywords
    if (lowerMessage.includes('credited')) return TransactionType.INCOME
    if (lowerMessage.includes('deposited')) return TransactionType.INCOME
    if (lowerMessage.includes('received')) return TransactionType.INCOME
    if (lowerMessage.includes('refund')) return TransactionType.INCOME
    if (lowerMessage.includes('cashback') && !lowerMessage.includes('earn cashback')) return TransactionType.INCOME

    return null
  }

  protected extractReference(message: string): string | null {
    // HDFC specific reference patterns
    const hdfcPatterns = [
      /Ref\s+(\d{9,12})/i,
      /UPI\s+Ref\s+No\s+(\d{12})/i,
      /Ref\s+No\.?\s+([A-Z0-9]+)/i,
      /(?:Ref|Reference)[:.\s]+([A-Z0-9]{6,})(?:\s*$|\s*Not\s+You)/i
    ]

    for (const pattern of hdfcPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1].trim()
      }
    }

    // Fall back to base extraction
    return super.extractReference(message)
  }

  protected extractAccountLast4(message: string): string | null {
    // HDFC specific patterns
    const hdfcPatterns = [
      /deposited\s+in\s+(?:HDFC\s+Bank\s+)?A\/c\s+(?:XX+)?(\d{4})/i,
      /from\s+(?:HDFC\s+Bank\s+)?A\/c\s+(?:XX+)?(\d{4})/i,
      /HDFC\s+Bank\s+A\/c\s+(\d{4})/i,
      /A\/c\s+(?:XX+)?(\d{4})/i
    ]

    for (const pattern of hdfcPatterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }

    return super.extractAccountLast4(message)
  }

  /**
   * Check if this is an E-Mandate notification (not a transaction)
   */
  isEMandateNotification(message: string): boolean {
    return message.toLowerCase().includes('e-mandate!')
  }

  /**
   * Check if this is a balance update notification (not a transaction)
   */
  isBalanceUpdateNotification(message: string): boolean {
    const lowerMessage = message.toLowerCase()
    
    // Check for balance update patterns
    return (lowerMessage.includes('available bal') || 
            lowerMessage.includes('avl bal') || 
            lowerMessage.includes('account balance') ||
            lowerMessage.includes('a/c balance')) &&
           lowerMessage.includes('as on') &&
           !lowerMessage.includes('debited') &&
           !lowerMessage.includes('credited') &&
           !lowerMessage.includes('withdrawn') &&
           !lowerMessage.includes('spent') &&
           !lowerMessage.includes('transferred')
  }

  /**
   * Parse balance update notification
   */
  parseBalanceUpdate(message: string): { bankName: string; accountLast4: string; balance: number; asOfDate?: string } | null {
    if (!this.isBalanceUpdateNotification(message)) {
      return null
    }
    
    // Extract account last 4 digits
    const accountLast4 = this.extractAccountLast4(message)
    if (!accountLast4) {
      return null
    }
    
    // Extract balance amount - pattern for "is INR 12,678.00"
    const balancePattern = /is\s+INR\s*([\d,]+(?:\.\d{2})?)/i
    const balanceMatch = message.match(balancePattern)
    if (!balanceMatch) {
      return null
    }
    
    const balanceStr = balanceMatch[1].replace(/,/g, '')
    const balance = parseFloat(balanceStr)
    if (isNaN(balance)) {
      return null
    }
    
    // Extract date if present (e.g., "as on yesterday:21-AUG-25")
    const datePattern = /as\s+on\s+(?:yesterday:)?(\d{1,2}-[A-Z]{3}-\d{2})/i
    const dateMatch = message.match(datePattern)
    const asOfDate = dateMatch ? dateMatch[1] : undefined
    
    return {
      bankName: this.config.bankName,
      accountLast4,
      balance,
      asOfDate
    }
  }

  protected isTransactionMessage(message: string): boolean {
    // Skip E-Mandate notifications
    if (this.isEMandateNotification(message)) {
      return false
    }

    // Skip balance update notifications
    if (this.isBalanceUpdateNotification(message)) {
      return false
    }

    // Skip future debit notifications (these are subscription alerts, not transactions)
    if (message.toLowerCase().includes('will be')) {
      return false
    }

    const lowerMessage = message.toLowerCase()

    // Check for payment alerts (current transactions)
    if (lowerMessage.includes('payment alert')) {
      // Make sure it's not a future debit
      if (!lowerMessage.includes('will be')) {
        return true
      }
    }

    // Skip payment request messages
    if (
      lowerMessage.includes('has requested') ||
      lowerMessage.includes('payment request') ||
      lowerMessage.includes('to pay, download') ||
      lowerMessage.includes('collect request') ||
      lowerMessage.includes('ignore if already paid')
    ) {
      return false
    }

    // Skip credit card payment confirmations
    if (lowerMessage.includes('received towards your credit card')) {
      return false
    }

    // Skip credit card payment credited notifications
    if (lowerMessage.includes('payment') && lowerMessage.includes('credited to your card')) {
      return false
    }

    // Skip OTP and promotional messages
    if (
      lowerMessage.includes('otp') ||
      lowerMessage.includes('one time password') ||
      lowerMessage.includes('verification code') ||
      lowerMessage.includes('offer') ||
      lowerMessage.includes('discount') ||
      lowerMessage.includes('cashback offer') ||
      lowerMessage.includes('win ')
    ) {
      return false
    }

    // HDFC specific transaction keywords
    const hdfcTransactionKeywords = [
      'debited',
      'credited',
      'withdrawn',
      'deposited',
      'spent',
      'received',
      'transferred',
      'paid',
      'sent', // HDFC uses "Sent Rs.X From HDFC Bank"
      'deducted', // Add support for "deducted from" pattern
      'txn' // HDFC uses "Txn Rs.X" for card transactions
    ]

    return hdfcTransactionKeywords.some(keyword => lowerMessage.includes(keyword))
  }
}