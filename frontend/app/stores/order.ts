import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Order,
  CreateOrderCommand,
  UpdateOrderStatusCommand,
  OrderSearchParams,
  OrderListResponse,
  OrderStatus,
  OrderType,
  OrderPriority
} from '~/schemas/order'

export const useOrderStore = defineStore('order', () => {
  // State
  const orders = ref<Order[]>([])
  const currentOrder = ref<Order | null>(null)
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
  const orderCount = computed(() => orders.value.length)
  const pendingOrders = computed(() => orders.value.filter(o => o.status === 'PENDING'))
  const confirmedOrders = computed(() => orders.value.filter(o => o.status === 'CONFIRMED'))
  const processingOrders = computed(() => orders.value.filter(o => o.status === 'PROCESSING'))
  const completedOrders = computed(() => orders.value.filter(o => o.status === 'COMPLETED'))
  const cancelledOrders = computed(() => orders.value.filter(o => o.status === 'CANCELLED'))

  const newOrders = computed(() => orders.value.filter(o => o.orderType === 'NEW'))
  const upgradeOrders = computed(() => orders.value.filter(o => o.orderType === 'UPGRADE'))
  const downgradeOrders = computed(() => orders.value.filter(o => o.orderType === 'DOWNGRADE'))
  const renewOrders = computed(() => orders.value.filter(o => o.orderType === 'RENEW'))
  const cancelOrders = computed(() => orders.value.filter(o => o.orderType === 'CANCEL'))

  const urgentOrders = computed(() => orders.value.filter(o => o.priority === 'URGENT'))
  const highPriorityOrders = computed(() => orders.value.filter(o => o.priority === 'HIGH'))

  const getOrderById = (id: string) => computed(() =>
    orders.value.find(o => o.id === id)
  )

  const getOrdersByType = (type: OrderType) => computed(() =>
    orders.value.filter(o => o.orderType === type)
  )

  const getOrdersByPriority = (priority: OrderPriority) => computed(() =>
    orders.value.filter(o => o.priority === priority)
  )

  // Actions
  async function fetchOrders(params: Partial<OrderSearchParams> = {}) {
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
        ...(params.type && { type: params.type }),
        ...(params.customerId && { customerId: params.customerId })
      }

      const response = await get<OrderListResponse>('/api/v1/orders', { query })
      orders.value = response.data.content

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
      error.value = err.message || 'Failed to fetch orders'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchOrderById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Order>(`/api/v1/orders/${id}`)
      currentOrder.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch order'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createOrder(data: CreateOrderCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Order>('/api/v1/orders', data)

      orders.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create order'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateOrderStatus(data: UpdateOrderStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Order>(`/api/v1/orders/${data.id}/status`, data)

      const index = orders.value.findIndex(o => o.id === data.id)
      if (index !== -1) {
        orders.value[index] = response.data
      }

      if (currentOrder.value?.id === data.id) {
        currentOrder.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to update order status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getOrdersByCustomer(customerId: string, params: Partial<OrderSearchParams> = {}) {
    return fetchOrders({ ...params, customerId })
  }

  async function getOrdersByStatus(status: OrderStatus, params: Partial<OrderSearchParams> = {}) {
    return fetchOrders({ ...params, status })
  }

  async function searchOrders(searchTerm: string, params: Partial<OrderSearchParams> = {}) {
    return fetchOrders({ ...params, searchTerm })
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
    orders.value = []
    currentOrder.value = null
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
    orders,
    currentOrder,
    loading,
    error,
    pagination,

    // Getters
    orderCount,
    pendingOrders,
    confirmedOrders,
    processingOrders,
    completedOrders,
    cancelledOrders,
    newOrders,
    upgradeOrders,
    downgradeOrders,
    renewOrders,
    cancelOrders,
    urgentOrders,
    highPriorityOrders,
    getOrderById,
    getOrdersByStatus,
    getOrdersByType,
    getOrdersByPriority,

    // Actions
    fetchOrders,
    fetchOrderById,
    createOrder,
    updateOrderStatus,
    getOrdersByCustomer,
    searchOrders,
    setPage,
    setSize,
    setSort,
    reset
  }
})
