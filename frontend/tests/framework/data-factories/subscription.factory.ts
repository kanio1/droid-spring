/**
 * Subscription Test Data Factory
 *
 * Implements Builder pattern for Subscription entities
 * Supports billing cycles, plan changes, and cancellation flows
 */

import { faker } from '@faker-js/faker'
import type { Subscription } from '../../app/schemas/subscription'
import { CustomerFactory, type Customer } from './customer.factory'

export type SubscriptionStatus = 'active' | 'cancelled' | 'expired' | 'paused' | 'trial' | 'past_due'

export interface SubscriptionPlan {
  id: string
  name: string
  price: number
  currency: string
  billingInterval: 'monthly' | 'quarterly' | 'yearly'
  features: string[]
  limits: Record<string, number>
}

export interface SubscriptionFactoryOptions {
  customer?: Customer
  plan?: SubscriptionPlan
  status?: SubscriptionStatus
  startDate?: Date
  endDate?: Date
  nextBillingDate?: Date
  cancelAtPeriodEnd?: boolean
  trialEndDate?: Date
  metadata?: Record<string, any>
}

export class SubscriptionFactory {
  private options: SubscriptionFactoryOptions = {}

  static create(): SubscriptionFactory {
    return new SubscriptionFactory()
  }

  static getBasicPlan(): SubscriptionPlan {
    return {
      id: 'plan_basic',
      name: 'Basic Plan',
      price: 29.99,
      currency: 'USD',
      billingInterval: 'monthly',
      features: [
        '10 GB Storage',
        'Basic Support',
        'Standard Features'
      ],
      limits: {
        projects: 5,
        users: 3,
        storage: 10
      }
    }
  }

  static getProPlan(): SubscriptionPlan {
    return {
      id: 'plan_pro',
      name: 'Professional Plan',
      price: 99.99,
      currency: 'USD',
      billingInterval: 'monthly',
      features: [
        '100 GB Storage',
        'Priority Support',
        'Advanced Features',
        'API Access'
      ],
      limits: {
        projects: 50,
        users: 25,
        storage: 100
      }
    }
  }

  static getEnterprisePlan(): SubscriptionPlan {
    return {
      id: 'plan_enterprise',
      name: 'Enterprise Plan',
      price: 499.99,
      currency: 'USD',
      billingInterval: 'monthly',
      features: [
        'Unlimited Storage',
        '24/7 Dedicated Support',
        'All Features',
        'Custom Integrations',
        'SLA Guarantee'
      ],
      limits: {
        projects: -1,
        users: -1,
        storage: -1
      }
    }
  }

  withCustomer(customer: Customer): SubscriptionFactory {
    this.options.customer = customer
    return this
  }

  withRandomCustomer(): SubscriptionFactory {
    this.options.customer = CustomerFactory.create().build()
    return this
  }

  withPlan(plan: SubscriptionPlan): SubscriptionFactory {
    this.options.plan = plan
    return this
  }

  withBasicPlan(): SubscriptionFactory {
    this.options.plan = SubscriptionFactory.getBasicPlan()
    return this
  }

  withProPlan(): SubscriptionFactory {
    this.options.plan = SubscriptionFactory.getProPlan()
    return this
  }

  withEnterprisePlan(): SubscriptionFactory {
    this.options.plan = SubscriptionFactory.getEnterprisePlan()
    return this
  }

  active(): SubscriptionFactory {
    this.options.status = 'active'
    return this
  }

  cancelled(): SubscriptionFactory {
    this.options.status = 'cancelled'
    return this
  }

  expired(): SubscriptionFactory {
    this.options.status = 'expired'
    return this
  }

  paused(): SubscriptionFactory {
    this.options.status = 'paused'
    return this
  }

  trial(): SubscriptionFactory {
    this.options.status = 'trial'
    return this
  }

  pastDue(): SubscriptionFactory {
    this.options.status = 'past_due'
    return this
  }

  withStartDate(date: Date): SubscriptionFactory {
    this.options.startDate = date
    return this
  }

  withRandomStartDate(yearsAgo: number = 1): SubscriptionFactory {
    this.options.startDate = faker.date.past({ years: yearsAgo })
    return this
  }

  withEndDate(date: Date): SubscriptionFactory {
    this.options.endDate = date
    return this
  }

