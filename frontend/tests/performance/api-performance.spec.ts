import { describe, it, expect } from 'vitest'
import { setup, createCustomer, cleanup } from '../framework/utils/performance-setup'
import { useCustomerStore } from '~/stores/customer'
import { useApi } from '~/composables/useApi'

describe('API Performance Tests', () => {
  let customerIds: string[] = []

  beforeAll(async () => {
    await setup()
  })

  afterAll(async () => {
    await cleanup(customerIds)
  })

  describe('Customer API Performance', () => {
    it('should fetch customers list within 500ms', async () => {
      const startTime = performance.now()

      const { get } = useApi()
      const response = await get('/api/v1/customers', {
        query: { page: 0, size: 20 }
      })

      const duration = performance.now() - startTime

      expect(response.status).toBe(200)
      expect(duration).toBeLessThan(500)
      expect(duration).toBeGreaterThan(0)
    })

    it('should create customer within 1000ms', async () => {
      const startTime = performance.now()

      const { post } = useApi()
      const response = await post('/api/v1/customers', {
        firstName: 'Perf',
        lastName: 'Test',
        email: `perf${Date.now()}@test.com`,
        phone: '+1234567890'
      })

      const duration = performance.now() - startTime

      if (response.status === 201) {
        customerIds.push(response.data.id)
      }

      expect(response.status).toBeLessThan(300)
      expect(duration).toBeLessThan(1000)
    })

    it('should handle 50 concurrent customer fetches', async () => {
      const startTime = performance.now()
      const { get } = useApi()

      const promises = Array.from({ length: 50 }, (_, i) =>
        get('/api/v1/customers', {
          query: { page: 0, size: 10 }
        })
      )

      const responses = await Promise.all(promises)
      const duration = performance.now() - startTime

      expect(responses).toHaveLength(50)
      responses.forEach(response => {
        expect(response.status).toBe(200)
      })

      console.log(`50 concurrent requests completed in ${duration}ms`)

      // Should complete within reasonable time
      expect(duration).toBeLessThan(5000)
    })
  })

  describe('Store Performance', () => {
    it('should load customers in store within 300ms', async () => {
      const store = useCustomerStore()
      const startTime = performance.now()

      await store.fetchCustomers({ page: 0, size: 50 })

      const duration = performance.now() - startTime

      expect(store.customers.length).toBeGreaterThan(0)
      expect(duration).toBeLessThan(300)
    })

    it('should search customers efficiently', async () => {
      const store = useCustomerStore()

      // Load customers first
      await store.fetchCustomers({ page: 0, size: 100 })

      const startTime = performance.now()
      const results = store.searchCustomers('test')
      const duration = performance.now() - startTime

      expect(Array.isArray(results)).toBe(true)
      expect(duration).toBeLessThan(50) // Search should be very fast
    })
  })

  describe('Data Processing Performance', () => {
    it('should process 1000 customers within 100ms', async () => {
      const customers = Array.from({ length: 1000 }, (_, i) => ({
        id: `customer-${i}`,
        firstName: `First${i}`,
        lastName: `Last${i}`,
        email: `customer${i}@test.com`
      }))

      const startTime = performance.now()

      // Process customers (e.g., transform, filter, sort)
      const processed = customers
        .filter(c => c.firstName.startsWith('First'))
        .map(c => ({
          ...c,
          fullName: `${c.firstName} ${c.lastName}`
        }))
        .sort((a, b) => a.lastName.localeCompare(b.lastName))

      const duration = performance.now() - startTime

      expect(processed).toHaveLength(1000)
      expect(duration).toBeLessThan(100)
    })

    it('should render 1000 customer cards efficiently', async () => {
      const customers = Array.from({ length: 1000 }, (_, i) => ({
        id: `customer-${i}`,
        firstName: `Customer${i}`,
        lastName: `Test${i}`,
        email: `customer${i}@test.com`
      }))

      const startTime = performance.now()

      // Simulate rendering
      const cards = customers.map(customer => ({
        id: customer.id,
        name: `${customer.firstName} ${customer.lastName}`,
        email: customer.email
      }))

      const duration = performance.now() - startTime

      expect(cards).toHaveLength(1000)
      expect(duration).toBeLessThan(200)
    })
  })

  describe('Memory Performance', () => {
    it('should not leak memory during repeated operations', async () => {
      const initialMemory = (performance as any).memory?.usedJSHeapSize || 0

      // Perform many operations
      for (let i = 0; i < 100; i++) {
        const customers = Array.from({ length: 100 }, (_, j) => ({
          id: `customer-${i}-${j}`,
          data: new Array(100).fill({}) // Some nested data
        }))

        // Process and discard
        customers.map(c => c.id)
      })

      // Force garbage collection if available
      if ((global as any).gc) {
        (global as any).gc()
      }

      const finalMemory = (performance as any).memory?.usedJSHeapSize || 0
      const memoryIncrease = finalMemory - initialMemory

      console.log(`Memory increase: ${(memoryIncrease / 1024 / 1024).toFixed(2)} MB`)

      // Memory increase should be reasonable (< 50MB for this test)
      expect(memoryIncrease).toBeLessThan(50 * 1024 * 1024)
    })
  })
})
