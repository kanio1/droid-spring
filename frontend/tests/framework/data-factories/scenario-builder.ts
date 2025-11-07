/**
 * Scenario Builder
 *
 * Provides predefined test scenarios as templates
 * Each scenario creates a complete, valid data set for testing
 */

import { faker } from '@faker-js/faker'
import { CustomerFactory, type Customer, type CustomerFactoryOptions } from './customer.factory'
import { OrderFactory, type Order, type OrderItem } from './order.factory'
import { InvoiceFactory, type Invoice } from './invoice.factory'
import { PaymentFactory, type Payment } from './payment.factory'
import { SubscriptionFactory, type Subscription, type SubscriptionPlan } from './subscription.factory'
import { dataCorrelator } from './data-correlator'

export interface ScenarioConfig {
  customerCount?: number
  ordersPerCustomer?: number
  invoicesPerOrder?: number
  paymentsPerInvoice?: number
  subscriptionsPerCustomer?: number
  includeCancelled?: boolean
  includeOverdue?: boolean
  includeFailedPayments?: boolean
  dateRange?: {
    start: Date
    end: Date
  }
  metadata?: Record<string, any>
}

export class ScenarioBuilder {
  /**
   * Complete business journey scenario
   * Creates: Customer -> Order -> Invoice -> Payment (full lifecycle)
   */
  static fullCustomerJourney(config: ScenarioConfig = {}): {
    customers: Customer[]
    orders: Order[]
    invoices: Invoice[]
    payments: Payment[]
    subscriptions: Subscription[]
  } {
    const {
      customerCount = 5,
      ordersPerCustomer = 2,
      invoicesPerOrder = 1,
      paymentsPerInvoice = 1,
      includeCancelled = true,
      includeOverdue = true,
      includeFailedPayments = true
    } = config

    const customers: Customer[] = []
    const orders: Order[] = []
    const invoices: Invoice[] = []
    const payments: Payment[] = []
    const subscriptions: Subscription[] = []

    // Create customers
    for (let i = 0; i < customerCount; i++) {
      const customer = CustomerFactory.create()
        .withRandomEmail()
        .withRandomFirstName()
        .withRandomLastName()
        .active()
        .build()

      customers.push(customer)
      dataCorrelator.registerEntity('customer', customer)

      // Create orders for customer
      for (let j = 0; j < ordersPerCustomer; j++) {
        const order = OrderFactory.create()
          .withCustomer(customer)
          .pending()
          .withRandomItems(faker.number.int({ min: 1, max: 5 }))
          .build()

        orders.push(order)
        dataCorrelator.registerEntity('order', order)
        dataCorrelator.linkEntities('customer', customer.id, 'order', order.id)

        // Create invoices for order
        for (let k = 0; k < invoicesPerOrder; k++) {
          const invoice = InvoiceFactory.create()
            .withCustomer(customer)
            .withOrder(order)
            .pending()
            .withRandomLineItems(faker.number.int({ min: 1, max: 3 }))
            .build()

          invoices.push(invoice)
          dataCorrelator.registerEntity('invoice', invoice)
          dataCorrelator.linkEntities('order', order.id, 'invoice', invoice.id)
          dataCorrelator.linkEntities('customer', customer.id, 'invoice', invoice.id)

          // Create payments for invoice
          for (let l = 0; l < paymentsPerInvoice; l++) {
            const shouldFail = includeFailedPayments && faker.datatype.boolean({ probability: 0.1 })
            const shouldCancel = includeCancelled && faker.datatype.boolean({ probability: 0.1 })

            const payment = shouldFail
              ? PaymentFactory.create()
                  .withCustomer(customer)
                  .withInvoice(invoice)
                  .failed()
                  .build()
              : PaymentFactory.create()
                  .withCustomer(customer)
                  .withInvoice(invoice)
                  .completed()
                  .withRandomAmount()
                  .build()

            payments.push(payment)
            dataCorrelator.registerEntity('payment', payment)
            dataCorrelator.linkEntities('invoice', invoice.id, 'payment', payment.id)
            dataCorrelator.linkEntities('customer', customer.id, 'payment', payment.id)

            // Update invoice status based on payment
            if (!shouldFail && !shouldCancel) {
              const paidInvoice = { ...invoice, status: 'paid' as const }
              dataCorrelator.registerEntity('invoice', paidInvoice, invoice.id)
            }
          }
        }
      }

      // Create subscriptions for customer
      const hasActiveSubscription = faker.datatype.boolean({ probability: 0.7 })
      if (hasActiveSubscription) {
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
    }

    // Mark some invoices as overdue
    if (includeOverdue) {
      const overdueCount = Math.floor(invoices.length * 0.2)
      for (let i = 0; i < overdueCount; i++) {
        const invoice = invoices[i]
        const overdueInvoice = { ...invoice, status: 'overdue' as const }
        dataCorrelator.registerEntity('invoice', overdueInvoice, invoice.id)
      }
    }

    return {
      customers,
      orders,
      invoices,
      payments,
      subscriptions
    }
  }

  /**
   * Enterprise scenario with large orders and premium customers
   */
  static enterpriseScenario(config: ScenarioConfig = {}): {
    customers: Customer[]
    orders: Order[]
    invoices: Invoice[]
    payments: Payment[]
  } {
    const {
      customerCount = 3,
      ordersPerCustomer = 5,
      includeOverdue = true
    } = config

    const customers: Customer[] = []
    const orders: Order[] = []
    const invoices: Invoice[] = []
    const payments: Payment[] = []

    for (let i = 0; i < customerCount; i++) {
      const customer = CustomerFactory.create()
        .withFirstName('Enterprise')
        .withLastName(`Client ${i + 1}`)
        .withEmail(`enterprise.client.${i + 1}@company.test`)
        .withPhone('+1-555-0199')
        .active()
        .withMetadata({
          tier: 'enterprise',
          accountManager: `manager.${i + 1}@company.test`,
          sla: 'premium',
          contractValue: faker.number.int({ min: 100000, max: 1000000 })
        })
        .build()

      customers.push(customer)
      dataCorrelator.registerEntity('customer', customer)

      for (let j = 0; j < ordersPerCustomer; j++) {
        const itemCount = faker.number.int({ min: 10, max: 50 })
        const items: OrderItem[] = []

        for (let k = 0; k < itemCount; k++) {
          items.push({
            productId: faker.string.uuid(),
            productName: `Enterprise Product ${k + 1}`,
            quantity: faker.number.int({ min: 1, max: 100 }),
            unitPrice: faker.number.float({ min: 100, max: 10000, multipleOf: 0.01 }),
            totalPrice: 0
          })
          items[k].totalPrice = items[k].quantity * items[k].unitPrice
        }

        const order = OrderFactory.create()
          .withCustomer(customer)
          .withItems(items)
          .delivered()
          .withTotalAmount(items.reduce((sum, item) => sum + item.totalPrice, 0))
          .build()

        orders.push(order)
        dataCorrelator.registerEntity('order', order)
        dataCorrelator.linkEntities('customer', customer.id, 'order', order.id)

        const invoice = InvoiceFactory.create()
          .withCustomer(customer)
          .withOrder(order)
          .paid()
          .withLineItems(items.map(item => ({
            description: item.productName,
            quantity: item.quantity,
            unitPrice: item.unitPrice,
            taxRate: 0.2,
            subtotal: item.totalPrice,
            taxAmount: item.totalPrice * 0.2,
            total: item.totalPrice * 1.2
          })))
          .withTotalAmount(order.totalAmount * 1.2)
          .build()

        invoices.push(invoice)
        dataCorrelator.registerEntity('invoice', invoice)
        dataCorrelator.linkEntities('order', order.id, 'invoice', invoice.id)

        const payment = PaymentFactory.create()
          .withCustomer(customer)
          .withInvoice(invoice)
          .completed()
          .withAmount(invoice.totalAmount)
          .withBankTransfer()
          .build()

        payments.push(payment)
        dataCorrelator.registerEntity('payment', payment)
        dataCorrelator.linkEntities('invoice', invoice.id, 'payment', payment.id)
      }
    }

    return { customers, orders, invoices, payments }
  }

  /**
   * Failed payment scenario
   * Creates customers with orders and payments that fail
   */
  static failedPaymentScenario(config: ScenarioConfig = {}): {
    customers: Customer[]
    orders: Order[]
    invoices: Invoice[]
    payments: Payment[]
  } {
    const { customerCount = 3 } = config

    const customers: Customer[] = []
    const orders: Order[] = []
    const invoices: Invoice[] = []
    const payments: Payment[] = []

    for (let i = 0; i < customerCount; i++) {
      const customer = CustomerFactory.create()
        .withRandomFirstName()
        .withRandomLastName()
        .withRandomEmail()
        .active()
        .build()

      customers.push(customer)
      dataCorrelator.registerEntity('customer', customer)

      for (let j = 0; j < 2; j++) {
        const order = OrderFactory.create()
          .withCustomer(customer)
          .processing()
          .withRandomItems(2)
          .build()

        orders.push(order)
        dataCorrelator.registerEntity('order', order)
        dataCorrelator.linkEntities('customer', customer.id, 'order', order.id)

        const invoice = InvoiceFactory.create()
          .withCustomer(customer)
          .withOrder(order)
          .pending()
          .build()

        invoices.push(invoice)
        dataCorrelator.registerEntity('invoice', invoice)
        dataCorrelator.linkEntities('order', order.id, 'invoice', invoice.id)

        // Create failed payment
        const payment = PaymentFactory.create()
          .withCustomer(customer)
          .withInvoice(invoice)
          .withRandomAmount(100, 5000)
          .withCreditCard()
          .failed()
          .build()

        payments.push(payment)
        dataCorrelator.registerEntity('payment', payment)
        dataCorrelator.linkEntities('invoice', invoice.id, 'payment', payment.id)
        dataCorrelator.linkEntities('customer', customer.id, 'payment', payment.id)
      }
    }

    return { customers, orders, invoices, payments }
  }

  /**
   * Trial to conversion scenario
   * Creates customers in trial and converts them to paid subscriptions
   */
  static trialConversionScenario(config: ScenarioConfig = {}): {
    customers: Customer[]
    subscriptions: Subscription[]
  } {
    const { customerCount = 10 } = config

    const customers: Customer[] = []
    const subscriptions: Subscription[] = []

    for (let i = 0; i < customerCount; i++) {
      const isConverting = faker.datatype.boolean({ probability: 0.6 })
      const customer = CustomerFactory.create()
        .withRandomFirstName()
        .withRandomLastName()
        .withRandomEmail()
        .active()
        .build()

      customers.push(customer)
      dataCorrelator.registerEntity('customer', customer)

      const plan = faker.helpers.arrayElement([
        SubscriptionFactory.getBasicPlan(),
        SubscriptionFactory.getProPlan()
      ])

      const subscription = SubscriptionFactory.create()
        .withCustomer(customer)
        .withPlan(plan)
        .withRandomTrialEndDate(isConverting ? 7 : 14)
        .withMetadata({
          convertedFromTrial: isConverting,
          conversionDate: isConverting ? new Date().toISOString() : null,
          source: 'trial',
          utmCampaign: faker.string.alphanumeric(8)
        })
        .build()

      if (isConverting) {
        subscription.status = 'active'
        delete subscription.trialEndDate
      } else {
        subscription.status = 'trial'
      }

      subscriptions.push(subscription)
      dataCorrelator.registerEntity('subscription', subscription)
      dataCorrelator.linkEntities('customer', customer.id, 'subscription', subscription.id)
    }

    return { customers, subscriptions }
  }

  /**
   * Bulk data generation scenario
   * Creates large amounts of test data for performance testing
   */
  static bulkDataScenario(config: ScenarioConfig = {}): {
    customers: Customer[]
    orders: Order[]
    invoices: Invoice[]
    payments: Payment[]
    subscriptions: Subscription[]
  } {
    const {
      customerCount = 100,
      ordersPerCustomer = 5,
      subscriptionsPerCustomer = 1
    } = config

    const customers: Customer[] = []
    const orders: Order[] = []
    const invoices: Invoice[] = []
    const payments: Payment[] = []
    const subscriptions: Subscription[] = []

    for (let i = 0; i < customerCount; i++) {
      const customer = CustomerFactory.create()
        .withRandomEmail()
        .withRandomFirstName()
        .withRandomLastName()
        .active()
        .build()

      customers.push(customer)
      dataCorrelator.registerEntity('customer', customer)

      // Create orders
      for (let j = 0; j < ordersPerCustomer; j++) {
        const order = OrderFactory.create()
          .withCustomer(customer)
          .withRandomItems(faker.number.int({ min: 1, max: 3 }))
          .delivered()
          .build()

        orders.push(order)
        dataCorrelator.registerEntity('order', order)
        dataCorrelator.linkEntities('customer', customer.id, 'order', order.id)

        // Create invoice and payment for each order
        const invoice = InvoiceFactory.create()
          .withCustomer(customer)
          .withOrder(order)
          .paid()
          .withRandomLineItems(2)
          .build()

        invoices.push(invoice)
        dataCorrelator.registerEntity('invoice', invoice)
        dataCorrelator.linkEntities('order', order.id, 'invoice', invoice.id)

        const payment = PaymentFactory.create()
          .withCustomer(customer)
          .withInvoice(invoice)
          .completed()
          .build()

        payments.push(payment)
        dataCorrelator.registerEntity('payment', payment)
        dataCorrelator.linkEntities('invoice', invoice.id, 'payment', payment.id)
      }

      // Create subscription
      const subscription = SubscriptionFactory.create()
        .withCustomer(customer)
        .withBasicPlan()
        .active()
        .build()

      subscriptions.push(subscription)
      dataCorrelator.registerEntity('subscription', subscription)
      dataCorrelator.linkEntities('customer', customer.id, 'subscription', subscription.id)
    }

    return { customers, orders, invoices, payments, subscriptions }
  }
}
