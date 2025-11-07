/**
 * Enhanced Data Generator
 * Advanced test data generation with Faker.js, synthetic data, and realistic scenarios
 */

import { faker } from '@faker-js/faker'
import { DataFactory } from '../data-factories'

// Configure faker
faker.setDefaultLocale('en_US')

export interface CustomerData {
  id: string
  firstName: string
  lastName: string
  email: string
  phone: string
  address: AddressData
  company?: string
  dateOfBirth: string
  status: 'active' | 'inactive' | 'pending'
  createdAt: string
  metadata?: Record<string, any>
}

export interface AddressData {
  street: string
  city: string
  state: string
  zipCode: string
  country: string
  isDefault: boolean
}

export interface OrderData {
  id: string
  customerId: string
  orderNumber: string
  items: OrderItemData[]
  totalAmount: number
  currency: string
  status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled'
  shippingAddress: AddressData
  billingAddress: AddressData
  paymentMethod: string
  createdAt: string
  shippedAt?: string
  deliveredAt?: string
}

export interface OrderItemData {
  productId: string
  productName: string
  quantity: number
  price: number
  total: number
}

export interface InvoiceData {
  id: string
  invoiceNumber: string
  customerId: string
  orderId?: string
  items: InvoiceItemData[]
  subtotal: number
  tax: number
  total: number
  currency: string
  status: 'draft' | 'sent' | 'paid' | 'overdue' | 'cancelled'
  dueDate: string
  paidAt?: string
  createdAt: string
}

export interface InvoiceItemData {
  description: string
  quantity: number
  unitPrice: number
  total: number
}

export class EnhancedDataGenerator {
  // Customer generation
  static generateCustomer(overrides: Partial<CustomerData> = {}): CustomerData {
    const customer: CustomerData = {
      id: faker.string.uuid(),
      firstName: faker.person.firstName(),
      lastName: faker.person.lastName(),
      email: faker.internet.email(),
      phone: faker.phone.number(),
      address: this.generateAddress(),
      company: faker.company.name(),
      dateOfBirth: faker.date.birthdate({ min: 18, max: 80, mode: 'age' }).toISOString().split('T')[0],
      status: faker.helpers.arrayElement(['active', 'inactive', 'pending']),
      createdAt: faker.date.recent({ days: 365 }).toISOString(),
      metadata: {
        source: faker.helpers.arrayElement(['web', 'mobile', 'api', 'import']),
        preferredContact: faker.helpers.arrayElement(['email', 'phone', 'sms']),
        newsletter: faker.datatype.boolean()
      },
      ...overrides
    }

    return customer
  }

  // Bulk customer generation
  static generateCustomers(count: number, options: {
    withOrders?: boolean
    withInvoices?: boolean
    statusFilter?: CustomerData['status']
  } = {}): CustomerData[] {
    const customers: CustomerData[] = []

    for (let i = 0; i < count; i++) {
      const status = options.statusFilter || faker.helpers.arrayElement(['active', 'inactive', 'pending'])
      const customer = this.generateCustomer({ status })

      customers.push(customer)
    }

    return customers
  }

  // Address generation
  static generateAddress(overrides: Partial<AddressData> = {}): AddressData {
    return {
      street: faker.location.streetAddress(),
      city: faker.location.city(),
      state: faker.location.state(),
      zipCode: faker.location.zipCode(),
      country: faker.helpers.arrayElement(['USA', 'Canada', 'UK', 'Germany', 'France', 'Australia']),
      isDefault: faker.datatype.boolean(),
      ...overrides
    }
  }

