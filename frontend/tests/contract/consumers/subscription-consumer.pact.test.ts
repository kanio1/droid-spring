/**
 * Subscription API Consumer Contract Tests
 *
 * Tests the frontend's interaction with the Subscription API
 * Generates pact files that can be used to verify the backend provider
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { createConsumerPact, PACT_CONFIG } from '../pact-config'
import { Matchers } from '@pact-foundation/pact'
import { TestDataGenerator } from '../../framework/data-factories'

// Import API client
// import { SubscriptionApiClient } from '@/api/clients/subscription-client'

describe('Subscription API Consumer', () => {
  const { consumer, provider } = PACT_CONFIG
  const pact = createConsumerPact(consumer.name, provider.name)

  beforeAll(async () => {
    await pact.setup()
  })

  afterAll(async () => {
    await pact.writePact()
    await pact.finalize()
  })

  describe('GET /api/subscriptions', () => {
    it('should return a list of subscriptions', async () => {
      const data = TestDataGenerator.scenario.fullCustomerJourney()

      await pact
        .given('subscriptions exist in the system')
        .uponReceiving('a request for all subscriptions')
        .withRequest({
          method: 'GET',
          path: '/api/subscriptions',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            page: '1',
            limit: '20'
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            data: Matchers.eachLike({
              id: Matchers.uuid(),
              customerId: Matchers.uuid(),
              customer: {
                firstName: Matchers.like('John'),
                lastName: Matchers.like('Doe')
              },
              planId: Matchers.like('plan_pro'),
              plan: {
                id: Matchers.like('plan_pro'),
                name: Matchers.like('Professional Plan'),
                price: Matchers.like(99.99),
                currency: Matchers.like('USD'),
                billingInterval: Matchers.like('monthly'),
                features: Matchers.eachLike(Matchers.like('Feature A'))
              },
              status: Matchers.like('active'),
              startDate: Matchers.iso8601DateTime(),
              endDate: Matchers.iso8601DateTime(),
              nextBillingDate: Matchers.iso8601DateTime(),
              cancelAtPeriodEnd: Matchers.like(false),
              trialEndDate: Matchers.iso8601DateTime(),
              createdAt: Matchers.iso8601DateTime(),
              updatedAt: Matchers.iso8601DateTime()
            }),
            pagination: {
              page: Matchers.like(1),
              limit: Matchers.like(20),
              total: Matchers.like(100),
              totalPages: Matchers.like(5)
            }
          }
        })
    })

    it('should filter subscriptions by status', async () => {
      await pact
        .given('subscriptions with different statuses exist')
        .uponReceiving('a request for active subscriptions')
        .withRequest({
          method: 'GET',
          path: '/api/subscriptions',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            status: 'active',
            page: '1',
            limit: '20'
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            data: Matchers.eachLike({
              id: Matchers.uuid(),
              status: Matchers.like('active')
            })
          }
        })
    })

    it('should filter subscriptions by plan', async () => {
      await pact
        .given('subscriptions with different plans exist')
        .uponReceiving('a request for premium plan subscriptions')
        .withRequest({
          method: 'GET',
          path: '/api/subscriptions',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            plan: 'plan_pro',
            page: '1',
            limit: '20'
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            data: Matchers.eachLike({
              id: Matchers.uuid(),
              planId: Matchers.like('plan_pro')
            })
          }
        })
    })
  })

  describe('GET /api/subscriptions/:id', () => {
    it('should return a subscription by ID', async () => {
      const subscriptionId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a subscription exists with ID 123e4567-e89b-12d3-a456-426614174000')
        .uponReceiving('a request for a specific subscription by ID')
        .withRequest({
          method: 'GET',
          path: `/api/subscriptions/${subscriptionId}`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(subscriptionId),
            customerId: Matchers.uuid(),
            customer: {
              firstName: Matchers.like('John'),
              lastName: Matchers.like('Doe'),
              email: Matchers.email()
            },
            planId: Matchers.like('plan_pro'),
            plan: {
              id: Matchers.like('plan_pro'),
              name: Matchers.like('Professional Plan'),
              price: Matchers.like(99.99),
              currency: Matchers.like('USD'),
              billingInterval: Matchers.like('monthly')
            },
            status: Matchers.like('active'),
            startDate: Matchers.iso8601DateTime(),
            nextBillingDate: Matchers.iso8601DateTime(),
            cancelAtPeriodEnd: Matchers.like(false),
            createdAt: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('POST /api/subscriptions', () => {
    it('should create a new subscription', async () => {
      const data = TestDataGenerator.scenario.trialConversion()

      await pact
        .given('a valid subscription payload with plan')
        .uponReceiving('a request to create a new subscription')
        .withRequest({
          method: 'POST',
          path: '/api/subscriptions',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            customerId: Matchers.uuid(),
            planId: Matchers.like('plan_pro'),
            billingInterval: Matchers.like('monthly'),
            trialDays: Matchers.like(14)
          }
        })
        .willRespondWith({
          status: 201,
          headers: {
            'Content-Type': 'application/json',
            'Location': Matchers.regex('\\/api\\/subscriptions\\/[0-9a-f-]+', '/api/subscriptions/123e4567-e89b-12d3-a456-426614174000')
          },
          body: {
            id: Matchers.uuid(),
            customerId: Matchers.uuid(),
            planId: Matchers.like('plan_pro'),
            status: Matchers.like('trial'),
            startDate: Matchers.iso8601DateTime(),
            trialEndDate: Matchers.iso8601DateTime(),
            nextBillingDate: Matchers.iso8601DateTime(),
            createdAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('PUT /api/subscriptions/:id/activate', () => {
    it('should activate a trial subscription', async () => {
      const subscriptionId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a trial subscription exists and can be activated')
        .uponReceiving('a request to activate a subscription')
        .withRequest({
          method: 'PUT',
          path: `/api/subscriptions/${subscriptionId}/activate`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            paymentMethod: Matchers.like('credit_card')
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(subscriptionId),
            status: Matchers.like('active'),
            activatedAt: Matchers.iso8601DateTime(),
            nextBillingDate: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('PUT /api/subscriptions/:id/cancel', () => {
    it('should cancel a subscription', async () => {
      const subscriptionId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('an active subscription exists and can be cancelled')
        .uponReceiving('a request to cancel a subscription')
        .withRequest({
          method: 'PUT',
          path: `/api/subscriptions/${subscriptionId}/cancel`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            reason: Matchers.like('customer_request'),
            cancelAtPeriodEnd: Matchers.like(false)
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(subscriptionId),
            status: Matchers.like('cancelled'),
            cancelledAt: Matchers.iso8601DateTime(),
            endDate: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('PUT /api/subscriptions/:id/change-plan', () => {
    it('should change subscription plan', async () => {
      const subscriptionId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a subscription exists and plan change is valid')
        .uponReceiving('a request to change subscription plan')
        .withRequest({
          method: 'PUT',
          path: `/api/subscriptions/${subscriptionId}/change-plan`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            newPlanId: Matchers.like('plan_enterprise'),
            upgradeType: Matchers.like('immediate')
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(subscriptionId),
            planId: Matchers.like('plan_enterprise'),
            plan: {
              id: Matchers.like('plan_enterprise'),
              name: Matchers.like('Enterprise Plan'),
              price: Matchers.like(499.99)
            },
            status: Matchers.like('active'),
            nextBillingDate: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('GET /api/subscriptions/:id/usage', () => {
    it('should return subscription usage statistics', async () => {
      const subscriptionId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a subscription with usage tracking exists')
        .uponReceiving('a request for subscription usage')
        .withRequest({
          method: 'GET',
          path: `/api/subscriptions/${subscriptionId}/usage`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            subscriptionId: Matchers.uuid(subscriptionId),
            period: Matchers.like('2025-11'),
            usage: {
              data: {
                used: Matchers.like(50),
                limit: Matchers.like(100),
                unit: Matchers.like('GB')
              },
              minutes: {
                used: Matchers.like(500),
                limit: Matchers.like(1000),
                unit: Matchers.like('minutes')
              },
              sms: {
                used: Matchers.like(100),
                limit: Matchers.like(500),
                unit: Matchers.like('messages')
              }
            },
            billing: {
              overageCharges: Matchers.like(0.00),
              nextBillingDate: Matchers.iso8601DateTime()
            }
          }
        })
    })
  })
})
