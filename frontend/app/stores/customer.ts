import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Customer,
  CreateCustomerCommand,
  UpdateCustomerCommand,
  ChangeCustomerStatusCommand,
  CustomerSearchParams,
  CustomerListResponse,
  CustomerStatus
} from '~/schemas/customer'

export const useCustomerStore = defineStore('customer', () => {
  // State
  const customers = ref<Customer[]>([])
  const currentCustomer = ref<Customer | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = reactive({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: false,
    numberOfElements: 0,
    empty: true
  })

  // Getters
  const customerCount = computed(() => customers.value.length)
  const activeCustomers = computed(() => customers.value.filter(c => c.status === 'ACTIVE'))
  const inactiveCustomers = computed(() => customers.value.filter(c => c.status === 'INACTIVE'))
  const suspendedCustomers = computed(() => customers.value.filter(c => c.status === 'SUSPENDED'))
  const terminatedCustomers = computed(() => customers.value.filter(c => c.status === 'TERMINATED'))

  const getCustomerById = (id: string) => computed(() =>
    customers.value.find(c => c.id === id)
  )

  // Actions
  async function fetchCustomers(params: Partial<CustomerSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc',
        ...(params.searchTerm && { search: params.searchTerm }),
        ...(params.status && { status: params.status })
      }

      const response = await get<CustomerListResponse>('/api/v1/customers', { query })
      customers.value = response.data.content

      pagination.page = response.data.page
      pagination.size = response.data.size
      pagination.totalElements = response.data.totalElements
      pagination.totalPages = response.data.totalPages
      pagination.first = response.data.first
      pagination.last = response.data.last
      pagination.numberOfElements = response.data.numberOfElements
      pagination.empty = response.data.empty

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch customers'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchCustomerById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Customer>(`/api/v1/customers/${id}`)
      currentCustomer.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch customer'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createCustomer(data: CreateCustomerCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Customer>('/api/v1/customers', data)

      // Add to list
      customers.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create customer'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateCustomer(data: UpdateCustomerCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Customer>(`/api/v1/customers/${data.id}`, data)

      // Update in list
      const index = customers.value.findIndex(c => c.id === data.id)
      if (index !== -1) {
        customers.value[index] = response.data
      }

      // Update current customer if it's the same
      if (currentCustomer.value?.id === data.id) {
        currentCustomer.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to update customer'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changeCustomerStatus(data: ChangeCustomerStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Customer>(`/api/v1/customers/${data.id}/status`, data)

      // Update in list
      const index = customers.value.findIndex(c => c.id === data.id)
      if (index !== -1) {
        customers.value[index] = response.data
      }

      // Update current customer if it's the same
      if (currentCustomer.value?.id === data.id) {
        currentCustomer.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to change customer status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteCustomer(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { del } = useApi()
      await del(`/api/v1/customers/${id}`)

      // Remove from list
      const index = customers.value.findIndex(c => c.id === id)
      if (index !== -1) {
        customers.value.splice(index, 1)
        pagination.totalElements--
      }

      // Clear current customer if it's the same
      if (currentCustomer.value?.id === id) {
        currentCustomer.value = null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to delete customer'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function searchCustomers(searchTerm: string, params: Partial<CustomerSearchParams> = {}) {
    return fetchCustomers({ ...params, searchTerm })
  }

  async function getCustomersByStatus(status: CustomerStatus, params: Partial<CustomerSearchParams> = {}) {
    return fetchCustomers({ ...params, status })
  }

  function setPage(page: number) {
    pagination.page = page
  }

  function setSize(size: number) {
    pagination.size = size
    pagination.page = 0
  }

  function setSort(sort: string) {
    pagination.page = 0
  }

  function reset() {
    customers.value = []
    currentCustomer.value = null
    error.value = null
    pagination.page = 0
    pagination.size = 20
    pagination.totalElements = 0
    pagination.totalPages = 0
    pagination.first = true
    pagination.last = false
    pagination.numberOfElements = 0
    pagination.empty = true
  }

  return {
    // State
    customers,
    currentCustomer,
    loading,
    error,
    pagination,

    // Getters
    customerCount,
    activeCustomers,
    inactiveCustomers,
    suspendedCustomers,
    terminatedCustomers,
    getCustomerById,

    // Actions
    fetchCustomers,
    fetchCustomerById,
    createCustomer,
    updateCustomer,
    changeCustomerStatus,
    deleteCustomer,
    searchCustomers,
    getCustomersByStatus,
    setPage,
    setSize,
    setSort,
    reset
  }
})
