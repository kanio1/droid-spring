/**
 * Order Test Data Factory
 *
 * Implements Builder pattern with relationships
 * Supports nested data generation (Customer, Products, etc.)
 */

import { faker } from '@faker-js/faker'
import type { Order, OrderItem } from '../../app/schemas/order'
import { CustomerFactory, type Customer } from './customer.factory'

export type OrderStatus = 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled' | 'refunded'

export interface OrderFactoryOptions {
  customer?: Customer
  status?: OrderStatus
  items?: OrderItem[]
  totalAmount?: number
  currency?: string
  createdAt?: Date
  updatedAt?: Date
  metadata?: Record<string, any>
}

export interface OrderItemOptions {
  productId?: string
  productName?: string
  quantity?: number
  unitPrice?: number
}

export class OrderItemFactory {
  private options: OrderItemOptions = {}

  static create(): OrderItemFactory {
    return new OrderItemFactory()
  }

  withProductId(id: string): OrderItemFactory {
    this.options.productId = id
    return this
  }

  withRandomProductId(): OrderItemFactory {
    this.options.productId = faker.string.uuid()
    return this
  }

  withProductName(name: string): OrderItemFactory {
    this.options.productName = name
    return this
  }

  withRandomProductName(): OrderItemFactory {
    this.options.productName = faker.commerce.productName()
    return this
  }

  withQuantity(quantity: number): OrderItemFactory {
    this.options.quantity = quantity
    return this
  }

  withUnitPrice(price: number): OrderItemFactory {
    this.options.unitPrice = price
    return this
  }

  build(): OrderItem {
    const quantity = this.options.quantity || faker.number.int({ min: 1, max: 10 })
    const unitPrice = this.options.unitPrice || faker.number.float({ min: 10, max: 1000 })

    return {
      productId: this.options.productId || faker.string.uuid(),
      productName: this.options.productName || faker.commerce.productName(),
      quantity,
      unitPrice,
      totalPrice: quantity * unitPrice
    }
  }

  buildMany(count: number): OrderItem[] {
    return Array.from({ length: count }, () => this.build())
  }
}

export class OrderFactory {
  private options: OrderFactoryOptions = {}

  static create(): OrderFactory {
    return new OrderFactory()
  }

  withCustomer(customer: Customer): OrderFactory {
    this.options.customer = customer
    return this
  }

  withRandomCustomer(): OrderFactory {
    this.options.customer = CustomerFactory.create().build()
    return this
  }

  pending(): OrderFactory {
    this.options.status = 'pending'
    return this
  }

  processing(): OrderFactory {
    this.options.status = 'processing'
    return this
  }

  shipped(): OrderFactory {
    this.options.status = 'shipped'
    return this
  }

  delivered(): OrderFactory {
    this.options.status = 'delivered'
    return this
  }

  cancelled(): OrderFactory {
    this.options.status = 'cancelled'
    return this
  }

  refunded(): OrderFactory {
    this.options.status = 'refunded'
    return this
  }

  withItems(items: OrderItem[]): OrderFactory {
    this.options.items = items
    return this
  }

  withRandomItems(count: number = 3): OrderFactory {
    this.options.items = OrderItemFactory.create().buildMany(count)
    return this
  }

  withTotalAmount(amount: number): OrderFactory {
    this.options.totalAmount = amount
    return this
  }

  withCurrency(currency: string): OrderFactory {
    this.options.currency = currency
    return this
  }

  withCreatedAt(date: Date): OrderFactory {
    this.options.createdAt = date
    return this
  }

  withUpdatedAt(date: Date): OrderFactory {
    this.options.updatedAt = date
    return this
  }

  withMetadata(metadata: Record<string, any>): OrderFactory {
    this.options.metadata = metadata
    return this
  }

  build(): Order {
    const now = new Date()
    const items = this.options.items || OrderItemFactory.create().buildMany(3)

    const totalAmount = this.options.totalAmount
      || items.reduce((sum, item) => sum + item.totalPrice, 0)

    return {
      id: faker.string.uuid(),
      customerId: this.options.customer?.id || faker.string.uuid(),
      customer: this.options.customer,
      items,
      status: this.options.status || 'pending',
      totalAmount,
      currency: this.options.currency || 'USD',
      createdAt: this.options.createdAt?.toISOString() || now.toISOString(),
      updatedAt: this.options.updatedAt?.toISOString() || now.toISOString(),
      metadata: this.options.metadata || {}
    }
  }

  buildMany(count: number): Order[] {
    return Array.from({ length: count }, () => this.build())
  }
}

export class OrderProfiles {
  static get pendingOrder(): Order {
    return OrderFactory.create()
      .pending()
      .withRandomItems(2)
      .build()
  }

  static get completedOrder(): Order {
    return OrderFactory.create()
      .delivered()
      .withRandomCustomer()
      .withRandomItems(5)
      .build()
  }

  static get largeOrder(): Order {
    const items = OrderItemFactory.create()
      .withProductName('Premium Package')
      .withQuantity(50)
      .withUnitPrice(100)
      .buildMany(10)

    return OrderFactory.create()
      .delivered()
      .withItems(items)
      .withTotalAmount(50000)
      .build()
  }

  static get internationalOrder(): Order {
    return OrderFactory.create()
      .processing()
      .withCurrency('EUR')
      .withRandomItems(3)
      .build()
  }
}
