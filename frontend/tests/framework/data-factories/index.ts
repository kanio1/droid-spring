/**
 * Test Data Factories - Central Export
 *
 * Comprehensive collection of Object Mother pattern implementations
 * for generating realistic test data across all BSS modules
 *
 * Usage:
 * ```typescript
 * import { CustomerFactory, OrderFactory } from '@/tests/framework/data-factories'
 *
 * const customer = CustomerFactory.create().active().build()
 * const order = OrderFactory.create().withCustomer(customer).build()
 * ```
 */

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

/**
 * Utility function to generate a complete test dataset
 * Creates a full customer journey with related entities
 *
 * Usage:
 * ```typescript
 * const { customer, order, invoice, payment, subscription } = TestDataGenerator.fullCustomerJourney()
 * ```
 */
export class TestDataGenerator {
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

    return {
      customer,
      order,
      invoice,
      payment,
      subscription
    }
  }

  /**
   * Generate multiple customer journeys for batch testing
   */
  static generateBatch(count: number, options: Parameters<typeof TestDataGenerator.fullCustomerJourney>[0] = {}) {
    return Array.from({ length: count }, () => this.fullCustomerJourney(options))
  }

  /**
   * Generate data with specific business scenarios
   */
  static enterpriseScenario() {
    return this.fullCustomerJourney({
      customerStatus: 'active',
      includeOrder: true,
      includeInvoice: true,
      includePayment: true,
      includeSubscription: true
    })
  }

  /**
   * Generate failed payment scenario
   */
  static failedPaymentScenario() {
    const customer = CustomerFactory.create().active().build()
    const order = OrderFactory.create().withCustomer(customer).delivered().build()
    const invoice = InvoiceFactory.create().withCustomer(customer).withOrder(order).pending().build()
    const payment = PaymentFactory.create()
      .withCustomer(customer)
      .withInvoice(invoice)
      .failed()
      .build()

    return {
      customer,
      order,
      invoice,
      payment,
      subscription: null
    }
  }

  /**
   * Generate trial-to-paid conversion scenario
   */
  static trialConversionScenario() {
    const customer = CustomerFactory.create().pending().build()
    const subscription = SubscriptionFactory.create()
      .withCustomer(customer)
      .trial()
      .withRandomTrialEndDate(7)
      .build()

    return {
      customer,
      order: null,
      invoice: null,
      payment: null,
      subscription
    }
  }
}
