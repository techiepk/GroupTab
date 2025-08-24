import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const HSBCConfig: BankConfig = {
  bankName: 'HSBC Bank',
  bankCode: 'hsbc',
  senders: {
    exact: ['HSBC', 'HSBCIN'],
    contains: ['HSBC', 'HSBCIN'],
    patterns: [
      /^[A-Z]{2}-HSBCIN-[A-Z]$/,
      /^[A-Z]{2}-HSBC-[A-Z]$/
    ]
  },
  transactionType: {
    expense: ['is paid from', 'is debited', 'withdrawn'],
    income: ['is credited to', 'deposited']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class HSBCBankParser extends BankParser {
  constructor() {
    super(HSBCConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: INR 49.00 is paid from
    // Pattern 2: INR 1000.50 is credited to
    const pattern1 = /INR\s+([\d,]+(?:\.\d{2})?)\s+is\s+(?:paid|credited|debited)/i
    const match1 = message.match(pattern1)
    
    if (match1) {
      const amount = match1[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    // Pattern 3: "used at ... for INR 305.00" (credit card)
    const creditCardPattern = /for\s+INR\s+([\d,]+(?:\.\d{2})?)/i
    const creditCardMatch = message.match(creditCardPattern)
    
    if (creditCardMatch) {
      const amount = creditCardMatch[1].replace(/,/g, '')
      const parsed = parseFloat(amount)
      return isNaN(parsed) ? null : parsed
    }
    
    return super.extractAmount(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 1: "used at [Merchant] for" (credit card)
    const creditCardPattern = /used\s+at\s+([^.]+?)\s+for\s+INR/i
    const creditCardMatch = message.match(creditCardPattern)
    if (creditCardMatch) {
      const merchant = this.cleanMerchantName(creditCardMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }
    
    // Pattern 2: "to [Merchant] on" for payments
    const paymentPattern = /to\s+([^.]+?)\s+on\s+\d/i
    const paymentMatch = message.match(paymentPattern)
    if (paymentMatch) {
      const merchant = this.cleanMerchantName(paymentMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }
    
    // Pattern 3: "from [Merchant]" for credits
    const creditPattern = /from\s+([^.]+?)(?:\s+on\s+|\s+with\s+|$)/i
    const creditMatch = message.match(creditPattern)
    if (creditMatch) {
      const merchant = this.cleanMerchantName(creditMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }
    
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: "creditcard xxxxx1234" or "credit card xxxxx1234"
    const creditCardPattern = /credit\s*card\s+[xX*]+(\d{4})/i
    const creditCardMatch = message.match(creditCardPattern)
    if (creditCardMatch) {
      return creditCardMatch[1]
    }
    
    // Pattern 2: account XXXXXX1234
    const accountPattern = /account\s+[X*]+(\d{4})/i
    const accountMatch = message.match(accountPattern)
    if (accountMatch) {
      return accountMatch[1]
    }
    
    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern: with ref 222222222222
    const pattern = /with\s+ref\s+(\w+)/i
    const match = message.match(pattern)
    if (match) {
      return match[1]
    }
    
    return super.extractReference(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()
    
    // Credit card transactions
    if (lowerMessage.includes('creditcard') || lowerMessage.includes('credit card')) {
      // Credit card transactions that say "used at" are expenses
      if (lowerMessage.includes('used at')) return TransactionType.EXPENSE
      return TransactionType.EXPENSE
    }
    
    if (lowerMessage.includes('is paid from')) return TransactionType.EXPENSE
    if (lowerMessage.includes('is debited')) return TransactionType.EXPENSE
    if (lowerMessage.includes('is credited to')) return TransactionType.INCOME
    if (lowerMessage.includes('deposited')) return TransactionType.INCOME
    
    return super.extractTransactionType(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()
    
    // Check for HSBC-specific transaction keywords
    if (lowerMessage.includes('is paid from') ||
        lowerMessage.includes('is credited to') ||
        lowerMessage.includes('is debited') ||
        (lowerMessage.includes('creditcard') && lowerMessage.includes('used at')) ||
        (lowerMessage.includes('credit card') && lowerMessage.includes('used at')) ||
        (lowerMessage.includes('inr') && lowerMessage.includes('account'))) {
      return true
    }
    
    return super.isTransactionMessage(message)
  }

  protected isValidMerchantName(name: string): boolean {
    return name.length > 0 && name.length < 100 && !name.match(/^[0-9]+$/)
  }
}