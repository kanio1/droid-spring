/**
 * Invoice Test Data Factory
 *
 * Implements Object Mother pattern for Invoice entities
 * Supports billing cycle management and payment tracking
 */

import { faker } from '@faker-js/faker'
import type { Invoice, InvoiceLineItem } from '../../app/schemas/invoice'
import { CustomerFactory, type Customer } from './customer.factory'
import { OrderFactory, type Order } from './order.factory'

export type InvoiceStatus = 'draft' | 'pending' | 'paid' | 'overdue' | 'cancelled' | 'refunded'

export interface InvoiceLineItemOptions {
  description?: string
  quantity?: number
  unitPrice?: number
  taxRate?: number
}

export class InvoiceLineItemFactory {
  private options: InvoiceLineItemOptions = {}

  static create(): InvoiceLineItemFactory {
    return new InvoiceLineItemFactory()
  }

  withDescription(description: string): InvoiceLineItemFactory {
    this.options.description = description
    return this
  }

  withRandomDescription(): InvoiceLineItemFactory {
    const descriptions = [
      'Monthly Service Fee',
      'Data Transfer',
      'Support Package',
      'Software License',
      'Infrastructure Usage'
    ]
    this.options.description = faker.helpers.arrayElement(descriptions)
    return this
  }

  withQuantity(quantity: number): InvoiceLineItemFactory {
    this.options.quantity = quantity
    return this
  }

  withUnitPrice(price: number): InvoiceLineItemFactory {
    this.options.unitPrice = price
    return this
  }

  withTaxRate(rate: number): InvoiceLineItemFactory {
    this.options.taxRate = rate
    return this
  }

  build(): InvoiceLineItem {
    const quantity = this.options.quantity || 1
    const unitPrice = this.options.unitPrice || faker.number.float({ min: 50, max: 1000 })
    const taxRate = this.options.taxRate || 0.2
    const subtotal = quantity * unitPrice
    const taxAmount = subtotal * taxRate

    return {
      description: this.options.description || faker.commerce.productDescription(),
      quantity,
      unitPrice,
      taxRate,
      subtotal,
      taxAmount,
      total: subtotal + taxAmount
    }
  }

  buildMany(count: number): InvoiceLineItem[] {
    return Array.from({ length: count }, () => this.build())
  }
}

export interface InvoiceFactoryOptions {
  customer?: Customer
  order?: Order
  status?: InvoiceStatus
  issueDate?: Date
  dueDate?: Date
  paidDate?: Date
  lineItems?: InvoiceLineItem[]
  subtotal?: number
  taxAmount?: number
  totalAmount?: number
  currency?: string
  metadata?: Record<string, any>
}

export class InvoiceFactory {
  private options: InvoiceFactoryOptions = {}

  static create(): InvoiceFactory {
    return new InvoiceFactory()
  }

  withCustomer(customer: Customer): InvoiceFactory {
    this.options.customer = customer
    return this
  }

  withRandomCustomer(): InvoiceFactory {
    this.options.customer = CustomerFactory.create().build()
    return this
  }

  withOrder(order: Order): InvoiceFactory {
    this.options.order = order
    return this
  }

  withRandomOrder(): InvoiceFactory {
    this.options.order = OrderFactory.create().build()
    return this
  }

  draft(): InvoiceFactory {
    this.options.status = 'draft'
    return this
  }

  pending(): InvoiceFactory {
    this.options.status = 'pending'
    return this
  }

  paid(): InvoiceFactory {
    this.options.status = 'paid'
    return this
  }

  overdue(): InvoiceFactory {
    this.options.status = 'overdue'
    return this
  }

  cancelled(): InvoiceFactory {
    this.options.status = 'cancelled'
    return this
  }

  refunded(): InvoiceFactory {
    this.options.status = 'refunded'
    return this
  }

  withIssueDate(date: Date): InvoiceFactory {
    this.options.issueDate = date
    return this
  }

  withRandomIssueDate(daysAgo: number = 30): InvoiceFactory {
    this.options.issueDate = faker.date.recent({ days: daysAgo })
    return this
  }

  withDueDate(date: Date): InvoiceFactory {
    this.options.dueDate = date
    return this
  }

