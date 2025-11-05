/**
 * OrderStore Tests
 *
 * Comprehensive test coverage for order store
 * Tests state management, filters, pagination, status tracking, and order lifecycle
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useOrderStore } from '@/stores/order'
import type { Order, OrderStatus } from '@/types/order'

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

const mockOrder: Order = {
  id: 'order-123',
  orderNumber: 'ORD-2024-001',
  customerId: 'cust-123',
  customerName: 'John Doe',
  status: 'PENDING' as OrderStatus,
  totalAmount: 1299.99,
  currency: 'USD',
  items: [
    {
      productId: 'prod-001',
      productName: '5G Router',
      quantity: 2,
      unitPrice: 649.99,
      totalPrice: 1299.98
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

const mockOrders: Order[] = [
  {
    id: 'order-123',
    orderNumber: 'ORD-2024-001',
    customerId: 'cust-123',
    customerName: 'John Doe',
    status: 'PENDING' as OrderStatus,
    totalAmount: 1299.99,
    currency: 'USD',
    items: [],
    createdAt: '2024-11-05T10:00:00Z',
    updatedAt: '2024-11-05T10:30:00Z'
  },
  {
    id: 'order-456',
    orderNumber: 'ORD-2024-002',
    customerId: 'cust-456',
    customerName: 'Jane Smith',
    status: 'PROCESSING' as OrderStatus,
    totalAmount: 599.99,
    currency: 'USD',
    items: [],
    createdAt: '2024-11-04T10:00:00Z',
    updatedAt: '2024-11-04T11:00:00Z'
  }
]

describe('OrderStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const orderStore = useOrderStore()

      expect(orderStore.orders).toEqual([])
      expect(orderStore.currentOrder).toBeNull()
      expect(orderStore.totalCount).toBe(0)
      expect(orderStore.currentPage).toBe(1)
      expect(orderStore.pageSize).toBe(20)
      expect(orderStore.filters).toEqual({
        status: [],
        dateFrom: null,
        dateTo: null,
        customerId: null,
        minAmount: null,
        maxAmount: null
      })
      expect(orderStore.searchQuery).toBe('')
      expect(orderStore.sortBy).toBe('createdAt')
      expect(orderStore.sortOrder).toBe('desc')
      expect(orderStore.isLoading).toBe(false)
      expect(orderStore.error).toBeNull()
      expect(orderStore.selectedOrders).toEqual([])
      expect(orderStore.realTimeUpdates).toBe(true)
    })
  })

  describe('fetchOrders', () => {
    it('should fetch orders successfully', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: {
          content: mockOrders,
          totalElements: 2,
          totalPages: 1,
          number: 0,
          size: 20
        }
      })

      await orderStore.fetchOrders()

      expect(apiClient.get).toHaveBeenCalledWith('/api/orders', {
        params: expect.objectContaining({
          page: 1,
          size: 20,
          sortBy: 'createdAt',
          sortOrder: 'desc'
        })
      })

      expect(orderStore.orders).toEqual(mockOrders)
      expect(orderStore.totalCount).toBe(2)
      expect(orderStore.error).toBeNull()
    })

    it('should respect filters when fetching', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.setFilter('status', ['PENDING', 'PROCESSING'])
      orderStore.setFilter('minAmount', 500)

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockOrder], totalElements: 1 }
      })

      await orderStore.fetchOrders()

      expect(apiClient.get).toHaveBeenCalledWith('/api/orders', {
        params: expect.objectContaining({
          status: ['PENDING', 'PROCESSING'],
          minAmount: 500
        })
      })
    })

    it('should handle API errors', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('API error'))

      await orderStore.fetchOrders()

      expect(orderStore.error).toBe('Failed to fetch orders: API error')
      expect(orderStore.orders).toEqual([])
    })
  })

  describe('fetchOrder', () => {
    it('should fetch single order successfully', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({ data: mockOrder })

      const order = await orderStore.fetchOrder('order-123')

      expect(apiClient.get).toHaveBeenCalledWith('/api/orders/order-123')
      expect(order).toEqual(mockOrder)
      expect(orderStore.currentOrder).toEqual(mockOrder)
    })

    it('should handle not found error', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('Not found'))

      const order = await orderStore.fetchOrder('order-999')

      expect(order).toBeNull()
      expect(orderStore.error).toBe('Order not found')
    })
  })

  describe('createOrder', () => {
    it('should create order successfully', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      const newOrder = {
        customerId: 'cust-123',
        items: [
          {
            productId: 'prod-001',
            quantity: 2,
            unitPrice: 649.99
          }
        ],
        shippingAddress: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          zipCode: '10001',
          country: 'US'
        }
      }

      vi.mocked(apiClient.post).mockResolvedValue({ data: mockOrder })

      const order = await orderStore.createOrder(newOrder)

      expect(apiClient.post).toHaveBeenCalledWith('/api/orders', newOrder)
      expect(order).toEqual(mockOrder)
      expect(orderStore.orders).toContain(mockOrder)
    })

    it('should validate required fields', async () => {
      const orderStore = useOrderStore()

      const invalidOrder = {
        customerId: '',
        items: [],
        shippingAddress: null as any
      }

      await expect(orderStore.createOrder(invalidOrder))
        .rejects.toThrow('Validation failed')

      expect(orderStore.error).toBeDefined()
    })

    it('should calculate order total', async () => {
      const orderStore = useOrderStore()
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

      const expectedOrder = {
        ...mockOrder,
        totalAmount: 350 // 200 + 150
      }

      vi.mocked(apiClient.post).mockResolvedValue({ data: expectedOrder })

      await orderStore.createOrder(orderData)

      const createdOrder = await orderStore.createOrder(orderData)
      expect(createdOrder?.totalAmount).toBe(350)
    })
  })

  describe('updateOrderStatus', () => {
    it('should update order status successfully', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.orders = [...mockOrders]
      const updatedOrder = { ...mockOrder, status: 'SHIPPED' as OrderStatus }

      vi.mocked(apiClient.patch).mockResolvedValue({ data: updatedOrder })

      const order = await orderStore.updateOrderStatus('order-123', 'SHIPPED')

      expect(apiClient.patch).toHaveBeenCalledWith('/api/orders/order-123/status', {
        status: 'SHIPPED'
      })
      expect(order?.status).toBe('SHIPPED')
      expect(orderStore.orders[0].status).toBe('SHIPPED')
    })

    it('should handle invalid status transition', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.patch).mockRejectedValue(new Error('Invalid transition'))

      await expect(orderStore.updateOrderStatus('order-123', 'DELIVERED'))
        .rejects.toThrow('Invalid transition')
    })

    it('should update with optimistic update', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.orders = [...mockOrders]
      const originalStatus = orderStore.orders[0].status

      vi.mocked(apiClient.patch).mockResolvedValue({ data: { ...mockOrder, status: 'SHIPPED' } })

      await orderStore.updateOrderStatus('order-123', 'SHIPPED', { optimistic: true })

      expect(orderStore.orders[0].status).toBe('SHIPPED')
    })
  })

  describe('cancelOrder', () => {
    it('should cancel order successfully', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.orders = [...mockOrders]

      vi.mocked(apiClient.post).mockResolvedValue({})

      await orderStore.cancelOrder('order-123', 'Customer request')

      expect(apiClient.post).toHaveBeenCalledWith('/api/orders/order-123/cancel', {
        reason: 'Customer request'
      })
      expect(orderStore.orders[0].status).toBe('CANCELLED')
    })

    it('should not cancel shipped order', async () => {
      const orderStore = useOrderStore()
      const shippedOrder = { ...mockOrder, status: 'SHIPPED' as OrderStatus }
      orderStore.orders = [shippedOrder]

      await expect(orderStore.cancelOrder('order-123', 'Test'))
        .rejects.toThrow('Cannot cancel shipped order')
    })

    it('should rollback on cancellation error', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.orders = [...mockOrders]

      vi.mocked(apiClient.post).mockRejectedValue(new Error('Cancel failed'))

      await expect(orderStore.cancelOrder('order-123', 'Test'))
        .rejects.toThrow()

      // Should still be pending
      expect(orderStore.orders[0].status).toBe('PENDING')
    })
  })

  describe('Filtering', () => {
    it('should set individual filter', () => {
      const orderStore = useOrderStore()

      orderStore.setFilter('status', ['PENDING', 'PROCESSING'])

      expect(orderStore.filters.status).toEqual(['PENDING', 'PROCESSING'])
    })

    it('should set multiple filters', () => {
      const orderStore = useOrderStore()

      orderStore.setFilters({
        status: ['PENDING'],
        minAmount: 100,
        maxAmount: 1000
      })

      expect(orderStore.filters.status).toEqual(['PENDING'])
      expect(orderStore.filters.minAmount).toBe(100)
      expect(orderStore.filters.maxAmount).toBe(1000)
    })

    it('should clear specific filter', () => {
      const orderStore = useOrderStore()

      orderStore.setFilters({ status: ['PENDING'] })
      orderStore.clearFilter('status')

      expect(orderStore.filters.status).toEqual([])
    })

    it('should clear all filters', () => {
      const orderStore = useOrderStore()

      orderStore.setFilters({
        status: ['PENDING'],
        minAmount: 100,
        customerId: 'cust-123'
      })

      orderStore.clearAllFilters()

      expect(orderStore.filters).toEqual({
        status: [],
        dateFrom: null,
        dateTo: null,
        customerId: null,
        minAmount: null,
        maxAmount: null
      })
    })

    it('should filter by date range', () => {
      const orderStore = useOrderStore()

      const dateFrom = new Date('2024-11-01')
      const dateTo = new Date('2024-11-30')

      orderStore.setFilter('dateFrom', dateFrom)
      orderStore.setFilter('dateTo', dateTo)

      expect(orderStore.filters.dateFrom).toBe(dateFrom)
      expect(orderStore.filters.dateTo).toBe(dateTo)
    })

    it('should filter by amount range', () => {
      const orderStore = useOrderStore()

      orderStore.setFilter('minAmount', 100)
      orderStore.setFilter('maxAmount', 1000)

      expect(orderStore.filters.minAmount).toBe(100)
      expect(orderStore.filters.maxAmount).toBe(1000)
    })
  })

  describe('Search', () => {
    it('should set search query', () => {
      const orderStore = useOrderStore()

      orderStore.setSearch('ORD-2024-001')

      expect(orderStore.searchQuery).toBe('ORD-2024-001')
    })

    it('should search by order number', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.setSearch('ORD-2024-001')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockOrder], totalElements: 1 }
      })

      await orderStore.fetchOrders()

      expect(apiClient.get).toHaveBeenCalledWith('/api/orders', {
        params: expect.objectContaining({
          search: 'ORD-2024-001'
        })
      })
    })

    it('should search by customer name', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.setSearch('John Doe')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockOrder], totalElements: 1 }
      })

      await orderStore.fetchOrders()

      expect(apiClient.get).toHaveBeenCalledWith('/api/orders', {
        params: expect.objectContaining({
          search: 'John Doe'
        })
      })
    })

    it('should clear search', () => {
      const orderStore = useOrderStore()

      orderStore.setSearch('test')
      orderStore.clearSearch()

      expect(orderStore.searchQuery).toBe('')
    })
  })

  describe('Sorting', () => {
    it('should set sort parameters', () => {
      const orderStore = useOrderStore()

      orderStore.setSorting('orderNumber', 'asc')

      expect(orderStore.sortBy).toBe('orderNumber')
      expect(orderStore.sortOrder).toBe('asc')
    })

    it('should toggle sort order', () => {
      const orderStore = useOrderStore()

      orderStore.setSorting('totalAmount', 'asc')
      orderStore.toggleSort('totalAmount')

      expect(orderStore.sortOrder).toBe('desc')
    })

    it('should change sort field', () => {
      const orderStore = useOrderStore()

      orderStore.setSorting('createdAt', 'desc')
      orderStore.setSorting('totalAmount', 'asc')

      expect(orderStore.sortBy).toBe('totalAmount')
      expect(orderStore.sortOrder).toBe('asc')
    })
  })

  describe('Pagination', () => {
    it('should set current page', () => {
      const orderStore = useOrderStore()

      orderStore.setPage(3)

      expect(orderStore.currentPage).toBe(3)
    })

    it('should set page size', () => {
      const orderStore = useOrderStore()

      orderStore.setPageSize(50)

      expect(orderStore.pageSize).toBe(50)
      expect(orderStore.currentPage).toBe(1)
    })

    it('should navigate to next page', () => {
      const orderStore = useOrderStore()

      orderStore.setPage(1)
      orderStore.nextPage()

      expect(orderStore.currentPage).toBe(2)
    })

    it('should navigate to previous page', () => {
      const orderStore = useOrderStore()

      orderStore.setPage(3)
      orderStore.prevPage()

      expect(orderStore.currentPage).toBe(2)
    })

    it('should calculate total pages', () => {
      const orderStore = useOrderStore()

      orderStore.totalCount = 100
      orderStore.pageSize = 20

      expect(orderStore.totalPages).toBe(5)
    })
  })

  describe('Real-time Updates', () => {
    it('should enable real-time updates', async () => {
      const orderStore = useOrderStore()
      const { websocket } = await import('@/services/websocket')

      orderStore.enableRealtimeUpdates()

      expect(orderStore.realTimeUpdates).toBe(true)
      expect(websocket.connect).toHaveBeenCalled()
    })

    it('should disable real-time updates', async () => {
      const orderStore = useOrderStore()
      const { websocket } = await import('@/services/websocket')

      orderStore.disableRealtimeUpdates()

      expect(orderStore.realTimeUpdates).toBe(false)
      expect(websocket.disconnect).toHaveBeenCalled()
    })

    it('should handle order status update via WebSocket', async () => {
      const orderStore = useOrderStore()
      const { websocket } = await import('@/services/websocket')

      orderStore.orders = [...mockOrders]

      // Simulate WebSocket event
      vi.mocked(websocket.on).mockImplementation((event, callback) => {
        if (event === 'order:updated') {
          callback({
            id: 'order-123',
            status: 'SHIPPED'
          })
        }
      })

      orderStore.enableRealtimeUpdates()

      // Find the callback and trigger it
      const callbacks = vi.mocked(websocket.on).mock.calls
      const orderUpdateCallback = callbacks.find(cb => cb[0] === 'order:updated')

      expect(orderStore.orders[0].status).toBe('PENDING')
    })

    it('should handle new order via WebSocket', async () => {
      const orderStore = useOrderStore()
      const { websocket } = await import('@/services/websocket')

      orderStore.orders = [...mockOrders]

      vi.mocked(websocket.on).mockImplementation((event, callback) => {
        if (event === 'order:created') {
          callback(mockOrder)
        }
      })

      orderStore.enableRealtimeUpdates()

      expect(orderStore.orders).toHaveLength(2) // Should be 3 with new order
    })
  })

  describe('Order Statistics', () => {
    it('should calculate order statistics', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: {
          total: 100,
          pending: 20,
          processing: 30,
          shipped: 25,
          delivered: 20,
          cancelled: 5,
          totalRevenue: 50000,
          averageOrderValue: 500
        }
      })

      const stats = await orderStore.fetchStatistics()

      expect(stats.total).toBe(100)
      expect(stats.pending).toBe(20)
      expect(stats.totalRevenue).toBe(50000)
      expect(stats.averageOrderValue).toBe(500)
    })

    it('should get status counts', () => {
      const orderStore = useOrderStore()

      orderStore.orders = [
        { ...mockOrder, status: 'PENDING' },
        { ...mockOrder, status: 'PENDING' },
        { ...mockOrder, status: 'PROCESSING' }
      ]

      const counts = orderStore.getStatusCounts()

      expect(counts.PENDING).toBe(2)
      expect(counts.PROCESSING).toBe(1)
    })
  })

  describe('Order Validation', () => {
    it('should validate order before creation', () => {
      const orderStore = useOrderStore()

      const validOrder = {
        customerId: 'cust-123',
        items: [{ productId: 'prod-001', quantity: 1, unitPrice: 100 }],
        shippingAddress: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          zipCode: '10001',
          country: 'US'
        }
      }

      expect(() => orderStore.validateOrder(validOrder)).not.toThrow()
    })

    it('should reject order with empty items', () => {
      const orderStore = useOrderStore()

      const invalidOrder = {
        customerId: 'cust-123',
        items: [],
        shippingAddress: null as any
      }

      expect(() => orderStore.validateOrder(invalidOrder))
        .toThrow('Order must have at least one item')
    })

    it('should reject order with invalid shipping address', () => {
      const orderStore = useOrderStore()

      const invalidOrder = {
        customerId: 'cust-123',
        items: [{ productId: 'prod-001', quantity: 1, unitPrice: 100 }],
        shippingAddress: {
          street: '',
          city: '',
          state: '',
          zipCode: '',
          country: ''
        }
      }

      expect(() => orderStore.validateOrder(invalidOrder))
        .toThrow('Shipping address is required')
    })
  })

  describe('Bulk Operations', () => {
    it('should select multiple orders', () => {
      const orderStore = useOrderStore()

      orderStore.selectOrders(['order-123', 'order-456'])

      expect(orderStore.selectedOrders).toEqual(['order-123', 'order-456'])
    })

    it('should deselect all orders', () => {
      const orderStore = useOrderStore()

      orderStore.selectedOrders = ['order-123', 'order-456']
      orderStore.deselectAll()

      expect(orderStore.selectedOrders).toEqual([])
    })

    it('should bulk update status', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.orders = [...mockOrders]
      orderStore.selectedOrders = ['order-123', 'order-456']

      vi.mocked(apiClient.patch).mockResolvedValue({})

      await orderStore.bulkUpdateStatus('PROCESSING')

      expect(apiClient.patch).toHaveBeenCalledWith('/api/orders/batch/status', {
        orderIds: ['order-123', 'order-456'],
        status: 'PROCESSING'
      })
    })

    it('should bulk cancel orders', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.orders = [...mockOrders]
      orderStore.selectedOrders = ['order-123']

      vi.mocked(apiClient.post).mockResolvedValue({})

      await orderStore.bulkCancel('Customer request')

      expect(apiClient.post).toHaveBeenCalledWith('/api/orders/batch/cancel', {
        orderIds: ['order-123'],
        reason: 'Customer request'
      })
    })
  })

  describe('Computed Properties', () => {
    it('should return filtered orders', () => {
      const orderStore = useOrderStore()

      orderStore.orders = [...mockOrders]
      orderStore.setFilter('status', ['PENDING'])

      const filtered = orderStore.filteredOrders

      expect(filtered.every(o => o.status === 'PENDING')).toBe(true)
    })

    it('should return sorted orders', () => {
      const orderStore = useOrderStore()

      orderStore.orders = [...mockOrders]
      orderStore.setSorting('totalAmount', 'desc')

      const sorted = orderStore.sortedOrders

      expect(sorted[0].totalAmount).toBeGreaterThanOrEqual(sorted[1].totalAmount)
    })

    it('should return paged orders', () => {
      const orderStore = useOrderStore()

      const manyOrders = Array.from({ length: 50 }, (_, i) => ({
        ...mockOrder,
        id: `order-${i}`,
        totalAmount: i * 100
      }))

      orderStore.orders = manyOrders
      orderStore.currentPage = 1
      orderStore.pageSize = 20

      const paged = orderStore.pagedOrders

      expect(paged).toHaveLength(20)
    })

    it('should check if has next page', () => {
      const orderStore = useOrderStore()

      orderStore.totalCount = 50
      orderStore.currentPage = 2
      orderStore.pageSize = 20

      expect(orderStore.hasNextPage).toBe(false)
    })

    it('should return selected orders data', () => {
      const orderStore = useOrderStore()

      orderStore.orders = [...mockOrders]
      orderStore.selectedOrders = ['order-123', 'order-456']

      const selected = orderStore.selectedOrdersData

      expect(selected).toHaveLength(2)
    })
  })

  describe('Error Handling', () => {
    it('should set error state', () => {
      const orderStore = useOrderStore()

      orderStore.setError('Test error')

      expect(orderStore.error).toBe('Test error')
    })

    it('should clear error', () => {
      const orderStore = useOrderStore()

      orderStore.error = 'Some error'
      orderStore.clearError()

      expect(orderStore.error).toBeNull()
    })

    it('should handle network errors', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('Network error'))

      await orderStore.fetchOrders()

      expect(orderStore.error).toContain('Network error')
    })
  })

  describe('Integration Tests', () => {
    it('should handle complete order lifecycle', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      // Create order
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockOrder })
      const created = await orderStore.createOrder({
        customerId: 'cust-123',
        items: [{ productId: 'prod-001', quantity: 1, unitPrice: 100 }],
        shippingAddress: {
          street: '123 Main St',
          city: 'New York',
          state: 'NY',
          zipCode: '10001',
          country: 'US'
        }
      })
      expect(created).toEqual(mockOrder)

      // Update status
      const shippedOrder = { ...mockOrder, status: 'SHIPPED' }
      vi.mocked(apiClient.patch).mockResolvedValue({ data: shippedOrder })
      const updated = await orderStore.updateStatus('order-123', 'SHIPPED')
      expect(updated?.status).toBe('SHIPPED')

      // Fetch statistics
      vi.mocked(apiClient.get).mockResolvedValue({
        data: { total: 1, totalRevenue: 100 }
      })
      const stats = await orderStore.fetchStatistics()
      expect(stats.total).toBe(1)
    })

    it('should handle filtering and pagination together', async () => {
      const orderStore = useOrderStore()
      const { apiClient } = await import('@/services/api')

      orderStore.setFilter('status', ['PENDING'])
      orderStore.setPage(2)
      orderStore.setPageSize(10)

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [], totalElements: 25 }
      })

      await orderStore.fetchOrders()

      expect(apiClient.get).toHaveBeenCalledWith('/api/orders', {
        params: expect.objectContaining({
          status: ['PENDING'],
          page: 2,
          size: 10
        })
      })
    })
  })
})
