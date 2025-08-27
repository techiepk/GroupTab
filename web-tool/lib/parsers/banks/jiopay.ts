import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const JioPayConfig: BankConfig = {
  bankName: 'JioPay',
  bankCode: 'jiopay',
  senders: {
    exact: ['JA-JIOPAY-S'],
    contains: ['JIOPAY'],
    patterns: []
  },
  transactionType: {
    expense: ['recharge successful', 'payment successful', 'bill payment'],
    income: []  // JioPay wallet transactions are marked as CREDIT to avoid double-counting
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

/**
 * Parser for JioPay wallet transactions.
 * Handles messages from JA-JioPay-S and similar senders.
 * 
 * Note: Wallet transactions are marked as CREDIT to avoid double-counting
 * (money already counted when loading wallet from bank account)
 */
export class JioPayParser extends BankParser {
  constructor() {
    super(JioPayConfig)
  }

  protected extractAmount(message: string): number | null {
    // Pattern 1: "Plan Name : 249.00"
    const planPattern = /Plan\s+Name\s*:\s*([0-9,]+(?:\.\d{2})?)/i
    let match = message.match(planPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Pattern 2: "Rs. 249.00" or "Rs 249"
    const rsPattern = /Rs\.?\s*([0-9,]+(?:\.\d{2})?)/i
    match = message.match(rsPattern)
    if (match) {
      const amount = match[1].replace(/,/g, '')
      const parsedAmount = parseFloat(amount)
      if (!isNaN(parsedAmount)) {
        return parsedAmount
      }
    }

    // Fall back to base class patterns
    return super.extractAmount(message)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    const lowerMessage = message.toLowerCase()

    // Jio Recharge
    if (lowerMessage.includes('recharge successful') && lowerMessage.includes('jio number')) {
      // Extract the phone number for reference
      const numberPattern = /Jio\s+Number\s*:\s*(\d{10})/i
      const match = message.match(numberPattern)
      if (match) {
        const number = match[1]
        return `Jio Recharge - ${number.substring(0, 4)}****`
      } else {
        return 'Jio Recharge'
      }
    }

    // Bill payment patterns
    if (lowerMessage.includes('bill payment')) {
      if (lowerMessage.includes('electricity')) return 'Electricity Bill'
      if (lowerMessage.includes('water')) return 'Water Bill'
      if (lowerMessage.includes('gas')) return 'Gas Bill'
      if (lowerMessage.includes('broadband')) return 'Broadband Bill'
      if (lowerMessage.includes('dth')) return 'DTH Recharge'
      return 'Bill Payment'
    }

    // Other recharges
    if (lowerMessage.includes('recharge')) {
      if (lowerMessage.includes('mobile')) return 'Mobile Recharge'
      if (lowerMessage.includes('dth')) return 'DTH Recharge'
      if (lowerMessage.includes('data')) return 'Data Recharge'
      return 'Recharge'
    }

    // Payment to merchant
    if (lowerMessage.includes('payment successful to')) {
      const toPattern = /payment\s+successful\s+to\s+([^.\n]+)/i
      const match = message.match(toPattern)
      if (match) {
        return this.cleanMerchantName(match[1].trim())
      }
      return 'JioPay Payment'
    }

    return super.extractMerchant(message, sender) || 'JioPay Transaction'
  }

  protected extractReference(message: string): string | null {
    // Pattern: "Transaction ID : BR000CAUBYON"
    const txnPattern = /Transaction\s+ID\s*:\s*([A-Z0-9]+)/i
    const match = message.match(txnPattern)
    if (match) {
      return match[1]
    }

    return super.extractReference(message)
  }

  protected extractTransactionType(message: string): TransactionType | null {
    // All JioPay wallet transactions are marked as CREDIT
    // to avoid double-counting (money was already debited when loading wallet)
    return TransactionType.CREDIT
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // JioPay messages don't use standard transaction keywords
    // but "recharge successful" indicates a transaction
    if (lowerMessage.includes('recharge successful')) {
      return true
    }

    return super.isTransactionMessage(message)
  }
}