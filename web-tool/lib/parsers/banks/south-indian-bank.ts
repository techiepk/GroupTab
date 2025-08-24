import { BankParser } from '../base'
import { BankConfig, TransactionType } from '../types'

const SouthIndianBankConfig: BankConfig = {
  bankName: 'South Indian Bank',
  bankCode: 'sib',
  senders: {
    exact: [
      'SIBSMS', 
      'AD-SIBSMS', 
      'CP-SIBSMS',
      'SIBSMS-S',
      'AD-SIBSMS-S',
      'CP-SIBSMS-S',
      'SOUTHINDIANBANK',
      'SIBBANK'
    ],
    contains: ['SIBSMS', 'SIBBANK'],
    patterns: [
      /^AD-SIB.*$/,
      /^CP-SIB.*$/,
      /^VM-SIB.*$/
    ]
  },
  transactionType: {
    expense: ['debit', 'withdrawn', 'spent', 'purchase', 'paid', 'transfer to'],
    income: ['credit', 'deposited', 'received', 'refund', 'transfer from', 'cashback']
  },
  skipPatterns: [
    'otp',
    'one time password',
    'verification code',
    'offer',
    'discount'
  ]
}

export class SouthIndianBankParser extends BankParser {
  constructor() {
    super(SouthIndianBankConfig)
  }

  protected extractMerchant(message: string, sender: string): string | null {
    // For UPI transactions, try to extract UPI ID or merchant name
    if (message.toLowerCase().includes('upi')) {
      // Pattern for "Info:UPI/IPOS/number/MERCHANT NAME on" format
      const infoPattern = /Info:UPI\/[^\/]+\/[^\/]+\/([^\/]+?)\s+on/i
      const infoMatch = message.match(infoPattern)
      if (infoMatch) {
        const merchant = infoMatch[1].trim()
        if (merchant) {
          return this.cleanMerchantName(merchant)
        }
      }

      // Check for "to" pattern (e.g., "to merchant@upi")
      const toPattern = /to\s+([^,\s]+(?:@[^\s,]+)?)/i
      const toMatch = message.match(toPattern)
      if (toMatch) {
        const merchant = toMatch[1].trim()
        if (merchant) {
          return this.cleanMerchantName(merchant)
        }
      }

      // Check for "from" pattern for incoming transfers
      if (message.toLowerCase().includes('credit')) {
        const fromPattern = /from\s+([^,\s]+(?:@[^\s,]+)?)/i
        const fromMatch = message.match(fromPattern)
        if (fromMatch) {
          const merchant = fromMatch[1].trim()
          if (merchant) {
            return this.cleanMerchantName(merchant)
          }
        }
      }

      // Default to UPI Transaction
      return 'UPI Transaction'
    }

    // For ATM withdrawals
    if (message.toLowerCase().includes('atm') || message.toLowerCase().includes('withdrawn')) {
      return 'ATM'
    }

    // For card transactions
    if (message.toLowerCase().includes('card')) {
      // Try to extract merchant after "at"
      const atPattern = /at\s+([^,\n]+?)(?:\s+on|\s*,|$)/i
      const match = message.match(atPattern)
      if (match) {
        const merchant = match[1].trim()
        if (merchant) {
          return this.cleanMerchantName(merchant)
        }
      }
    }

    return super.extractMerchant(message, sender)
  }

  protected extractReference(message: string): string | null {
    // Pattern for RRN (e.g., "RRN:523273398527")
    const rrnPattern = /RRN[:\s]*(\d{12})/i
    const rrnMatch = message.match(rrnPattern)
    if (rrnMatch) {
      return rrnMatch[1].trim()
    }

    // Pattern for reference number
    const refPattern = /Ref(?:erence)?[:\s]*([A-Z0-9]+)/i
    const refMatch = message.match(refPattern)
    if (refMatch) {
      return refMatch[1].trim()
    }

    return super.extractReference(message)
  }

  protected extractAccountLast4(message: string): string | null {
    // Pattern for "A/c X1234" or "A/c XX1234" or "A/c XXX1234"
    const patterns = [
      /A\/c\s+[X*]*(\d{4})/i,
      /Account\s+[X*]*(\d{4})/i,
      /from\s+[X*]*(\d{4})/i,
      /to\s+[X*]*(\d{4})/i
    ]

    for (const pattern of patterns) {
      const match = message.match(pattern)
      if (match) {
        return match[1]
      }
    }

    return super.extractAccountLast4(message)
  }

  protected extractBalance(message: string): number | null {
    // Pattern for "Bal:Rs.1234.17" or "Balance:Rs.1234.17" or "Final balance is Rs.1234.17"
    const patterns = [
      /Final\s+balance\s+is\s+Rs\.?\s*([\d,]+(?:\.\d{2})?)/i,
      /Bal(?:ance)?[:\s]*Rs\.?\s*([\d,]+(?:\.\d{2})?)/i,
      /Available\s+Bal(?:ance)?[:\s]*Rs\.?\s*([\d,]+(?:\.\d{2})?)/i,
      /Avl\s+Bal[:\s]*Rs\.?\s*([\d,]+(?:\.\d{2})?)/i
    ]

    for (const pattern of patterns) {
      const match = message.match(pattern)
      if (match) {
        const balanceStr = match[1].replace(/,/g, '')
        const balance = parseFloat(balanceStr)
        if (!isNaN(balance)) {
          return balance
        }
      }
    }

    return super.extractBalance(message)
  }

  /**
   * Extract date and time from message.
   * Format: "20-08-25 12:13:23" (YY-MM-DD HH:MM:SS)
   */
  protected extractDateTime(message: string): Date | null {
    // Pattern for "20-08-25 12:13:23" format
    const dateTimePattern = /(\d{2}-\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2})/
    const match = message.match(dateTimePattern)
    
    if (match) {
      const dateStr = match[1]
      const timeStr = match[2]
      
      try {
        // Parse YY-MM-DD format
        const [yearStr, monthStr, dayStr] = dateStr.split('-')
        const year = 2000 + parseInt(yearStr)
        const month = parseInt(monthStr) - 1 // JavaScript months are 0-indexed
        const day = parseInt(dayStr)
        
        // Parse HH:MM:SS format
        const [hourStr, minuteStr, secondStr] = timeStr.split(':')
        const hour = parseInt(hourStr)
        const minute = parseInt(minuteStr)
        const second = parseInt(secondStr)
        
        return new Date(year, month, day, hour, minute, second)
      } catch (e) {
        return null
      }
    }
    
    return null
  }

  protected isTransactionMessage(message: string): boolean {
    const lowerMessage = message.toLowerCase()

    // Skip OTP and promotional messages
    if (
      lowerMessage.includes('otp') ||
      lowerMessage.includes('one time password') ||
      lowerMessage.includes('verification code') ||
      lowerMessage.includes('offer') ||
      lowerMessage.includes('discount')
    ) {
      return false
    }

    // Skip UPI auto-pay scheduled reminders
    if (lowerMessage.includes('upi auto pay') && 
        lowerMessage.includes('is scheduled on')) {
      return false
    }

    // Check for transaction keywords
    const transactionKeywords = [
      'debit',
      'credit',
      'withdrawn',
      'deposited',
      'spent',
      'received',
      'transferred',
      'paid',
      'purchase',
      'refund',
      'cashback',
      'upi'
    ]

    return transactionKeywords.some(keyword => lowerMessage.includes(keyword))
  }
}