/**
 * Advanced API Testing Examples
 * Demonstrates REST, GraphQL, Contract Testing
 */

import { test, expect } from '@playwright/test'
import { DataFactory } from '../framework/data-factories'

test.describe('Advanced API Testing', () => {
  test.describe('GraphQL API', () => {
    test('should validate schema introspection', async ({ request }) => {
      const introspectionQuery = `
        query IntrospectionQuery {
          __schema {
            queryType { name }
            mutationType { name }
            subscriptionType { name }
            types {
              ...FullType
            }
          }
        }
        fragment FullType on __Type {
          kind
          name
          description
          fields(includeDeprecated: true) {
            name
            description
            args {
              ...InputValue
            }
            type {
              ...TypeRef
            }
            isDeprecated
            deprecationReason
          }
          inputFields {
            ...InputValue
          }
          interfaces {
            ...TypeRef
          }
          enumValues(includeDeprecated: true) {
            name
            description
            isDeprecated
            deprecationReason
          }
          possibleTypes {
            ...TypeRef
          }
        }
        fragment InputValue on __InputValue {
          name
          description
          type { ...TypeRef }
          defaultValue
        }
        fragment TypeRef on __Type {
          kind
          name
          ofType {
            kind
            name
            ofType {
              kind
              name
              ofType {
                kind
                name
                ofType {
                  kind
                  name
                  ofType {
                    kind
                    name
                    ofType {
                      kind
                      name
                      ofType {
                        kind
                        name
                      }
                    }
                  }
                }
              }
            }
          }
        }
      `

      const response = await request.post('/graphql', {
        data: { query: introspectionQuery }
      })

      expect(response.status()).toBe(200)

      const schema = await response.json()
      expect(schema.data.__schema.queryType).toBeDefined()
      expect(schema.data.__schema.types).toBeInstanceOf(Array)
    })

    test('should create and validate customer with GraphQL', async ({ request }) => {
      const customerData = DataFactory.createCustomer()

      const createCustomerMutation = `
        mutation CreateCustomer($input: CreateCustomerInput!) {
          createCustomer(input: $input) {
            id
            firstName
            lastName
            email
            status
            createdAt
          }
        }
      `

      // Create customer
      let response = await request.post('/graphql', {
        data: {
          query: createCustomerMutation,
          variables: {
            input: {
              firstName: customerData.firstName,
              lastName: customerData.lastName,
              email: customerData.email,
              phone: customerData.phone
            }
          }
        }
      })

      expect(response.status()).toBe(200)
      const createResult = await response.json()

      expect(createResult.data.createCustomer.id).toBeDefined()
      expect(createResult.data.createCustomer.email).toBe(customerData.email)

      const customerId = createResult.data.createCustomer.id

      // Query customer
      const getCustomerQuery = `
        query GetCustomer($id: ID!) {
          customer(id: $id) {
            id
            firstName
            lastName
            email
            status
            orders {
              id
              total
              status
            }
          }
        }
      `

      response = await request.post('/graphql', {
        data: {
          query: getCustomerQuery,
          variables: { id: customerId }
        }
      })

      expect(response.status()).toBe(200)
      const queryResult = await response.json()

      expect(queryResult.data.customer.id).toBe(customerId)
      expect(queryResult.data.customer.email).toBe(customerData.email)
    })

    test('should handle batch operations', async ({ request }) => {
      const orders = DataFactory.createOrders(5)

      const batchMutation = `
        mutation BatchCreateOrders($orders: [CreateOrderInput!]!) {
          batchCreateOrders(orders: $orders) {
            successful
            failed {
              index
              error
            }
            orders {
              id
              orderNumber
              status
            }
          }
        }
      `

      const response = await request.post('/graphql', {
        data: {
          query: batchMutation,
          variables: {
            orders: orders.map(order => ({
              customerId: order.customerId,
              items: order.items,
              totalAmount: order.totalAmount
            }))
          }
        }
      })

      expect(response.status()).toBe(200)
      const result = await response.json()

      expect(result.data.batchCreateOrders.successful).toBe(true)
      expect(result.data.batchCreateOrders.orders).toHaveLength(5)
    })
  })

  test.describe('RESTful API', () => {
    test('should validate API versioning', async ({ request }) => {
      const versions = ['v1', 'v2', 'v3']

      for (const version of versions) {
        const response = await request.get(`/api/${version}/customers`)

        // v1 and v2 should work, v3 might be experimental
        if (version === 'v3') {
          expect([200, 404, 501]).toContain(response.status())
        } else {
          expect(response.status()).toBe(200)
        }

        const headers = response.headers()
        expect(headers['api-version']).toBe(version)
      }
    })

    test('should validate rate limiting', async ({ request }) => {
      const requests = Array.from({ length: 110 }, () =>
        request.get('/api/v1/customers')
      )

      const responses = await Promise.all(requests)

      // First 100 requests should succeed
      for (let i = 0; i < 100; i++) {
        expect(responses[i].status()).toBe(200)
      }

      // Remaining requests should be rate limited
      for (let i = 100; i < responses.length; i++) {
        expect([429, 503]).toContain(responses[i].status())
        const rateLimitHeaders = responses[i].headers()
        expect(rateLimitHeaders['x-ratelimit-limit']).toBeDefined()
        expect(rateLimitHeaders['x-ratelimit-retry-after']).toBeDefined()
      }
    })

    test('should validate HATEOAS (Hypermedia)', async ({ request }) => {
      const response = await request.get('/api/v1/customers/cust-001')

      expect(response.status()).toBe(200)
      const customer = await response.json()

      // Check for hypermedia links
      expect(customer._links).toBeDefined()
      expect(customer._links.self.href).toContain('/api/v1/customers/cust-001')
      expect(customer._links.orders).toBeDefined()
      expect(customer._links.invoices).toBeDefined()

      // Follow links
      const ordersResponse = await request.get(customer._links.orders.href)
      expect(ordersResponse.status()).toBe(200)
    })

    test('should validate content negotiation', async ({ request }) => {
      // JSON response (default)
      let response = await request.get('/api/v1/customers', {
        headers: { 'Accept': 'application/json' }
      })
      expect(response.status()).toBe(200)
      expect(response.headers()['content-type']).toContain('application/json')

      // XML response
      response = await request.get('/api/v1/customers', {
        headers: { 'Accept': 'application/xml' }
      })
      expect(response.status()).toBe(200)
      expect(response.headers()['content-type']).toContain('application/xml')

      // CSV response
      response = await request.get('/api/v1/customers', {
        headers: { 'Accept': 'text/csv' }
      })
      expect(response.status()).toBe(200)
      expect(response.headers()['content-type']).toContain('text/csv')
    })
  })

  test.describe('API Error Handling', () => {
    test('should validate 404 handling', async ({ request }) => {
      const response = await request.get('/api/v1/customers/non-existent-id')

      expect(response.status()).toBe(404)

      const error = await response.json()
      expect(error.code).toBe('CUSTOMER_NOT_FOUND')
      expect(error.message).toContain('Customer not found')
      expect(error.traceId).toBeDefined()
    })

    test('should validate validation errors', async ({ request }) => {
      const response = await request.post('/api/v1/customers', {
        data: {
          // Missing required fields
          firstName: 'John'
        }
      })

      expect(response.status()).toBe(400)

      const error = await response.json()
      expect(error.code).toBe('VALIDATION_ERROR')
      expect(error.details).toBeInstanceOf(Array)
      expect(error.details[0].field).toBe('lastName')
      expect(error.details[0].message).toContain('required')
    })

    test('should handle concurrent modifications', async ({ request }) => {
      const customerId = 'cust-concurrent-test'

      // Create customer
      await request.post('/api/v1/customers', {
        data: {
          id: customerId,
          firstName: 'John',
          lastName: 'Doe',
          email: 'john@example.com'
        }
      })

      // Get current version
      let response = await request.get(`/api/v1/customers/${customerId}`)
      let customer = await response.json()
      const originalVersion = customer.version

      // Update with stale version
      response = await request.patch(`/api/v1/customers/${customerId}`, {
        data: {
          firstName: 'Jane',
          version: originalVersion // Stale version
        }
      })

      // Should fail with conflict
      expect(response.status()).toBe(409)
      const error = await response.json()
      expect(error.code).toBe('VERSION_CONFLICT')
    })
  })

  test.describe('API Performance', () => {
    test('should validate response times', async ({ request }) => {
      const startTime = Date.now()

      const response = await request.get('/api/v1/customers')

      const endTime = Date.now()
      const duration = endTime - startTime

      expect(response.status()).toBe(200)
      expect(duration).toBeLessThan(1000) // Should respond within 1 second
    })

    test('should validate pagination', async ({ request }) => {
      const pageSize = 20

      const response = await request.get(`/api/v1/customers?limit=${pageSize}&offset=0`)

      expect(response.status()).toBe(200)
      const data = await response.json()

      expect(data.items).toHaveLength(pageSize)
      expect(data.total).toBeGreaterThan(pageSize)
      expect(data.hasMore).toBe(true)
      expect(data.nextOffset).toBe(pageSize)
    })

    test('should validate filtering and sorting', async ({ request }) => {
      // Filter by status
      let response = await request.get('/api/v1/customers?status=active')
      expect(response.status()).toBe(200)
      let data = await response.json()
      data.items.forEach((customer: any) => {
        expect(customer.status).toBe('active')
      })

      // Sort by creation date
      response = await request.get('/api/v1/customers?sort=createdAt:desc')
      expect(response.status()).toBe(200)
      data = await response.json()

      // Verify descending order
      for (let i = 0; i < data.items.length - 1; i++) {
        const current = new Date(data.items[i].createdAt).getTime()
        const next = new Date(data.items[i + 1].createdAt).getTime()
        expect(current).toBeGreaterThanOrEqual(next)
      }
    })
  })
})
