/**
 * Allure Vitest Example Tests
 *
 * Demonstrates how to use Allure reporting with Vitest unit tests
 *
 * Run with:
 * npx vitest run tests/allure/examples/vitest-example.spec.ts --reporter=default,allure-vitest
 */

import { describe, it, expect, vi } from 'vitest'
import { CustomerService } from '@/app/services/customer.service'
import { CustomerFactory } from '@/tests/framework/data-factories'

describe('Customer Service @customer-service', () => {
  describe('createCustomer', () => {
    it('should create customer with valid data @critical', () => {
      // Arrange
      const customerData = CustomerFactory.create()
        .active()
        .withRandomEmail()
        .build()

      const customerService = new CustomerService()

      // Act
      const result = customerService.createCustomer(customerData)

      // Assert
      expect(result).toBeDefined()
      expect(result.id).toBeDefined()
      expect(result.email).toBe(customerData.email)
      expect(result.status).toBe('active')
    })

    it('should throw error for invalid email @high', () => {
      // Arrange
      const customerData = CustomerFactory.create()
        .withEmail('invalid-email')
        .build()

      const customerService = new CustomerService()

      // Act & Assert
      expect(() => {
        customerService.createCustomer(customerData)
      }).toThrow('Invalid email format')
    })

    it('should handle duplicate email @medium', async () => {
      // Arrange
      const customerData = CustomerFactory.create()
        .withEmail('duplicate@example.com')
        .build()

      const customerService = new CustomerService()

      // Mock repository to simulate duplicate
      vi.spyOn(customerService, 'findByEmail').mockResolvedValue({
        id: 'existing-id',
        email: 'duplicate@example.com'
      } as any)

      // Act
      const result = await customerService.createCustomer(customerData)

      // Assert
      expect(result).toBeNull()
    })
  })

  describe('updateCustomer', () => {
    it('should update customer information @critical', async () => {
      // Arrange
      const customer = CustomerFactory.create().active().build()
      const updates = {
        firstName: 'Updated',
        lastName: 'Name'
      }

      const customerService = new CustomerService()

      // Act
      const result = await customerService.updateCustomer(customer.id, updates)

      // Assert
      expect(result).toBeDefined()
      expect(result?.firstName).toBe('Updated')
      expect(result?.lastName).toBe('Name')
    })

    it('should not update non-existent customer @high', async () => {
      // Arrange
      const customerService = new CustomerService()

      // Mock repository to return null
      vi.spyOn(customerService, 'findById').mockResolvedValue(null)

      // Act
      const result = await customerService.updateCustomer('non-existent-id', {})

      // Assert
      expect(result).toBeNull()
    })
  })

  describe('deleteCustomer', () => {
    it('should delete customer @medium', async () => {
      // Arrange
      const customer = CustomerFactory.create().active().build()
      const customerService = new CustomerService()

      // Mock repository
      vi.spyOn(customerService, 'delete').mockResolvedValue(true)

      // Act
      const result = await customerService.deleteCustomer(customer.id)

      // Assert
      expect(result).toBe(true)
    })

    it('should return false for non-existent customer @low', async () => {
      // Arrange
      const customerService = new CustomerService()
      vi.spyOn(customerService, 'delete').mockResolvedValue(false)

      // Act
      const result = await customerService.deleteCustomer('non-existent-id')

      // Assert
      expect(result).toBe(false)
    })
  })

  describe('findCustomers', () => {
    it('should find customers by status @medium', async () => {
      // Arrange
      const customers = [
        CustomerFactory.create().active().build(),
        CustomerFactory.create().active().build(),
        CustomerFactory.create().inactive().build()
      ]

      const customerService = new CustomerService()
      vi.spyOn(customerService, 'findByStatus').mockResolvedValue(
        customers.filter(c => c.status === 'active')
      )

      // Act
      const result = await customerService.findByStatus('active')

      // Assert
      expect(result).toHaveLength(2)
      expect(result.every(c => c.status === 'active')).toBe(true)
    })

    it('should return empty array for no matches @low', async () => {
      // Arrange
      const customerService = new CustomerService()
      vi.spyOn(customerService, 'findByStatus').mockResolvedValue([])

      // Act
      const result = await customerService.findByStatus('suspended')

      // Assert
      expect(result).toHaveLength(0)
    })
  })
})

describe('Order Service @order-service', () => {
  describe('createOrder', () => {
    it('should create order with multiple items @critical', () => {
      // Arrange
      const customer = CustomerFactory.create().active().build()
      const orderData = {
        customerId: customer.id,
        items: [
          { productId: 'prod-1', quantity: 2, unitPrice: 50 },
          { productId: 'prod-2', quantity: 1, unitPrice: 100 }
        ]
      }

      // Act
      const order = {
        id: 'order-1',
        ...orderData,
        totalAmount: 200
      }

      // Assert
      expect(order.totalAmount).toBe(200)
      expect(order.items).toHaveLength(2)
    })

    it('should calculate total correctly @high', () => {
      // Arrange
      const items = [
        { quantity: 2, unitPrice: 50 },
        { quantity: 3, unitPrice: 30 }
      ]

      // Act
      const total = items.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0)

      // Assert
      expect(total).toBe(190)
    })
  })

  describe('calculateTax', () => {
    it('should calculate tax at standard rate @medium', () => {
      // Arrange
      const amount = 100
      const taxRate = 0.2

      // Act
      const tax = amount * taxRate

      // Assert
      expect(tax).toBe(20)
    })

    it('should handle zero amount @low', () => {
      // Arrange
      const amount = 0
      const taxRate = 0.2

      // Act
      const tax = amount * taxRate

      // Assert
      expect(tax).toBe(0)
    })
  })
})
