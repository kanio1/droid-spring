import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Service,
  CreateServiceCommand,
  UpdateServiceCommand,
  ChangeServiceStatusCommand,
  ActivateServiceCommand,
  DeactivateServiceCommand,
  ServiceSearchParams,
  ServiceListResponse,
  ServiceStatus,
  ServiceType,
  ServiceCategory,
  ServiceTechnology,
  ServiceStatistics
} from '~/schemas/service'

export const useServiceStore = defineStore('service', () => {
  // State
  const services = ref<Service[]>([])
  const currentService = ref<Service | null>(null)
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
  const serviceCount = computed(() => services.value.length)
  const activeServices = computed(() => services.value.filter(s => s.status === 'ACTIVE'))
  const inactiveServices = computed(() => services.value.filter(s => s.status === 'INACTIVE'))
  const plannedServices = computed(() => services.value.filter(s => s.status === 'PLANNED'))
  const deprecatedServices = computed(() => services.value.filter(s => s.status === 'DEPRECATED'))
  const suspendedServices = computed(() => services.value.filter(s => s.status === 'SUSPENDED'))

  const servicesByType = computed(() => (type: ServiceType) =>
    services.value.filter(s => s.type === type)
  )

  const servicesByCategory = computed(() => (category: ServiceCategory) =>
    services.value.filter(s => s.category === category)
  )

  const servicesByTechnology = computed(() => (tech: ServiceTechnology) =>
    services.value.filter(s => s.technology === tech)
  )

  const servicesByStatus = computed(() => (status: ServiceStatus) =>
    services.value.filter(s => s.status === status)
  )

  const averagePrice = computed(() => {
    if (services.value.length === 0) return 0
    const sum = services.value.reduce((acc, s) => acc + s.price, 0)
    return Math.round(sum / services.value.length * 100) / 100
  })

  const totalRevenue = computed(() => {
    return services.value.reduce((acc, s) => acc + (s.price * s.activeCustomerCount), 0)
  })

  const popularServices = computed(() => {
    return [...services.value]
      .sort((a, b) => b.activeCustomerCount - a.activeCustomerCount)
      .slice(0, 10)
  })

  const topRevenueServices = computed(() => {
    return [...services.value]
      .sort((a, b) => (b.price * b.activeCustomerCount) - (a.price * a.activeCustomerCount))
      .slice(0, 10)
  })

  const servicesWithDataLimit = computed(() =>
    services.value.filter(s => s.dataLimit !== undefined && s.dataLimit > 0)
  )

  const servicesWithSpeed = computed(() =>
    services.value.filter(s => s.speed !== undefined && s.speed > 0)
  )

  const servicesWithVoice = computed(() =>
    services.value.filter(s => s.voiceMinutes !== undefined && s.voiceMinutes > 0)
  )

  const servicesWithSms = computed(() =>
    services.value.filter(s => s.smsCount !== undefined && s.smsCount > 0)
  )

  const unlimitedDataServices = computed(() =>
    services.value.filter(s => !s.dataLimit || s.dataLimit === 0)
  )

  const getServiceById = (id: string) => computed(() =>
    services.value.find(s => s.id === id)
  )

  // Actions
  async function fetchServices(params: Partial<ServiceSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()

      // Backend API: GET /api/services
      const response = await get<Service[]>('/services')

      services.value = response.data

      pagination.totalElements = response.data.length
      pagination.totalPages = Math.ceil(response.data.length / pagination.size)
      pagination.empty = response.data.length === 0

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch services'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchServiceById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Service>(`/services/${id}`)
      currentService.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch service'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createService(data: CreateServiceCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Service>('/services', data)

      // Add to list
      services.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create service'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateService(data: UpdateServiceCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Service>(`/services/${data.id}`, data)

      // Update in list
      const index = services.value.findIndex(s => s.id === data.id)
      if (index !== -1) {
        services.value[index] = response.data
      }

      // Update current service if it's the same
      if (currentService.value?.id === data.id) {
        currentService.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to update service'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changeServiceStatus(data: ChangeServiceStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Service>(`/services/${data.id}/status`, data)

      // Update in list
      const index = services.value.findIndex(s => s.id === data.id)
      if (index !== -1) {
        services.value[index] = response.data
      }

      // Update current service if it's the same
      if (currentService.value?.id === data.id) {
        currentService.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to change service status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteService(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { del } = useApi()
      await del(`/services/${id}`)

      // Remove from list
      const index = services.value.findIndex(s => s.id === id)
      if (index !== -1) {
        services.value.splice(index, 1)
        pagination.totalElements--
      }

      // Clear current service if it's the same
      if (currentService.value?.id === id) {
        currentService.value = null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to delete service'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function activateServiceForCustomer(data: ActivateServiceCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()

      // Backend API: POST /api/services/activations
      const response = await post('/services/activations', {
        serviceId: data.serviceId,
        customerId: data.customerId,
        startDate: data.startDate,
        activationType: data.activationType,
        notes: data.notes
      })

      // Update service customer count
      const service = services.value.find(s => s.id === data.serviceId)
      if (service) {
        service.activeCustomerCount++
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to activate service for customer'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deactivateServiceForCustomer(data: DeactivateServiceCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()

      // Backend API: POST /api/services/activations/{activationId}/deactivate
      const response = await post(`/services/activations/${data.activationId}/deactivate`, {
        reason: data.reason
      })

      // Update service customer count
      const service = services.value.find(s => s.id === data.serviceId)
      if (service) {
        service.activeCustomerCount = Math.max(0, service.activeCustomerCount - 1)
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to deactivate service for customer'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function searchServices(searchTerm: string, params: Partial<ServiceSearchParams> = {}) {
    return fetchServices({ ...params, searchTerm })
  }

  async function getServicesByStatus(status: ServiceStatus, params: Partial<ServiceSearchParams> = {}) {
    return fetchServices({ ...params, status })
  }

  async function getServicesByType(type: ServiceType, params: Partial<ServiceSearchParams> = {}) {
    return fetchServices({ ...params, type })
  }

  async function getServicesByCategory(category: ServiceCategory, params: Partial<ServiceSearchParams> = {}) {
    return fetchServices({ ...params, category })
  }

  async function getServicesByTechnology(technology: ServiceTechnology, params: Partial<ServiceSearchParams> = {}) {
    return fetchServices({ ...params, technology })
  }

  async function getServicesByPriceRange(minPrice: number, maxPrice: number, params: Partial<ServiceSearchParams> = {}) {
    return fetchServices({ ...params, minPrice, maxPrice })
  }

  async function getServiceStatistics(): Promise<ServiceStatistics> {
    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<ServiceStatistics>('/services/statistics')
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch service statistics'
      throw err
    }
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
    services.value = []
    currentService.value = null
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
    services,
    currentService,
    loading,
    error,
    pagination,

    // Getters
    serviceCount,
    activeServices,
    inactiveServices,
    plannedServices,
    deprecatedServices,
    suspendedServices,
    servicesByType,
    servicesByCategory,
    servicesByTechnology,
    servicesByStatus,
    averagePrice,
    totalRevenue,
    popularServices,
    topRevenueServices,
    servicesWithDataLimit,
    servicesWithSpeed,
    servicesWithVoice,
    servicesWithSms,
    unlimitedDataServices,
    getServiceById,

    // Actions
    fetchServices,
    fetchServiceById,
    createService,
    updateService,
    changeServiceStatus,
    deleteService,
    activateServiceForCustomer,
    deactivateServiceForCustomer,
    searchServices,
    getServicesByStatus,
    getServicesByType,
    getServicesByCategory,
    getServicesByTechnology,
    getServicesByPriceRange,
    getServiceStatistics,
    setPage,
    setSize,
    setSort,
    reset
  }
})
