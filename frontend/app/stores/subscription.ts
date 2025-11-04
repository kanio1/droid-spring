import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Subscription,
  CreateSubscriptionCommand,
  UpdateSubscriptionCommand,
  ChangeSubscriptionStatusCommand,
  SubscriptionSearchParams,
  SubscriptionListResponse,
  SubscriptionStatus
} from '~/schemas/subscription'

export const useSubscriptionStore = defineStore('subscription', () => {
  // State
  const subscriptions = ref<Subscription[]>([])
  const currentSubscription = ref<Subscription | null>(null)
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
  const subscriptionCount = computed(() => subscriptions.value.length)
  const activeSubscriptions = computed(() => subscriptions.value.filter(s => s.status === 'ACTIVE'))
  const suspendedSubscriptions = computed(() => subscriptions.value.filter(s => s.status === 'SUSPENDED'))
  const cancelledSubscriptions = computed(() => subscriptions.value.filter(s => s.status === 'CANCELLED'))
  const expiredSubscriptions = computed(() => subscriptions.value.filter(s => s.status === 'EXPIRED'))

  const autoRenewSubscriptions = computed(() => subscriptions.value.filter(s => s.autoRenew))
  const expiringSoonSubscriptions = computed(() => {
    const today = new Date()
    const thirtyDaysFromNow = new Date(today.getTime() + 30 * 24 * 60 * 60 * 1000)
    return subscriptions.value.filter(s => {
      if (!s.nextBillingDate) return false
      const nextBilling = new Date(s.nextBillingDate)
      return nextBilling <= thirtyDaysFromNow && nextBilling >= today
    })
  })

  const getSubscriptionById = (id: string) => computed(() =>
    subscriptions.value.find(s => s.id === id)
  )

  const getSubscriptionsByProduct = (productId: string) => computed(() =>
    subscriptions.value.filter(s => s.productId === productId)
  )

  // Actions
  async function fetchSubscriptions(params: Partial<SubscriptionSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc',
        ...(params.searchTerm && { searchTerm: params.searchTerm }),
        ...(params.status && { status: params.status }),
        ...(params.customerId && { customerId: params.customerId }),
        ...(params.productId && { productId: params.productId })
      }

      const response = await get<SubscriptionListResponse>('/subscriptions', { query })
      subscriptions.value = response.data.content

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
      error.value = err.message || 'Failed to fetch subscriptions'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchSubscriptionById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Subscription>(`/subscriptions/${id}`)
      currentSubscription.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createSubscription(data: CreateSubscriptionCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Subscription>('/subscriptions', data)

      subscriptions.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateSubscription(data: UpdateSubscriptionCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Subscription>(`/subscriptions/${data.id}`, data)

      const index = subscriptions.value.findIndex(s => s.id === data.id)
      if (index !== -1) {
        subscriptions.value[index] = response.data
      }

      if (currentSubscription.value?.id === data.id) {
        currentSubscription.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to update subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changeSubscriptionStatus(data: ChangeSubscriptionStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Subscription>(`/subscriptions/${data.id}/status`, data)

      const index = subscriptions.value.findIndex(s => s.id === data.id)
      if (index !== -1) {
        subscriptions.value[index] = response.data
      }

      if (currentSubscription.value?.id === data.id) {
        currentSubscription.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to change subscription status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function renewSubscription(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Subscription>(`/subscriptions/${id}/renew`, {})

      const index = subscriptions.value.findIndex(s => s.id === id)
      if (index !== -1) {
        subscriptions.value[index] = response.data
      }

      if (currentSubscription.value?.id === id) {
        currentSubscription.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to renew subscription'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getSubscriptionsByCustomer(customerId: string, params: Partial<SubscriptionSearchParams> = {}) {
    return fetchSubscriptions({ ...params, customerId })
  }

  async function getSubscriptionsByStatus(status: SubscriptionStatus, params: Partial<SubscriptionSearchParams> = {}) {
    return fetchSubscriptions({ ...params, status })
  }

  async function searchSubscriptions(searchTerm: string, params: Partial<SubscriptionSearchParams> = {}) {
    return fetchSubscriptions({ ...params, searchTerm })
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
    subscriptions.value = []
    currentSubscription.value = null
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
    subscriptions,
    currentSubscription,
    loading,
    error,
    pagination,

    // Getters
    subscriptionCount,
    activeSubscriptions,
    suspendedSubscriptions,
    cancelledSubscriptions,
    expiredSubscriptions,
    autoRenewSubscriptions,
    expiringSoonSubscriptions,
    getSubscriptionById,
    getSubscriptionsByProduct,

    // Actions
    fetchSubscriptions,
    fetchSubscriptionById,
    createSubscription,
    updateSubscription,
    changeSubscriptionStatus,
    renewSubscription,
    getSubscriptionsByCustomer,
    getSubscriptionsByStatus,
    searchSubscriptions,
    setPage,
    setSize,
    setSort,
    reset
  }
})
