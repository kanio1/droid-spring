/**
 * Payment Test Data Factory
 *
 * Implements Builder pattern for Payment entities
 * Supports multiple payment methods and status transitions
 */

import { faker } from '@faker-js/faker'
import type { Payment } from '../../app/schemas/payment'
import { CustomerFactory, type Customer } from './customer.factory'
import { InvoiceFactory, type Invoice } from './invoice.factory'

export type PaymentStatus = 'pending' | 'processing' | 'completed' | 'failed' | 'cancelled' | 'refunded'
export type PaymentMethod = 'credit_card' | 'debit_card' | 'paypal' | 'bank_transfer' | 'crypto' | 'cash'

export interface PaymentMethodData {
  type: 'card'
  lastFourDigits?: string
  brand?: 'visa' | 'mastercard' | 'amex' | 'discover'
  expiryMonth?: number
  expiryYear?: number
} | {
  type: 'paypal'
  email: string
} | {
  type: 'bank_transfer'
  iban?: string
  swift?: string
  accountNumber?: string
} | {
  type: 'crypto'
  currency: string
  walletAddress: string
  transactionHash: string
} | {
  type: 'cash'
  reference: string
}

export interface PaymentFactoryOptions {
  amount?: number
  currency?: string
  method?: PaymentMethod
  methodData?: PaymentMethodData
  status?: PaymentStatus
  customer?: Customer
  invoice?: Invoice
  transactionId?: string
  gatewayTransactionId?: string
  processedAt?: Date
  failureReason?: string
  metadata?: Record<string, any>
}

export class PaymentFactory {
  private options: PaymentFactoryOptions = {}

  static create(): PaymentFactory {
    return new PaymentFactory()
  }

  withAmount(amount: number): PaymentFactory {
    this.options.amount = amount
    return this
  }

  withRandomAmount(min: number = 50, max: number = 10000): PaymentFactory {
    this.options.amount = faker.number.float({ min, max, multipleOf: 0.01 })
    return this
  }

  withCurrency(currency: string): PaymentFactory {
    this.options.currency = currency
    return this
  }

  withMethod(method: PaymentMethod): PaymentFactory {
    this.options.method = method
    return this
  }

  withCreditCard(): PaymentFactory {
    this.options.method = 'credit_card'
    this.options.methodData = {
      type: 'card',
      lastFourDigits: faker.string.numeric(4),
      brand: faker.helpers.arrayElement(['visa', 'mastercard', 'amex', 'discover']),
      expiryMonth: faker.number.int({ min: 1, max: 12 }),
      expiryYear: faker.number.int({ min: 2025, max: 2030 })
    }
    return this
  }

  withDebitCard(): PaymentFactory {
    this.options.method = 'debit_card'
    this.options.methodData = {
      type: 'card',
      lastFourDigits: faker.string.numeric(4),
      brand: faker.helpers.arrayElement(['visa', 'mastercard'])
    }
    return this
  }

  withPayPal(): PaymentFactory {
    this.options.method = 'paypal'
    this.options.methodData = {
      type: 'paypal',
      email: faker.internet.email()
    }
    return this
  }

  withBankTransfer(): PaymentFactory {
    this.options.method = 'bank_transfer'
    this.options.methodData = {
      type: 'bank_transfer',
      iban: faker.finance.iban(),
      swift: faker.string.alphanumeric(8).toUpperCase(),
      accountNumber: faker.string.numeric(10)
    }
    return this
  }

  withCrypto(): PaymentFactory {
    this.options.method = 'crypto'
    this.options.methodData = {
      type: 'crypto',
      currency: faker.helpers.arrayElement(['BTC', 'ETH', 'USDT', 'USDC']),
      walletAddress: faker.string.hexadecimal({ length: 42 }),
      transactionHash: faker.string.hexadecimal({ length: 64 })
    }
    return this
  }

  withCash(): PaymentFactory {
    this.options.method = 'cash'
    this.options.methodData = {
      type: 'cash',
      reference: faker.string.alphanumeric(12).toUpperCase()
    }
    return this
  }

  pending(): PaymentFactory {
    this.options.status = 'pending'
    return this
  }

  processing(): PaymentFactory {
    this.options.status = 'processing'
    return this
  }

  completed(): PaymentFactory {
    this.options.status = 'completed'
    this.options.processedAt = new Date()
    return this
  }

