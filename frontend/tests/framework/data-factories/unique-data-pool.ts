/**
 * Unique Data Pool
 *
 * Prevents test data collisions by maintaining unique pools
 * Tracks used data and ensures no duplicates across test runs
 */

import { faker } from '@faker-js/faker'

interface UsedData {
  emails: Set<string>
  phoneNumbers: Set<string>
  invoiceNumbers: Set<string>
  paymentTransactionIds: Set<string>
  orderNumbers: Set<string>
  subscriptionNumbers: Set<string>
  customIds: Map<string, Set<string>>
}

class UniqueDataPool {
  private usedData: UsedData = {
    emails: new Set(),
    phoneNumbers: new Set(),
    invoiceNumbers: new Set(),
    paymentTransactionIds: new Set(),
    orderNumbers: new Set(),
    subscriptionNumbers: new Set(),
    customIds: new Map()
  }

  private getRandomUnique(
    type: keyof UsedData,
    generator: () => string,
    maxAttempts: number = 100
  ): string {
    for (let i = 0; i < maxAttempts; i++) {
      const value = generator()
      if (!this.usedData[type].has(value)) {
        this.usedData[type].add(value)
        return value
      }
    }
    throw new Error(`Could not generate unique ${type} after ${maxAttempts} attempts`)
  }

  /**
   * Generate a unique email address
   */
  getUniqueEmail(prefix: string = 'test', domain: string = 'example.test'): string {
    return this.getRandomUnique('emails', () => {
      const timestamp = Date.now()
      const random = faker.string.alphanumeric(8)
      return `${prefix}.${random}.${timestamp}@${domain}`
    })
  }

  /**
   * Generate a unique phone number
   */
  getUniquePhone(): string {
    return this.getRandomUnique('phoneNumbers', () => {
      // Generate phone in format +1-555-0XXX
      const number = faker.string.numeric(4)
      return `+1-555-01${number}`
    })
  }

  /**
   * Generate a unique invoice number
   */
  getUniqueInvoiceNumber(): string {
    return this.getRandomUnique('invoiceNumbers', () => {
      const year = new Date().getFullYear()
      const num = faker.string.numeric(6)
      return `INV-${year}-${num}`
    })
  }

  /**
   * Generate a unique payment transaction ID
   */
  getUniqueTransactionId(): string {
    return this.getRandomUnique('paymentTransactionIds', () => {
      const timestamp = Date.now()
      const random = faker.string.alphanumeric(16)
      return `txn_${timestamp}_${random}`
    })
  }

  /**
   * Generate a unique order number
   */
  getUniqueOrderNumber(): string {
    return this.getRandomUnique('orderNumbers', () => {
      const year = new Date().getFullYear()
      const num = faker.string.numeric(8)
      return `ORD-${year}-${num}`
    })
  }

  /**
   * Generate a unique subscription number
   */
  getUniqueSubscriptionNumber(): string {
    return this.getRandomUnique('subscriptionNumbers', () => {
      const timestamp = Date.now()
      const random = faker.string.alphanumeric(12)
      return `SUB-${timestamp}-${random}`
    })
  }

  /**
   * Register a custom ID to track uniqueness
   */
  registerCustomId(category: string, value: string): void {
    if (!this.usedData.customIds.has(category)) {
      this.usedData.customIds.set(category, new Set())
    }
    this.usedData.customIds.get(category)!.add(value)
  }

  /**
   * Check if a custom ID is already used
   */
  isCustomIdUsed(category: string, value: string): boolean {
    return this.usedData.customIds.get(category)?.has(value) || false
  }

  /**
   * Generate a unique custom ID
   */
  getUniqueCustomId(category: string, prefix: string = ''): string {
    return this.getRandomUnique('customIds' as any, () => {
      const timestamp = Date.now()
      const random = faker.string.alphanumeric(8)
      return `${prefix}${prefix ? '-' : ''}${category}-${timestamp}-${random}`
    })
  }

  /**
   * Generate a unique UUID (just an alias for clarity)
   */
  getUniqueUUID(): string {
    const uuid = faker.string.uuid()
    this.registerCustomId('uuid', uuid)
    return uuid
  }

  /**
   * Get statistics about used data
   */
  getStatistics(): Record<string, number> {
    const stats: Record<string, number> = {}

    for (const [key, value] of Object.entries(this.usedData)) {
      if (value instanceof Set) {
        stats[key] = value.size
      } else if (value instanceof Map) {
        stats[`${key} total`] = Array.from(value.values()).reduce((sum, set) => sum + set.size, 0)
        for (const [category, set] of value) {
          stats[`${key}.${category}`] = set.size
        }
      }
    }

    return stats
  }

  /**
   * Check if data is unique
   */
  isEmailUnique(email: string): boolean {
    return !this.usedData.emails.has(email)
  }

  isPhoneUnique(phone: string): boolean {
    return !this.usedData.phoneNumbers.has(phone)
  }

  isInvoiceNumberUnique(invoiceNumber: string): boolean {
    return !this.usedData.invoiceNumbers.has(invoiceNumber)
  }

  isTransactionIdUnique(transactionId: string): boolean {
    return !this.usedData.paymentTransactionIds.has(transactionId)
  }

  isOrderNumberUnique(orderNumber: string): boolean {
    return !this.usedData.orderNumbers.has(orderNumber)
  }

  isSubscriptionNumberUnique(subscriptionNumber: string): boolean {
    return !this.usedData.subscriptionNumbers.has(subscriptionNumber)
  }

  /**
   * Clear all tracked data
   * WARNING: This should only be used in test teardown or setup
   */
  clear(): void {
    for (const key in this.usedData) {
      const value = this.usedData[key as keyof UsedData]
      if (value instanceof Set) {
        value.clear()
      } else if (value instanceof Map) {
        value.clear()
      }
    }
  }

  /**
   * Export used data for debugging
   */
  exportUsedData(): Record<string, string[]> {
    const exportData: Record<string, string[]> = {}

    for (const [key, value] of Object.entries(this.usedData)) {
      if (value instanceof Set) {
        exportData[key] = Array.from(value)
      } else if (value instanceof Map) {
        exportData[key] = []
        for (const [, set] of value) {
          exportData[key].push(...Array.from(set))
        }
      }
    }

    return exportData
  }
}

/**
 * Singleton instance of UniqueDataPool
 */
export const uniqueDataPool = new UniqueDataPool()

/**
 * Helper functions for factories
 */
export const DataGenerators = {
  /**
   * Generate a unique email for a customer
   */
  uniqueCustomerEmail(): string {
    return uniqueDataPool.getUniqueEmail('customer')
  },

  /**
   * Generate a unique phone for a customer
   */
  uniqueCustomerPhone(): string {
    return uniqueDataPool.getUniquePhone()
  },

  /**
   * Generate a unique order number
   */
  uniqueOrderNumber(): string {
    return uniqueDataPool.getUniqueOrderNumber()
  },

  /**
   * Generate a unique invoice number
   */
  uniqueInvoiceNumber(): string {
    return uniqueDataPool.getUniqueInvoiceNumber()
  },

  /**
   * Generate a unique payment transaction ID
   */
  uniqueTransactionId(): string {
    return uniqueDataPool.getUniqueTransactionId()
  },

  /**
   * Generate a unique subscription number
   */
  uniqueSubscriptionNumber(): string {
    return uniqueDataPool.getUniqueSubscriptionNumber()
  }
}
