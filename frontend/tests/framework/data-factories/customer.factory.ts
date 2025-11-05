/**
 * Customer Test Data Factory
 *
 * Implements Object Mother pattern for generating test data
 * Provides fluent interface for creating Customer entities
 *
 * Usage:
 * ```typescript
 * const customer = CustomerFactory.create()
 *   .withEmail('test@example.com')
 *   .active()
 *   .build()
 * ```
 */

import { faker } from '@faker-js/faker'
import type { Customer } from '../../app/schemas/customer'

export type CustomerStatus = 'active' | 'inactive' | 'pending' | 'suspended'

export interface CustomerFactoryOptions {
  email?: string
  firstName?: string
  lastName?: string
  phone?: string
  status?: CustomerStatus
  createdAt?: Date
  updatedAt?: Date
  metadata?: Record<string, any>
}

export class CustomerFactory {
  private options: CustomerFactoryOptions = {}

  static create(): CustomerFactory {
    return new CustomerFactory()
  }

  withEmail(email: string): CustomerFactory {
    this.options.email = email
    return this
  }

  withRandomEmail(): CustomerFactory {
    this.options.email = faker.internet.email()
    return this
  }

  withFirstName(name: string): CustomerFactory {
    this.options.firstName = name
    return this
  }

  withRandomFirstName(): CustomerFactory {
    this.options.firstName = faker.person.firstName()
    return this
  }

  withLastName(name: string): CustomerFactory {
    this.options.lastName = name
    return this
  }

  withRandomLastName(): CustomerFactory {
    this.options.lastName = faker.person.lastName()
    return this
  }

  withPhone(phone: string): CustomerFactory {
    this.options.phone = phone
    return this
  }

  withRandomPhone(): CustomerFactory {
    this.options.phone = faker.phone.number()
    return this
  }

  active(): CustomerFactory {
    this.options.status = 'active'
    return this
  }

  inactive(): CustomerFactory {
    this.options.status = 'inactive'
    return this
  }

  pending(): CustomerFactory {
    this.options.status = 'pending'
    return this
  }

  suspended(): CustomerFactory {
    this.options.status = 'suspended'
    return this
  }

  withCreatedAt(date: Date): CustomerFactory {
    this.options.createdAt = date
    return this
  }

  withUpdatedAt(date: Date): CustomerFactory {
    this.options.updatedAt = date
    return this
  }

  withMetadata(metadata: Record<string, any>): CustomerFactory {
    this.options.metadata = metadata
    return this
  }

  build(): Customer {
    const now = new Date()

    return {
      id: faker.string.uuid(),
      email: this.options.email || faker.internet.email(),
      firstName: this.options.firstName || faker.person.firstName(),
      lastName: this.options.lastName || faker.person.lastName(),
      phone: this.options.phone || faker.phone.number(),
      status: this.options.status || 'active',
      createdAt: this.options.createdAt?.toISOString() || now.toISOString(),
      updatedAt: this.options.updatedAt?.toISOString() || now.toISOString(),
      metadata: this.options.metadata || {}
    }
  }

  buildMany(count: number): Customer[] {
    return Array.from({ length: count }, () => this.build())
  }
}

/**
 * Predefined customer profiles for common test scenarios
 */
export class CustomerProfiles {
  static get activeCustomer(): Customer {
    return CustomerFactory.create()
      .active()
      .build()
  }

  static get pendingCustomer(): Customer {
    return CustomerFactory.create()
      .pending()
      .build()
  }

  static get suspendedCustomer(): Customer {
    return CustomerFactory.create()
      .suspended()
      .build()
  }

  static get enterpriseCustomer(): Customer {
    return CustomerFactory.create()
      .withFirstName('Enterprise')
      .withLastName('Client')
      .withEmail('enterprise@company.test')
      .withPhone('+1-555-0199')
      .active()
      .withMetadata({
        tier: 'enterprise',
        accountManager: 'john.doe@company.test',
        sla: 'premium'
      })
      .build()
  }

  static get vipCustomer(): Customer {
    return CustomerFactory.create()
      .withFirstName('VIP')
      .withLastName('Customer')
      .withEmail('vip@example.test')
      .withPhone('+1-555-0188')
      .active()
      .withMetadata({
        tier: 'vip',
        priority: 'high',
        discountCode: 'VIP2024'
      })
      .build()
  }
}
