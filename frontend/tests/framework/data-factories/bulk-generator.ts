/**
 * Bulk Generator
 *
 * Efficiently generates large volumes of test data (1000+ entities)
 * Optimized for performance and memory usage
 */

import { faker } from '@faker-js/faker'
import { CustomerFactory } from './customer.factory'
import { OrderFactory } from './order.factory'
import { InvoiceFactory } from './invoice.factory'
import { PaymentFactory } from './payment.factory'
import { SubscriptionFactory } from './subscription.factory'
import { dataCorrelator } from './data-correlator'
import { uniqueDataPool, DataGenerators } from './unique-data-pool'
import type { Customer } from '../../../app/schemas/customer'
import type { Order } from '../../../app/schemas/order'
import type { Invoice } from '../../../app/schemas/invoice'
import type { Payment } from '../../../app/schemas/payment'
import type { Subscription } from '../../../app/schemas/subscription'

export interface BulkGenerationConfig {
  customerCount: number
  ordersPerCustomer: number
  invoicesPerOrder: number
  paymentsPerInvoice: number
  subscriptionsPerCustomer: number
  includeCancelled: boolean
  includeOverdue: boolean
  includeFailedPayments: boolean
  batchSize?: number // Process in batches to avoid memory issues
}

export class BulkGenerator {
  private config: BulkGenerationConfig
  private stats = {
    customersCreated: 0,
    ordersCreated: 0,
    invoicesCreated: 0,
    paymentsCreated: 0,
    subscriptionsCreated: 0,
    startTime: 0,
    endTime: 0
  }

  constructor(config: BulkGenerationConfig) {
    this.config = {
      batchSize: 100,
      ...config
    }
  }

  /**
   * Generate all entities in bulk
   */
  async generateAll(): Promise<{
    customers: Customer[]
    orders: Order[]
    invoices: Invoice[]
    payments: Payment[]
    subscriptions: Subscription[]
    statistics: typeof this.stats
  }> {
    this.stats.startTime = Date.now()
    dataCorrelator.clear()
    uniqueDataPool.clear()

    const customers: Customer[] = []
    const orders: Order[] = []
    const invoices: Invoice[] = []
    const payments: Payment[] = []
    const subscriptions: Subscription[] = []

    // Generate customers in batches
    const customerBatches = this.chunkArray(
      Array.from({ length: this.config.customerCount }),
      this.config.batchSize
    )

    for (const batch of customerBatches) {
      const batchCustomers = await this.generateCustomerBatch(batch.length)
      customers.push(...batchCustomers)
      this.stats.customersCreated += batch.length

      // Generate orders for this batch
      for (const customer of batchCustomers) {
        const ordersForCustomer = await this.generateOrdersForCustomer(
          customer,
          this.config.ordersPerCustomer
        )
        orders.push(...ordersForCustomer)
        this.stats.ordersCreated += ordersForCustomer.length

        // Generate invoices and payments for each order
        for (const order of ordersForCustomer) {
          const invoicesForOrder = await this.generateInvoicesForOrder(
            order,
            this.config.invoicesPerOrder
          )
          invoices.push(...invoicesForOrder)
          this.stats.invoicesCreated += invoicesForOrder.length

          for (const invoice of invoicesForOrder) {
            const paymentsForInvoice = await this.generatePaymentsForInvoice(
              invoice,
              this.config.paymentsPerInvoice
            )
            payments.push(...paymentsForInvoice)
            this.stats.paymentsCreated += paymentsForInvoice.length
          }
        }

        // Generate subscriptions for customer
        const subscriptionsForCustomer = await this.generateSubscriptionsForCustomer(
          customer,
          this.config.subscriptionsPerCustomer
        )
        subscriptions.push(...subscriptionsForCustomer)
        this.stats.subscriptionsCreated += subscriptionsForCustomer.length
      }

      // Yield control to event loop to prevent blocking
      await this.yieldToEventLoop()
    }

    this.stats.endTime = Date.now()

    return {
      customers,
      orders,
      invoices,
      payments,
      subscriptions,
      statistics: { ...this.stats }
    }
  }

  /**
   * Generate only customers in bulk
   */
  async generateCustomers(count: number): Promise<Customer[]> {
    this.stats.startTime = Date.now()
    const customers: Customer[] = []

    const batches = this.chunkArray(Array.from({ length: count }), this.config.batchSize)
    for (const batch of batches) {
      const batchCustomers = await this.generateCustomerBatch(batch.length)
      customers.push(...batchCustomers)
      this.stats.customersCreated += batch.length
      await this.yieldToEventLoop()
    }

    this.stats.endTime = Date.now()
    return customers
  }

