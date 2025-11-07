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

describe('Payment Contract Tests', () => {
  before(async () => {
    await mockServer.setup()
  })

  after(async () => {
    await mockServer.finalize()
  })

  describe('GET /api/v1/payments', () => {
    it('returns a list of payments', async () => {
      const expected = {
        data: [
          {
            id: 'pay-123',
            paymentNumber: 'PAY-2023-001',
            customerId: '123e4567-e89b-12d3-a456-426614174000',
            invoiceId: 'inv-123',
            amount: 1000.00,
            currency: 'USD',
            paymentMethod: 'CARD',
            paymentStatus: 'COMPLETED',
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
        .uponReceiving('a request to get payments')
        .withRequest({
          method: 'GET',
          path: '/api/v1/payments',
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
      const response = await get('/api/v1/payments', {
        query: { page: 0, size: 20, sort: 'createdAt,desc' }
      })

      expect(response.data).toMatchObject(expected)
    })
  })

  describe('POST /api/v1/payments', () => {
    it('creates a new payment', async () => {
      const requestBody = {
        customerId: '123e4567-e89b-12d3-a456-426614174000',
        invoiceId: 'inv-123',
        amount: 1000.00,
        currency: 'USD',
        paymentMethod: 'CARD'
      }

      const expected = {
        id: 'pay-456',
        paymentNumber: 'PAY-2023-002',
        customerId: '123e4567-e89b-12d3-a456-426614174000',
        invoiceId: 'inv-123',
        amount: 1000.00,
        currency: 'USD',
        paymentMethod: 'CARD',
        paymentStatus: 'PENDING',
        version: 1,
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      await mockServer
        .uponReceiving('a request to create payment')
        .withRequest({
          method: 'POST',
          path: '/api/v1/payments',
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
      const response = await post('/api/v1/payments', requestBody)

      expect(response.data).toMatchObject(expected)
    })
  })
})
