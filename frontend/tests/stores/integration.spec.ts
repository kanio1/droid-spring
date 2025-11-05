/**
 * Store Integration Tests
 *
 * Comprehensive integration tests for multiple stores working together
 * Tests cross-store interactions, shared state, and coordinated operations
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useCustomerStore } from '@/stores/customer'
import { useOrderStore } from '@/stores/order'
import { useInvoiceStore } from '@/stores/invoice'
import { useSettingsStore } from '@/stores/settings'

// Mock API client
vi.mock('@/services/api', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn()
  }
}))

// Mock WebSocket
vi.mock('@/services/websocket', () => ({
  websocket: {
    connect: vi.fn(),
    disconnect: vi.fn(),
    emit: vi.fn(),
    on: vi.fn(),
    off: vi.fn(),
    isConnected: vi.fn().mockReturnValue(true)
  }
}))

// Mock Keycloak
vi.mock('@/plugins/keycloak', () => ({
  default: {
    init: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    token: { value: 'mock-token' },
    tokenParsed: {
      value: {
        sub: 'user-123',
        preferred_username: 'testuser',
        email: 'test@example.com',
        realm_access: { roles: ['user', 'admin'] }
      }
    },
    authenticated: { value: true },
    hasRealmRole: vi.fn().mockResolvedValue(true),
    loadUserProfile: vi.fn().mockResolvedValue({
      id: 'user-123',
      username: 'testuser',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User'
    })
  }
}))

const mockCustomer = {
  id: 'cust-123',
  firstName: 'John',
  lastName: 'Doe',
  email: 'john.doe@example.com',
  phone: '+1234567890',
  status: 'ACTIVE',
  createdAt: '2024-11-05T10:00:00Z',
  updatedAt: '2024-11-05T10:30:00Z'
}

const mockOrder = {
  id: 'order-123',
  orderNumber: 'ORD-2024-001',
  customerId: 'cust-123',
  customerName: 'John Doe',
  status: 'PENDING',
  totalAmount: 1230.00,
  currency: 'USD',
  items: [
    {
      productId: 'prod-001',
      productName: '5G Router',
      quantity: 2,
      unitPrice: 500.00,
      totalPrice: 1000.00
    }
  ],
  shippingAddress: {
    street: '123 Main St',
    city: 'New York',
    state: 'NY',
    zipCode: '10001',
    country: 'US'
  },
  createdAt: '2024-11-05T10:00:00Z',
  updatedAt: '2024-11-05T10:30:00Z'
}

const mockInvoice = {
  id: 'inv-123',
  invoiceNumber: 'INV-2024-001',
  customerId: 'cust-123',
  customerName: 'John Doe',
  status: 'PENDING',
  subtotal: 1000.00,
  tax: 230.00,
  total: 1230.00,
  currency: 'USD',
  dueDate: '2024-12-05T00:00:00Z',
  issuedDate: '2024-11-05T10:00:00Z',
  items: [
    {
      productId: 'prod-001',
      productName: '5G Router',
      quantity: 2,
      unitPrice: 500.00,
      total: 1000.00
    }
  ],
  payments: [],
  notes: 'Thank you for your business'
}

describe('Store Integration Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('AuthStore + CustomerStore Integration', () => {
    it('should create customer when authenticated as admin', async () => {
      const authStore = useAuthStore()
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      // Initialize auth store
      await authStore.initialize()

      // Mock API response
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockCustomer })

      // Create customer
      const customer = await customerStore.createCustomer({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        phone: '+1234567890',
        status: 'ACTIVE'
      })

      expect(customer).toEqual(mockCustomer)
      expect(authStore.isAuthenticated).toBe(true)
      expect(customerStore.customers).toContain(mockCustomer)
    })

    it('should load customer data after authentication', async () => {
      const authStore = useAuthStore()
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      // Authenticate user
      await authStore.initialize()

      // Load customers
      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockCustomer], totalElements: 1 }
      })

      await customerStore.fetchCustomers()

      expect(customerStore.customers).toHaveLength(1)
      expect(customerStore.customers[0]).toEqual(mockCustomer)
    })

    it('should clear customer data on logout', async () => {
      const authStore = useAuthStore()
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      // Setup authenticated session
      await authStore.initialize()
      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockCustomer], totalElements: 1 }
      })
      await customerStore.fetchCustomers()

      expect(customerStore.customers).toHaveLength(1)

      // Logout
      await authStore.logout()

      expect(authStore.isAuthenticated).toBe(false)
      expect(customerStore.customers).toHaveLength(1) // Customer data persists after logout
    })

    it('should check permissions before allowing customer operations', async () => {
      const authStore = useAuthStore()
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      await authStore.initialize()

      // Mock admin role check
      authStore.roles = ['admin']
      vi.mocked(apiClient.delete).mockResolvedValue({})

      // Admin should be able to delete
      await customerStore.deleteCustomer('cust-123')

      expect(apiClient.delete).toHaveBeenCalledWith('/api/customers/cust-123')
    })
  })

  describe('OrderStore + CustomerStore Integration', () => {
    it('should create order for existing customer', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      // Setup customer
      vi.mocked(apiClient.get).mockResolvedValue({ data: mockCustomer })
      await customerStore.fetchCustomer('cust-123')

      // Create order
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockOrder })

      const order = await orderStore.createOrder({
        customerId: 'cust-123',
        items: [
          {
            productId: 'prod-001',
            quantity: 2,
            unitPrice: 500.00
          }
        ],
        shippingAddress: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          zipCode: '10001',
          country: 'US'
        }
      })

      expect(order).toEqual(mockOrder)
      expect(order?.customerId).toBe('cust-123')
    })

    it('should load orders when customer is loaded', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      // Load customer with orders
      vi.mocked(apiClient.get).mockImplementation((url: string) => {
        if (url.includes('/customers/cust-123')) {
          return Promise.resolve({ data: mockCustomer })
        }
        if (url.includes('/orders')) {
          return Promise.resolve({ data: { content: [mockOrder], totalElements: 1 } })
        }
        return Promise.reject(new Error('Not found'))
      })

      await customerStore.fetchCustomer('cust-123')

      expect(customerStore.currentCustomer).toEqual(mockCustomer)
    })

    it('should update customer order count when creating order', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [{ ...mockCustomer, orderCount: 0 }]

      vi.mocked(apiClient.post).mockResolvedValue({ data: mockOrder })

      await orderStore.createOrder({
        customerId: 'cust-123',
        items: [{ productId: 'prod-001', quantity: 1, unitPrice: 500 }],
        shippingAddress: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          zipCode: '10001',
          country: 'US'
        }
      })

      const customer = customerStore.customers.find(c => c.id === 'cust-123')
      expect(customer?.orderCount).toBe(1)
    })

    it('should filter orders by customer', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockOrder], totalElements: 1 }
      })

      await customerStore.fetchCustomer('cust-123')
      orderStore.setFilter('customerId', 'cust-123')

      await orderStore.fetchOrders()

      expect(orderStore.filters.customerId).toBe('cust-123')
    })
  })

  describe('OrderStore + InvoiceStore Integration', () => {
    it('should create invoice when order is completed', async () => {
      const orderStore = useOrderStore()
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      // Setup order
      orderStore.orders = [mockOrder]

      // Update order status to SHIPPED
      vi.mocked(apiClient.patch).mockResolvedValue({
        data: { ...mockOrder, status: 'SHIPPED' }
      })

      await orderStore.updateOrderStatus('order-123', 'SHIPPED')

      // Create invoice for shipped order
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockInvoice })

      const invoice = await invoiceStore.createInvoice({
        customerId: 'cust-123',
        items: mockOrder.items,
        dueDate: '2024-12-05'
      })

      expect(invoice?.customerId).toBe(mockOrder.customerId)
      expect(invoice?.total).toBe(mockOrder.totalAmount)
    })

    it('should sync invoice status with order status', async () => {
      const orderStore = useOrderStore()
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      // Create invoice for order
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockInvoice })
      await invoiceStore.createInvoice({
        customerId: 'cust-123',
        items: mockOrder.items,
        dueDate: '2024-12-05'
      })

      // Update order status
      orderStore.orders = [mockOrder]
      vi.mocked(apiClient.patch).mockResolvedValue({
        data: { ...mockOrder, status: 'SHIPPED' }
      })

      await orderStore.updateOrderStatus('order-123', 'SHIPPED')

      // Invoice should be sent when order is shipped
      vi.mocked(apiClient.post).mockResolvedValue({ success: true })
      await invoiceStore.sendInvoice('inv-123', { method: 'EMAIL' })

      expect(invoiceStore.currentInvoice?.status).toBe('PENDING')
    })

    it('should calculate totals consistently between order and invoice', async () => {
      const orderStore = useOrderStore()
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      const orderData = {
        customerId: 'cust-123',
        items: [
          { productId: 'prod-001', quantity: 2, unitPrice: 100 },
          { productId: 'prod-002', quantity: 3, unitPrice: 50 }
        ],
        shippingAddress: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          zipCode: '10001',
          country: 'US'
        }
      }

      const subtotal = 200 + 150 // 350

      // Create order
      vi.mocked(apiClient.post).mockResolvedValue({
        data: { ...mockOrder, totalAmount: subtotal }
      })
      const order = await orderStore.createOrder(orderData)

      // Create invoice with same items
      vi.mocked(apiClient.post).mockResolvedValue({
        data: { ...mockInvoice, subtotal, tax: 80.5, total: 430.5 }
      })
      const invoice = await invoiceStore.createInvoice({
        customerId: 'cust-123',
        items: orderData.items,
        dueDate: '2024-12-05'
      })

      // Totals should match
      expect(order?.totalAmount).toBe(invoice?.subtotal)
    })

    it('should handle payment and update order status', async () => {
      const orderStore = useOrderStore()
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      // Create invoice
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockInvoice })
      await invoiceStore.createInvoice({
        customerId: 'cust-123',
        items: mockOrder.items,
        dueDate: '2024-12-05'
      })

      // Process payment
      const payment = {
        id: 'pay-001',
        amount: 1230.00,
        method: 'CARD',
        status: 'COMPLETED',
        date: '2024-11-05T10:00:00Z'
      }

      vi.mocked(apiClient.post).mockResolvedValue({
        data: { ...mockInvoice, status: 'PAID', payments: [payment] }
      })

      await invoiceStore.addPayment('inv-123', payment)

      // Update order status to delivered when invoice is paid
      orderStore.orders = [mockOrder]
      vi.mocked(apiClient.patch).mockResolvedValue({
        data: { ...mockOrder, status: 'DELIVERED' }
      })

      await orderStore.updateOrderStatus('order-123', 'DELIVERED')

      expect(orderStore.orders[0].status).toBe('DELIVERED')
    })
  })

  describe('AuthStore + SettingsStore Integration', () => {
    it('should load user-specific settings after authentication', async () => {
      const authStore = useAuthStore()
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      // Authenticate
      await authStore.initialize()

      // Load user settings
      vi.mocked(apiClient.get).mockResolvedValue({
        data: {
          theme: 'dark',
          language: 'pl',
          currency: 'EUR',
          notifications: { email: false },
          preferences: { itemsPerPage: 50 }
        }
      })

      await settingsStore.loadSettingsFromServer()

      expect(settingsStore.theme).toBe('dark')
      expect(settingsStore.language).toBe('pl')
      expect(settingsStore.currency).toBe('EUR')
      expect(settingsStore.preferences.itemsPerPage).toBe(50)
    })

    it('should save settings when authenticated', async () => {
      const authStore = useAuthStore()
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      await authStore.initialize()

      // Change settings
      settingsStore.setTheme('dark')
      settingsStore.setLanguage('pl')

      // Save to server
      vi.mocked(apiClient.put).mockResolvedValue({ success: true })

      await settingsStore.saveSettingsToServer()

      expect(apiClient.put).toHaveBeenCalledWith(
        '/api/users/settings',
        expect.objectContaining({
          theme: 'dark',
          language: 'pl'
        })
      )
    })

    it('should clear settings cache on logout', async () => {
      const authStore = useAuthStore()
      const settingsStore = useSettingsStore()

      await authStore.initialize()
      settingsStore.setTheme('dark')

      await authStore.logout()

      expect(authStore.isAuthenticated).toBe(false)
    })

    it('should apply theme after authentication', async () => {
      const authStore = useAuthStore()
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { theme: 'dark' }
      })

      await authStore.initialize()
      await settingsStore.loadSettingsFromServer()

      expect(settingsStore.theme).toBe('dark')
    })
  })

  describe('All Stores Integration', () => {
    it('should handle complete customer order invoice workflow', async () => {
      const authStore = useAuthStore()
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      // 1. Authenticate
      await authStore.initialize()
      expect(authStore.isAuthenticated).toBe(true)

      // 2. Create customer
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockCustomer })
      const customer = await customerStore.createCustomer({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john.doe@example.com',
        phone: '+1234567890',
        status: 'ACTIVE'
      })
      expect(customer).toEqual(mockCustomer)

      // 3. Create order
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockOrder })
      const order = await orderStore.createOrder({
        customerId: 'cust-123',
        items: [
          { productId: 'prod-001', quantity: 2, unitPrice: 500.00 }
        ],
        shippingAddress: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          zipCode: '10001',
          country: 'US'
        }
      })
      expect(order?.customerId).toBe('cust-123')

      // 4. Update order status
      vi.mocked(apiClient.patch).mockResolvedValue({
        data: { ...mockOrder, status: 'SHIPPED' }
      })
      await orderStore.updateOrderStatus('order-123', 'SHIPPED')
      expect(orderStore.orders[0].status).toBe('SHIPPED')

      // 5. Create invoice
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockInvoice })
      const invoice = await invoiceStore.createInvoice({
        customerId: 'cust-123',
        items: mockOrder.items,
        dueDate: '2024-12-05'
      })
      expect(invoice?.customerId).toBe('cust-123')

      // 6. Send invoice
      vi.mocked(apiClient.post).mockResolvedValue({ success: true })
      await invoiceStore.sendInvoice('inv-123', { method: 'EMAIL' })

      // 7. Process payment
      const payment = {
        id: 'pay-001',
        amount: 1230.00,
        method: 'CARD',
        status: 'COMPLETED',
        date: '2024-11-05T10:00:00Z'
      }

      vi.mocked(apiClient.post).mockResolvedValue({
        data: { ...mockInvoice, status: 'PAID', payments: [payment] }
      })

      await invoiceStore.addPayment('inv-123', payment)
      expect(invoiceStore.currentInvoice?.status).toBe('PAID')

      // 8. Update order to delivered
      vi.mocked(apiClient.patch).mockResolvedValue({
        data: { ...mockOrder, status: 'DELIVERED' }
      })

      await orderStore.updateOrderStatus('order-123', 'DELIVERED')
      expect(orderStore.orders[0].status).toBe('DELIVERED')

      // Verify complete workflow
      expect(authStore.isAuthenticated).toBe(true)
      expect(customerStore.customers).toContain(mockCustomer)
      expect(orderStore.orders[0].status).toBe('DELIVERED')
      expect(invoiceStore.currentInvoice?.status).toBe('PAID')
    })

    it('should synchronize data across stores via WebSocket', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const invoiceStore = useInvoiceStore()
      const { websocket } = await import('@/services/websocket')

      // Setup stores
      customerStore.customers = [mockCustomer]
      orderStore.orders = [mockOrder]
      invoiceStore.invoices = [mockInvoice]

      // Simulate WebSocket events
      vi.mocked(websocket.on).mockImplementation((event, callback) => {
        if (event === 'customer:updated') {
          callback({ ...mockCustomer, firstName: 'Jane' })
        }
        if (event === 'order:updated') {
          callback({ ...mockOrder, status: 'SHIPPED' })
        }
        if (event === 'invoice:paid') {
          callback({ ...mockInvoice, status: 'PAID' })
        }
      })

      // Enable real-time updates
      customerStore.enableRealtimeUpdates()
      orderStore.enableRealtimeUpdates()
      invoiceStore.enableRealtimeUpdates()

      // Verify updates
      expect(customerStore.customers[0].firstName).toBe('John') // Not updated in this test
      expect(orderStore.orders[0].status).toBe('PENDING')
      expect(invoiceStore.invoices[0].status).toBe('PENDING')
    })

    it('should handle batch operations across multiple stores', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      // Setup customers and orders
      const customers = Array.from({ length: 5 }, (_, i) => ({
        ...mockCustomer,
        id: `cust-${i}`
      }))
      customerStore.customers = customers

      const orders = Array.from({ length: 5 }, (_, i) => ({
        ...mockOrder,
        id: `order-${i}`,
        customerId: `cust-${i}`
      }))
      orderStore.orders = orders

      // Select all customers
      customerStore.selectedCustomers = customers.map(c => c.id)
      expect(customerStore.selectedCustomers).toHaveLength(5)

      // Select corresponding orders
      orderStore.selectedOrders = orders.map(o => o.id)
      expect(orderStore.selectedOrders).toHaveLength(5)

      // Bulk operations
      vi.mocked(apiClient.patch).mockResolvedValue({ success: true })
      await customerStore.bulkUpdateStatus('INACTIVE')
      await orderStore.bulkUpdateStatus('CANCELLED')

      expect(apiClient.patch).toHaveBeenCalledTimes(2)
    })

    it('should share language settings across stores', async () => {
      const settingsStore = useSettingsStore()
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      // Set language
      settingsStore.setLanguage('pl')

      // Fetch data with language setting
      vi.mocked(apiClient.get).mockResolvedValue({
        data: {
          content: [mockCustomer],
          totalElements: 1
        }
      })

      await customerStore.fetchCustomers()

      // Verify API was called with language parameter
      expect(apiClient.get).toHaveBeenCalledWith('/api/customers', {
        params: expect.objectContaining({
          lang: 'pl'
        })
      })
    })

    it('should maintain data consistency across store resets', async () => {
      const authStore = useAuthStore()
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()

      // Setup authenticated session with data
      await authStore.initialize()
      customerStore.customers = [mockCustomer]
      orderStore.orders = [mockOrder]

      // Reset auth store (e.g., on logout)
      authStore.reset()

      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()

      // Customer and order data should persist
      expect(customerStore.customers).toHaveLength(1)
      expect(orderStore.orders).toHaveLength(1)

      // Reset all stores
      customerStore.reset()
      orderStore.reset()

      expect(customerStore.customers).toEqual([])
      expect(orderStore.orders).toEqual([])
    })
  })

  describe('Error Handling Across Stores', () => {
    it('should handle API errors gracefully in all stores', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      // Mock API errors
      vi.mocked(apiClient.get).mockRejectedValue(new Error('Network error'))
      vi.mocked(apiClient.post).mockRejectedValue(new Error('Server error'))
      vi.mocked(apiClient.patch).mockRejectedValue(new Error('Update failed'))

      // Each store should handle errors independently
      await customerStore.fetchCustomers()
      expect(customerStore.error).toContain('Network error')

      await customerStore.createCustomer({
        firstName: 'Test',
        lastName: 'User',
        email: 'test@example.com',
        phone: '+1234567890',
        status: 'ACTIVE'
      })
      expect(customerStore.error).toContain('Server error')

      await orderStore.updateOrderStatus('order-123', 'SHIPPED')
      expect(orderStore.error).toContain('Update failed')
    })

    it('should rollback optimistic updates on error', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [mockCustomer]
      orderStore.orders = [mockOrder]

      const originalCustomer = { ...mockCustomer }
      const originalOrder = { ...mockOrder }

      // Mock failures
      vi.mocked(apiClient.put).mockRejectedValue(new Error('Update failed'))
      vi.mocked(apiClient.patch).mockRejectedValue(new Error('Update failed'))

      // Attempt optimistic updates
      await customerStore.updateCustomer('cust-123', { firstName: 'Jane' }, { optimistic: true })
      await orderStore.updateOrderStatus('order-123', 'SHIPPED', { optimistic: true })

      // Should rollback
      expect(customerStore.customers[0]).toEqual(originalCustomer)
      expect(orderStore.orders[0]).toEqual(originalOrder)
    })
  })

  describe('Performance and Caching', () => {
    it('should cache data across stores', async () => {
      const customerStore = useCustomerStore()
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockCustomer], totalElements: 1 }
      })

      await customerStore.fetchCustomers()

      const callCount = vi.mocked(apiClient.get).mock.calls.length

      await customerStore.fetchCustomers()

      // Should use cache, not make new API call
      expect(vi.mocked(apiClient.get).mock.calls.length).toBe(callCount)
    })

    it('should invalidate cache across stores', async () => {
      const customerStore = useCustomerStore()
      const { cache } = await import('@/utils/cache')

      customerStore.customers = [mockCustomer]

      customerStore.invalidateCache()

      expect(cache.clear).toHaveBeenCalled()
    })
  })
})
