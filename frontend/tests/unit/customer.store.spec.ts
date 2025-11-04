import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCustomerStore } from '~/stores/customer'
import type { Customer, CustomerStatus } from '~/schemas/customer'

// Mock useApi composable
const mockUseApi = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: mockUseApi
}))

describe('Customer Store', () => {
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
    const store = useCustomerStore()

    expect(store.customers).toEqual([])
    expect(store.currentCustomer).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.pagination.page).toBe(0)
    expect(store.pagination.size).toBe(20)
    expect(store.pagination.totalElements).toBe(0)
  })

  it('should fetch customers successfully', async () => {
    const store = useCustomerStore()
    const mockCustomers: Customer[] = [
      {
        id: '1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        status: 'ACTIVE' as CustomerStatus,
        statusDisplayName: 'Active',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        firstName: 'Jane',
        lastName: 'Smith',
        email: 'jane@example.com',
        status: 'INACTIVE' as CustomerStatus,
        statusDisplayName: 'Inactive',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const getMock = vi.fn().mockResolvedValueOnce({
      data: {
        content: mockCustomers,
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 2,
        empty: false
      }
    } as any)

    mockUseApi.mockReturnValue({
      get: getMock,
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

    await store.fetchCustomers()

    expect(store.customers).toEqual(mockCustomers)
    expect(store.pagination.totalElements).toBe(2)
    expect(store.pagination.totalPages).toBe(1)
    expect(getMock).toHaveBeenCalledWith('/customers', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc'
      }
    })
  })

  it('should filter customers by status', async () => {
    const store = useCustomerStore()
    const mockCustomers: Customer[] = [
      {
        id: '1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        status: 'ACTIVE' as CustomerStatus,
        statusDisplayName: 'Active',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockCustomers,
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

    await store.getCustomersByStatus('ACTIVE')

    expect(get).toHaveBeenCalledWith('/customers', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        status: 'ACTIVE'
      }
    })
  })

  it('should create a customer', async () => {
    const store = useCustomerStore()
    const newCustomer = {
      firstName: 'Alice',
      lastName: 'Brown',
      email: 'alice@example.com',
      status: 'ACTIVE' as CustomerStatus
    }

    const mockCustomer: Customer = {
      id: '3',
      ...newCustomer,
      statusDisplayName: 'Active',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    const { post } = mockUseApi()
    vi.mocked(post).mockResolvedValueOnce({
      data: mockCustomer
    } as any)

    await store.createCustomer(newCustomer)

    expect(store.customers).toHaveLength(1)
    expect(store.customers[0]).toEqual(mockCustomer)
    expect(post).toHaveBeenCalledWith('/customers', newCustomer)
  })

  it('should update a customer', async () => {
    const store = useCustomerStore()

    const existingCustomer: Customer = {
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      status: 'ACTIVE' as CustomerStatus,
      statusDisplayName: 'Active',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.customers = [existingCustomer]

    const updateData = {
      id: '1',
      firstName: 'John',
      lastName: 'Doe Updated',
      email: 'john.updated@example.com',
      version: 1
    }

    const mockUpdatedCustomer: Customer = {
      ...existingCustomer,
      ...updateData,
      updatedAt: '2024-01-02T00:00:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedCustomer
    } as any)

    await store.updateCustomer(updateData)

    expect(store.customers[0].lastName).toBe('Doe Updated')
    expect(store.customers[0].email).toBe('john.updated@example.com')
    expect(store.customers[0].version).toBe(2)
  })

  it('should delete a customer', async () => {
    const store = useCustomerStore()

    const existingCustomer: Customer = {
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      status: 'ACTIVE' as CustomerStatus,
      statusDisplayName: 'Active',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.customers = [existingCustomer]
    store.pagination.totalElements = 1

    const { del } = mockUseApi()
    vi.mocked(del).mockResolvedValueOnce({} as any)

    await store.deleteCustomer('1')

    expect(store.customers).toHaveLength(0)
    expect(store.pagination.totalElements).toBe(0)
    expect(del).toHaveBeenCalledWith('/customers/1')
  })

  it('should change customer status', async () => {
    const store = useCustomerStore()

    const existingCustomer: Customer = {
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      status: 'ACTIVE' as CustomerStatus,
      statusDisplayName: 'Active',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.customers = [existingCustomer]

    const statusData = {
      id: '1',
      status: 'SUSPENDED' as CustomerStatus
    }

    const mockUpdatedCustomer: Customer = {
      ...existingCustomer,
      status: 'SUSPENDED' as CustomerStatus,
      statusDisplayName: 'Suspended',
      updatedAt: '2024-01-02T00:00:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedCustomer
    } as any)

    await store.changeCustomerStatus(statusData)

    expect(store.customers[0].status).toBe('SUSPENDED')
    expect(store.customers[0].version).toBe(2)
  })

  it('should reset store state', () => {
    const store = useCustomerStore()

    store.customers = [
      {
        id: '1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        status: 'ACTIVE' as CustomerStatus,
        statusDisplayName: 'Active',
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    store.pagination.totalElements = 1
    store.currentCustomer = {
      id: '1',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@example.com',
      status: 'ACTIVE' as CustomerStatus,
      statusDisplayName: 'Active',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.reset()

    expect(store.customers).toEqual([])
    expect(store.currentCustomer).toBeNull()
    expect(store.pagination.totalElements).toBe(0)
  })
})
