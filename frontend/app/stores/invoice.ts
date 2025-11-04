import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Invoice,
  CreateInvoiceCommand,
  ChangeInvoiceStatusCommand,
  InvoiceSearchParams,
  InvoiceListResponse,
  InvoiceStatus,
  InvoiceType
} from '~/schemas/invoice'

export const useInvoiceStore = defineStore('invoice', () => {
  // State
  const invoices = ref<Invoice[]>([])
  const currentInvoice = ref<Invoice | null>(null)
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
  const invoiceCount = computed(() => invoices.value.length)
  const draftInvoices = computed(() => invoices.value.filter(i => i.status === 'DRAFT'))
  const issuedInvoices = computed(() => invoices.value.filter(i => i.status === 'ISSUED'))
  const sentInvoices = computed(() => invoices.value.filter(i => i.status === 'SENT'))
  const paidInvoices = computed(() => invoices.value.filter(i => i.status === 'PAID'))
  const overdueInvoices = computed(() => invoices.value.filter(i => i.status === 'OVERDUE'))
  const cancelledInvoices = computed(() => invoices.value.filter(i => i.status === 'CANCELLED'))

  const unpaidInvoices = computed(() => invoices.value.filter(i => i.isUnpaid))
  const recurringInvoices = computed(() => invoices.value.filter(i => i.invoiceType === 'RECURRING'))
  const oneTimeInvoices = computed(() => invoices.value.filter(i => i.invoiceType === 'ONE_TIME'))
  const adjustmentInvoices = computed(() => invoices.value.filter(i => i.invoiceType === 'ADJUSTMENT'))

  const totalOutstandingAmount = computed(() =>
    unpaidInvoices.value.reduce((sum, inv) => sum + inv.totalAmount, 0)
  )

  const totalOverdueAmount = computed(() =>
    overdueInvoices.value.reduce((sum, inv) => sum + inv.totalAmount, 0)
  )

  const getInvoiceById = (id: string) => computed(() =>
    invoices.value.find(i => i.id === id)
  )

  // Actions
  async function fetchInvoices(params: Partial<InvoiceSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc',
        ...(params.query && { query: params.query }),
        ...(params.status && { status: params.status }),
        ...(params.type && { type: params.type }),
        ...(params.customerId && { customerId: params.customerId }),
        ...(params.startDate && { startDate: params.startDate }),
        ...(params.endDate && { endDate: params.endDate }),
        ...(params.unpaid !== undefined && { unpaid: params.unpaid }),
        ...(params.overdue !== undefined && { overdue: params.overdue })
      }

      const response = await get<InvoiceListResponse>('/invoices', { query })
      invoices.value = response.data.content

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
      error.value = err.message || 'Failed to fetch invoices'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchInvoiceById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Invoice>(`/invoices/${id}`)
      currentInvoice.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch invoice'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createInvoice(data: CreateInvoiceCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Invoice>('/invoices', data)

      invoices.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create invoice'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changeInvoiceStatus(data: ChangeInvoiceStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Invoice>(`/invoices/${data.id}/status`, data)

      const index = invoices.value.findIndex(i => i.id === data.id)
      if (index !== -1) {
        invoices.value[index] = response.data
      }

      if (currentInvoice.value?.id === data.id) {
        currentInvoice.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to change invoice status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getInvoicesByCustomer(customerId: string, params: Partial<InvoiceSearchParams> = {}) {
    return fetchInvoices({ ...params, customerId })
  }

  async function getInvoicesByStatus(status: InvoiceStatus, params: Partial<InvoiceSearchParams> = {}) {
    return fetchInvoices({ ...params, status })
  }

  async function getUnpaidInvoices(params: Partial<InvoiceSearchParams> = {}) {
    return fetchInvoices({ ...params, unpaid: true })
  }

  async function getOverdueInvoices(params: Partial<InvoiceSearchParams> = {}) {
    return fetchInvoices({ ...params, overdue: true })
  }

  async function searchInvoices(query: string, params: Partial<InvoiceSearchParams> = {}) {
    return fetchInvoices({ ...params, query })
  }

  async function getInvoicesByDateRange(startDate: string, endDate: string, params: Partial<InvoiceSearchParams> = {}) {
    return fetchInvoices({ ...params, startDate, endDate })
  }

  async function getInvoicesByInvoiceNumber(invoiceNumber: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Invoice>(`/invoices/by-invoice-number/${invoiceNumber}`)
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch invoice by number'
      throw err
    } finally {
      loading.value = false
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
    invoices.value = []
    currentInvoice.value = null
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
    invoices,
    currentInvoice,
    loading,
    error,
    pagination,

    // Getters
    invoiceCount,
    draftInvoices,
    issuedInvoices,
    sentInvoices,
    paidInvoices,
    overdueInvoices,
    cancelledInvoices,
    unpaidInvoices,
    recurringInvoices,
    oneTimeInvoices,
    adjustmentInvoices,
    totalOutstandingAmount,
    totalOverdueAmount,
    getInvoiceById,

    // Actions
    fetchInvoices,
    fetchInvoiceById,
    createInvoice,
    changeInvoiceStatus,
    getInvoicesByCustomer,
    getInvoicesByStatus,
    getUnpaidInvoices,
    getOverdueInvoices,
    searchInvoices,
    getInvoicesByDateRange,
    getInvoicesByInvoiceNumber,
    setPage,
    setSize,
    setSort,
    reset
  }
})
