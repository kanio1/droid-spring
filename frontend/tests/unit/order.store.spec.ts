import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useOrderStore } from '~/stores/order'
import type { Order, OrderStatus, OrderType, OrderPriority } from '~/schemas/order'

// Mock useApi composable
const mockUseApi = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: mockUseApi
}))

describe('Order Store', () => {
  beforeEach(() => {
    mockUseApi.mockReturnValue({
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      del: vi.fn(),
      patch: vi.fn(),
      create: vi.fn(),
      read: vi.fn(),
      update: vi.fn(),
      remove: vi.fn(),
      paginatedGet: vi.fn(),
      request: vi.fn(),
      loading: vi.fn(() => false),
      handleSuccess: vi.fn(),
      buildUrl: vi.fn(),
      baseURL: 'http://localhost:8080/api'
    })
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useOrderStore()

    expect(store.orders).toEqual([])
    expect(store.currentOrder).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.pagination.page).toBe(0)
    expect(store.pagination.size).toBe(20)
    expect(store.pagination.totalElements).toBe(0)
  })

  it('should fetch orders successfully', async () => {
    const store = useOrderStore()
    const mockOrders: Order[] = [
      {
        id: '1',
        orderNumber: 'ORD-2024-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        orderType: 'NEW' as OrderType,
        orderTypeDisplayName: 'New',
        status: 'PENDING' as OrderStatus,
        statusDisplayName: 'Pending',
        priority: 'NORMAL' as OrderPriority,
        priorityDisplayName: 'Normal',
        totalAmount: 99.99,
        currency: 'PLN',
        requestedDate: '2024-01-15',
        promisedDate: null,
        completedDate: null,
        orderChannel: 'Web',
        salesRepId: 'rep-1',
        salesRepName: 'Jane Smith',
        notes: 'Customer requested premium package',
        isPending: true,
        isCompleted: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockOrders,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.fetchOrders()

    expect(store.orders).toEqual(mockOrders)
    expect(store.pagination.totalElements).toBe(1)
  })

  it('should filter orders by status', async () => {
    const store = useOrderStore()
    const mockOrders: Order[] = [
      {
        id: '1',
        orderNumber: 'ORD-2024-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        orderType: 'NEW' as OrderType,
        orderTypeDisplayName: 'New',
        status: 'PENDING' as OrderStatus,
        statusDisplayName: 'Pending',
        priority: 'NORMAL' as OrderPriority,
        priorityDisplayName: 'Normal',
        totalAmount: 99.99,
        currency: 'PLN',
        requestedDate: '2024-01-15',
        promisedDate: null,
        completedDate: null,
        orderChannel: 'Web',
        salesRepId: 'rep-1',
        salesRepName: 'Jane Smith',
        notes: 'Customer requested premium package',
        isPending: true,
        isCompleted: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockOrders,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.getOrdersByStatus('PENDING')

    expect(get).toHaveBeenCalledWith('/orders', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        status: 'PENDING'
      }
    })
  })

  it('should create an order', async () => {
    const store = useOrderStore()
    const newOrder = {
      orderNumber: 'ORD-2024-002',
      customerId: 'cust-2',
      orderType: 'UPGRADE' as OrderType,
      status: 'PENDING' as OrderStatus,
      priority: 'HIGH' as OrderPriority,
      totalAmount: 149.99,
      currency: 'PLN',
      requestedDate: '2024-01-20',
      orderChannel: 'Phone',
      salesRepId: 'rep-2',
      notes: 'Upgrade to premium'
    }

    const mockOrder: Order = {
      id: '2',
      ...newOrder,
      orderTypeDisplayName: 'Upgrade',
      statusDisplayName: 'Pending',
      priorityDisplayName: 'High',
      promisedDate: null,
      completedDate: null,
      salesRepName: 'Bob Johnson',
      isPending: true,
      isCompleted: false,
      canBeCancelled: true,
      itemCount: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    const { post } = mockUseApi()
    vi.mocked(post).mockResolvedValueOnce({
      data: mockOrder
    } as any)

    await store.createOrder(newOrder)

    expect(store.orders).toHaveLength(1)
    expect(store.orders[0]).toEqual(mockOrder)
  })

  it('should update order status', async () => {
    const store = useOrderStore()

    const existingOrder: Order = {
      id: '1',
      orderNumber: 'ORD-2024-001',
      customerId: 'cust-1',
      customerName: 'John Doe',
      orderType: 'NEW' as OrderType,
      orderTypeDisplayName: 'New',
      status: 'PENDING' as OrderStatus,
      statusDisplayName: 'Pending',
      priority: 'NORMAL' as OrderPriority,
      priorityDisplayName: 'Normal',
      totalAmount: 99.99,
      currency: 'PLN',
      requestedDate: '2024-01-15',
      promisedDate: null,
      completedDate: null,
      orderChannel: 'Web',
      salesRepId: 'rep-1',
      salesRepName: 'Jane Smith',
      notes: 'Customer requested premium package',
      isPending: true,
      isCompleted: false,
      canBeCancelled: true,
      itemCount: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.orders = [existingOrder]

    const statusData = {
      id: '1',
      status: 'CONFIRMED' as OrderStatus
    }

    const mockUpdatedOrder: Order = {
      ...existingOrder,
      status: 'CONFIRMED' as OrderStatus,
      statusDisplayName: 'Confirmed',
      isPending: false,
      promisedDate: '2024-01-16',
      updatedAt: '2024-01-02T00:00:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedOrder
    } as any)

    await store.updateOrderStatus(statusData)

    expect(store.orders[0].status).toBe('CONFIRMED')
    expect(store.orders[0].version).toBe(2)
    expect(store.orders[0].isPending).toBe(false)
  })

  it('should compute filtered orders correctly', () => {
    const store = useOrderStore()

    const mockOrders: Order[] = [
      {
        id: '1',
        orderNumber: 'ORD-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        orderType: 'NEW' as OrderType,
        orderTypeDisplayName: 'New',
        status: 'PENDING' as OrderStatus,
        statusDisplayName: 'Pending',
        priority: 'NORMAL' as OrderPriority,
        priorityDisplayName: 'Normal',
        totalAmount: 99.99,
        currency: 'PLN',
        requestedDate: '2024-01-15',
        promisedDate: null,
        completedDate: null,
        orderChannel: 'Web',
        salesRepId: 'rep-1',
        salesRepName: 'Jane Smith',
        notes: '',
        isPending: true,
        isCompleted: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        orderNumber: 'ORD-002',
        customerId: 'cust-2',
        customerName: 'Jane Doe',
        orderType: 'UPGRADE' as OrderType,
        orderTypeDisplayName: 'Upgrade',
        status: 'COMPLETED' as OrderStatus,
        statusDisplayName: 'Completed',
        priority: 'URGENT' as OrderPriority,
        priorityDisplayName: 'Urgent',
        totalAmount: 149.99,
        currency: 'PLN',
        requestedDate: '2024-01-10',
        promisedDate: '2024-01-12',
        completedDate: '2024-01-12',
        orderChannel: 'Phone',
        salesRepId: 'rep-2',
        salesRepName: 'Bob Smith',
        notes: '',
        isPending: false,
        isCompleted: true,
        canBeCancelled: false,
        itemCount: 2,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '3',
        orderNumber: 'ORD-003',
        customerId: 'cust-3',
        customerName: 'Bob Doe',
        orderType: 'CANCEL' as OrderType,
        orderTypeDisplayName: 'Cancel',
        status: 'CANCELLED' as OrderStatus,
        statusDisplayName: 'Cancelled',
        priority: 'LOW' as OrderPriority,
        priorityDisplayName: 'Low',
        totalAmount: 0,
        currency: 'PLN',
        requestedDate: '2024-01-05',
        promisedDate: null,
        completedDate: null,
        orderChannel: 'Web',
        salesRepId: 'rep-3',
        salesRepName: 'Alice Smith',
        notes: 'Customer cancelled',
        isPending: false,
        isCompleted: false,
        canBeCancelled: false,
        itemCount: 0,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    store.orders = mockOrders

    expect(store.pendingOrders).toHaveLength(1)
    expect(store.confirmedOrders).toHaveLength(0)
    expect(store.processingOrders).toHaveLength(0)
    expect(store.completedOrders).toHaveLength(1)
    expect(store.cancelledOrders).toHaveLength(1)
    expect(store.newOrders).toHaveLength(1)
    expect(store.upgradeOrders).toHaveLength(1)
    expect(store.cancelOrders).toHaveLength(1)
    expect(store.urgentOrders).toHaveLength(1)
    expect(store.highPriorityOrders).toHaveLength(0)
  })

  it('should get orders by customer', async () => {
    const store = useOrderStore()
    const mockOrders: Order[] = [
      {
        id: '1',
        orderNumber: 'ORD-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        orderType: 'NEW' as OrderType,
        orderTypeDisplayName: 'New',
        status: 'PENDING' as OrderStatus,
        statusDisplayName: 'Pending',
        priority: 'NORMAL' as OrderPriority,
        priorityDisplayName: 'Normal',
        totalAmount: 99.99,
        currency: 'PLN',
        requestedDate: '2024-01-15',
        promisedDate: null,
        completedDate: null,
        orderChannel: 'Web',
        salesRepId: 'rep-1',
        salesRepName: 'Jane Smith',
        notes: '',
        isPending: true,
        isCompleted: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockOrders,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.getOrdersByCustomer('cust-1')

    expect(get).toHaveBeenCalledWith('/orders', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        customerId: 'cust-1'
      }
    })
  })

  it('should reset store state', () => {
    const store = useOrderStore()

    store.orders = [
      {
        id: '1',
        orderNumber: 'ORD-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        orderType: 'NEW' as OrderType,
        orderTypeDisplayName: 'New',
        status: 'PENDING' as OrderStatus,
        statusDisplayName: 'Pending',
        priority: 'NORMAL' as OrderPriority,
        priorityDisplayName: 'Normal',
        totalAmount: 99.99,
        currency: 'PLN',
        requestedDate: '2024-01-15',
        promisedDate: null,
        completedDate: null,
        orderChannel: 'Web',
        salesRepId: 'rep-1',
        salesRepName: 'Jane Smith',
        notes: '',
        isPending: true,
        isCompleted: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    store.pagination.totalElements = 1
    store.currentOrder = {
      id: '1',
      orderNumber: 'ORD-001',
      customerId: 'cust-1',
      customerName: 'John Doe',
      orderType: 'NEW' as OrderType,
      orderTypeDisplayName: 'New',
      status: 'PENDING' as OrderStatus,
      statusDisplayName: 'Pending',
      priority: 'NORMAL' as OrderPriority,
      priorityDisplayName: 'Normal',
      totalAmount: 99.99,
      currency: 'PLN',
      requestedDate: '2024-01-15',
      promisedDate: null,
      completedDate: null,
      orderChannel: 'Web',
      salesRepId: 'rep-1',
      salesRepName: 'Jane Smith',
      notes: '',
      isPending: true,
      isCompleted: false,
      canBeCancelled: true,
      itemCount: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.reset()

    expect(store.orders).toEqual([])
    expect(store.currentOrder).toBeNull()
    expect(store.pagination.totalElements).toBe(0)
  })
})
