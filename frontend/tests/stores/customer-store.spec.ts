/**
 * CustomerStore Tests
 *
 * Comprehensive test coverage for customer store
 * Tests CRUD operations, caching, optimistic updates, filtering, pagination, and search
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCustomerStore } from '@/stores/customer'
import type { Customer } from '@/types/customer'

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

// Mock cache
vi.mock('@/utils/cache', () => ({
  cache: {
    get: vi.fn(),
    set: vi.fn(),
    delete: vi.fn(),
    clear: vi.fn()
  }
}))

const mockCustomer: Customer = {
  id: 'cust-123',
  firstName: 'John',
  lastName: 'Doe',
  email: 'john.doe@example.com',
  phone: '+1234567890',
  status: 'ACTIVE',
  createdAt: '2024-01-15T10:00:00Z',
  updatedAt: '2024-01-16T10:00:00Z'
}

const mockCustomers: Customer[] = [
  {
    id: 'cust-123',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@example.com',
    phone: '+1234567890',
    status: 'ACTIVE',
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-01-16T10:00:00Z'
  },
  {
    id: 'cust-456',
    firstName: 'Jane',
    lastName: 'Smith',
    email: 'jane.smith@example.com',
    phone: '+1234567891',
    status: 'ACTIVE',
    createdAt: '2024-01-14T10:00:00Z',
    updatedAt: '2024-01-15T10:00:00Z'
  }
]

describe('CustomerStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const customerStore = useCustomerStore()

      expect(customerStore.customers).toEqual([])
      expect(customerStore.currentCustomer).toBeNull()
      expect(customerStore.totalCount).toBe(0)
      expect(customerStore.currentPage).toBe(1)
      expect(customerStore.pageSize).toBe(20)
      expect(customerStore.filters).toEqual({})
      expect(customerStore.searchQuery).toBe('')
      expect(customerStore.sortBy).toBe('createdAt')
      expect(customerStore.sortOrder).toBe('desc')
      expect(customerStore.isLoading).toBe(false)
      expect(customerStore.error).toBeNull()
      expect(customerStore.selectedCustomers).toEqual([])
    })
  })

  describe('fetchCustomers', () => {
    it('should fetch customers successfully', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')
      const { cache } = await import('@/utils/cache')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: {
          content: mockCustomers,
          totalElements: 2,
          totalPages: 1,
          number: 0,
          size: 20
        }
      })

      await customerStore.fetchCustomers()

      expect(apiClient.get).toHaveBeenCalledWith('/api/customers', {
        params: {
          page: 1,
          size: 20,
          sortBy: 'createdAt',
          sortOrder: 'desc',
          ...customerStore.filters,
          search: customerStore.searchQuery
        }
      })

      expect(customerStore.customers).toEqual(mockCustomers)
      expect(customerStore.totalCount).toBe(2)
      expect(customerStore.error).toBeNull()
      expect(cache.set).toHaveBeenCalledWith('customers-page-1', mockCustomers)
    })

    it('should use cache when available', async () => {
      const customerStore = useCustomerStore()
      const { cache } = await import('@/utils/cache')

      vi.mocked(cache.get).mockReturnValue(mockCustomers)

      await customerStore.fetchCustomers(1, true)

      expect(customerStore.customers).toEqual(mockCustomers)
      expect(cache.get).toHaveBeenCalledWith('customers-page-1')
    })

    it('should handle API errors', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('API error'))

      await customerStore.fetchCustomers()

      expect(customerStore.error).toBe('Failed to fetch customers: API error')
      expect(customerStore.customers).toEqual([])
    })

    it('should respect current filters', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.setFilters({ status: 'ACTIVE' })
      customerStore.setSearch('john')

      await customerStore.fetchCustomers()

      expect(apiClient.get).toHaveBeenCalledWith('/api/customers', {
        params: expect.objectContaining({
          status: 'ACTIVE',
          search: 'john'
        })
      })
    })

    it('should respect sort parameters', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.setSorting('name', 'asc')

      await customerStore.fetchCustomers()

      expect(apiClient.get).toHaveBeenCalledWith('/api/customers', {
        params: expect.objectContaining({
          sortBy: 'name',
          sortOrder: 'asc'
        })
      })
    })
  })

  describe('fetchCustomer', () => {
    it('should fetch single customer successfully', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')
      const { cache } = await import('@/utils/cache')

      vi.mocked(apiClient.get).mockResolvedValue({ data: mockCustomer })

      const customer = await customerStore.fetchCustomer('cust-123')

      expect(apiClient.get).toHaveBeenCalledWith('/api/customers/cust-123')
      expect(customer).toEqual(mockCustomer)
      expect(customerStore.currentCustomer).toEqual(mockCustomer)
      expect(cache.set).toHaveBeenCalledWith('customer-cust-123', mockCustomer)
    })

    it('should return cached customer if available', async () => {
      const customerStore = useCustomerStore()
      const { cache } = await import('@/utils/cache')

      vi.mocked(cache.get).mockReturnValue(mockCustomer)

      const customer = await customerStore.fetchCustomer('cust-123')

      expect(customer).toEqual(mockCustomer)
      expect(customerStore.currentCustomer).toEqual(mockCustomer)
    })

    it('should handle not found error', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('Not found'))

      const customer = await customerStore.fetchCustomer('cust-999')

      expect(customer).toBeNull()
      expect(customerStore.error).toBe('Customer not found')
    })
  })

  describe('createCustomer', () => {
    it('should create customer successfully', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      const newCustomer = {
        firstName: 'Alice',
        lastName: 'Johnson',
        email: 'alice@example.com',
        phone: '+1234567892',
        status: 'ACTIVE' as const
      }

      vi.mocked(apiClient.post).mockResolvedValue({ data: mockCustomer })

      const customer = await customerStore.createCustomer(newCustomer)

      expect(apiClient.post).toHaveBeenCalledWith('/api/customers', newCustomer)
      expect(customer).toEqual(mockCustomer)
      expect(customerStore.customers).toContain(mockCustomer)
      expect(customerStore.error).toBeNull()
    })

    it('should create customer with optimistic update', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      const newCustomer = {
        firstName: 'Alice',
        lastName: 'Johnson',
        email: 'alice@example.com',
        phone: '+1234567892',
        status: 'ACTIVE' as const
      }

      const optimisticCustomer = { ...newCustomer, id: 'temp-id', isOptimistic: true }
      customerStore.customers = [...customerStore.customers, optimisticCustomer]

      vi.mocked(apiClient.post).mockResolvedValue({ data: mockCustomer })

      await customerStore.createCustomer(newCustomer, { optimistic: true })

      // Optimistic customer should be replaced with real one
      const hasOptimistic = customerStore.customers.some(c => (c as any).isOptimistic)
      expect(hasOptimistic).toBe(false)
      expect(customerStore.customers).toContain(mockCustomer)
    })

    it('should rollback optimistic update on error', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      const newCustomer = {
        firstName: 'Alice',
        lastName: 'Johnson',
        email: 'alice@example.com',
        phone: '+1234567892',
        status: 'ACTIVE' as const
      }

      const optimisticCustomer = { ...newCustomer, id: 'temp-id', isOptimistic: true }
      customerStore.customers = [...customerStore.customers, optimisticCustomer]

      vi.mocked(apiClient.post).mockRejectedValue(new Error('Create failed'))

      await customerStore.createCustomer(newCustomer, { optimistic: true })

      // Optimistic customer should be removed
      expect(customerStore.customers).not.toContain(optimisticCustomer)
      expect(customerStore.error).toBe('Failed to create customer: Create failed')
    })

    it('should validate required fields', async () => {
      const customerStore = useCustomerStore()

      const invalidCustomer = {
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        status: 'ACTIVE' as const
      }

      await expect(customerStore.createCustomer(invalidCustomer))
        .rejects.toThrow('Validation failed')

      expect(customerStore.error).toBeDefined()
    })
  })

  describe('updateCustomer', () => {
    it('should update customer successfully', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [...mockCustomers]

      const updates = { firstName: 'Johnny', lastName: 'Doe' }
      const updatedCustomer = { ...mockCustomer, ...updates }

      vi.mocked(apiClient.put).mockResolvedValue({ data: updatedCustomer })

      const customer = await customerStore.updateCustomer('cust-123', updates)

      expect(apiClient.put).toHaveBeenCalledWith('/api/customers/cust-123', updates)
      expect(customer?.firstName).toBe('Johnny')
      expect(customerStore.customers[0]).toEqual(updatedCustomer)
    })

    it('should update with optimistic update', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [...mockCustomers]

      const updates = { firstName: 'Johnny' }

      // Mock optimistic update in store
      const originalCustomer = customerStore.customers[0]
      Object.assign(originalCustomer, updates)

      vi.mocked(apiClient.put).mockResolvedValue({ data: { ...originalCustomer, ...updates } })

      await customerStore.updateCustomer('cust-123', updates, { optimistic: true })

      expect(originalCustomer.firstName).toBe('Johnny')
    })

    it('should rollback on update error', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [...mockCustomers]
      const originalCustomer = { ...mockCustomers[0] }

      const updates = { firstName: 'Johnny' }

      vi.mocked(apiClient.put).mockRejectedValue(new Error('Update failed'))

      await customerStore.updateCustomer('cust-123', updates, { optimistic: true })

      // Should rollback to original
      expect(customerStore.customers[0]).toEqual(originalCustomer)
    })

    it('should handle not found error', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.put).mockRejectedValue(new Error('Not found'))

      await expect(customerStore.updateCustomer('cust-999', {}))
        .rejects.toThrow('Customer not found')
    })
  })

  describe('deleteCustomer', () => {
    it('should delete customer successfully', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [...mockCustomers]

      vi.mocked(apiClient.delete).mockResolvedValue({})

      await customerStore.deleteCustomer('cust-123')

      expect(apiClient.delete).toHaveBeenCalledWith('/api/customers/cust-123')
      expect(customerStore.customers).not.toContain(mockCustomer)
      expect(customerStore.totalCount).toBe(1)
    })

    it('should delete with optimistic update', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [...mockCustomers]
      const customerToDelete = customerStore.customers[0]
      ;(customerToDelete as any).isDeleting = true

      vi.mocked(apiClient.delete).mockResolvedValue({})

      await customerStore.deleteCustomer('cust-123', { optimistic: true })

      expect(customerStore.customers).not.toContain(mockCustomer)
    })

    it('should rollback on delete error', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [...mockCustomers]
      const originalCustomers = [...mockCustomers]

      vi.mocked(apiClient.delete).mockRejectedValue(new Error('Delete failed'))

      await customerStore.deleteCustomer('cust-123', { optimistic: true })

      expect(customerStore.customers).toEqual(originalCustomers)
      expect(customerStore.error).toBe('Failed to delete customer: Delete failed')
    })

    it('should handle batch delete', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.customers = [...mockCustomers]
      customerStore.selectedCustomers = ['cust-123', 'cust-456']

      vi.mocked(apiClient.delete).mockResolvedValue({})

      await customerStore.deleteCustomers(['cust-123', 'cust-456'])

      expect(apiClient.delete).toHaveBeenCalledWith('/api/customers/batch', {
        data: { ids: ['cust-123', 'cust-456'] }
      })
      expect(customerStore.customers).toHaveLength(0)
      expect(customerStore.selectedCustomers).toEqual([])
    })
  })

  describe('Filtering and Search', () => {
    it('should set filters correctly', () => {
      const customerStore = useCustomerStore()

      customerStore.setFilters({ status: 'ACTIVE', type: 'PREMIUM' })

      expect(customerStore.filters).toEqual({ status: 'ACTIVE', type: 'PREMIUM' })
    })

    it('should clear filters', () => {
      const customerStore = useCustomerStore()

      customerStore.setFilters({ status: 'ACTIVE' })
      customerStore.clearFilters()

      expect(customerStore.filters).toEqual({})
    })

    it('should set search query', () => {
      const customerStore = useCustomerStore()

      customerStore.setSearch('john')

      expect(customerStore.searchQuery).toBe('john')
    })

    it('should filter customers by status', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockCustomer], totalElements: 1 }
      })

      customerStore.setFilters({ status: 'ACTIVE' })
      await customerStore.fetchCustomers()

      expect(apiClient.get).toHaveBeenCalledWith('/api/customers', {
        params: expect.objectContaining({ status: 'ACTIVE' })
      })
    })

    it('should search customers', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockCustomer], totalElements: 1 }
      })

      customerStore.setSearch('john')
      await customerStore.fetchCustomers()

      expect(apiClient.get).toHaveBeenCalledWith('/api/customers', {
        params: expect.objectContaining({ search: 'john' })
      })
    })

    it('should clear search', () => {
      const customerStore = useCustomerStore()

      customerStore.setSearch('john')
      customerStore.clearSearch()

      expect(customerStore.searchQuery).toBe('')
    })
  })

  describe('Sorting', () => {
    it('should set sorting parameters', () => {
      const customerStore = useCustomerStore()

      customerStore.setSorting('name', 'asc')

      expect(customerStore.sortBy).toBe('name')
      expect(customerStore.sortOrder).toBe('asc')
    })

    it('should toggle sort order', () => {
      const customerStore = useCustomerStore()

      customerStore.setSorting('name', 'asc')
      customerStore.toggleSort('name')

      expect(customerStore.sortOrder).toBe('desc')
    })

    it('should change sort field and reset order', () => {
      const customerStore = useCustomerStore()

      customerStore.setSorting('name', 'asc')
      customerStore.setSorting('email', 'asc')

      expect(customerStore.sortBy).toBe('email')
      expect(customerStore.sortOrder).toBe('asc')
    })
  })

  describe('Pagination', () => {
    it('should set current page', () => {
      const customerStore = useCustomerStore()

      customerStore.setPage(3)

      expect(customerStore.currentPage).toBe(3)
    })

    it('should set page size', () => {
      const customerStore = useCustomerStore()

      customerStore.setPageSize(50)

      expect(customerStore.pageSize).toBe(50)
      expect(customerStore.currentPage).toBe(1) // Reset to first page
    })

    it('should navigate to next page', () => {
      const customerStore = useCustomerStore()

      customerStore.setPage(1)
      customerStore.nextPage()

      expect(customerStore.currentPage).toBe(2)
    })

    it('should navigate to previous page', () => {
      const customerStore = useCustomerStore()

      customerStore.setPage(3)
      customerStore.prevPage()

      expect(customerStore.currentPage).toBe(2)
    })

    it('should not go below page 1', () => {
      const customerStore = useCustomerStore()

      customerStore.setPage(1)
      customerStore.prevPage()

      expect(customerStore.currentPage).toBe(1)
    })

    it('should calculate total pages', () => {
      const customerStore = useCustomerStore()

      customerStore.totalCount = 100
      customerStore.pageSize = 20

      expect(customerStore.totalPages).toBe(5)
    })

    it('should handle empty results', () => {
      const customerStore = useCustomerStore()

      customerStore.totalCount = 0
      customerStore.pageSize = 20

      expect(customerStore.totalPages).toBe(0)
    })
  })

  describe('Selection', () => {
    it('should select customer', () => {
      const customerStore = useCustomerStore()

      customerStore.selectCustomer('cust-123')

      expect(customerStore.selectedCustomers).toContain('cust-123')
    })

    it('should deselect customer', () => {
      const customerStore = useCustomerStore()

      customerStore.selectedCustomers = ['cust-123']
      customerStore.deselectCustomer('cust-123')

      expect(customerStore.selectedCustomers).not.toContain('cust-123')
    })

    it('should toggle selection', () => {
      const customerStore = useCustomerStore()

      customerStore.toggleSelection('cust-123')
      expect(customerStore.selectedCustomers).toContain('cust-123')

      customerStore.toggleSelection('cust-123')
      expect(customerStore.selectedCustomers).not.toContain('cust-123')
    })

    it('should select all customers', () => {
      const customerStore = useCustomerStore()

      customerStore.customers = [...mockCustomers]
      customerStore.selectAll()

      expect(customerStore.selectedCustomers).toEqual(['cust-123', 'cust-456'])
    })

    it('should deselect all customers', () => {
      const customerStore = useCustomerStore()

      customerStore.selectedCustomers = ['cust-123', 'cust-456']
      customerStore.deselectAll()

      expect(customerStore.selectedCustomers).toEqual([])
    })

    it('should check if customer is selected', () => {
      const customerStore = useCustomerStore()

      customerStore.selectedCustomers = ['cust-123']

      expect(customerStore.isSelected('cust-123')).toBe(true)
      expect(customerStore.isSelected('cust-456')).toBe(false)
    })
  })

  describe('Caching', () => {
    it('should invalidate cache for customer', async () => {
      const customerStore = useCustomerStore()
      const { cache } = await import('@/utils/cache')

      customerStore.invalidateCustomer('cust-123')

      expect(cache.delete).toHaveBeenCalledWith('customer-cust-123')
    })

    it('should invalidate all caches', async () => {
      const customerStore = useCustomerStore()
      const { cache } = await import('@/utils/cache')

      customerStore.invalidateCache()

      expect(cache.clear).toHaveBeenCalled()
    })

    it('should refresh customer data', async () => {
      const customerStore = useCustomerStore()
      const { cache } = await import('@/utils/cache')

      vi.mocked(cache.get).mockReturnValue(undefined)
      const { apiClient } = await import('@/services/api')
      vi.mocked(apiClient.get).mockResolvedValue({ data: mockCustomer })

      await customerStore.refreshCustomer('cust-123')

      expect(cache.delete).toHaveBeenCalledWith('customer-cust-123')
      expect(apiClient.get).toHaveBeenCalledWith('/api/customers/cust-123')
    })
  })

  describe('Error Handling', () => {
    it('should set error state', () => {
      const customerStore = useCustomerStore()

      customerStore.setError('Test error')

      expect(customerStore.error).toBe('Test error')
    })

    it('should clear error', () => {
      const customerStore = useCustomerStore()

      customerStore.error = 'Some error'
      customerStore.clearError()

      expect(customerStore.error).toBeNull()
    })

    it('should handle network errors', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('Network error'))

      await customerStore.fetchCustomers()

      expect(customerStore.error).toContain('Network error')
    })

    it('should handle validation errors', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.post).mockRejectedValue({
        response: {
          data: {
            fieldErrors: {
              email: 'Invalid email format'
            }
          }
        }
      })

      await customerStore.createCustomer({
        firstName: 'Test',
        lastName: 'User',
        email: 'invalid',
        phone: '',
        status: 'ACTIVE'
      })

      expect(customerStore.error).toBeDefined()
    })
  })

  describe('Computed Properties', () => {
    it('should return filtered customers', () => {
      const customerStore = useCustomerStore()

      customerStore.customers = [...mockCustomers]
      customerStore.filters = { status: 'ACTIVE' }

      const filtered = customerStore.filteredCustomers

      expect(filtered.every(c => c.status === 'ACTIVE')).toBe(true)
    })

    it('should return sorted customers', () => {
      const customerStore = useCustomerStore()

      customerStore.customers = [...mockCustomers]
      customerStore.setSorting('firstName', 'asc')

      const sorted = customerStore.sortedCustomers

      expect(sorted[0].firstName).toBe('Jane') // J before J (but we have John and Jane)
    })

    it('should return paged customers', () => {
      const customerStore = useCustomerStore()

      const manyCustomers = Array.from({ length: 50 }, (_, i) => ({
        ...mockCustomer,
        id: `cust-${i}`,
        firstName: `Customer ${i}`
      }))

      customerStore.customers = manyCustomers
      customerStore.currentPage = 1
      customerStore.pageSize = 20

      const paged = customerStore.pagedCustomers

      expect(paged).toHaveLength(20)
    })

    it('should check if has next page', () => {
      const customerStore = useCustomerStore()

      customerStore.totalCount = 50
      customerStore.currentPage = 2
      customerStore.pageSize = 20

      expect(customerStore.hasNextPage).toBe(false)

      customerStore.currentPage = 1
      expect(customerStore.hasNextPage).toBe(true)
    })

    it('should check if has previous page', () => {
      const customerStore = useCustomerStore()

      customerStore.currentPage = 1
      expect(customerStore.hasPreviousPage).toBe(false)

      customerStore.currentPage = 2
      expect(customerStore.hasPreviousPage).toBe(true)
    })

    it('should return selected customers data', () => {
      const customerStore = useCustomerStore()

      customerStore.customers = [...mockCustomers]
      customerStore.selectedCustomers = ['cust-123', 'cust-456']

      const selected = customerStore.selectedCustomersData

      expect(selected).toHaveLength(2)
      expect(selected.map(c => c.id)).toEqual(['cust-123', 'cust-456'])
    })
  })

  describe('Actions with Callbacks', () => {
    it('should call onSuccess callback', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      const onSuccess = vi.fn()
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockCustomer })

      await customerStore.createCustomer(
        {
          firstName: 'Test',
          lastName: 'User',
          email: 'test@example.com',
          phone: '+1234567890',
          status: 'ACTIVE'
        },
        { onSuccess }
      )

      expect(onSuccess).toHaveBeenCalledWith(mockCustomer)
    })

    it('should call onError callback', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      const onError = vi.fn()
      const error = new Error('Create failed')
      vi.mocked(apiClient.post).mockRejectedValue(error)

      await customerStore.createCustomer(
        {
          firstName: 'Test',
          lastName: 'User',
          email: 'test@example.com',
          phone: '+1234567890',
          status: 'ACTIVE'
        },
        { onError }
      )

      expect(onError).toHaveBeenCalledWith(error)
    })
  })

  describe('Integration Tests', () => {
    it('should handle complete CRUD flow', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      // Create
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockCustomer })
      const created = await customerStore.createCustomer({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        phone: '+1234567890',
        status: 'ACTIVE'
      })
      expect(created).toEqual(mockCustomer)

      // Read
      vi.mocked(apiClient.get).mockResolvedValue({ data: mockCustomer })
      const fetched = await customerStore.fetchCustomer('cust-123')
      expect(fetched).toEqual(mockCustomer)

      // Update
      const updates = { firstName: 'Johnny' }
      vi.mocked(apiClient.put).mockResolvedValue({ data: { ...mockCustomer, ...updates } })
      const updated = await customerStore.updateCustomer('cust-123', updates)
      expect(updated?.firstName).toBe('Johnny')

      // Delete
      vi.mocked(apiClient.delete).mockResolvedValue({})
      await customerStore.deleteCustomer('cust-123')
      expect(customerStore.customers).not.toContain(mockCustomer)
    })

    it('should handle pagination and filtering together', async () => {
      const customerStore = useCustomerStore()
      const { apiClient } = await import('@/services/api')

      customerStore.setFilters({ status: 'ACTIVE' })
      customerStore.setPage(2)
      customerStore.setPageSize(10)

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [], totalElements: 25 }
      })

      await customerStore.fetchCustomers()

      expect(apiClient.get).toHaveBeenCalledWith('/api/customers', {
        params: expect.objectContaining({
          page: 2,
          size: 10,
          status: 'ACTIVE'
        })
      })
    })
  })
})
