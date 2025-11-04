import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSubscriptionStore } from '~/stores/subscription'
import type { Subscription, SubscriptionStatus } from '~/schemas/subscription'

// Mock useApi composable
const mockUseApi = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: mockUseApi
}))

describe('Subscription Store', () => {
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
    const store = useSubscriptionStore()

    expect(store.subscriptions).toEqual([])
    expect(store.currentSubscription).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.pagination.page).toBe(0)
    expect(store.pagination.size).toBe(20)
    expect(store.pagination.totalElements).toBe(0)
  })

  it('should fetch subscriptions successfully', async () => {
    const store = useSubscriptionStore()
    const mockSubscriptions: Subscription[] = [
      {
        id: '1',
        subscriptionNumber: 'SUB-2024-001',
        customerId: 'cust-1',
        productId: 'prod-1',
        orderId: 'order-1',
        status: 'ACTIVE' as SubscriptionStatus,
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        billingStart: '2024-01-01',
        nextBillingDate: '2024-02-01',
        billingPeriod: 'monthly',
        price: 99.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 99.99,
        configuration: { feature1: true, feature2: false },
        autoRenew: true,
        renewalNoticeSent: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockSubscriptions,
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

    await store.fetchSubscriptions()

    expect(store.subscriptions).toEqual(mockSubscriptions)
    expect(store.pagination.totalElements).toBe(1)
  })

  it('should filter subscriptions by status', async () => {
    const store = useSubscriptionStore()
    const mockSubscriptions: Subscription[] = [
      {
        id: '1',
        subscriptionNumber: 'SUB-2024-001',
        customerId: 'cust-1',
        productId: 'prod-1',
        orderId: 'order-1',
        status: 'ACTIVE' as SubscriptionStatus,
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        billingStart: '2024-01-01',
        nextBillingDate: '2024-02-01',
        billingPeriod: 'monthly',
        price: 99.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 99.99,
        configuration: { feature1: true },
        autoRenew: true,
        renewalNoticeSent: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockSubscriptions,
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

    await store.getSubscriptionsByStatus('ACTIVE')

    expect(get).toHaveBeenCalledWith('/subscriptions', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        status: 'ACTIVE'
      }
    })
  })

  it('should create a subscription', async () => {
    const store = useSubscriptionStore()
    const newSubscription = {
      subscriptionNumber: 'SUB-2024-002',
      customerId: 'cust-2',
      productId: 'prod-2',
      orderId: 'order-2',
      status: 'ACTIVE' as SubscriptionStatus,
      startDate: '2024-02-01',
      endDate: '2025-01-31',
      billingStart: '2024-02-01',
      nextBillingDate: '2024-03-01',
      billingPeriod: 'monthly',
      price: 149.99,
      currency: 'PLN',
      discountAmount: 10.00,
      netAmount: 139.99,
      configuration: { premium: true },
      autoRenew: true
    }

    const mockSubscription: Subscription = {
      id: '2',
      ...newSubscription,
      createdAt: '2024-02-01T00:00:00Z',
      updatedAt: '2024-02-01T00:00:00Z',
      version: 1
    }

    const { post } = mockUseApi()
    vi.mocked(post).mockResolvedValueOnce({
      data: mockSubscription
    } as any)

    await store.createSubscription(newSubscription)

    expect(store.subscriptions).toHaveLength(1)
    expect(store.subscriptions[0]).toEqual(mockSubscription)
  })

  it('should update a subscription', async () => {
    const store = useSubscriptionStore()

    const existingSubscription: Subscription = {
      id: '1',
      subscriptionNumber: 'SUB-2024-001',
      customerId: 'cust-1',
      productId: 'prod-1',
      orderId: 'order-1',
      status: 'ACTIVE' as SubscriptionStatus,
      startDate: '2024-01-01',
      endDate: '2024-12-31',
      billingStart: '2024-01-01',
      nextBillingDate: '2024-02-01',
      billingPeriod: 'monthly',
      price: 99.99,
      currency: 'PLN',
      discountAmount: 0,
      netAmount: 99.99,
      configuration: { feature1: true },
      autoRenew: true,
      renewalNoticeSent: false,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.subscriptions = [existingSubscription]

    const updateData = {
      id: '1',
      endDate: '2025-12-31',
      nextBillingDate: '2024-03-01',
      price: 89.99,
      discountAmount: 5.00,
      netAmount: 84.99,
      configuration: { feature1: true, feature2: true },
      autoRenew: false,
      version: 1
    }

    const mockUpdatedSubscription: Subscription = {
      ...existingSubscription,
      ...updateData,
      updatedAt: '2024-01-15T00:00:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedSubscription
    } as any)

    await store.updateSubscription(updateData)

    expect(store.subscriptions[0].endDate).toBe('2025-12-31')
    expect(store.subscriptions[0].price).toBe(89.99)
    expect(store.subscriptions[0].autoRenew).toBe(false)
    expect(store.subscriptions[0].version).toBe(2)
  })

  it('should change subscription status', async () => {
    const store = useSubscriptionStore()

    const existingSubscription: Subscription = {
      id: '1',
      subscriptionNumber: 'SUB-2024-001',
      customerId: 'cust-1',
      productId: 'prod-1',
      orderId: 'order-1',
      status: 'ACTIVE' as SubscriptionStatus,
      startDate: '2024-01-01',
      endDate: '2024-12-31',
      billingStart: '2024-01-01',
      nextBillingDate: '2024-02-01',
      billingPeriod: 'monthly',
      price: 99.99,
      currency: 'PLN',
      discountAmount: 0,
      netAmount: 99.99,
      configuration: { feature1: true },
      autoRenew: true,
      renewalNoticeSent: false,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.subscriptions = [existingSubscription]

    const statusData = {
      id: '1',
      status: 'SUSPENDED' as SubscriptionStatus
    }

    const mockUpdatedSubscription: Subscription = {
      ...existingSubscription,
      status: 'SUSPENDED' as SubscriptionStatus,
      updatedAt: '2024-01-10T00:00:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedSubscription
    } as any)

    await store.changeSubscriptionStatus(statusData)

    expect(store.subscriptions[0].status).toBe('SUSPENDED')
    expect(store.subscriptions[0].version).toBe(2)
  })

  it('should renew a subscription', async () => {
    const store = useSubscriptionStore()

    const existingSubscription: Subscription = {
      id: '1',
      subscriptionNumber: 'SUB-2024-001',
      customerId: 'cust-1',
      productId: 'prod-1',
      orderId: 'order-1',
      status: 'ACTIVE' as SubscriptionStatus,
      startDate: '2024-01-01',
      endDate: '2024-12-31',
      billingStart: '2024-01-01',
      nextBillingDate: '2024-01-01',
      billingPeriod: 'monthly',
      price: 99.99,
      currency: 'PLN',
      discountAmount: 0,
      netAmount: 99.99,
      configuration: { feature1: true },
      autoRenew: true,
      renewalNoticeSent: false,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.subscriptions = [existingSubscription]

    const mockRenewedSubscription: Subscription = {
      ...existingSubscription,
      startDate: '2025-01-01',
      endDate: '2025-12-31',
      nextBillingDate: '2025-02-01',
      updatedAt: '2024-12-31T23:59:59Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockRenewedSubscription
    } as any)

    await store.renewSubscription('1')

    expect(store.subscriptions[0].startDate).toBe('2025-01-01')
    expect(store.subscriptions[0].endDate).toBe('2025-12-31')
    expect(store.subscriptions[0].version).toBe(2)
  })

  it('should get subscriptions by customer', async () => {
    const store = useSubscriptionStore()
    const mockSubscriptions: Subscription[] = [
      {
        id: '1',
        subscriptionNumber: 'SUB-2024-001',
        customerId: 'cust-1',
        productId: 'prod-1',
        orderId: 'order-1',
        status: 'ACTIVE' as SubscriptionStatus,
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        billingStart: '2024-01-01',
        nextBillingDate: '2024-02-01',
        billingPeriod: 'monthly',
        price: 99.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 99.99,
        configuration: { feature1: true },
        autoRenew: true,
        renewalNoticeSent: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockSubscriptions,
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

    await store.getSubscriptionsByCustomer('cust-1')

    expect(get).toHaveBeenCalledWith('/subscriptions', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        customerId: 'cust-1'
      }
    })
  })

  it('should compute filtered subscriptions correctly', () => {
    const store = useSubscriptionStore()

    const mockSubscriptions: Subscription[] = [
      {
        id: '1',
        subscriptionNumber: 'SUB-001',
        customerId: 'cust-1',
        productId: 'prod-1',
        orderId: 'order-1',
        status: 'ACTIVE' as SubscriptionStatus,
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        billingStart: '2024-01-01',
        nextBillingDate: '2024-02-01',
        billingPeriod: 'monthly',
        price: 99.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 99.99,
        configuration: { feature1: true },
        autoRenew: true,
        renewalNoticeSent: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        subscriptionNumber: 'SUB-002',
        customerId: 'cust-2',
        productId: 'prod-2',
        orderId: 'order-2',
        status: 'SUSPENDED' as SubscriptionStatus,
        startDate: '2024-01-01',
        endDate: '2024-06-30',
        billingStart: '2024-01-01',
        nextBillingDate: '2024-02-01',
        billingPeriod: 'monthly',
        price: 149.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 149.99,
        configuration: { premium: true },
        autoRenew: false,
        renewalNoticeSent: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '3',
        subscriptionNumber: 'SUB-003',
        customerId: 'cust-3',
        productId: 'prod-3',
        orderId: 'order-3',
        status: 'CANCELLED' as SubscriptionStatus,
        startDate: '2023-01-01',
        endDate: '2023-12-31',
        billingStart: '2023-01-01',
        nextBillingDate: null,
        billingPeriod: 'monthly',
        price: 79.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 79.99,
        configuration: { basic: true },
        autoRenew: false,
        renewalNoticeSent: false,
        createdAt: '2023-01-01T00:00:00Z',
        updatedAt: '2023-12-31T00:00:00Z',
        version: 1
      }
    ]

    store.subscriptions = mockSubscriptions

    expect(store.activeSubscriptions).toHaveLength(1)
    expect(store.suspendedSubscriptions).toHaveLength(1)
    expect(store.cancelledSubscriptions).toHaveLength(1)
    expect(store.expiredSubscriptions).toHaveLength(0)
    expect(store.autoRenewSubscriptions).toHaveLength(1)
  })

  it('should identify expiring soon subscriptions', () => {
    const store = useSubscriptionStore()

    const mockSubscriptions: Subscription[] = [
      {
        id: '1',
        subscriptionNumber: 'SUB-001',
        customerId: 'cust-1',
        productId: 'prod-1',
        orderId: 'order-1',
        status: 'ACTIVE' as SubscriptionStatus,
        startDate: '2025-01-01',
        endDate: '2026-12-31',
        billingStart: '2025-01-01',
        nextBillingDate: '2025-12-20',
        billingPeriod: 'monthly',
        price: 99.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 99.99,
        configuration: { feature1: true },
        autoRenew: true,
        renewalNoticeSent: false,
        createdAt: '2025-01-01T00:00:00Z',
        updatedAt: '2025-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        subscriptionNumber: 'SUB-002',
        customerId: 'cust-2',
        productId: 'prod-2',
        orderId: 'order-2',
        status: 'ACTIVE' as SubscriptionStatus,
        startDate: '2025-01-01',
        endDate: '2025-12-31',
        billingStart: '2025-01-01',
        nextBillingDate: '2025-11-10',
        billingPeriod: 'monthly',
        price: 149.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 149.99,
        configuration: { premium: true },
        autoRenew: true,
        renewalNoticeSent: false,
        createdAt: '2025-01-01T00:00:00Z',
        updatedAt: '2025-01-01T00:00:00Z',
        version: 1
      }
    ]

    store.subscriptions = mockSubscriptions

    // Subscription with nextBillingDate on 2025-11-10 should be in expiring soon (within 30 days)
    expect(store.expiringSoonSubscriptions).toHaveLength(1)
  })

  it('should reset store state', () => {
    const store = useSubscriptionStore()

    store.subscriptions = [
      {
        id: '1',
        subscriptionNumber: 'SUB-001',
        customerId: 'cust-1',
        productId: 'prod-1',
        orderId: 'order-1',
        status: 'ACTIVE' as SubscriptionStatus,
        startDate: '2024-01-01',
        endDate: '2024-12-31',
        billingStart: '2024-01-01',
        nextBillingDate: '2024-02-01',
        billingPeriod: 'monthly',
        price: 99.99,
        currency: 'PLN',
        discountAmount: 0,
        netAmount: 99.99,
        configuration: { feature1: true },
        autoRenew: true,
        renewalNoticeSent: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    store.pagination.totalElements = 1
    store.currentSubscription = {
      id: '1',
      subscriptionNumber: 'SUB-001',
      customerId: 'cust-1',
      productId: 'prod-1',
      orderId: 'order-1',
      status: 'ACTIVE' as SubscriptionStatus,
      startDate: '2024-01-01',
      endDate: '2024-12-31',
      billingStart: '2024-01-01',
      nextBillingDate: '2024-02-01',
      billingPeriod: 'monthly',
      price: 99.99,
      currency: 'PLN',
      discountAmount: 0,
      netAmount: 99.99,
      configuration: { feature1: true },
      autoRenew: true,
      renewalNoticeSent: false,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.reset()

    expect(store.subscriptions).toEqual([])
    expect(store.currentSubscription).toBeNull()
    expect(store.pagination.totalElements).toBe(0)
  })
})
