import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  CoverageNode,
  CreateCoverageNodeCommand,
  UpdateCoverageNodeCommand,
  ChangeCoverageNodeStatusCommand,
  CoverageNodeSearchParams,
  CoverageNodeListResponse,
  CoverageNodeStatus,
  CoverageNodeType,
  Technology,
  CoverageStatistics
} from '~/schemas/coverage-node'

export const useCoverageNodeStore = defineStore('coverageNode', () => {
  // State
  const nodes = ref<CoverageNode[]>([])
  const currentNode = ref<CoverageNode | null>(null)
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
  const nodeCount = computed(() => nodes.value.length)
  const activeNodes = computed(() => nodes.value.filter(n => n.status === 'ACTIVE'))
  const inactiveNodes = computed(() => nodes.value.filter(n => n.status === 'INACTIVE'))
  const maintenanceNodes = computed(() => nodes.value.filter(n => n.status === 'MAINTENANCE'))
  const plannedNodes = computed(() => nodes.value.filter(n => n.status === 'PLANNED'))
  const decommissionedNodes = computed(() => nodes.value.filter(n => n.status === 'DECOMMISSIONED'))

  const overloadedNodes = computed(() => nodes.value.filter(n => n.capacityPercentage > 90))
  const healthyNodes = computed(() => nodes.value.filter(n => n.status === 'ACTIVE' && n.capacityPercentage < 80))

  const nodesByType = computed(() => (type: CoverageNodeType) =>
    nodes.value.filter(n => n.type === type)
  )

  const nodesByTechnology = computed(() => (tech: Technology) =>
    nodes.value.filter(n => n.technology === tech)
  )

  const nodesByCity = computed(() => (city: string) =>
    nodes.value.filter(n => n.city === city)
  )

  const averageCapacity = computed(() => {
    if (nodes.value.length === 0) return 0
    const sum = nodes.value.reduce((acc, n) => acc + n.capacityPercentage, 0)
    return Math.round(sum / nodes.value.length)
  })

  const totalCoverageArea = computed(() => {
    return nodes.value.reduce((acc, n) => acc + (n.coverageArea || 0), 0)
  })

  const getNodeById = (id: string) => computed(() =>
    nodes.value.find(n => n.id === id)
  )

  // Actions
  async function fetchNodes(params: Partial<CoverageNodeSearchParams> = {}) {
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
        ...(params.type && { type: params.type }),
        ...(params.status && { status: params.status }),
        ...(params.technology && { technology: params.technology }),
        ...(params.city && { city: params.city }),
        ...(params.country && { country: params.country }),
        ...(params.minCapacity && { minCapacity: params.minCapacity }),
        ...(params.maxCapacity && { maxCapacity: params.maxCapacity })
      }

      const response = await get<CoverageNodeListResponse>('/coverage-nodes', { query })
      nodes.value = response.data.content

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
      error.value = err.message || 'Failed to fetch coverage nodes'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchNodeById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<CoverageNode>(`/coverage-nodes/${id}`)
      currentNode.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch coverage node'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createNode(data: CreateCoverageNodeCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<CoverageNode>('/coverage-nodes', data)

      // Add to list
      nodes.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create coverage node'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateNode(data: UpdateCoverageNodeCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<CoverageNode>(`/coverage-nodes/${data.id}`, data)

      // Update in list
      const index = nodes.value.findIndex(n => n.id === data.id)
      if (index !== -1) {
        nodes.value[index] = response.data
      }

      // Update current node if it's the same
      if (currentNode.value?.id === data.id) {
        currentNode.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to update coverage node'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changeNodeStatus(data: ChangeCoverageNodeStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<CoverageNode>(`/coverage-nodes/${data.id}/status`, data)

      // Update in list
      const index = nodes.value.findIndex(n => n.id === data.id)
      if (index !== -1) {
        nodes.value[index] = response.data
      }

      // Update current node if it's the same
      if (currentNode.value?.id === data.id) {
        currentNode.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to change coverage node status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteNode(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { del } = useApi()
      await del(`/coverage-nodes/${id}`)

      // Remove from list
      const index = nodes.value.findIndex(n => n.id === id)
      if (index !== -1) {
        nodes.value.splice(index, 1)
        pagination.totalElements--
      }

      // Clear current node if it's the same
      if (currentNode.value?.id === id) {
        currentNode.value = null
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to delete coverage node'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function searchNodes(searchTerm: string, params: Partial<CoverageNodeSearchParams> = {}) {
    return fetchNodes({ ...params, searchTerm })
  }

  async function getNodesByStatus(status: CoverageNodeStatus, params: Partial<CoverageNodeSearchParams> = {}) {
    return fetchNodes({ ...params, status })
  }

  async function getNodesByType(type: CoverageNodeType, params: Partial<CoverageNodeSearchParams> = {}) {
    return fetchNodes({ ...params, type })
  }

  async function getNodesByTechnology(technology: Technology, params: Partial<CoverageNodeSearchParams> = {}) {
    return fetchNodes({ ...params, technology })
  }

  async function getNodesByCity(city: string, params: Partial<CoverageNodeSearchParams> = {}) {
    return fetchNodes({ ...params, city })
  }

  async function getNodesByCountry(country: string, params: Partial<CoverageNodeSearchParams> = {}) {
    return fetchNodes({ ...params, country })
  }

  async function getCoverageStatistics(): Promise<CoverageStatistics> {
    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<CoverageStatistics>('/coverage-nodes/statistics')
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch coverage statistics'
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
    nodes.value = []
    currentNode.value = null
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
    nodes,
    currentNode,
    loading,
    error,
    pagination,

    // Getters
    nodeCount,
    activeNodes,
    inactiveNodes,
    maintenanceNodes,
    plannedNodes,
    decommissionedNodes,
    overloadedNodes,
    healthyNodes,
    nodesByType,
    nodesByTechnology,
    nodesByCity,
    averageCapacity,
    totalCoverageArea,
    getNodeById,

    // Actions
    fetchNodes,
    fetchNodeById,
    createNode,
    updateNode,
    changeNodeStatus,
    deleteNode,
    searchNodes,
    getNodesByStatus,
    getNodesByType,
    getNodesByTechnology,
    getNodesByCity,
    getNodesByCountry,
    getCoverageStatistics,
    setPage,
    setSize,
    setSort,
    reset
  }
})
