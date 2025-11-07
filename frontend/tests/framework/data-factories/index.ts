/**
 * Test Data Factories - Central Export
 *
 * Comprehensive collection of Object Mother pattern implementations
 * for generating realistic test data across all BSS modules
 *
 * Enhanced Phase 2 Features:
 * - Data Correlator: Manages entity relationships and validates data consistency
 * - Scenario Builder: Predefined test scenarios for common use cases
 * - Unique Data Pool: Prevents test data collisions
 * - Bulk Generator: Efficiently generates large volumes of test data
 * - Database Seeding: Automates environment setup and teardown
 *
 * Usage:
 * ```typescript
 * import { CustomerFactory, OrderFactory, ScenarioBuilder } from '@/tests/framework/data-factories'
 *
 * const customer = CustomerFactory.create().active().build()
 * const order = OrderFactory.create().withCustomer(customer).build()
 *
 * // Use scenario builder for complete workflows
 * const data = ScenarioBuilder.fullCustomerJourney({ customerCount: 10 })
 *
 * // Use database seeder for test setup
 * const seed = await databaseSeeder.seed({ scenario: 'fullJourney' })
 * ```
 */

// Core Factory Exports
// Customer Factories
export { CustomerFactory, CustomerProfiles, type CustomerFactoryOptions, type CustomerStatus }
  from './customer.factory'

// Order Factories
export { OrderFactory, OrderProfiles, OrderItemFactory, type OrderFactoryOptions, type OrderStatus }
  from './order.factory'

// Invoice Factories
export { InvoiceFactory, InvoiceProfiles, InvoiceLineItemFactory, type InvoiceFactoryOptions, type InvoiceStatus }
  from './invoice.factory'

// Payment Factories
export { PaymentFactory, PaymentProfiles, type PaymentFactoryOptions, type PaymentStatus, type PaymentMethod }
  from './payment.factory'

// Subscription Factories
export { SubscriptionFactory, SubscriptionProfiles, type SubscriptionFactoryOptions, type SubscriptionStatus }
  from './subscription.factory'

// Enhanced Data Factory Exports
// Data Correlator - Manages entity relationships
export { DataCorrelator, dataCorrelator, type EntityRelationship }
  from './data-correlator'

// Scenario Builder - Predefined test scenarios
export { ScenarioBuilder, type ScenarioConfig }
  from './scenario-builder'

// Unique Data Pool - Prevents data collisions
export { UniqueDataPool, uniqueDataPool, DataGenerators }
  from './unique-data-pool'

// Bulk Generator - Large-scale data generation
export { BulkGenerator, generateBulkData, type BulkGenerationConfig }
  from './bulk-generator'

// Database Seeding - Environment automation
export { DatabaseSeeder, databaseSeeder, SeedHelper, type SeedConfig, type SeedResult }
  from './database-seeding'

/**
 * TestDataGenerator - Enhanced with Phase 2 features
 *
 * Provides convenient methods for generating test data using the new infrastructure
 * Integrates with DataCorrelator, ScenarioBuilder, and BulkGenerator
 *
 * Usage:
 * ```typescript
 * // Legacy API - still supported
 * const { customer, order } = TestDataGenerator.fullCustomerJourney()
 *
 * // Enhanced API - using scenario builder
 * const data = TestDataGenerator.scenario.fullCustomerJourney({ customerCount: 10 })
 *
 * // Bulk generation
 * const bulkData = await TestDataGenerator.generateBulk({
 *   customerCount: 100,
 *   ordersPerCustomer: 3
 * })
 *
 * // Database seeding
 * const seed = await TestDataGenerator.seed({ scenario: 'enterprise' })
 * ```
 */
