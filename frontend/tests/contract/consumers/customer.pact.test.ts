import { Pact } from '@pact-foundation/pact'
import { Matchers } from '@pact-foundation/pact'
import { describe, it, before, after } from 'vitest'
import { expect } from 'vitest'
import { useCustomerStore } from '~/stores/customer'

const mockServer = new Pact({
  port: 1234,
  host: '127.0.0.1',
  log: 'logs/pact.log',
  dir: 'tests/contract/pacts',
  consumer: 'BSS Frontend',
  provider: 'BSS Backend API',
  spec: 2
})

describe('Customer Contract Tests', () => {
  before(async () => {
    await mockServer.setup()
  })

  after(async () => {
    await mockServer.finalize()
  })

  describe('GET /api/v1/customers', () => {
    it('returns a list of customers', async () => {
      const expected = {
        data: [
          {
            id: '123e4567-e89b-12d3-a456-426614174000',
            firstName: 'John',
            lastName: 'Doe',
            email: 'john.doe@example.com',
            phone: '+1234567890',
            pesel: '12345678901',
            nip: '1234567890',
            status: 'ACTIVE',
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
        .uponReceiving('a request to get customers')
        .withRequest({
          method: 'GET',
          path: '/api/v1/customers',
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
      const response = await get('/api/v1/customers', {
        query: { page: 0, size: 20, sort: 'createdAt,desc' }
      })

      expect(response.data).toMatchObject(expected)
    })
  })

  describe('GET /api/v1/customers/{id}', () => {
    it('returns a single customer', async () => {
      const customerId = '123e4567-e89b-12d3-a456-426614174000'
      const expected = {
        id: customerId,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        phone: '+1234567890',
        pesel: '12345678901',
        nip: '1234567890',
        status: 'ACTIVE',
        version: 1,
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      await mockServer
        .uponReceiving('a request to get customer by ID')
        .withRequest({
          method: 'GET',
          path: `/api/v1/customers/${customerId}`
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: Pact.like(expected)
        })

      const { get } = useApi()
      const response = await get(`/api/v1/customers/${customerId}`)

      expect(response.data).toMatchObject(expected)
    })
  })

  describe('POST /api/v1/customers', () => {
    it('creates a new customer', async () => {
      const requestBody = {
        firstName: 'Jane',
        lastName: 'Smith',
        email: 'jane.smith@example.com',
        phone: '+1987654321',
        pesel: '98765432109',
        nip: '9876543210'
      }

      const expected = {
        id: '456e7890-e89b-12d3-a456-426614174000',
        firstName: 'Jane',
        lastName: 'Smith',
        email: 'jane.smith@example.com',
        phone: '+1987654321',
        pesel: '98765432109',
        nip: '9876543210',
        status: 'ACTIVE',
        version: 1,
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-01T00:00:00Z'
      }

      await mockServer
        .uponReceiving('a request to create customer')
        .withRequest({
          method: 'POST',
          path: '/api/v1/customers',
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
      const response = await post('/api/v1/customers', requestBody)

      expect(response.data).toMatchObject(expected)
    })
  })

  describe('PUT /api/v1/customers/{id}', () => {
    it('updates a customer', async () => {
      const customerId = '123e4567-e89b-12d3-a456-426614174000'
      const requestBody = {
        id: customerId,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.updated@example.com',
        phone: '+1234567890',
        pesel: '12345678901',
        nip: '1234567890',
        version: 1
      }

      const expected = {
        id: customerId,
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.updated@example.com',
        phone: '+1234567890',
        pesel: '12345678901',
        nip: '1234567890',
        status: 'ACTIVE',
        version: 2,
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-01-02T00:00:00Z'
      }

      await mockServer
        .uponReceiving('a request to update customer')
        .withRequest({
          method: 'PUT',
          path: `/api/v1/customers/${customerId}`,
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
      const response = await put(`/api/v1/customers/${customerId}`, requestBody)

      expect(response.data).toMatchObject(expected)
    })
  })

  describe('DELETE /api/v1/customers/{id}', () => {
    it('deletes a customer', async () => {
      const customerId = '123e4567-e89b-12d3-a456-426614174000'

      await mockServer
        .uponReceiving('a request to delete customer')
        .withRequest({
          method: 'DELETE',
          path: `/api/v1/customers/${customerId}`
        })
        .willRespondWith({
          status: 204
        })

      const { del } = useApi()
      const response = await del(`/api/v1/customers/${customerId}`)

      expect(response.status).toBe(204)
    })
  })
})