  withRandomDueDate(daysFromNow: number = 30): InvoiceFactory {
    this.options.dueDate = faker.date.soon({ days: daysFromNow })
    return this
  }

  withPaidDate(date: Date): InvoiceFactory {
    this.options.paidDate = date
    return this
  }

  withLineItems(items: InvoiceLineItem[]): InvoiceFactory {
    this.options.lineItems = items
    return this
  }

  withRandomLineItems(count: number = 3): InvoiceFactory {
    this.options.lineItems = InvoiceLineItemFactory.create().buildMany(count)
    return this
  }

  withSubtotal(amount: number): InvoiceFactory {
    this.options.subtotal = amount
    return this
  }

  withTaxAmount(amount: number): InvoiceFactory {
    this.options.taxAmount = amount
    return this
  }

  withTotalAmount(amount: number): InvoiceFactory {
    this.options.totalAmount = amount
    return this
  }

  withCurrency(currency: string): InvoiceFactory {
    this.options.currency = currency
    return this
  }

  withMetadata(metadata: Record<string, any>): InvoiceFactory {
    this.options.metadata = metadata
    return this
  }

  build(): Invoice {
    const now = new Date()
    const issueDate = this.options.issueDate || now
    const dueDate = this.options.dueDate || faker.date.soon({ days: 30, from: issueDate })
    const lineItems = this.options.lineItems || InvoiceLineItemFactory.create().buildMany(3)

    const subtotal = this.options.subtotal
      || lineItems.reduce((sum, item) => sum + item.subtotal, 0)

    const taxAmount = this.options.taxAmount
      || lineItems.reduce((sum, item) => sum + item.taxAmount, 0)

    const totalAmount = this.options.totalAmount
      || lineItems.reduce((sum, item) => sum + item.total, 0)

    return {
      id: faker.string.uuid(),
      invoiceNumber: `INV-${faker.number.int({ min: 10000, max: 99999 })}`,
      customerId: this.options.customer?.id || faker.string.uuid(),
      customer: this.options.customer,
      orderId: this.options.order?.id,
      order: this.options.order,
      status: this.options.status || 'pending',
      issueDate: issueDate.toISOString(),
      dueDate: dueDate.toISOString(),
      paidDate: this.options.paidDate?.toISOString(),
      lineItems,
      subtotal,
      taxAmount,
      totalAmount,
      currency: this.options.currency || 'USD',
      createdAt: issueDate.toISOString(),
      updatedAt: now.toISOString(),
      metadata: this.options.metadata || {}
    }
  }

  buildMany(count: number): Invoice[] {
    return Array.from({ length: count }, () => this.build())
  }
}

export class InvoiceProfiles {
  static get unpaidInvoice(): Invoice {
    return InvoiceFactory.create()
      .pending()
      .withRandomCustomer()
      .withRandomLineItems(2)
      .build()
  }

  static get paidInvoice(): Invoice {
    const issueDate = faker.date.past({ years: 1 })
    return InvoiceFactory.create()
      .paid()
      .withIssueDate(issueDate)
      .withPaidDate(issueDate)
      .withRandomCustomer()
      .withRandomLineItems(5)
      .build()
  }

  static get overdueInvoice(): Invoice {
    const issueDate = faker.date.past({ years: 1 })
    return InvoiceFactory.create()
      .overdue()
      .withIssueDate(issueDate)
      .withDueDate(faker.date.past({ years: 1 }))
      .withRandomCustomer()
      .build()
  }

  static get largeInvoice(): Invoice {
    const lineItems = InvoiceLineItemFactory.create()
      .withDescription('Enterprise Support Package')
      .withQuantity(1)
      .withUnitPrice(50000)
      .withTaxRate(0.2)
      .buildMany(5)

    return InvoiceFactory.create()
      .pending()
      .withLineItems(lineItems)
      .withTotalAmount(300000)
      .build()
  }

  static get monthlyRecurringInvoice(): Invoice {
    const issueDate = new Date()
    return InvoiceFactory.create()
      .pending()
      .withIssueDate(issueDate)
      .withDueDate(faker.date.soon({ days: 30 }))
      .withMetadata({
        billingCycle: 'monthly',
        recurring: true,
        servicePeriod: `${issueDate.toISOString()} - ${faker.date.soon({ days: 30 }).toISOString()}`
      })
      .build()
  }
}
