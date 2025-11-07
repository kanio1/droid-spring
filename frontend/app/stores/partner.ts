import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Partner,
  CreatePartnerCommand,
  UpdatePartnerCommand,
  SuspendPartnerCommand,
  TerminatePartnerCommand,
  PartnerSearchParams,
  PartnerSummary,
  PartnerStatistics,
  PartnerSettlement,
  PartnerType,
  PartnerStatus
} from '~/schemas/partner'

export const usePartnerStore = defineStore('partner', () => {
  // State
  const partners = ref<Partner[]>([])
  const currentPartner = ref<Partner | null>(null)
  const settlements = ref<PartnerSettlement[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const partnerCount = computed(() => partners.value.length)
  const activePartners = computed(() => partners.value.filter(p => p.status === 'ACTIVE'))
  const suspendedPartners = computed(() => partners.value.filter(p => p.status === 'SUSPENDED'))
  const terminatedPartners = computed(() => partners.value.filter(p => p.status === 'TERMINATED'))
  const pendingPartners = computed(() => partners.value.filter(p => p.status === 'PENDING_APPROVAL'))
  const onHoldPartners = computed(() => partners.value.filter(p => p.status === 'ON_HOLD'))

  const resellers = computed(() => partners.value.filter(p => p.partnerType === 'RESELLER'))
  const distributors = computed(() => partners.value.filter(p => p.partnerType === 'DISTRIBUTOR'))
  const mvnos = computed(() => partners.value.filter(p => p.partnerType === 'MVNO'))

  const totalSales = computed(() =>
    partners.value.reduce((sum, p) => sum + p.totalSales, 0)
  )

  const totalCommission = computed(() =>
    partners.value.reduce((sum, p) => sum + p.totalCommission, 0)
  )

  const getPartnerById = (id: string) => computed(() =>
    partners.value.find(p => p.id === id)
  )

  const getPartnerByCode = (code: string) => computed(() =>
    partners.value.find(p => p.partnerCode === code)
  )

  // Actions
  async function fetchPartners(params: Partial<PartnerSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()

      const query: any = {}
      if (params.type) {
        query.type = params.type
      }
      if (params.status) {
        query.status = params.status
      }

      const partnersList = await get<Partner[]>('/api/partners', { params: query })

      // Filter by search term if provided (client-side filtering for now)
      if (params.searchTerm) {
        const term = params.searchTerm.toLowerCase()
        partners.value = partnersList.filter(p =>
          p.name.toLowerCase().includes(term) ||
          p.partnerCode.toLowerCase().includes(term) ||
          p.email?.toLowerCase().includes(term)
        )
      } else {
        partners.value = partnersList
      }

      return partners.value
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch partners'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function searchPartners(query: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const partnersList = await get<Partner[]>('/api/partners/search', {
        params: { q: query }
      })

      partners.value = partnersList
      return partnersList
    } catch (err: any) {
      error.value = err.message || 'Failed to search partners'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPartnerById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const partner = await get<Partner>(`/api/partners/${id}`)

      const index = partners.value.findIndex(p => p.id === id)
      if (index !== -1) {
        partners.value[index] = partner
      } else {
        partners.value.push(partner)
      }

      currentPartner.value = partner
      return partner
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch partner'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPartnerByCode(code: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const partner = await get<Partner>(`/api/partners/code/${code}`)

      const index = partners.value.findIndex(p => p.partnerCode === code)
      if (index !== -1) {
        partners.value[index] = partner
      } else {
        partners.value.push(partner)
      }

      currentPartner.value = partner
      return partner
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch partner'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createPartner(command: CreatePartnerCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const partner = await post<Partner>('/api/partners', command)

      partners.value.unshift(partner)
      return partner
    } catch (err: any) {
      error.value = err.message || 'Failed to create partner'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updatePartner(id: string, command: UpdatePartnerCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const partner = await put<Partner>(`/api/partners/${id}`, command)

      const index = partners.value.findIndex(p => p.id === id)
      if (index !== -1) {
        partners.value[index] = partner
      }

      if (currentPartner.value?.id === id) {
        currentPartner.value = partner
      }

      return partner
    } catch (err: any) {
      error.value = err.message || 'Failed to update partner'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function activatePartner(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const partner = await post<Partner>(`/api/partners/${id}/activate`, {})

      const index = partners.value.findIndex(p => p.id === id)
      if (index !== -1) {
        partners.value[index] = partner
      }

      if (currentPartner.value?.id === id) {
        currentPartner.value = partner
      }

      return partner
    } catch (err: any) {
      error.value = err.message || 'Failed to activate partner'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function suspendPartner(id: string, reason: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const partner = await post<Partner>(`/api/partners/${id}/suspend`, { reason })

      const index = partners.value.findIndex(p => p.id === id)
      if (index !== -1) {
        partners.value[index] = partner
      }

      if (currentPartner.value?.id === id) {
        currentPartner.value = partner
      }

      return partner
    } catch (err: any) {
      error.value = err.message || 'Failed to suspend partner'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function terminatePartner(id: string, reason: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const partner = await post<Partner>(`/api/partners/${id}/terminate`, { reason })

      const index = partners.value.findIndex(p => p.id === id)
      if (index !== -1) {
        partners.value[index] = partner
      }

      if (currentPartner.value?.id === id) {
        currentPartner.value = partner
      }

      return partner
    } catch (err: any) {
      error.value = err.message || 'Failed to terminate partner'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPartnerSettlements(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const settlementsList = await get<PartnerSettlement[]>(`/api/partners/${id}/settlements`)

      settlements.value = settlementsList
      return settlementsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch settlements'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPartnerSummary(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const summary = await get<PartnerSummary>(`/api/partners/${id}/summary`)

      return summary
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch partner summary'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchTopPerformers(limit: number = 10) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const topPartners = await get<Partner[]>('/api/partners/top-performers', {
        params: { limit }
      })

      return topPartners
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch top performers'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchExpiringContracts(days: number = 30) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const expiringPartners = await get<Partner[]>('/api/partners/expiring-contracts', {
        params: { days }
      })

      return expiringPartners
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch expiring contracts'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPartnerStatistics() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const stats = await get<PartnerStatistics>('/api/partners/statistics')

      return stats
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch partner statistics'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentPartner() {
    currentPartner.value = null
  }

  function clearSettlements() {
    settlements.value = []
  }

  return {
    // State
    partners,
    currentPartner,
    settlements,
    loading,
    error,

    // Getters
    partnerCount,
    activePartners,
    suspendedPartners,
    terminatedPartners,
    pendingPartners,
    onHoldPartners,
    resellers,
    distributors,
    mvnos,
    totalSales,
    totalCommission,
    getPartnerById,
    getPartnerByCode,

    // Actions
    fetchPartners,
    searchPartners,
    fetchPartnerById,
    fetchPartnerByCode,
    createPartner,
    updatePartner,
    activatePartner,
    suspendPartner,
    terminatePartner,
    fetchPartnerSettlements,
    fetchPartnerSummary,
    fetchTopPerformers,
    fetchExpiringContracts,
    fetchPartnerStatistics,
    clearError,
    clearCurrentPartner,
    clearSettlements
  }
})
