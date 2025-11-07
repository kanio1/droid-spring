/**
 * Payment API Consumer Contract Tests
 *
 * Tests the frontend's interaction with the Payment API
 * Generates pact files that can be used to verify the backend provider
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { createConsumerPact, PACT_CONFIG } from '../pact-config'
import { Matchers } from '@pact-foundation/pact'
import { TestDataGenerator } from '../../framework/data-factories'

// Import API client
// import { PaymentApiClient } from '@/api/clients/payment-client'

describe('Payment API Consumer', () => {
  const { consumer, provider } = PACT_CONFIG
  const pact = createConsumerPact(consumer.name, provider.name)

  beforeAll(async () => {
    await pact.setup()
  })

  afterAll(async () => {
    await pact.writePact()
    await pact.finalize()
  })

  describe('GET /api/payments', () => {
    it('should return a list of payments', async () => {
      const data = TestDataGenerator.scenario.fullCustomerJourney()
      const payment = data.payment!

      await pact
        .given('payments exist in the system')
        .uponReceiving('a request for all payments')
        .withRequest({
          method: 'GET',
          path: '/api/payments',
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
              transactionId: Matchers.like('txn_abc123'),
              gatewayTransactionId: Matchers.like('gate_xyz789'),
              amount: Matchers.like(200.00),
              currency: Matchers.like('USD'),
              method: Matchers.like('credit_card'),
              methodData: Matchers.like({
                type: 'card',
                lastFourDigits: '1234',
                brand: 'visa'
              }),
              status: Matchers.like('completed'),
              customerId: Matchers.uuid(),
              customer: {
                firstName: Matchers.like('John'),
                lastName: Matchers.like('Doe')
              },
              invoiceId: Matchers.uuid(),
              processedAt: Matchers.iso8601DateTime(),
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

    it('should filter payments by status', async () => {
      await pact
        .given('payments with different statuses exist')
        .uponReceiving('a request for successful payments')
        .withRequest({
          method: 'GET',
          path: '/api/payments',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            status: 'completed',
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
              status: Matchers.like('completed')
            })
          }
        })
    })

    it('should filter payments by method', async () => {
      await pact
        .given('payments with different methods exist')
        .uponReceiving('a request for credit card payments')
        .withRequest({
          method: 'GET',
          path: '/api/payments',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            method: 'credit_card',
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
              method: Matchers.like('credit_card')
            })
          }
        })
    })
  })

  describe('GET /api/payments/:id', () => {
    it('should return a payment by ID', async () => {
      const paymentId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a payment exists with ID 123e4567-e89b-12d3-a456-426614174000')
        .uponReceiving('a request for a specific payment by ID')
        .withRequest({
          method: 'GET',
          path: `/api/payments/${paymentId}`,
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
            id: Matchers.uuid(paymentId),
            transactionId: Matchers.like('txn_abc123'),
            amount: Matchers.like(200.00),
            currency: Matchers.like('USD'),
            method: Matchers.like('credit_card'),
            methodData: Matchers.like({
              type: 'card',
              lastFourDigits: '1234',
              brand: 'visa'
            }),
            status: Matchers.like('completed'),
            customerId: Matchers.uuid(),
            invoiceId: Matchers.uuid(),
            processedAt: Matchers.iso8601DateTime(),
            createdAt: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('POST /api/payments', () => {
    it('should create a new payment', async () => {
      const data = TestDataGenerator.scenario.fullCustomerJourney()
      const customer = data.customer
      const invoice = data.invoice

      await pact
        .given('a valid payment payload')
        .uponReceiving('a request to create a new payment')
        .withRequest({
          method: 'POST',
          path: '/api/payments',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            customerId: Matchers.uuid(),
            invoiceId: Matchers.uuid(),
            amount: Matchers.like(200.00),
            method: Matchers.like('credit_card'),
            methodData: Matchers.like({
              type: 'card',
              token: Matchers.like('payment_token_123')
            })
          }
        })
        .willRespondWith({
          status: 201,
          headers: {
            'Content-Type': 'application/json',
            'Location': Matchers.regex('\\/api\\/payments\\/[0-9a-f-]+', '/api/payments/123e4567-e89b-12d3-a456-426614174000')
          },
          body: {
            id: Matchers.uuid(),
            transactionId: Matchers.like('txn_abc123'),
            amount: Matchers.like(200.00),
            status: Matchers.like('pending'),
            createdAt: Matchers.iso8601DateTime()
          }
        })
    })

    it('should process a payment successfully', async () => {
      await pact
        .given('a valid payment with valid payment method')
        .uponReceiving('a request to process a payment')
        .withRequest({
          method: 'POST',
          path: '/api/payments',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            customerId: Matchers.uuid(),
            amount: Matchers.like(200.00),
            method: Matchers.like('credit_card'),
            methodData: Matchers.like({
              type: 'card',
              token: Matchers.like('valid_payment_token')
            })
          }
        })
        .willRespondWith({
          status: 201,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(),
            transactionId: Matchers.like('txn_success123'),
            amount: Matchers.like(200.00),
            status: Matchers.like('completed'),
            processedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('POST /api/payments/:id/refund', () => {
    it('should refund a payment', async () => {
      const paymentId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a payment exists and can be refunded')
        .uponReceiving('a request to refund a payment')
        .withRequest({
          method: 'POST',
          path: `/api/payments/${paymentId}/refund`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            amount: Matchers.like(200.00),
            reason: Matchers.like('customer_request'),
            notes: Matchers.like('Customer requested refund')
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(paymentId),
            status: Matchers.like('refunded'),
            refundAmount: Matchers.like(200.00),
            refundReason: Matchers.like('customer_request'),
            refundedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('GET /api/payments/:id/history', () => {
    it('should return payment history', async () => {
      const paymentId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a payment with history exists')
        .uponReceiving('a request for payment history')
        .withRequest({
          method: 'GET',
          path: `/api/payments/${paymentId}/history`,
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
            data: Matchers.eachLike({
              id: Matchers.uuid(),
              action: Matchers.like('payment_created'),
              timestamp: Matchers.iso8601DateTime(),
              user: Matchers.like('system'),
              details: Matchers.like('Payment created via API')
            })
          }
        })
    })
  })
})
