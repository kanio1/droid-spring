/**
 * Database Seeding
 *
 * Automates environment setup and teardown for tests
 * Provides clean test data for each test run
 */

import { dataCorrelator } from './data-correlator'
import { uniqueDataPool } from './unique-data-pool'
import { ScenarioBuilder } from './scenario-builder'
import { BulkGenerator } from './bulk-generator'
import type { Customer } from '../../../app/schemas/customer'
import type { Order } from '../../../app/schemas/order'
import type { Invoice } from '../../../app/schemas/invoice'
import type { Payment } from '../../../app/schemas/payment'
import type { Subscription } from '../../../app/schemas/subscription'

export interface SeedConfig {
  scenario?: 'fullJourney' | 'enterprise' | 'failedPayments' | 'trialConversion' | 'bulk' | 'custom'
  customerCount?: number
  includeCleanup?: boolean
  seedId?: string // Unique identifier for this seed
}

export interface SeedResult {
  seedId: string
  customers: Customer[]
  orders: Order[]
  invoices: Invoice[]
  payments: Payment[]
  subscriptions: Subscription[]
  dataCorrelator: typeof dataCorrelator
}

class DatabaseSeeder {
  private activeSeeds: Map<string, SeedResult> = new Map()

  /**
   * Seed the database with test data
   */
  async seed(config: SeedConfig = {}): Promise<SeedResult> {
    const seedId = config.seedId || this.generateSeedId()
    const scenario = config.scenario || 'fullJourney'
    const customerCount = config.customerCount || 10

    // Clear previous data
    this.clear()

    let result: SeedResult

    switch (scenario) {
      case 'fullJourney':
        result = await this.seedFullJourney(customerCount, seedId)
        break

      case 'enterprise':
        result = await this.seedEnterprise(customerCount, seedId)
        break

      case 'failedPayments':
        result = await this.seedFailedPayments(customerCount, seedId)
        break

      case 'trialConversion':
        result = await this.seedTrialConversion(customerCount, seedId)
        break

      case 'bulk':
        result = await this.seedBulkData(customerCount, seedId)
        break

      case 'custom':
        throw new Error('Custom scenario requires explicit data provision')

      default:
        throw new Error(`Unknown scenario: ${scenario}`)
    }

    this.activeSeeds.set(seedId, result)

    console.log(`✅ Database seeded with scenario: ${scenario}, seedId: ${seedId}`)
    console.log(`📊 Stats: ${JSON.stringify(result.dataCorrelator.getStatistics(), null, 2)}`)

    return result
  }

  /**
   * Get an active seed by ID
   */
  getSeed(seedId: string): SeedResult | undefined {
    return this.activeSeeds.get(seedId)
  }

  /**
   * Get all active seeds
   */
  getAllSeeds(): SeedResult[] {
    return Array.from(this.activeSeeds.values())
  }

  /**
   * Clean up a specific seed
   */
  async cleanup(seedId: string): Promise<void> {
    const seed = this.activeSeeds.get(seedId)
    if (!seed) {
      console.warn(`Seed ${seedId} not found for cleanup`)
      return
    }

    // Clean up entities from correlator
    for (const customer of seed.customers) {
      await this.deleteCustomer(customer.id)
    }

    this.activeSeeds.delete(seedId)
    uniqueDataPool.clear()
    dataCorrelator.clear()

    console.log(`🧹 Cleaned up seed: ${seedId}`)
  }

  /**
   * Clean up all seeds
   */
  async cleanupAll(): Promise<void> {
    const seedIds = Array.from(this.activeSeeds.keys())
    for (const seedId of seedIds) {
      await this.cleanup(seedId)
    }
    console.log('🧹 Cleaned up all seeds')
  }

  /**
   * Get seed statistics
   */
  getStatistics(): Record<string, any> {
    const stats: Record<string, any> = {
      activeSeeds: this.activeSeeds.size,
      seeds: []
    }

    for (const [seedId, seed] of this.activeSeeds) {
      stats.seeds.push({
        seedId,
        customerCount: seed.customers.length,
        orderCount: seed.orders.length,
        invoiceCount: seed.invoices.length,
        paymentCount: seed.payments.length,
        subscriptionCount: seed.subscriptions.length
      })
    }

    return stats
  }