  // Order generation
  static generateOrder(customerId?: string, overrides: Partial<OrderData> = {}): OrderData {
    const customer = customerId || faker.string.uuid()
    const itemCount = faker.number.int({ min: 1, max: 5 })
    const items: OrderItemData[] = []

    for (let i = 0; i < itemCount; i++) {
      const quantity = faker.number.int({ min: 1, max: 10 })
      const price = faker.number.float({ min: 10, max: 500, precision: 0.01 })
      items.push({
        productId: faker.string.uuid(),
        productName: faker.commerce.productName(),
        quantity,
        price,
        total: quantity * price
      })
    }

    const totalAmount = items.reduce((sum, item) => sum + item.total, 0)

    const order: OrderData = {
      id: faker.string.uuid(),
      customerId: customer,
      orderNumber: `ORD-${faker.number.int({ min: 100000, max: 999999 })}`,
      items,
      totalAmount: Math.round(totalAmount * 100) / 100,
      currency: 'USD',
      status: faker.helpers.arrayElement(['pending', 'processing', 'shipped', 'delivered', 'cancelled']),
      shippingAddress: this.generateAddress(),
      billingAddress: this.generateAddress(),
      paymentMethod: faker.helpers.arrayElement(['credit_card', 'paypal', 'bank_transfer', 'cash']),
      createdAt: faker.date.recent({ days: 90 }).toISOString(),
      ...overrides
    }

    // Add shipped/delivered dates if applicable
    if (order.status === 'shipped' || order.status === 'delivered') {
      order.shippedAt = new Date(new Date(order.createdAt).getTime() + 24 * 60 * 60 * 1000).toISOString()
    }
    if (order.status === 'delivered') {
      order.deliveredAt = new Date(new Date(order.shippedAt!).getTime() + 2 * 24 * 60 * 60 * 1000).toISOString()
    }

    return order
  }

  // Complex order with customer
  static generateCustomerWithOrders(customerCount: number, ordersPerCustomer: number) {
    const customers: CustomerData[] = []
    const orders: OrderData[] = []

    for (let i = 0; i < customerCount; i++) {
      const customer = this.generateCustomer()
      customers.push(customer)

      for (let j = 0; j < ordersPerCustomer; j++) {
        const order = this.generateOrder(customer.id, {
          createdAt: faker.date.between({
            from: customer.createdAt,
            to: new Date()
          }).toISOString()
        })
        orders.push(order)
      }
    }

    return { customers, orders }
  }

  // Invoice generation
  static generateInvoice(customerId?: string, orderId?: string, overrides: Partial<InvoiceData> = {}): InvoiceData {
    const itemCount = faker.number.int({ min: 1, max: 10 })
    const items: InvoiceItemData[] = []

    for (let i = 0; i < itemCount; i++) {
      const quantity = faker.number.int({ min: 1, max: 5 })
      const unitPrice = faker.number.float({ min: 20, max: 1000, precision: 0.01 })
      const total = quantity * unitPrice
      items.push({
        description: faker.commerce.productDescription(),
        quantity,
        unitPrice,
        total: Math.round(total * 100) / 100
      })
    }

    const subtotal = items.reduce((sum, item) => sum + item.total, 0)
    const tax = subtotal * 0.1 // 10% tax
    const total = subtotal + tax

    const dueDate = faker.date.future({ years: 1 })

    return {
      id: faker.string.uuid(),
      invoiceNumber: `INV-${faker.number.int({ min: 100000, max: 999999 })}`,
      customerId: customerId || faker.string.uuid(),
      orderId,
      items,
      subtotal: Math.round(subtotal * 100) / 100,
      tax: Math.round(tax * 100) / 100,
      total: Math.round(total * 100) / 100,
      currency: 'USD',
      status: faker.helpers.arrayElement(['draft', 'sent', 'paid', 'overdue', 'cancelled']),
      dueDate: dueDate.toISOString().split('T')[0],
      createdAt: faker.date.recent({ days: 180 }).toISOString(),
      ...overrides
    }
  }

  // Payment generation
  static generatePayment(invoiceId?: string, overrides: any = {}) {
    const amount = faker.number.float({ min: 50, max: 5000, precision: 0.01 })
    return {
      id: faker.string.uuid(),
      invoiceId: invoiceId || faker.string.uuid(),
      paymentMethod: faker.helpers.arrayElement(['credit_card', 'paypal', 'bank_transfer', 'cash', 'check']),
      amount: Math.round(amount * 100) / 100,
      currency: 'USD',
      status: faker.helpers.arrayElement(['pending', 'completed', 'failed', 'refunded']),
      transactionId: faker.string.alphanumeric(32),
      processedAt: faker.date.recent({ days: 90 }).toISOString(),
      ...overrides
    }
  }

