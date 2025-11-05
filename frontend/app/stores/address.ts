import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Address,
  CreateAddressCommand,
  UpdateAddressCommand,
  ChangeAddressStatusCommand,
  AddressSearchParams,
  AddressListResponse,
  AddressStatus,
  AddressType,
  Country
} from '~/schemas/address'

export const useAddressStore = defineStore('address', () => {
  // State
  const addresses = ref<Address[]>([])
  const currentAddress = ref<Address | null>(null)
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
  const addressCount = computed(() => addresses.value.length)
  const activeAddresses = computed(() => addresses.value.filter(a => a.status === 'ACTIVE'))
  const inactiveAddresses = computed(() => addresses.value.filter(a => a.status === 'INACTIVE'))
  const pendingAddresses = computed(() => addresses.value.filter(a => a.status === 'PENDING'))

  const billingAddresses = computed(() => addresses.value.filter(a => a.type === 'BILLING'))
  const shippingAddresses = computed(() => addresses.value.filter(a => a.type === 'SHIPPING'))
  const serviceAddresses = computed(() => addresses.value.filter(a => a.type === 'SERVICE'))
  const correspondenceAddresses = computed(() => addresses.value.filter(a => a.type === 'CORRESPONDENCE'))

  const addressesByCustomer = computed(() => (customerId: string) =>
    addresses.value.filter(a => a.customerId === customerId)
  )

  const primaryAddresses = computed(() => addresses.value.filter(a => a.isPrimary))

  const getAddressById = (id: string) => computed(() =>
    addresses.value.find(a => a.id === id)
  )

  // Actions
  async function fetchAddresses(params: Partial<AddressSearchParams> = {}) {
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
        ...(params.customerId && { customerId: params.customerId }),
        ...(params.type && { type: params.type }),
        ...(params.status && { status: params.status }),
        ...(params.country && { country: params.country })
      }

      const response = await get<AddressListResponse>('/addresses', { query })
      addresses.value = response.data.content

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
      error.value = err.message || 'Failed to fetch addresses'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchAddressById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Address>(`/addresses/${id}`)
      currentAddress.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch address'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createAddress(data: CreateAddressCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Address>('/addresses', data)

      // Add to list
      addresses.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create address'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateAddress(data: UpdateAddressCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Address>(`/addresses/${data.id}`, data)

      // Update in list
      const index = addresses.value.findIndex(a => a.id === data.id)
      if (index !== -1) {
        addresses.value[index] = response.data
      }

      // Update current address if it's the same
      if (currentAddress.value?.id === data.id) {
        currentAddress.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to update address'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changeAddressStatus(data: ChangeAddressStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Address>(`/addresses/${data.id}/status`, data)

      // Update in list
      const index = addresses.value.findIndex(a => a.id === data.id)
      if (index !== -1) {
        addresses.value[index] = response.data
      }

      // Update current address if it's the same
      if (currentAddress.value?.id === data.id) {
        currentAddress.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to change address status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteAddress(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { del } = useApi()
      await del(`/addresses/${id}`)

      // Remove from list
      const index = addresses.value.findIndex(a => a.id === id)
      if (index !== -1) {
        addresses.value.splice(index, 1)
        pagination.totalElements--
      }

      // Clear current address if it's the same
      if (currentAddress.value?.id === id) {
        currentAddress.value = null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to delete address'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function searchAddresses(searchTerm: string, params: Partial<AddressSearchParams> = {}) {
    return fetchAddresses({ ...params, searchTerm })
  }

  async function getAddressesByCustomer(customerId: string, params: Partial<AddressSearchParams> = {}) {
    return fetchAddresses({ ...params, customerId })
  }

  async function getAddressesByStatus(status: AddressStatus, params: Partial<AddressSearchParams> = {}) {
    return fetchAddresses({ ...params, status })
  }

  async function getAddressesByType(type: AddressType, params: Partial<AddressSearchParams> = {}) {
    return fetchAddresses({ ...params, type })
  }

  async function getAddressesByCountry(country: Country, params: Partial<AddressSearchParams> = {}) {
    return fetchAddresses({ ...params, country })
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
    addresses.value = []
    currentAddress.value = null
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
    addresses,
    currentAddress,
    loading,
    error,
    pagination,

    // Getters
    addressCount,
    activeAddresses,
    inactiveAddresses,
    pendingAddresses,
    billingAddresses,
    shippingAddresses,
    serviceAddresses,
    correspondenceAddresses,
    addressesByCustomer,
    primaryAddresses,
    getAddressById,

    // Actions
    fetchAddresses,
    fetchAddressById,
    createAddress,
    updateAddress,
    changeAddressStatus,
    deleteAddress,
    searchAddresses,
    getAddressesByCustomer,
    getAddressesByStatus,
    getAddressesByType,
    getAddressesByCountry,
    setPage,
    setSize,
    setSort,
    reset
  }
})
