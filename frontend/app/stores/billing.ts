import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  UsageRecord,
  CreateUsageRecordCommand,
  BillingCycle,
  CreateBillingCycleCommand,
  BillingCycleSearchParams,
  BillingCycleListResponse,
  UsageRecordSearchParams,
  UsageRecordListResponse
} from '~/schemas/billing'

export const useBillingStore = defineStore('billing', () => {
  // State
  const usageRecords = ref<UsageRecord[]>([])
  const billingCycles = ref<BillingCycle[]>([])
  const currentBillingCycle = ref<BillingCycle | null>(null)
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
  const unratedUsageRecords = computed(() => usageRecords.value.filter(u => !u.isRated))
  const ratedUsageRecords = computed(() => usageRecords.value.filter(u => u.isRated))
  const pendingBillingCycles = computed(() => billingCycles.value.filter(b => b.status === 'PENDING'))
  const processingBillingCycles = computed(() => billingCycles.value.filter(b => b.status === 'PROCESSING'))
  const completedBillingCycles = computed(() => billingCycles.value.filter(b => b.status === 'COMPLETED'))

  const currentCycle = computed(() => billingCycles.value.find(b => b.status === 'PROCESSING') || null)
  const nextCycle = computed(() => {
    const sorted = billingCycles.value
      .filter(b => b.status === 'PENDING')
      .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())
    return sorted[0] || null
  })

  const totalUnratedUsage = computed(() =>
    unratedUsageRecords.value.reduce((sum, u) => sum + u.usageAmount, 0)
  )

  const getUsageRecordsByCustomer = (customerId: string) => computed(() =>
    usageRecords.value.filter(u => u.customerId === customerId)
  )

  const getBillingCyclesByCustomer = (customerId: string) => computed(() =>
    billingCycles.value.filter(b => b.customerId === customerId)
  )

  // Actions
  async function fetchUsageRecords(params: Partial<UsageRecordSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()

      // Backend API: GET /api/billing/usage-records
      const response = await get<UsageRecord[]>('/billing/usage-records')
      usageRecords.value = response.data

      pagination.totalElements = response.data.length
      pagination.totalPages = Math.ceil(response.data.length / pagination.size)
      pagination.empty = response.data.length === 0

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch usage records'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchBillingCycles(params: Partial<BillingCycleSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()

      // Backend API: GET /api/billing/cycles
      const response = await get<BillingCycle[]>('/billing/cycles')
      billingCycles.value = response.data

      pagination.totalElements = response.data.length
      pagination.totalPages = Math.ceil(response.data.length / pagination.size)
      pagination.empty = response.data.length === 0

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch billing cycles'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchBillingCycleById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<BillingCycle>(`/billing/cycles/${id}`)
      currentBillingCycle.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch billing cycle'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function ingestUsageRecord(data: CreateUsageRecordCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<UsageRecord>('/billing/usage-records', data)
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to ingest usage record'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function startBillingCycle(data: CreateBillingCycleCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<BillingCycle>('/billing/cycles', data)
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to start billing cycle'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function processBillingCycle(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<BillingCycle>(`/billing/cycles/${id}/process`, {})
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to process billing cycle'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    usageRecords,
    billingCycles,
    currentBillingCycle,
    loading,
    error,
    pagination,
    // Getters
    unratedUsageRecords,
    ratedUsageRecords,
    pendingBillingCycles,
    processingBillingCycles,
    completedBillingCycles,
    currentCycle,
    nextCycle,
    totalUnratedUsage,
    getUsageRecordsByCustomer,
    getBillingCyclesByCustomer,
    // Actions
    fetchUsageRecords,
    fetchBillingCycles,
    fetchBillingCycleById,
    ingestUsageRecord,
    startBillingCycle,
    processBillingCycle
  }
})