  failed(): PaymentFactory {
    this.options.status = 'failed'
    this.options.failureReason = faker.helpers.arrayElement([
      'Insufficient funds',
      'Card declined',
      'Network error',
      'Invalid credentials',
      '3D Secure authentication failed'
    ])
    return this
  }

  cancelled(): PaymentFactory {
    this.options.status = 'cancelled'
    return this
  }

  refunded(): PaymentFactory {
    this.options.status = 'refunded'
    this.options.processedAt = new Date()
    return this
  }

  withCustomer(customer: Customer): PaymentFactory {
    this.options.customer = customer
    return this
  }

  withRandomCustomer(): PaymentFactory {
    this.options.customer = CustomerFactory.create().build()
    return this
  }

  withInvoice(invoice: Invoice): PaymentFactory {
    this.options.invoice = invoice
    this.options.amount = invoice.totalAmount
    return this
  }

  withRandomInvoice(): PaymentFactory {
    this.options.invoice = InvoiceFactory.create().build()
    this.options.amount = this.options.invoice.totalAmount
    return this
  }

  withTransactionId(id: string): PaymentFactory {
    this.options.transactionId = id
    return this
  }

  withRandomTransactionId(): PaymentFactory {
    this.options.transactionId = `txn_${faker.string.alphanumeric(24)}`
    return this
  }

  withGatewayTransactionId(id: string): PaymentFactory {
    this.options.gatewayTransactionId = id
    return this
  }

  withRandomGatewayTransactionId(): PaymentFactory {
    this.options.gatewayTransactionId = faker.string.alphanumeric(32)
    return this
  }

  withProcessedAt(date: Date): PaymentFactory {
    this.options.processedAt = date
    return this
  }

  withRandomProcessedAt(): PaymentFactory {
    this.options.processedAt = faker.date.recent({ days: 30 })
    return this
  }

  withMetadata(metadata: Record<string, any>): PaymentFactory {
    this.options.metadata = metadata
    return this
  }

  build(): Payment {
    const now = new Date()

    return {
      id: faker.string.uuid(),
      transactionId: this.options.transactionId || `txn_${faker.string.alphanumeric(24)}`,
      gatewayTransactionId: this.options.gatewayTransactionId || faker.string.alphanumeric(32),
      amount: this.options.amount || faker.number.float({ min: 50, max: 10000, multipleOf: 0.01 }),
      currency: this.options.currency || 'USD',
      method: this.options.method || 'credit_card',
      methodData: this.options.methodData as any,
      status: this.options.status || 'pending',
      customerId: this.options.customer?.id || faker.string.uuid(),
      customer: this.options.customer,
      invoiceId: this.options.invoice?.id,
      invoice: this.options.invoice,
      processedAt: this.options.processedAt?.toISOString(),
      failureReason: this.options.failureReason,
      createdAt: now.toISOString(),
      updatedAt: now.toISOString(),
      metadata: this.options.metadata || {}
    }
  }

  buildMany(count: number): Payment[] {
    return Array.from({ length: count }, () => this.build())
  }
}

export class PaymentProfiles {
  static get successfulCreditCardPayment(): Payment {
    return PaymentFactory.create()
      .completed()
      .withCreditCard()
      .withRandomAmount(100, 5000)
      .build()
  }

  static get failedPayment(): Payment {
    return PaymentFactory.create()
      .failed()
      .withDebitCard()
      .build()
  }

  static get pendingPayment(): Payment {
    return PaymentFactory.create()
      .processing()
      .withPayPal()
      .build()
  }

  static get refundedPayment(): Payment {
    return PaymentFactory.create()
      .refunded()
      .withBankTransfer()
      .build()
  }

  static get cryptoPayment(): Payment {
    return PaymentFactory.create()
      .completed()
      .withCrypto()
      .build()
  }

  static get largePayment(): Payment {
    return PaymentFactory.create()
      .completed()
      .withAmount(100000)
      .withBankTransfer()
      .withMetadata({
        reference: 'Large Enterprise Payment',
        approvalCode: faker.string.alphanumeric(8).toUpperCase()
      })
      .build()
  }

  static get monthlyRecurringPayment(): Payment {
    return PaymentFactory.create()
      .completed()
      .withCreditCard()
      .withRandomAmount(99, 299)
      .withMetadata({
        recurring: true,
        billingCycle: 'monthly',
        nextBillingDate: faker.date.soon({ days: 30 }).toISOString()
      })
      .build()
  }
}
