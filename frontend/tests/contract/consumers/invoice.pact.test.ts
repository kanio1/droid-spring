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

describe('Invoice Contract Tests', () => {
  before(async () => {
    await mockServer.setup()
  })

  after(async () => {
    await mockServer.finalize()
  })

  describe('GET /api/v1/invoices', () => {
    it('returns a list of invoices', async () => {
      const expected = {
        data: [
          {
            id: 'inv-123',
            invoiceNumber: 'INV-2023-001',
            customerId: '123e4567-e89b-12d3-a456-426614174000',
            orderId: 'ord-123',
            status: 'ISSUED',
            type: 'STANDARD',
            totalAmount: 1000.00,
            currency: 'USD',
            issueDate: '2023-01-01',
            dueDate: '2023-01-31',
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
        .uponReceiving('a request to get invoices')
        .withRequest({
          method: 'GET',
          path: '/api/v1/invoices',
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
      const response = await get('/api/v1/invoices', {
        query: { page: 0, size: 20, sort: 'createdAt,desc' }
      })

      expect(response.data).toMatchObject(expected)
    })
  })

  describe('GET /api/v1/invoices/{id}', () => {
    it('returns a single invoice', async () => {
      const invoiceId = 'inv-123'
      const expected = {
        id: invoiceId,
        invoiceNumber: 'INV-2023-001',
        customerId: '123e4567-e89b-12d3-a456-426614174000',
        orderId: 'ord-123',
        status: 'ISSUED',
        type: 'STANDARD',
        totalAmount: 1000.00,
        currency: 'USD',
        issueDate: '2023-01-01',
        dueDate: '2023-01-31',
        version: 1,
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      await mockServer
        .uponReceiving('a request to get invoice by ID')
        .withRequest({
          method: 'GET',
          path: `/api/v1/invoices/${invoiceId}`
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: Pact.like(expected)
        })

      const { get } = useApi()
      const response = await get(`/api/v1/invoices/${invoiceId}`)

      expect(response.data).toMatchObject(expected)
    })
  })
})
