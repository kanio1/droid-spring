import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Asset,
  NetworkElement,
  SIMCard,
  CreateAssetCommand,
  CreateNetworkElementCommand,
  CreateSIMCardCommand,
  AssetSearchParams,
  AssetListResponse,
  NetworkElementSearchParams,
  NetworkElementListResponse,
  SIMCardSearchParams,
  SIMCardListResponse
} from '~/schemas/asset'

export const useAssetStore = defineStore('asset', () => {
  // State
  const assets = ref<Asset[]>([])
  const networkElements = ref<NetworkElement[]>([])
  const simCards = ref<SIMCard[]>([])
  const currentAsset = ref<Asset | null>(null)
  const currentNetworkElement = ref<NetworkElement | null>(null)
  const currentSIMCard = ref<SIMCard | null>(null)
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
  const availableAssets = computed(() => assets.value.filter(a => a.status === 'AVAILABLE'))
  const assignedAssets = computed(() => assets.value.filter(a => a.status === 'ASSIGNED'))
  const inUseAssets = computed(() => assets.value.filter(a => a.status === 'IN_USE'))
  const maintenanceAssets = computed(() => assets.value.filter(a => a.status === 'MAINTENANCE'))

  const availableSIMs = computed(() => simCards.value.filter(s => s.status === 'AVAILABLE'))
  const assignedSIMs = computed(() => simCards.value.filter(s => s.status === 'ASSIGNED'))
  const activeSIMs = computed(() => simCards.value.filter(s => s.status === 'ACTIVE'))
  const expiredSIMs = computed(() => simCards.value.filter(s => s.status === 'EXPIRED'))
  const expiringSoonSIMs = computed(() => {
    const today = new Date()
    const thirtyDaysFromNow = new Date(today.getTime() + 30 * 24 * 60 * 60 * 1000)
    return simCards.value.filter(s => {
      if (!s.expiryDate) return false
      const expiry = new Date(s.expiryDate)
      return expiry <= thirtyDaysFromNow && expiry >= today && s.status !== 'EXPIRED'
    })
  })

  const onlineNetworkElements = computed(() => networkElements.value.filter(n => n.isOnline))
  const offlineNetworkElements = computed(() => networkElements.value.filter(n => !n.isOnline))
  const maintenanceNetworkElements = computed(() => networkElements.value.filter(n => n.isMaintenance))

  const assetsByCustomer = (customerId: string) => computed(() =>
    assets.value.filter(a => a.customerId === customerId)
  )

  const simsByCustomer = (customerId: string) => computed(() =>
    simCards.value.filter(s => s.customerId === customerId)
  )

  const networkElementsByType = (type: string) => computed(() =>
    networkElements.value.filter(n => n.elementType === type)
  )

  const assetsExpiringSoon = computed(() => {
    const today = new Date()
    const ninetyDaysFromNow = new Date(today.getTime() + 90 * 24 * 60 * 60 * 1000)
    return assets.value.filter(a => {
      if (!a.warrantyExpiryDate) return false
      const warranty = new Date(a.warrantyExpiryDate)
      return warranty <= ninetyDaysFromNow && warranty >= today
    })
  })

  // Actions
  async function fetchAssets(params: Partial<AssetSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc',
        ...(params.status && { status: params.status }),
        ...(params.type && { type: params.type }),
        ...(params.customerId && { customerId: params.customerId }),
        ...(params.warrantyExpiring !== undefined && { warrantyExpiring: params.warrantyExpiring })
      }

      const response = await get<AssetListResponse>('/assets', { query })
      assets.value = response.data.content

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
      error.value = err.message || 'Failed to fetch assets'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchNetworkElements(params: Partial<NetworkElementSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc',
        ...(params.online !== undefined && { online: params.online }),
        ...(params.maintenance !== undefined && { maintenance: params.maintenance })
      }

      const response = await get<NetworkElementListResponse>('/assets/elements', { query })
      networkElements.value = response.data.content

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
      error.value = err.message || 'Failed to fetch network elements'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchSIMCards(params: Partial<SIMCardSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc',
        ...(params.status && { status: params.status }),
        ...(params.customerId && { customerId: params.customerId }),
        ...(params.expired !== undefined && { expired: params.expired }),
        ...(params.expiring !== undefined && { expiring: params.expiring })
      }

      const response = await get<SIMCardListResponse>('/assets/sim-cards', { query })
      simCards.value = response.data.content

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
      error.value = err.message || 'Failed to fetch SIM cards'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchAssetById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Asset>(`/assets/${id}`)
      currentAsset.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch asset'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createAsset(data: CreateAssetCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Asset>('/assets', data)
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create asset'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function assignAsset(id: string, customerId: string, location?: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Asset>(`/assets/${id}/assign`, { customerId, location })
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to assign asset'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function releaseAsset(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Asset>(`/assets/${id}/release`, {})
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to release asset'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createNetworkElement(data: CreateNetworkElementCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<NetworkElement>('/assets/elements', data)
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create network element'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateNetworkElementHeartbeat(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<NetworkElement>(`/assets/elements/${id}/heartbeat`, {})
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to update heartbeat'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createSIMCard(data: CreateSIMCardCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<SIMCard>('/assets/sim-cards', data)
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create SIM card'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function assignSIMCard(id: string, customerId: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<SIMCard>(`/assets/sim-cards/${id}/assign`, { customerId })
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to assign SIM card'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    assets,
    networkElements,
    simCards,
    currentAsset,
    currentNetworkElement,
    currentSIMCard,
    loading,
    error,
    pagination,
    // Getters
    availableAssets,
    assignedAssets,
    inUseAssets,
    maintenanceAssets,
    availableSIMs,
    assignedSIMs,
    activeSIMs,
    expiredSIMs,
    expiringSoonSIMs,
    onlineNetworkElements,
    offlineNetworkElements,
    maintenanceNetworkElements,
    assetsByCustomer,
    simsByCustomer,
    networkElementsByType,
    assetsExpiringSoon,
    // Actions
    fetchAssets,
    fetchNetworkElements,
    fetchSIMCards,
    fetchAssetById,
    createAsset,
    assignAsset,
    releaseAsset,
    createNetworkElement,
    updateNetworkElementHeartbeat,
    createSIMCard,
    assignSIMCard
  }
})
