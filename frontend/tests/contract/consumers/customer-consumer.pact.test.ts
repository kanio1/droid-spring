/**
 * Customer API Consumer Contract Tests
 *
 * Tests the frontend's interaction with the Customer API
 * Generates pact files that can be used to verify the backend provider
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { createConsumerPact, PACT_CONFIG } from '../pact-config'
import { Matchers } from '@pact-foundation/pact'
import { TestDataGenerator } from '../../framework/data-factories'

// Import API client
// Note: Replace with actual API client import
// import { CustomerApiClient } from '@/api/clients/customer-client'

describe('Customer API Consumer', () => {
  const { consumer, provider } = PACT_CONFIG
  const pact = createConsumerPact(consumer.name, provider.name)

  beforeAll(async () => {
    await pact.setup()
  })

  afterAll(async () => {
    await pact.writePact()
    await pact.finalize()
  })

  describe('GET /api/customers', () => {
    it('should return a list of customers', async () => {
      const expectedCustomer = TestDataGenerator.fullCustomerJourney().customer

      await pact
        .given('customers exist in the system')
        .uponReceiving('a request for all customers')
        .withRequest({
          method: 'GET',
          path: '/api/customers',
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
              firstName: Matchers.like('John'),
              lastName: Matchers.like('Doe'),
              email: Matchers.email(),
              phone: Matchers.like('+1-555-0123'),
              status: Matchers.like('active'),
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

      // Execute the test
      // const response = await CustomerApiClient.getCustomers(1, 20)
      // expect(response.data).toHaveLength(1)
    })

    it('should filter customers by status', async () => {
      await pact
        .given('customers with different statuses exist')
        .uponReceiving('a request for active customers')
        .withRequest({
          method: 'GET',
          path: '/api/customers',
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
              firstName: Matchers.like('Jane'),
              lastName: Matchers.like('Smith'),
              email: Matchers.email(),
              status: Matchers.like('active')
            }),
            pagination: Matchers.like({
              page: 1,
              limit: 20,
              total: Matchers.like(50),
              totalPages: Matchers.like(3)
            })
          }
        })
    })

    it('should search customers by name', async () => {
      await pact
        .given('customers with various names exist')
        .uponReceiving('a search request for customers by name')
        .withRequest({
          method: 'GET',
          path: '/api/customers',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            search: 'john',
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
              firstName: Matchers.like('John'),
              lastName: Matchers.like('Doe'),
              email: Matchers.email()
            })
          }
        })
    })

    it('should handle empty results', async () => {
      await pact
        .given('no customers exist')
        .uponReceiving('a request for customers with no results')
        .withRequest({
          method: 'GET',
          path: '/api/customers',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            search: 'nonexistent',
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
            data: [],
            pagination: {
              page: Matchers.like(1),
              limit: Matchers.like(20),
              total: Matchers.like(0),
              totalPages: Matchers.like(0)
            }
          }
        })
    })
  })

  describe('GET /api/customers/:id', () => {
    it('should return a customer by ID', async () => {
      const customerId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a customer exists with ID 123e4567-e89b-12d3-a456-426614174000')
        .uponReceiving('a request for a specific customer by ID')
        .withRequest({
          method: 'GET',
          path: `/api/customers/${customerId}`,
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
            id: Matchers.uuid(customerId),
            firstName: Matchers.like('John'),
            lastName: Matchers.like('Doe'),
            email: Matchers.email(),
            phone: Matchers.like('+1-555-0123'),
            status: Matchers.like('active'),
            createdAt: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })

    it('should return 404 for non-existent customer', async () => {
      const customerId = '00000000-0000-0000-0000-000000000000'

      await pact
        .given('no customer exists with ID 00000000-0000-0000-0000-000000000000')
        .uponReceiving('a request for a non-existent customer')
        .withRequest({
          method: 'GET',
          path: `/api/customers/${customerId}`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          }
        })
        .willRespondWith({
          status: 404,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            code: Matchers.like('CUSTOMER_NOT_FOUND'),
            title: Matchers.like('Customer not found'),
            detail: Matchers.like('The requested customer could not be found'),
            traceId: Matchers.string('trace-id')
          }
        })
    })
  })

  describe('POST /api/customers', () => {
    it('should create a new customer', async () => {
      const customerData = TestDataGenerator.fullCustomerJourney().customer

      await pact
        .given('a valid customer payload')
        .uponReceiving('a request to create a new customer')
        .withRequest({
          method: 'POST',
          path: '/api/customers',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            firstName: Matchers.like('Alice'),
            lastName: Matchers.like('Johnson'),
            email: Matchers.email(),
            phone: Matchers.like('+1-555-0456')
          }
        })
        .willRespondWith({
          status: 201,
          headers: {
            'Content-Type': 'application/json',
            'Location': Matchers.regex('\\/api\\/customers\\/[0-9a-f-]+', '/api/customers/123e4567-e89b-12d3-a456-426614174000')
          },
          body: {
            id: Matchers.uuid(),
            firstName: Matchers.like('Alice'),
            lastName: Matchers.like('Johnson'),
            email: Matchers.email(),
            phone: Matchers.like('+1-555-0456'),
            status: Matchers.like('active'),
            createdAt: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })

    it('should reject invalid customer data', async () => {
      await pact
        .given('an invalid customer payload with missing required fields')
        .uponReceiving('a request to create a customer with invalid data')
        .withRequest({
          method: 'POST',
          path: '/api/customers',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            firstName: Matchers.like('Bob')
            // Missing lastName, email
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
              field: Matchers.like('email'),
              message: Matchers.like('Email is required')
            }),
            traceId: Matchers.string('trace-id')
          }
        })
    })

    it('should reject duplicate email', async () => {
      const customerData = TestDataGenerator.fullCustomerJourney().customer

      await pact
        .given('a customer with email already exists')
        .uponReceiving('a request to create a customer with duplicate email')
        .withRequest({
          method: 'POST',
          path: '/api/customers',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            firstName: Matchers.like('Charlie'),
            lastName: Matchers.like('Brown'),
            email: Matchers.like('existing@example.com'),
            phone: Matchers.like('+1-555-0789')
          }
        })
        .willRespondWith({
          status: 409,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            code: Matchers.like('DUPLICATE_EMAIL'),
            title: Matchers.like('Duplicate email'),
            detail: Matchers.like('A customer with this email already exists'),
            traceId: Matchers.string('trace-id')
          }
        })
    })
  })

  describe('PUT /api/customers/:id', () => {
    it('should update a customer', async () => {
      const customerId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a customer exists and needs to be updated')
        .uponReceiving('a request to update a customer')
        .withRequest({
          method: 'PUT',
          path: `/api/customers/${customerId}`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            firstName: Matchers.like('John'),
            lastName: Matchers.like('Doe Updated'),
            email: Matchers.email(),
            phone: Matchers.like('+1-555-0999')
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(customerId),
            firstName: Matchers.like('John'),
            lastName: Matchers.like('Doe Updated'),
            email: Matchers.email(),
            phone: Matchers.like('+1-555-0999'),
            status: Matchers.like('active'),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('DELETE /api/customers/:id', () => {
    it('should delete a customer', async () => {
      const customerId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('a customer exists and can be deleted')
        .uponReceiving('a request to delete a customer')
        .withRequest({
          method: 'DELETE',
          path: `/api/customers/${customerId}`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token')
          }
        })
        .willRespondWith({
          status: 204
        })
    })

    it('should return 404 when deleting non-existent customer', async () => {
      const customerId = '00000000-0000-0000-0000-000000000000'

      await pact
        .given('no customer exists with the given ID')
        .uponReceiving('a request to delete a non-existent customer')
        .withRequest({
          method: 'DELETE',
          path: `/api/customers/${customerId}`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token')
          }
        })
        .willRespondWith({
          status: 404,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            code: Matchers.like('CUSTOMER_NOT_FOUND'),
            title: Matchers.like('Customer not found'),
            detail: Matchers.like('The requested customer could not be found'),
            traceId: Matchers.string('trace-id')
          }
        })
    })
  })
})
