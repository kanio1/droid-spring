/**
 * Order API Consumer Contract Tests
 *
 * Tests the frontend's interaction with the Order API
 * Generates pact files that can be used to verify the backend provider
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { createConsumerPact, PACT_CONFIG } from '../pact-config'
import { Matchers } from '@pact-foundation/pact'
import { TestDataGenerator } from '../../framework/data-factories'

// Import API client
// import { OrderApiClient } from '@/api/clients/order-client'

describe('Order API Consumer', () => {
  const { consumer, provider } = PACT_CONFIG
  const pact = createConsumerPact(consumer.name, provider.name)

  beforeAll(async () => {
    await pact.setup()
  })

  afterAll(async () => {
    await pact.writePact()
    await pact.finalize()
  })

  describe('GET /api/orders', () => {
    it('should return a list of orders', async () => {
      const data = TestDataGenerator.scenario.fullCustomerJourney()
      const order = data.order!

      await pact
        .given('orders exist in the system')
        .uponReceiving('a request for all orders')
        .withRequest({
          method: 'GET',
          path: '/api/orders',
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
              orderNumber: Matchers.like('ORD-2025-000001'),
              customerId: Matchers.uuid(),
              customer: {
                firstName: Matchers.like('John'),
                lastName: Matchers.like('Doe')
              },
              items: Matchers.eachLike({
                productId: Matchers.uuid(),
                productName: Matchers.like('Product A'),
                quantity: Matchers.like(2),
                unitPrice: Matchers.like(100.00),
                totalPrice: Matchers.like(200.00)
              }),
              status: Matchers.like('pending'),
              totalAmount: Matchers.like(200.00),
              currency: Matchers.like('USD'),
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

    it('should filter orders by status', async () => {
      await pact
        .given('orders with different statuses exist')
        .uponReceiving('a request for delivered orders')
        .withRequest({
          method: 'GET',
          path: '/api/orders',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            status: 'delivered',
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
              status: Matchers.like('delivered')
            })
          }
        })
    })

    it('should filter orders by customer', async () => {
      const customerId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('orders for a specific customer exist')
        .uponReceiving('a request for orders by customer')
        .withRequest({
          method: 'GET',
          path: '/api/orders',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            customerId,
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
              customerId: Matchers.uuid(customerId)
            })
          }
        })
    })
  })

  describe('GET /api/orders/:id', () => {
    it('should return an order by ID', async () => {
      const orderId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('an order exists with ID 123e4567-e89b-12d3-a456-426614174000')
        .uponReceiving('a request for a specific order by ID')
        .withRequest({
          method: 'GET',
          path: `/api/orders/${orderId}`,
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
            id: Matchers.uuid(orderId),
            orderNumber: Matchers.like('ORD-2025-000001'),
            customerId: Matchers.uuid(),
            customer: {
              firstName: Matchers.like('John'),
              lastName: Matchers.like('Doe'),
              email: Matchers.email()
            },
            items: Matchers.eachLike({
              productId: Matchers.uuid(),
              productName: Matchers.like('Product A'),
              quantity: Matchers.like(2),
              unitPrice: Matchers.like(100.00),
              totalPrice: Matchers.like(200.00)
            }),
            status: Matchers.like('pending'),
            totalAmount: Matchers.like(200.00),
            currency: Matchers.like('USD'),
            createdAt: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('POST /api/orders', () => {
    it('should create a new order', async () => {
      const data = TestDataGenerator.scenario.fullCustomerJourney()
      const customer = data.customer

      await pact
        .given('a valid order payload with items')
        .uponReceiving('a request to create a new order')
        .withRequest({
          method: 'POST',
          path: '/api/orders',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            customerId: Matchers.uuid(),
            items: Matchers.eachLike({
              productId: Matchers.uuid(),
              quantity: Matchers.like(2),
              unitPrice: Matchers.like(100.00)
            })
          }
        })
        .willRespondWith({
          status: 201,
          headers: {
            'Content-Type': 'application/json',
            'Location': Matchers.regex('\\/api\\/orders\\/[0-9a-f-]+', '/api/orders/123e4567-e89b-12d3-a456-426614174000')
          },
          body: {
            id: Matchers.uuid(),
            orderNumber: Matchers.like('ORD-2025-000001'),
            customerId: Matchers.uuid(),
            status: Matchers.like('pending'),
            totalAmount: Matchers.like(200.00),
            currency: Matchers.like('USD'),
            createdAt: Matchers.iso8601DateTime()
          }
        })
    })

    it('should reject order with invalid items', async () => {
      await pact
        .given('an order payload with invalid items')
        .uponReceiving('a request to create an order with invalid items')
        .withRequest({
          method: 'POST',
          path: '/api/orders',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            customerId: Matchers.uuid(),
            items: [] // Empty items array
          }
        })
        .willRespondWith({
          status: 400,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            code: Matchers.like('VALIDATION_ERROR'),
            title: Matchers.like('Validation failed'),
            detail: Matchers.like('The request contains invalid data'),
            errors: Matchers.eachLike({
              field: Matchers.like('items'),
              message: Matchers.like('At least one item is required')
            })
          }
        })
    })
  })

  describe('PUT /api/orders/:id/status', () => {
    it('should update order status', async () => {
      const orderId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('an order exists and can be updated')
        .uponReceiving('a request to update order status to delivered')
        .withRequest({
          method: 'PUT',
          path: `/api/orders/${orderId}/status`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            status: Matchers.like('delivered'),
            trackingNumber: Matchers.like('TRACK123456')
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(orderId),
            status: Matchers.like('delivered'),
            trackingNumber: Matchers.like('TRACK123456'),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('DELETE /api/orders/:id', () => {
    it('should delete an order', async () => {
      const orderId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('an order exists and can be deleted')
        .uponReceiving('a request to delete an order')
        .withRequest({
          method: 'DELETE',
          path: `/api/orders/${orderId}`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token')
          }
        })
        .willRespondWith({
          status: 204
        })
    })
  })
})