  private async seedFullJourney(customerCount: number, seedId: string): Promise<SeedResult> {
    const data = ScenarioBuilder.fullCustomerJourney({ customerCount })
    return {
      seedId,
      ...data,
      dataCorrelator
    }
  }

  private async seedEnterprise(customerCount: number, seedId: string): Promise<SeedResult> {
    const data = ScenarioBuilder.enterpriseScenario({ customerCount })
    return {
      seedId,
      ...data,
      dataCorrelator
    }
  }

  private async seedFailedPayments(customerCount: number, seedId: string): Promise<SeedResult> {
    const data = ScenarioBuilder.failedPaymentScenario({ customerCount })
    return {
      seedId,
      ...data,
      dataCorrelator
    }
  }

  private async seedTrialConversion(customerCount: number, seedId: string): Promise<SeedResult> {
    const data = ScenarioBuilder.trialConversionScenario({ customerCount })
    return {
      seedId,
      customers: data.customers,
      orders: [],
      invoices: [],
      payments: [],
      subscriptions: data.subscriptions,
      dataCorrelator
    }
  }

  private async seedBulkData(customerCount: number, seedId: string): Promise<SeedResult> {
    const generator = new BulkGenerator({
      customerCount,
      ordersPerCustomer: 5,
      invoicesPerOrder: 1,
      paymentsPerInvoice: 1,
      subscriptionsPerCustomer: 1,
      includeCancelled: true,
      includeOverdue: true,
      includeFailedPayments: true,
      batchSize: 50
    })

    const data = await generator.generateAll()
    return {
      seedId,
      ...data,
      dataCorrelator
    }
  }

  private generateSeedId(): string {
    const timestamp = Date.now()
    const random = Math.random().toString(36).substring(2, 8)
    return `seed_${timestamp}_${random}`
  }

  private clear(): void {
    dataCorrelator.clear()
    uniqueDataPool.clear()
  }

  // Placeholder for actual API cleanup operations
  // These would be implemented based on the actual API
  private async deleteCustomer(customerId: string): Promise<void> {
    // TODO: Implement actual API call to delete customer
    // This would call the backend API to clean up test data
    console.log(`Would delete customer: ${customerId}`)
  }

  private async deleteOrder(orderId: string): Promise<void> {
    // TODO: Implement actual API call to delete order
    console.log(`Would delete order: ${orderId}`)
  }

  private async deleteInvoice(invoiceId: string): Promise<void> {
    // TODO: Implement actual API call to delete invoice
    console.log(`Would delete invoice: ${invoiceId}`)
  }

  private async deletePayment(paymentId: string): Promise<void> {
    // TODO: Implement actual API call to delete payment
    console.log(`Would delete payment: ${paymentId}`)
  }

  private async deleteSubscription(subscriptionId: string): Promise<void> {
    // TODO: Implement actual API call to delete subscription
    console.log(`Would delete subscription: ${subscriptionId}`)
  }
}

/**
 * Singleton instance of DatabaseSeeder
 */
export const databaseSeeder = new DatabaseSeeder()

/**
 * Convenience functions
 */
export const SeedHelper = {
  /**
   * Quick seed for unit tests
   */
  async forUnitTest(): Promise<SeedResult> {
    return databaseSeeder.seed({
      scenario: 'fullJourney',
      customerCount: 3
    })
  },

  /**
   * Quick seed for integration tests
   */
  async forIntegrationTest(): Promise<SeedResult> {
    return databaseSeeder.seed({
      scenario: 'fullJourney',
      customerCount: 10
    })
  },

  /**
   * Quick seed for E2E tests
   */
  async forE2ETest(): Promise<SeedResult> {
    return databaseSeeder.seed({
      scenario: 'fullJourney',
      customerCount: 5
    })
  },

  /**
   * Quick seed for performance tests
   */
  async forPerformanceTest(): Promise<SeedResult> {
    return databaseSeeder.seed({
      scenario: 'bulk',
      customerCount: 100
    })
  },

  /**
   * Quick seed for enterprise tests
   */
  async forEnterpriseTest(): Promise<SeedResult> {
    return databaseSeeder.seed({
      scenario: 'enterprise',
      customerCount: 5
    })
  }
}