  withRandomEndDate(): SubscriptionFactory {
    this.options.endDate = faker.date.soon({ days: 365 })
    return this
  }

  withNextBillingDate(date: Date): SubscriptionFactory {
    this.options.nextBillingDate = date
    return this
  }

  withRandomNextBillingDate(daysFromNow: number = 30): SubscriptionFactory {
    this.options.nextBillingDate = faker.date.soon({ days: daysFromNow })
    return this
  }

  withCancelAtPeriodEnd(cancel: boolean = true): SubscriptionFactory {
    this.options.cancelAtPeriodEnd = cancel
    return this
  }

  withTrialEndDate(date: Date): SubscriptionFactory {
    this.options.trialEndDate = date
    return this
  }

  withRandomTrialEndDate(daysFromNow: number = 14): SubscriptionFactory {
    this.options.trialEndDate = faker.date.soon({ days: daysFromNow })
    return this
  }

  withMetadata(metadata: Record<string, any>): SubscriptionFactory {
    this.options.metadata = metadata
    return this
  }

  build(): Subscription {
    const now = new Date()
    const startDate = this.options.startDate || now
    const endDate = this.options.endDate
    const plan = this.options.plan || SubscriptionFactory.getBasicPlan()

    // Calculate next billing date based on plan
    let nextBillingDate = this.options.nextBillingDate
    if (!nextBillingDate) {
      const intervalDays = {
        monthly: 30,
        quarterly: 90,
        yearly: 365
      }[plan.billingInterval]
      nextBillingDate = faker.date.soon({ days: intervalDays })
    }

    return {
      id: faker.string.uuid(),
      customerId: this.options.customer?.id || faker.string.uuid(),
      customer: this.options.customer,
      planId: plan.id,
      plan: plan,
      status: this.options.status || 'active',
      startDate: startDate.toISOString(),
      endDate: endDate?.toISOString(),
      nextBillingDate: nextBillingDate.toISOString(),
      cancelAtPeriodEnd: this.options.cancelAtPeriodEnd || false,
      trialEndDate: this.options.trialEndDate?.toISOString(),
      createdAt: startDate.toISOString(),
      updatedAt: now.toISOString(),
      metadata: this.options.metadata || {}
    }
  }

  buildMany(count: number): Subscription[] {
    return Array.from({ length: count }, () => this.build())
  }
}

export class SubscriptionProfiles {
  static get activeBasicSubscription(): Subscription {
    return SubscriptionFactory.create()
      .active()
      .withBasicPlan()
      .build()
  }

  static get activeProSubscription(): Subscription {
    return SubscriptionFactory.create()
      .active()
      .withProPlan()
      .withRandomStartDate(2)
      .withRandomNextBillingDate(25)
      .build()
  }

  static get enterpriseSubscription(): Subscription {
    return SubscriptionFactory.create()
      .active()
      .withEnterprisePlan()
      .withCustomer(CustomerFactory.create().enterpriseCustomer)
      .build()
  }

  static get trialSubscription(): Subscription {
    return SubscriptionFactory.create()
      .trial()
      .withProPlan()
      .withRandomTrialEndDate(14)
      .build()
  }

  static get cancelledSubscription(): Subscription {
    return SubscriptionFactory.create()
      .cancelled()
      .withBasicPlan()
      .withEndDate(new Date())
      .build()
  }

  static get expiredSubscription(): Subscription {
    return SubscriptionFactory.create()
      .expired()
      .withBasicPlan()
      .withEndDate(faker.date.past({ years: 1 }))
      .build()
  }

  static get subscriptionWithAutoRenew(): Subscription {
    return SubscriptionFactory.create()
      .active()
      .withProPlan()
      .withCancelAtPeriodEnd(false)
      .withRandomNextBillingDate(28)
      .withMetadata({
        autoRenew: true,
        paymentMethodId: faker.string.uuid(),
        lastPaymentStatus: 'succeeded'
      })
      .build()
  }

  static get pastDueSubscription(): Subscription {
    return SubscriptionFactory.create()
      .pastDue()
      .withBasicPlan()
      .withMetadata({
        failedPaymentsCount: 2,
        lastFailedPaymentDate: faker.date.past({ days: 5 }).toISOString(),
        dunningStatus: 'active'
      })
      .build()
  }
}
