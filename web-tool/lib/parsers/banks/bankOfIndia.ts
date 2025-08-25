import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const BankOfIndiaConfig: BankConfig = {
  bankName: 'Bank of India',
  bankCode: 'boi',
  senders: {
    exact: ['BOIIND', 'BOIBNK'],
    contains: [],
    patterns: [
      /^[A-Z]{2}-BOIIND-[ST]$/,
      /^[A-Z]{2}-BOIBNK-[ST]$/,
      /^[A-Z]{2}-BOI-[ST]$/,
      /^[A-Z]{2}-BOIIND$/,
      /^[A-Z]{2}-BOIBNK$/,
      /^[A-Z]{2}-BOI$/,
      /^BK-BOIIND.*$/,
      /^JD-BOIIND.*$/
    ]
  },
  transactionType: {
    expense: ['debited', 'withdrawn', 'spent', 'charged', 'paid', 'purchase', 'transferred'],
    income: ['credited', 'deposited', 'received', 'refund', 'cashback']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount',
    'cashback offer',
    'win '
  ]
}

export class BankOfIndiaParser extends BankParser {
  constructor() {
    super(BankOfIndiaConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: Rs.200.00 debited/credited
    const rsPattern = /Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:debited|credited)/i
    const rsMatch = message.match(rsPattern)
    if (rsMatch) {
      const amountStr = rsMatch[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) {
        return amount
      }
    }

    // Pattern 2: INR format
    const inrPattern = /INR\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:debited|credited)/i
    const inrMatch = message.match(inrPattern)
    if (inrMatch) {
      const amountStr = inrMatch[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) {
        return amount
      }
    }

    // Pattern 3: withdrawn Rs 500
    const withdrawnPattern = /withdrawn\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    const withdrawnMatch = message.match(withdrawnPattern)
    if (withdrawnMatch) {
      const amountStr = withdrawnMatch[1].replace(/,/g, '')
      const amount = parseFloat(amountStr)
      if (!isNaN(amount)) {
        return amount
      }
    }

