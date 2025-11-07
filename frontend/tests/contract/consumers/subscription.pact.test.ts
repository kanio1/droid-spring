import { Pact } from '@pact-foundation/pact'
import { describe, it, before, after } from 'vitest'
import { expect } from 'vitest'

const mockServer = new Pact({
  port: 1234,
  host: '127.0.0.1',
  log: 'logs/pact.log',
  dir: 'tests/contract/pacts',
  consumer: 'BSS Frontend',
  provider: 'BSS Backend API',
  spec: 2
})

describe('Subscription Contract Tests', () => {
  before(async () => {
    await mockServer.setup()
  })

  after(async () => {
    await mockServer.finalize()
  })

  describe('GET /api/v1/subscriptions', () => {
    it('returns a list of subscriptions', async () => {
      const expected = {
        data: [
          {
            id: 'sub-123',
            subscriptionNumber: 'SUB-2023-001',
            customerId: '123e4567-e89b-12d3-a456-426614174000',
            productId: 'prod-123',
            status: 'ACTIVE',
            planType: 'PREMIUM',
            startDate: '2023-01-01',
            endDate: '2024-01-01',
            billingCycle: 'MONTHLY',
            amount: 99.99,
            currency: 'USD',
            version: 1,
            createdAt: '2023-01-01T00:00:00Z',
            updatedAt: '2023-01-01T00:00:00Z'
          }
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }

      await mockServer
        .uponReceiving('a request to get subscriptions')
        .withRequest({
          method: 'GET',
          path: '/api/v1/subscriptions',
          query: {
            page: '0',
            size: '20',
            sort: 'createdAt,desc'
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: Pact.like(expected)
        })

      const { get } = useApi()
      const response = await get('/api/v1/subscriptions', {
        query: { page: 0, size: 20, sort: 'createdAt,desc' }
      })

      expect(response.data).toMatchObject(expected)
    })
  })

  describe('POST /api/v1/subscriptions', () => {
    it('creates a new subscription', async () => {
      const requestBody = {
        customerId: '123e4567-e89b-12d3-a456-426614174000',
        productId: 'prod-123',
        planType: 'PREMIUM',
        billingCycle: 'MONTHLY',
        startDate: '2023-01-01'
      }

      const expected = {
        id: 'sub-456',
        subscriptionNumber: 'SUB-2023-002',
        customerId: '123e4567-e89b-12d3-a456-426614174000',
        productId: 'prod-123',
        status: 'PENDING',
        planType: 'PREMIUM',
        startDate: '2023-01-01',
        endDate: '2023-02-01',
        billingCycle: 'MONTHLY',
        amount: 99.99,
        currency: 'USD',
        version: 1,
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      await mockServer
        .uponReceiving('a request to create subscription')
        .withRequest({
          method: 'POST',
          path: '/api/v1/subscriptions',
          headers: {
            'Content-Type': 'application/json'
          },
          body: Pact.like(requestBody)
        })
        .willRespondWith({
          status: 201,
          headers: {
            'Content-Type': 'application/json'
          },
          body: Pact.like(expected)
        })

      const { post } = useApi()
      const response = await post('/api/v1/subscriptions', requestBody)

      expect(response.data).toMatchObject(expected)
    })
  })

  describe('PUT /api/v1/subscriptions/{id}/cancel', () => {
    it('cancels a subscription', async () => {
      const subscriptionId = 'sub-123'
      const requestBody = {
        cancellationReason: 'Customer request',
        immediate: false
      }

      const expected = {
        id: subscriptionId,
        subscriptionNumber: 'SUB-2023-001',
        customerId: '123e4567-e89b-12d3-a456-426614174000',
        productId: 'prod-123',
        status: 'CANCELLED',
        planType: 'PREMIUM',
        startDate: '2023-01-01',
        endDate: '2023-02-01',
        billingCycle: 'MONTHLY',
        amount: 99.99,
        currency: 'USD',
        cancellationReason: 'Customer request',
        cancelledAt: '2023-01-15T00:00:00Z',
        version: 2,
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-15T00:00:00Z'
      }

      await mockServer
        .uponReceiving('a request to cancel subscription')
        .withRequest({
          method: 'PUT',
          path: `/api/v1/subscriptions/${subscriptionId}/cancel`,
          headers: {
            'Content-Type': 'application/json'
          },
          body: Pact.like(requestBody)
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: Pact.like(expected)
        })

      const { put } = useApi()
      const response = await put(`/api/v1/subscriptions/${subscriptionId}/cancel`, requestBody)

      expect(response.data).toMatchObject(expected)
      expect(response.data.status).toBe('CANCELLED')
    })
  })
})
