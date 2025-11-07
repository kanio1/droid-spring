import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Payment,
  CreatePaymentCommand,
  ChangePaymentStatusCommand,
  PaymentSearchParams,
  PaymentListResponse,
  PaymentStatus,
  PaymentMethod
} from '~/schemas/payment'

export const usePaymentStore = defineStore('payment', () => {
  // State
  const payments = ref<Payment[]>([])
  const currentPayment = ref<Payment | null>(null)
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
  const paymentCount = computed(() => payments.value.length)
  const pendingPayments = computed(() => payments.value.filter(p => p.paymentStatus === 'PENDING'))
  const processingPayments = computed(() => payments.value.filter(p => p.paymentStatus === 'PROCESSING'))
  const completedPayments = computed(() => payments.value.filter(p => p.paymentStatus === 'COMPLETED'))
  const failedPayments = computed(() => payments.value.filter(p => p.paymentStatus === 'FAILED'))
  const refundedPayments = computed(() => payments.value.filter(p => p.paymentStatus === 'REFUNDED'))

  const cardPayments = computed(() => payments.value.filter(p => p.paymentMethod === 'CARD'))
  const bankTransferPayments = computed(() => payments.value.filter(p => p.paymentMethod === 'BANK_TRANSFER'))
  const cashPayments = computed(() => payments.value.filter(p => p.paymentMethod === 'CASH'))
  const directDebitPayments = computed(() => payments.value.filter(p => p.paymentMethod === 'DIRECT_DEBIT'))
  const mobilePayPayments = computed(() => payments.value.filter(p => p.paymentMethod === 'MOBILE_PAY'))

  const totalPaidAmount = computed(() =>
    completedPayments.value.reduce((sum, p) => sum + p.amount, 0)
  )

  const totalPendingAmount = computed(() =>
    pendingPayments.value.reduce((sum, p) => sum + p.amount, 0)
  )

  const getPaymentById = (id: string) => computed(() =>
    payments.value.find(p => p.id === id)
  )

  // Actions
  async function fetchPayments(params: Partial<PaymentSearchParams> = {}) {
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
        ...(params.invoiceId && { invoiceId: params.invoiceId }),
        ...(params.paymentMethod && { paymentMethod: params.paymentMethod })
      }

      const response = await get<PaymentListResponse>('/api/v1/payments', { query })
      payments.value = response.data.content

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
      error.value = err.message || 'Failed to fetch payments'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPaymentById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const response = await get<Payment>(`/api/v1/payments/${id}`)
      currentPayment.value = response.data
      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch payment'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createPayment(data: CreatePaymentCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const response = await post<Payment>('/api/v1/payments', data)

      payments.value.unshift(response.data)
      pagination.totalElements++

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to create payment'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changePaymentStatus(data: ChangePaymentStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const response = await put<Payment>(`/api/v1/payments/${data.id}/status`, data)

      const index = payments.value.findIndex(p => p.id === data.id)
      if (index !== -1) {
        payments.value[index] = response.data
      }

      if (currentPayment.value?.id === data.id) {
        currentPayment.value = response.data
      }

      return response.data
    } catch (err: any) {
      error.value = err.message || 'Failed to change payment status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function getPaymentsByCustomer(customerId: string, params: Partial<PaymentSearchParams> = {}) {
    return fetchPayments({ ...params, customerId })
  }

  async function getPaymentsByInvoice(invoiceId: string, params: Partial<PaymentSearchParams> = {}) {
    return fetchPayments({ ...params, invoiceId })
  }

  async function getPaymentsByStatus(status: PaymentStatus, params: Partial<PaymentSearchParams> = {}) {
    return fetchPayments({ ...params, status })
  }

  async function searchPayments(searchTerm: string, params: Partial<PaymentSearchParams> = {}) {
    return fetchPayments({ ...params, searchTerm })
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
    payments.value = []
    currentPayment.value = null
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
    payments,
    currentPayment,
    loading,
    error,
    pagination,

    // Getters
    paymentCount,
    pendingPayments,
    processingPayments,
    completedPayments,
    failedPayments,
    refundedPayments,
    cardPayments,
    bankTransferPayments,
    cashPayments,
    directDebitPayments,
    mobilePayPayments,
    totalPaidAmount,
    totalPendingAmount,
    getPaymentById,

    // Actions
    fetchPayments,
    fetchPaymentById,
    createPayment,
    changePaymentStatus,
    getPaymentsByCustomer,
    getPaymentsByInvoice,
    getPaymentsByStatus,
    searchPayments,
    setPage,
    setSize,
    setSort,
    reset
  }
})