export class TestDataGenerator {
  // Legacy API - Maintained for backward compatibility
  static fullCustomerJourney(options: {
    customerStatus?: 'active' | 'pending' | 'suspended'
    orderStatus?: 'pending' | 'processing' | 'delivered'
    invoiceStatus?: 'draft' | 'pending' | 'paid'
    paymentStatus?: 'pending' | 'processing' | 'completed'
    subscriptionStatus?: 'active' | 'trial' | 'cancelled'
    includeOrder?: boolean
    includeInvoice?: boolean
    includePayment?: boolean
    includeSubscription?: boolean
  } = {}) {
    const {
      customerStatus = 'active',
      orderStatus = 'delivered',
      invoiceStatus = 'paid',
      paymentStatus = 'completed',
      subscriptionStatus = 'active',
      includeOrder = true,
      includeInvoice = true,
      includePayment = true,
      includeSubscription = true
    } = options

    // Create customer
    const customer = CustomerFactory.create()
      [customerStatus]()
      .build()

    // Create order (if included)
    const order = includeOrder
      ? OrderFactory.create()
          .withCustomer(customer)
          [orderStatus]()
          .build()
      : null

    // Create invoice (if included)
    const invoice = includeInvoice
      ? InvoiceFactory.create()
          .withCustomer(customer)
          .withOrder(order || undefined)
          [invoiceStatus]()
          .build()
      : null

    // Create payment (if included)
    const payment = includePayment && invoice
      ? PaymentFactory.create()
          .withCustomer(customer)
          .withInvoice(invoice)
          [paymentStatus]()
          .build()
      : null

    // Create subscription (if included)
    const subscription = includeSubscription
      ? SubscriptionFactory.create()
          .withCustomer(customer)
          [subscriptionStatus]()
          .build()
      : null

    // Register in data correlator
    dataCorrelator.registerEntity('customer', customer)
    if (order) {
      dataCorrelator.registerEntity('order', order)
      dataCorrelator.linkEntities('customer', customer.id, 'order', order.id)
    }
    if (invoice) {
      dataCorrelator.registerEntity('invoice', invoice)
      if (order) {
        dataCorrelator.linkEntities('order', order.id, 'invoice', invoice.id)
      }
      dataCorrelator.linkEntities('customer', customer.id, 'invoice', invoice.id)
    }
    if (payment) {
      dataCorrelator.registerEntity('payment', payment)
      if (invoice) {
        dataCorrelator.linkEntities('invoice', invoice.id, 'payment', payment.id)
      }
      dataCorrelator.linkEntities('customer', customer.id, 'payment', payment.id)
    }
    if (subscription) {
      dataCorrelator.registerEntity('subscription', subscription)
      dataCorrelator.linkEntities('customer', customer.id, 'subscription', subscription.id)
    }

    return {
      customer,
      order,
      invoice,
      payment,
      subscription
    }
  }

  // ========== Enhanced API ==========

  /**
   * Access to Scenario Builder methods
   */
  static scenario = {
    /**
     * Create a full customer journey with multiple customers
     */
    fullCustomerJourney: (config?: ScenarioConfig) => ScenarioBuilder.fullCustomerJourney(config),

    /**
     * Enterprise scenario with large orders
     */
    enterprise: (config?: ScenarioConfig) => ScenarioBuilder.enterpriseScenario(config),

    /**
     * Failed payment scenario
     */
    failedPayments: (config?: ScenarioConfig) => ScenarioBuilder.failedPaymentScenario(config),

    /**
     * Trial to conversion scenario
     */
    trialConversion: (config?: ScenarioConfig) => ScenarioBuilder.trialConversionScenario(config),

    /**
     * Bulk data for performance testing
     */
    bulk: (config?: ScenarioConfig) => ScenarioBuilder.bulkDataScenario(config)
  }

  /**
   * Generate bulk data using BulkGenerator
   */
  static async generateBulk(config: BulkGenerationConfig) {
    return generateBulkData(config)
  }

  /**
   * Database seeding methods
   */
  static async seed(config: SeedConfig) {
    return databaseSeeder.seed(config)
  }

  static async seedForUnitTest() {
    return SeedHelper.forUnitTest()
  }

  static async seedForIntegrationTest() {
    return SeedHelper.forIntegrationTest()
  }

  static async seedForE2ETest() {
    return SeedHelper.forE2ETest()
  }

  static async seedForPerformanceTest() {
    return SeedHelper.forPerformanceTest()
  }

  static async seedForEnterpriseTest() {
    return SeedHelper.forEnterpriseTest()
  }

  // Legacy methods - maintained for backward compatibility
  static generateBatch(count: number, options: Parameters<typeof TestDataGenerator.fullCustomerJourney>[0] = {}) {
    return Array.from({ length: count }, () => this.fullCustomerJourney(options))
  }

  static enterpriseScenario() {
    return this.scenario.enterprise()
  }

  static failedPaymentScenario() {
    return this.scenario.failedPayments()
  }

  static trialConversionScenario() {
    return this.scenario.trialConversion()
  }
}