  /**
   * Generate only orders in bulk
   */
  async generateOrders(count: number, customerPool: Customer[]): Promise<Order[]> {
    this.stats.startTime = Date.now()
    const orders: Order[] = []

    const batches = this.chunkArray(Array.from({ length: count }), this.config.batchSize)
    for (const batch of batches) {
      const batchOrders = await this.generateOrderBatch(batch.length, customerPool)
      orders.push(...batchOrders)
      this.stats.ordersCreated += batch.length
      await this.yieldToEventLoop()
    }

    this.stats.endTime = Date.now()
    return orders
  }

  /**
   * Generate only invoices in bulk
   */
  async generateInvoices(count: number, orderPool: Order[]): Promise<Invoice[]> {
    this.stats.startTime = Date.now()
    const invoices: Invoice[] = []

    const batches = this.chunkArray(Array.from({ length: count }), this.config.batchSize)
    for (const batch of batches) {
      const batchInvoices = await this.generateInvoiceBatch(batch.length, orderPool)
      invoices.push(...batchInvoices)
      this.stats.invoicesCreated += batch.length
      await this.yieldToEventLoop()
    }

    this.stats.endTime = Date.now()
    return invoices
  }

  /**
   * Generate only payments in bulk
   */
  async generatePayments(count: number, invoicePool: Invoice[]): Promise<Payment[]> {
    this.stats.startTime = Date.now()
    const payments: Payment[] = []

    const batches = this.chunkArray(Array.from({ length: count }), this.config.batchSize)
    for (const batch of batches) {
      const batchPayments = await this.generatePaymentBatch(batch.length, invoicePool)
      payments.push(...batchPayments)
      this.stats.paymentsCreated += batch.length
      await this.yieldToEventLoop()
    }

    this.stats.endTime = Date.now()
    return payments
  }

  /**
   * Generate only subscriptions in bulk
   */
  async generateSubscriptions(count: number, customerPool: Customer[]): Promise<Subscription[]> {
    this.stats.startTime = Date.now()
    const subscriptions: Subscription[] = []

    const batches = this.chunkArray(Array.from({ length: count }), this.config.batchSize)
    for (const batch of batches) {
      const batchSubscriptions = await this.generateSubscriptionBatch(batch.length, customerPool)
      subscriptions.push(...batchSubscriptions)
      this.stats.subscriptionsCreated += batch.length
      await this.yieldToEventLoop()
    }

    this.stats.endTime = Date.now()
    return subscriptions
  }

  /**
   * Get generation statistics
   */
  getStatistics(): typeof this.stats {
    return { ...this.stats }
  }

  private async generateCustomerBatch(count: number): Promise<Customer[]> {
    const customers: Customer[] = []

    for (let i = 0; i < count; i++) {
      const customer = CustomerFactory.create()
        .withEmail(DataGenerators.uniqueCustomerEmail())
        .withPhone(DataGenerators.uniqueCustomerPhone())
        .withRandomFirstName()
        .withRandomLastName()
        .active()
        .build()

      customers.push(customer)
      dataCorrelator.registerEntity('customer', customer)
    }

    return customers
  }

  private async generateOrderBatch(count: number, customerPool: Customer[]): Promise<Order[]> {
    const orders: Order[] = []

    for (let i = 0; i < count; i++) {
      const customer = faker.helpers.arrayElement(customerPool)
      const order = OrderFactory.create()
        .withCustomer(customer)
        .withRandomItems(faker.number.int({ min: 1, max: 5 }))
        .delivered()
        .build()

      orders.push(order)
      dataCorrelator.registerEntity('order', order)
      dataCorrelator.linkEntities('customer', customer.id, 'order', order.id)
    }

    return orders
  }

  private async generateInvoiceBatch(count: number, orderPool: Order[]): Promise<Invoice[]> {
    const invoices: Invoice[] = []

    for (let i = 0; i < count; i++) {
      const order = faker.helpers.arrayElement(orderPool)
      const invoice = InvoiceFactory.create()
        .withCustomer(order.customer!)
        .withOrder(order)
        .paid()
        .withRandomLineItems(faker.number.int({ min: 1, max: 3 }))
        .build()

      invoices.push(invoice)
      dataCorrelator.registerEntity('invoice', invoice)
      dataCorrelator.linkEntities('order', order.id, 'invoice', invoice.id)
    }

    return invoices
  }