  // Subscription generation
  static generateSubscription(customerId?: string, overrides: any = {}) {
    const startDate = faker.date.past({ years: 2 })
    const endDate = faker.date.future({ years: 2, refDate: startDate })
    return {
      id: faker.string.uuid(),
      customerId: customerId || faker.string.uuid(),
      planName: faker.helpers.arrayElement(['Basic', 'Premium', 'Enterprise', 'Professional']),
      price: faker.helpers.arrayElement([29.99, 49.99, 99.99, 199.99]),
      currency: 'USD',
      billingCycle: faker.helpers.arrayElement(['monthly', 'quarterly', 'yearly']),
      status: faker.helpers.arrayElement(['active', 'cancelled', 'expired', 'suspended']),
      startDate: startDate.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0],
      autoRenew: faker.datatype.boolean(),
      ...overrides
    }
  }

  // Product generation
  static generateProduct(overrides: any = {}) {
    return {
      id: faker.string.uuid(),
      name: faker.commerce.productName(),
      description: faker.commerce.productDescription(),
      price: faker.number.float({ min: 10, max: 1000, precision: 0.01 }),
      currency: 'USD',
      category: faker.commerce.department(),
      sku: faker.string.alphanumeric(10).toUpperCase(),
      stock: faker.number.int({ min: 0, max: 1000 }),
      status: faker.helpers.arrayElement(['active', 'inactive', 'discontinued']),
      createdAt: faker.date.recent({ days: 365 }).toISOString(),
      ...overrides
    }
  }

  // Realistic data scenario generators
  static generateEcommerceScenario() {
    const customers = this.generateCustomers(50)
    const products = Array.from({ length: 100 }, () => this.generateProduct())
    const orders: OrderData[] = []
    const invoices: InvoiceData[] = []
    const payments: any[] = []

    // Generate orders for customers
    for (const customer of customers) {
      const orderCount = faker.number.int({ min: 0, max: 10 })
      for (let i = 0; i < orderCount; i++) {
        const order = this.generateOrder(customer.id)
        orders.push(order)

        // Generate invoice for order
        const invoice = this.generateInvoice(customer.id, order.id, {
          createdAt: order.createdAt
        })
        invoices.push(invoice)

        // Generate payment for invoice (if paid)
        if (invoice.status === 'paid') {
          const payment = this.generatePayment(invoice.id, {
            processedAt: faker.date.between({
              from: invoice.createdAt,
              to: new Date()
            }).toISOString()
          })
          payments.push(payment)
        }
      }
    }

    return {
      customers,
      products,
      orders,
      invoices,
      payments,
      summary: {
        totalCustomers: customers.length,
        totalProducts: products.length,
        totalOrders: orders.length,
        totalInvoices: invoices.length,
        totalPayments: payments.length,
        totalRevenue: payments.reduce((sum, p) => sum + p.amount, 0)
      }
    }
  }

  // GDPR-compliant data generator (anonymous/pseudonymous)
  static generateAnonymousCustomer() {
    return {
      id: `anon_${faker.string.alphanumeric(16)}`,
      firstName: faker.person.firstName(), // Pseudonymized
      lastName: faker.person.lastName(),   // Pseudonymized
      email: `user+${faker.string.alphanumeric(8)}@example.com`,
      preferences: {
        marketing: faker.datatype.boolean(),
        analytics: faker.datatype.boolean()
      }
    }
  }

  // Time-based data generation (for testing historical data)
  static generateHistoricalData(
    startDate: Date,
    endDate: Date,
    entityType: 'customers' | 'orders' | 'invoices'
  ) {
    const data: any[] = []
    const daysBetween = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24))

    for (let i = 0; i < daysBetween; i++) {
      const currentDate = new Date(startDate.getTime() + i * 24 * 60 * 60 * 1000)
      const count = faker.number.int({ min: 0, max: 20 })

      for (let j = 0; j < count; j++) {
        const entityDate = faker.date.between({
          from: currentDate,
          to: new Date(currentDate.getTime() + 24 * 60 * 60 * 1000)
        })

        switch (entityType) {
          case 'customers':
            data.push(this.generateCustomer({
              createdAt: entityDate.toISOString()
            }))
            break
          case 'orders':
            data.push(this.generateOrder(undefined, {
              createdAt: entityDate.toISOString()
            }))
            break
          case 'invoices':
            data.push(this.generateInvoice(undefined, undefined, {
              createdAt: entityDate.toISOString()
            }))
            break
        }
      }
    }

    return data
  }

  // Generate test data with specific patterns
  static generateCustomersWithPattern(pattern: 'vip' | 'churn-risk' | 'new' | 'loyal') {
    switch (pattern) {
      case 'vip':
        return this.generateCustomers(10).map(customer => ({
          ...customer,
          status: 'active',
          metadata: {
            ...customer.metadata,
            tier: 'vip',
            lifetimeValue: faker.number.float({ min: 10000, max: 100000 })
          }
        }))

      case 'churn-risk':
        return this.generateCustomers(10).map(customer => ({
          ...customer,
          status: 'active',
          lastLogin: faker.date.recent({ days: 60 }).toISOString(),
          ordersCount: faker.number.int({ min: 0, max: 2 }),
          metadata: {
            ...customer.metadata,
            churnScore: faker.number.float({ min: 0.7, max: 1.0 })
          }
        }))

      case 'new':
        return this.generateCustomers(20).map(customer => ({
          ...customer,
          createdAt: faker.date.recent({ days: 30 }).toISOString(),
          metadata: {
            ...customer.metadata,
            source: 'campaign',
            campaign: faker.helpers.arrayElement(['summer-2024', 'black-friday', 'new-year'])
          }
        }))

      case 'loyal':
        return this.generateCustomers(15).map(customer => ({
          ...customer,
          status: 'active',
          createdAt: faker.date.recent({ years: 2 }),
          ordersCount: faker.number.int({ min: 10, max: 100 }),
          metadata: {
            ...customer.metadata,
            loyaltyPoints: faker.number.int({ min: 1000, max: 50000 }),
            tenure: faker.number.int({ min: 365, max: 730 })
          }
        }))

      default:
        return this.generateCustomers(10)
    }
  }

  // File data generation
  static generateCSVData(dataType: 'customers' | 'products' | 'orders', count: number) {
    let headers: string[] = []
    let rows: string[] = []

    switch (dataType) {
      case 'customers':
        headers = ['id', 'firstName', 'lastName', 'email', 'phone', 'city', 'state']
        for (let i = 0; i < count; i++) {
          const customer = this.generateCustomer()
          rows.push([
            customer.id,
            customer.firstName,
            customer.lastName,
            customer.email,
            customer.phone,
            customer.address.city,
            customer.address.state
          ].join(','))
        }
        break

      case 'products':
        headers = ['id', 'name', 'price', 'category', 'sku', 'stock']
        for (let i = 0; i < count; i++) {
          const product = this.generateProduct()
          rows.push([
            product.id,
            product.name,
            product.price,
            product.category,
            product.sku,
            product.stock
          ].join(','))
        }
        break

      case 'orders':
        headers = ['id', 'orderNumber', 'customerId', 'totalAmount', 'status', 'createdAt']
        for (let i = 0; i < count; i++) {
          const order = this.generateOrder()
          rows.push([
            order.id,
            order.orderNumber,
            order.customerId,
            order.totalAmount,
            order.status,
            order.createdAt
          ].join(','))
        }
        break
    }

    return [headers.join(','), ...rows].join('\n')
  }

  // Image data generation for testing
  static generateTestImage(width: number, height: number, format: 'png' | 'jpg' = 'png') {
    const { createCanvas } = require('canvas')
    const canvas = createCanvas(width, height)
    const ctx = canvas.getContext('2d')

    // Fill with random color
    ctx.fillStyle = faker.color.rgb()
    ctx.fillRect(0, 0, width, height)

    // Add some text
    ctx.fillStyle = '#ffffff'
    ctx.font = '20px Arial'
    ctx.fillText(faker.lorem.words(3), 10, 30)

    const buffer = canvas.toBuffer(`image/${format}`)
    return buffer
  }

  // PDF data generation for testing
  static generateTestPDF(title: string, content: string) {
    // This would require a PDF library like PDFKit
    // For now, return a placeholder
    return Buffer.from(`PDF: ${title}\n\n${content}`)
  }
}

// Export convenience functions
export const generateCustomer = (overrides?: Partial<CustomerData>) =>
  EnhancedDataGenerator.generateCustomer(overrides)

export const generateOrder = (customerId?: string, overrides?: Partial<OrderData>) =>
  EnhancedDataGenerator.generateOrder(customerId, overrides)

export const generateInvoice = (customerId?: string, orderId?: string, overrides?: Partial<InvoiceData>) =>
  EnhancedDataGenerator.generateInvoice(customerId, orderId, overrides)

export const generateBulkCustomers = (count: number, options?: any) =>
  EnhancedDataGenerator.generateCustomers(count, options)

export const generateEcommerceDataset = () =>
  EnhancedDataGenerator.generateEcommerceScenario()
