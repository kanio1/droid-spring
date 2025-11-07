/**
 * Invoice API Consumer Contract Tests
 *
 * Tests the frontend's interaction with the Invoice API
 * Generates pact files that can be used to verify the backend provider
 */

import { describe, it, expect, beforeAll, afterAll } from 'vitest'
import { createConsumerPact, PACT_CONFIG } from '../pact-config'
import { Matchers } from '@pact-foundation/pact'
import { TestDataGenerator } from '../../framework/data-factories'

// Import API client
// import { InvoiceApiClient } from '@/api/clients/invoice-client'

describe('Invoice API Consumer', () => {
  const { consumer, provider } = PACT_CONFIG
  const pact = createConsumerPact(consumer.name, provider.name)

  beforeAll(async () => {
    await pact.setup()
  })

  afterAll(async () => {
    await pact.writePact()
    await pact.finalize()
  })

  describe('GET /api/invoices', () => {
    it('should return a list of invoices', async () => {
      const data = TestDataGenerator.scenario.fullCustomerJourney()
      const invoice = data.invoice!

      await pact
        .given('invoices exist in the system')
        .uponReceiving('a request for all invoices')
        .withRequest({
          method: 'GET',
          path: '/api/invoices',
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
              invoiceNumber: Matchers.like('INV-2025-000001'),
              customerId: Matchers.uuid(),
              customer: {
                firstName: Matchers.like('John'),
                lastName: Matchers.like('Doe')
              },
              orderId: Matchers.uuid(),
              status: Matchers.like('pending'),
              issueDate: Matchers.iso8601DateTime(),
              dueDate: Matchers.iso8601DateTime(),
              lineItems: Matchers.eachLike({
                description: Matchers.like('Product A'),
                quantity: Matchers.like(2),
                unitPrice: Matchers.like(100.00),
                subtotal: Matchers.like(200.00),
                taxAmount: Matchers.like(40.00),
                total: Matchers.like(240.00)
              }),
              subtotal: Matchers.like(200.00),
              taxAmount: Matchers.like(40.00),
              totalAmount: Matchers.like(240.00),
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

    it('should filter invoices by status', async () => {
      await pact
        .given('invoices with different statuses exist')
        .uponReceiving('a request for paid invoices')
        .withRequest({
          method: 'GET',
          path: '/api/invoices',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            status: 'paid',
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
              status: Matchers.like('paid'),
              paidDate: Matchers.iso8601DateTime()
            })
          }
        })
    })

    it('should filter invoices by date range', async () => {
      await pact
        .given('invoices with various issue dates exist')
        .uponReceiving('a request for invoices in a date range')
        .withRequest({
          method: 'GET',
          path: '/api/invoices',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          query: {
            dateFrom: '2025-01-01',
            dateTo: '2025-12-31',
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
              issueDate: Matchers.iso8601DateTime()
            })
          }
        })
    })
  })

  describe('GET /api/invoices/:id', () => {
    it('should return an invoice by ID', async () => {
      const invoiceId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('an invoice exists with ID 123e4567-e89b-12d3-a456-426614174000')
        .uponReceiving('a request for a specific invoice by ID')
        .withRequest({
          method: 'GET',
          path: `/api/invoices/${invoiceId}`,
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
            id: Matchers.uuid(invoiceId),
            invoiceNumber: Matchers.like('INV-2025-000001'),
            customerId: Matchers.uuid(),
            customer: {
              firstName: Matchers.like('John'),
              lastName: Matchers.like('Doe'),
              email: Matchers.email()
            },
            orderId: Matchers.uuid(),
            status: Matchers.like('pending'),
            issueDate: Matchers.iso8601DateTime(),
            dueDate: Matchers.iso8601DateTime(),
            lineItems: Matchers.eachLike({
              description: Matchers.like('Product A'),
              quantity: Matchers.like(2),
              unitPrice: Matchers.like(100.00),
              subtotal: Matchers.like(200.00),
              taxAmount: Matchers.like(40.00),
              total: Matchers.like(240.00)
            }),
            subtotal: Matchers.like(200.00),
            taxAmount: Matchers.like(40.00),
            totalAmount: Matchers.like(240.00),
            currency: Matchers.like('USD'),
            createdAt: Matchers.iso8601DateTime(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('POST /api/invoices', () => {
    it('should create a new invoice', async () => {
      const data = TestDataGenerator.scenario.fullCustomerJourney()
      const customer = data.customer
      const order = data.order

      await pact
        .given('a valid invoice payload with line items')
        .uponReceiving('a request to create a new invoice')
        .withRequest({
          method: 'POST',
          path: '/api/invoices',
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            customerId: Matchers.uuid(),
            orderId: Matchers.uuid(),
            lineItems: Matchers.eachLike({
              description: Matchers.like('Product A'),
              quantity: Matchers.like(2),
              unitPrice: Matchers.like(100.00)
            }),
            dueDate: Matchers.iso8601DateTime()
          }
        })
        .willRespondWith({
          status: 201,
          headers: {
            'Content-Type': 'application/json',
            'Location': Matchers.regex('\\/api\\/invoices\\/[0-9a-f-]+', '/api/invoices/123e4567-e89b-12d3-a456-426614174000')
          },
          body: {
            id: Matchers.uuid(),
            invoiceNumber: Matchers.like('INV-2025-000001'),
            customerId: Matchers.uuid(),
            status: Matchers.like('pending'),
            totalAmount: Matchers.like(200.00),
            currency: Matchers.like('USD'),
            createdAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('POST /api/invoices/:id/send', () => {
    it('should send an invoice via email', async () => {
      const invoiceId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('an invoice exists and can be sent')
        .uponReceiving('a request to send an invoice via email')
        .withRequest({
          method: 'POST',
          path: `/api/invoices/${invoiceId}/send`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            email: Matchers.email(),
            subject: Matchers.like('Invoice from Company'),
            message: Matchers.like('Please find attached invoice')
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            success: Matchers.like(true),
            message: Matchers.like('Invoice sent successfully'),
            sentAt: Matchers.iso8601DateTime()
          }
        })
    })
  })

  describe('PUT /api/invoices/:id/status', () => {
    it('should update invoice status to paid', async () => {
      const invoiceId = '123e4567-e89b-12d3-a456-426614174000'

      await pact
        .given('an invoice exists and payment has been received')
        .uponReceiving('a request to mark invoice as paid')
        .withRequest({
          method: 'PUT',
          path: `/api/invoices/${invoiceId}/status`,
          headers: {
            'Authorization': Matchers.like('Bearer test-token'),
            'Content-Type': 'application/json'
          },
          body: {
            status: Matchers.like('paid'),
            paymentMethod: Matchers.like('credit_card'),
            paymentId: Matchers.uuid()
          }
        })
        .willRespondWith({
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: {
            id: Matchers.uuid(invoiceId),
            status: Matchers.like('paid'),
            paidDate: Matchers.iso8601DateTime(),
            paymentId: Matchers.uuid(),
            updatedAt: Matchers.iso8601DateTime()
          }
        })
    })
  })
})
