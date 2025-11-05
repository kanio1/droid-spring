import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Service,
  ServiceActivation,
  CreateServiceActivationCommand,
  ServiceActivationSearchParams,
  ServiceActivationListResponse,
  ServiceSearchParams,
  ServiceListResponse
} from '~/schemas/service'

export const useServiceStore = defineStore('service', () => {
  // State
  const services = ref<Service[]>([])
  const serviceActivations = ref<ServiceActivation[]>([])
  const currentService = ref<Service | null>(null)
  const currentActivation = ref<ServiceActivation | null>(null)
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
  const activeServices = computed(() => services.value.filter(s => s.status === 'ACTIVE'))
  const inactiveServices = computed(() => services.value.filter(s => s.status === 'INACTIVE'))
  const internetServices = computed(() => services.value.filter(s => s.category === 'INTERNET'))
  const telephonyServices = computed(() => services.value.filter(s => s.category === 'TELEPHONY'))
  const televisionServices = computed(() => services.value.filter(s => s.category === 'TELEVISION'))
  const mobileServices = computed(() => services.value.filter(s => s.category === 'MOBILE'))
  const cloudServices = computed(() => services.value.filter(s => s.category === 'CLOUD'))

  const pendingActivations = computed(() => serviceActivations.value.filter(a => a.status === 'PENDING'))
  const scheduledActivations = computed(() => serviceActivations.value.filter(a => a.status === 'SCHEDULED'))
  const provisioningActivations = computed(() => serviceActivations.value.filter(a => a.status === 'PROVISIONING'))
  const activeActivations = computed(() => serviceActivations.value.filter(a => a.status === 'ACTIVE'))
  const failedActivations = computed(() => serviceActivations.value.filter(a => a.status === 'FAILED'))
  const cancelledActivations = computed(() => serviceActivations.value.filter(a => a.status === 'CANCELLED'))

  const getActivationsByCustomer = (customerId: string) => computed(() =>
    serviceActivations.value.filter(a => a.customerId === customerId)
  )

  const getActivationsByService = (serviceCode: string) => computed(() =>
    serviceActivations.value.filter(a => a.serviceCode === serviceCode)
  )

  const getPendingActivationsByService = (serviceCode: string) => computed(() =>
    serviceActivations.value.filter(a => a.serviceCode === serviceCode && a.status === 'PENDING')
  )

  const servicesByCategory = (category: string) => computed(() =>
    services.value.filter(s => s.category === category)
  )

  const servicesByType = (type: string) => computed(() =>
    services.value.filter(s => s.serviceType === type)
  )

  // Actions
  async function fetchServices(params: Partial<ServiceSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'serviceName,asc',
        ...(params.category && { category: params.category }),
        ...(params.type && { type: params.type })
      }

      const response = await get<ServiceListResponse>('/services', { query })
      services.value = response.data.content

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
      error.value = err.message || 'Failed to fetch services'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchServiceActivations(params: Partial<ServiceActivationSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc',
        ...(params.customerId && { customerId: params.customerId }),
        ...(params.status && { status: params.status }),
        ...(params.serviceCode && { serviceCode: params.serviceCode })
      }

      const response = await get<ServiceActivationListResponse>('/services/activations', { query })
      serviceActivations.value = response.data.content

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
      error.value = err.message || 'Failed to fetch service activations'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchServiceActivationById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<ServiceActivation>(`/services/activations/${id}`)
      currentActivation.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch service activation'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createServiceActivation(data: CreateServiceActivationCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<ServiceActivation>('/services/activations', data)
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create service activation'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deactivateService(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<ServiceActivation>(`/services/activations/${id}/deactivate`, {})
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to deactivate service'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function checkEligibility(serviceCode: string, customerId: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<{ eligible: boolean; reasons: string[] }>('/services/check-eligibility', {
        serviceCode,
        customerId
      })
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to check eligibility'
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

  return {
    // State
    services,
    serviceActivations,
    currentService,
    currentActivation,
    loading,
    error,
    pagination,
    // Getters
    activeServices,
    inactiveServices,
    internetServices,
    telephonyServices,
    televisionServices,
    mobileServices,
    cloudServices,
    pendingActivations,
    scheduledActivations,
    provisioningActivations,
    activeActivations,
    failedActivations,
    cancelledActivations,
    getActivationsByCustomer,
    getActivationsByService,
    getPendingActivationsByService,
    servicesByCategory,
    servicesByType,
    // Actions
    fetchServices,
    fetchServiceActivations,
    fetchServiceActivationById,
    createServiceActivation,
    deactivateService,
    checkEligibility,
    fetchServiceById
  }
})