    // Fall back to base class patterns
    return super.extractAmount(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    const lowerMessage = message.toLowerCase()

    // BOI specific: "debited A/c... and credited to" pattern indicates expense
    if (lowerMessage.includes('debited') && lowerMessage.includes('and credited to')) {
      return TransactionType.EXPENSE
    }

    // BOI specific: "credited A/c... and debited from" pattern indicates income
    if (lowerMessage.includes('credited') && lowerMessage.includes('and debited from')) {
      return TransactionType.INCOME
    }

    // Standard patterns
    return super.extractTransactionType(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // Pattern 1: "credited to MERCHANT via UPI" (for debits)
    const creditedToPattern = /credited\s+to\s+([^.\n]+?)(?:\s+via|\s+Ref|\s+on|$)/i
    const creditedToMatch = message.match(creditedToPattern)
    if (creditedToMatch) {
      const merchant = this.cleanMerchantName(creditedToMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 2: "debited from MERCHANT via UPI" (for credits)
    const debitedFromPattern = /debited\s+from\s+([^.\n]+?)(?:\s+via|\s+Ref|\s+on|$)/i
    const debitedFromMatch = message.match(debitedFromPattern)
    if (debitedFromMatch) {
      const merchant = this.cleanMerchantName(debitedFromMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 3: ATM withdrawal
    if (/ATM/i.test(message) || /withdrawn/i.test(message)) {
      const atmPattern = /(?:ATM|withdrawn)\s+(?:at\s+)?([^.\n]+?)(?:\s+on|\s+Ref|$)/i
      const atmMatch = message.match(atmPattern)
      if (atmMatch) {
        const location = this.cleanMerchantName(atmMatch[1].trim())
        if (this.isValidMerchantName(location)) {
          return `ATM - ${location}`
        }
      }
      return 'ATM'
    }

    // Pattern 4: "to MERCHANT" (generic)
    const toPattern = /to\s+([^.\n]+?)(?:\s+via|\s+Ref|\s+on|$)/i
    const toMatch = message.match(toPattern)
    if (toMatch) {
      const merchant = this.cleanMerchantName(toMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Pattern 5: "from MERCHANT" (generic)
    const fromPattern = /from\s+([^.\n]+?)(?:\s+via|\s+Ref|\s+on|$)/i
    const fromMatch = message.match(fromPattern)
    if (fromMatch) {
      const merchant = this.cleanMerchantName(fromMatch[1].trim())
      if (this.isValidMerchantName(merchant)) {
        return merchant
      }
    }

    // Fall back to base class patterns
    return super.extractMerchant(message, sender)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern 1: A/cXX5468 or A/c XX5468 (BOI format)
    const accountPattern = /A\/c\s*(?:XX|X\*+)?(\d{4})/i
    const accountMatch = message.match(accountPattern)
    if (accountMatch) {
      return accountMatch[1]
    }

    // Pattern 2: Account ending 1234
    const endingPattern = /(?:Account|A\/c)\s+ending\s+(\d{4})/i
    const endingMatch = message.match(endingPattern)
    if (endingMatch) {
      return endingMatch[1]
    }

    // Pattern 3: A/c No. XX1234
    const accountNoPattern = /A\/c\s+No\.?\s*(?:XX|X\*+)?(\d{4})/i
    const accountNoMatch = message.match(accountNoPattern)
    if (accountNoMatch) {
      return accountNoMatch[1]
    }

    // Fall back to base class
    return super.extractAccountLast4(message)
  }

  protected extractReference(message: string): string | null {
    // Pattern 1: Ref No 315439383341 (BOI format)
    const refNoPattern = /Ref\s+No\.?\s*(\d+)/i
    const refNoMatch = message.match(refNoPattern)
    if (refNoMatch) {
      return refNoMatch[1]
    }

    // Pattern 2: Reference: 123456
    const referencePattern = /Reference[:\s]+(\w+)/i
    const referenceMatch = message.match(referencePattern)
    if (referenceMatch) {
      return referenceMatch[1]
    }

    // Pattern 3: Txn ID/Txn#
    const txnPattern = /Txn\s*(?:ID|#)[:\s]*(\w+)/i
    const txnMatch = message.match(txnPattern)
    if (txnMatch) {
      return txnMatch[1]
    }

    // Pattern 4: UPI reference
    const upiPattern = /UPI[:\s]+(\d+)/i
    const upiMatch = message.match(upiPattern)
    if (upiMatch) {
      return upiMatch[1]
    }

    // Fall back to base class
    return super.extractReference(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern 1: Bal: Rs 1000.00
    const balRsPattern = /Bal[:\s]+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    const balRsMatch = message.match(balRsPattern)
    if (balRsMatch) {
      const balanceStr = balRsMatch[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    // Pattern 2: Available Balance: Rs 1000.00
    const availableBalPattern = /Available\s+Balance[:\s]+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    const availableBalMatch = message.match(availableBalPattern)
    if (availableBalMatch) {
      const balanceStr = availableBalMatch[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    // Pattern 3: Avl Bal Rs 1000.00
    const avlBalPattern = /Avl\s+Bal[:\s]+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)/i
    const avlBalMatch = message.match(avlBalPattern)
    if (avlBalMatch) {
      const balanceStr = avlBalMatch[1].replace(/,/g, '')
      const balance = parseFloat(balanceStr)
      if (!isNaN(balance)) {
        return balance
      }
    }

    // Fall back to base class
    return super.extractBalance(message)
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip if it contains the customer care message but ensure it's a transaction
    if (lowerMessage.includes('call') && lowerMessage.includes('if not done by you')) {
      // This is likely a transaction message with a security notice
      // Check if it contains transaction keywords
      if (
        lowerMessage.includes('debited') ||
        lowerMessage.includes('credited') ||
        lowerMessage.includes('withdrawn') ||
        lowerMessage.includes('transferred')
      ) {
        return true
      }
    }

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

    // Fall back to base class for standard checks
    return super.isTransactionMessage(message)
  }
}