  private async generatePaymentBatch(count: number, invoicePool: Invoice[]): Promise<Payment[]> {
    const payments: Payment[] = []

    for (let i = 0; i < count; i++) {
      const invoice = faker.helpers.arrayElement(invoicePool)
      const payment = PaymentFactory.create()
        .withCustomer(invoice.customer!)
        .withInvoice(invoice)
        .completed()
        .withAmount(invoice.totalAmount)
        .build()

      payments.push(payment)
      dataCorrelator.registerEntity('payment', payment)
      dataCorrelator.linkEntities('invoice', invoice.id, 'payment', payment.id)
    }

    return payments
  }

  private async generateSubscriptionBatch(count: number, customerPool: Customer[]): Promise<Subscription[]> {
    const subscriptions: Subscription[] = []

    for (let i = 0; i < count; i++) {
      const customer = faker.helpers.arrayElement(customerPool)
      const plan = faker.helpers.arrayElement([
        SubscriptionFactory.getBasicPlan(),
        SubscriptionFactory.getProPlan(),
        SubscriptionFactory.getEnterprisePlan()
      ])

      const subscription = SubscriptionFactory.create()
        .withCustomer(customer)
        .withPlan(plan)
        .active()
        .build()

      subscriptions.push(subscription)
      dataCorrelator.registerEntity('subscription', subscription)
      dataCorrelator.linkEntities('customer', customer.id, 'subscription', subscription.id)
    }

    return subscriptions
  }

  private async generateOrdersForCustomer(customer: Customer, count: number): Promise<Order[]> {
    const orders: Order[] = []

    for (let i = 0; i < count; i++) {
      const order = OrderFactory.create()
        .withCustomer(customer)
        .withRandomItems(faker.number.int({ min: 1, max: 5 }))
        .delivered()
        .build()

      orders.push(order)
      dataCorrelator.registerEntity('order', order)
      dataCorrelator.linkEntities('customer', customer.id, 'order', order.id)
    }

    return orders
  }

  private async generateInvoicesForOrder(order: Order, count: number): Promise<Invoice[]> {
    const invoices: Invoice[] = []

    for (let i = 0; i < count; i++) {
      const invoice = InvoiceFactory.create()
        .withCustomer(order.customer!)
        .withOrder(order)
        .paid()
        .withRandomLineItems(faker.number.int({ min: 1, max: 3 }))
        .build()

      invoices.push(invoice)
      dataCorrelator.registerEntity('invoice', invoice)
      dataCorrelator.linkEntities('order', order.id, 'invoice', invoice.id)
    }

    return invoices
  }

  private async generatePaymentsForInvoice(invoice: Invoice, count: number): Promise<Payment[]> {
    const payments: Payment[] = []

    for (let i = 0; i < count; i++) {
      const payment = PaymentFactory.create()
        .withCustomer(invoice.customer!)
        .withInvoice(invoice)
        .completed()
        .withAmount(invoice.totalAmount)
        .build()

      payments.push(payment)
      dataCorrelator.registerEntity('payment', payment)
      dataCorrelator.linkEntities('invoice', invoice.id, 'payment', payment.id)
    }

    return payments
  }

  private async generateSubscriptionsForCustomer(customer: Customer, count: number): Promise<Subscription[]> {
    const subscriptions: Subscription[] = []

    for (let i = 0; i < count; i++) {
      const plan = faker.helpers.arrayElement([
        SubscriptionFactory.getBasicPlan(),
        SubscriptionFactory.getProPlan(),
        SubscriptionFactory.getEnterprisePlan()
      ])

      const subscription = SubscriptionFactory.create()
        .withCustomer(customer)
        .withPlan(plan)
        .active()
        .build()

      subscriptions.push(subscription)
      dataCorrelator.registerEntity('subscription', subscription)
      dataCorrelator.linkEntities('customer', customer.id, 'subscription', subscription.id)
    }

    return subscriptions
  }

  private chunkArray<T>(array: T[], chunkSize: number): T[][] {
    const chunks: T[][] = []
    for (let i = 0; i < array.length; i += chunkSize) {
      chunks.push(array.slice(i, i + chunkSize))
    }
    return chunks
  }

  private async yieldToEventLoop(): Promise<void> {
    return new Promise(resolve => setImmediate(resolve))
  }
}

/**
 * Convenience function for quick bulk generation
 */
export async function generateBulkData(config: BulkGenerationConfig): Promise<ReturnType<BulkGenerator['generateAll']>> {
  const generator = new BulkGenerator(config)
  return generator.generateAll()
}
