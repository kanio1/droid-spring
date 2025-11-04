import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePaymentStore } from '~/stores/payment'
import type { Payment, PaymentStatus, PaymentMethod } from '~/schemas/payment'

// Mock useApi composable
const mockUseApi = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: mockUseApi
}))

describe('Payment Store', () => {
  beforeEach(() => {
    mockUseApi.mockReturnValue({
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      del: vi.fn(),
      patch: vi.fn(),
      create: vi.fn(),
      read: vi.fn(),
      update: vi.fn(),
      remove: vi.fn(),
      paginatedGet: vi.fn(),
      request: vi.fn(),
      loading: vi.fn(() => false),
      handleSuccess: vi.fn(),
      buildUrl: vi.fn(),
      baseURL: 'http://localhost:8080/api'
    })
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = usePaymentStore()

    expect(store.payments).toEqual([])
    expect(store.currentPayment).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.pagination.page).toBe(0)
    expect(store.pagination.size).toBe(20)
    expect(store.pagination.totalElements).toBe(0)
  })

  it('should fetch payments successfully', async () => {
    const store = usePaymentStore()
    const mockPayments: Payment[] = [
      {
        id: '1',
        paymentNumber: 'PAY-2024-001',
        customerId: 'cust-1',
        invoiceId: 'inv-1',
        amount: 123.00,
        currency: 'PLN',
        paymentMethod: 'CARD' as PaymentMethod,
        paymentMethodDisplayName: 'Card',
        paymentStatus: 'COMPLETED' as PaymentStatus,
        paymentStatusDisplayName: 'Completed',
        transactionId: 'txn-123456',
        gateway: 'Stripe',
        paymentDate: '2024-01-15',
        receivedDate: '2024-01-15',
        referenceNumber: 'REF-001',
        notes: 'Payment for invoice INV-2024-001',
        reversalReason: null,
        createdAt: '2024-01-15T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockPayments,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.fetchPayments()

    expect(store.payments).toEqual(mockPayments)
    expect(store.pagination.totalElements).toBe(1)
  })

  it('should filter payments by status', async () => {
    const store = usePaymentStore()
    const mockPayments: Payment[] = [
      {
        id: '1',
        paymentNumber: 'PAY-2024-001',
        customerId: 'cust-1',
        invoiceId: 'inv-1',
        amount: 123.00,
        currency: 'PLN',
        paymentMethod: 'CARD' as PaymentMethod,
        paymentMethodDisplayName: 'Card',
        paymentStatus: 'PENDING' as PaymentStatus,
        paymentStatusDisplayName: 'Pending',
        transactionId: null,
        gateway: 'Stripe',
        paymentDate: '2024-01-15',
        receivedDate: null,
        referenceNumber: 'REF-001',
        notes: 'Pending payment',
        reversalReason: null,
        createdAt: '2024-01-15T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockPayments,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.getPaymentsByStatus('PENDING')

    expect(get).toHaveBeenCalledWith('/payments', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        status: 'PENDING'
      }
    })
  })

  it('should create a payment', async () => {
    const store = usePaymentStore()
    const newPayment = {
      paymentNumber: 'PAY-2024-002',
      customerId: 'cust-2',
      invoiceId: 'inv-2',
      amount: 246.00,
      currency: 'PLN',
      paymentMethod: 'BANK_TRANSFER' as PaymentMethod,
      paymentDate: '2024-01-20',
      transactionId: 'txn-789012',
      gateway: 'Bank',
      referenceNumber: 'REF-002',
      notes: 'Bank transfer payment'
    }

    const mockPayment: Payment = {
      id: '2',
      ...newPayment,
      paymentMethodDisplayName: 'Bank Transfer',
      paymentStatus: 'PROCESSING' as PaymentStatus,
      paymentStatusDisplayName: 'Processing',
      receivedDate: null,
      reversalReason: null,
      createdAt: '2024-01-20T00:00:00Z',
      updatedAt: '2024-01-20T00:00:00Z',
      version: 1
    }

    const { post } = mockUseApi()
    vi.mocked(post).mockResolvedValueOnce({
      data: mockPayment
    } as any)

    await store.createPayment(newPayment)

    expect(store.payments).toHaveLength(1)
    expect(store.payments[0]).toEqual(mockPayment)
  })

  it('should change payment status', async () => {
    const store = usePaymentStore()

    const existingPayment: Payment = {
      id: '1',
      paymentNumber: 'PAY-2024-001',
      customerId: 'cust-1',
      invoiceId: 'inv-1',
      amount: 123.00,
      currency: 'PLN',
      paymentMethod: 'CARD' as PaymentMethod,
      paymentMethodDisplayName: 'Card',
      paymentStatus: 'PENDING' as PaymentStatus,
      paymentStatusDisplayName: 'Pending',
      transactionId: null,
      gateway: 'Stripe',
      paymentDate: '2024-01-15',
      receivedDate: null,
      referenceNumber: 'REF-001',
      notes: 'Pending payment',
      reversalReason: null,
      createdAt: '2024-01-15T00:00:00Z',
      updatedAt: '2024-01-15T00:00:00Z',
      version: 1
    }

    store.payments = [existingPayment]

    const statusData = {
      id: '1',
      paymentStatus: 'COMPLETED' as PaymentStatus
    }

    const mockUpdatedPayment: Payment = {
      ...existingPayment,
      paymentStatus: 'COMPLETED' as PaymentStatus,
      paymentStatusDisplayName: 'Completed',
      transactionId: 'txn-123456',
      receivedDate: '2024-01-15',
      updatedAt: '2024-01-15T10:30:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedPayment
    } as any)

    await store.changePaymentStatus(statusData)

    expect(store.payments[0].paymentStatus).toBe('COMPLETED')
    expect(store.payments[0].transactionId).toBe('txn-123456')
    expect(store.payments[0].version).toBe(2)
  })

  it('should get payments by customer', async () => {
    const store = usePaymentStore()
    const mockPayments: Payment[] = [
      {
        id: '1',
        paymentNumber: 'PAY-2024-001',
        customerId: 'cust-1',
        invoiceId: 'inv-1',
        amount: 123.00,
        currency: 'PLN',
        paymentMethod: 'CARD' as PaymentMethod,
        paymentMethodDisplayName: 'Card',
        paymentStatus: 'COMPLETED' as PaymentStatus,
        paymentStatusDisplayName: 'Completed',
        transactionId: 'txn-123456',
        gateway: 'Stripe',
        paymentDate: '2024-01-15',
        receivedDate: '2024-01-15',
        referenceNumber: 'REF-001',
        notes: '',
        reversalReason: null,
        createdAt: '2024-01-15T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockPayments,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.getPaymentsByCustomer('cust-1')

    expect(get).toHaveBeenCalledWith('/payments', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        customerId: 'cust-1'
      }
    })
  })

  it('should get payments by invoice', async () => {
    const store = usePaymentStore()
    const mockPayments: Payment[] = [
      {
        id: '1',
        paymentNumber: 'PAY-2024-001',
        customerId: 'cust-1',
        invoiceId: 'inv-1',
        amount: 123.00,
        currency: 'PLN',
        paymentMethod: 'CARD' as PaymentMethod,
        paymentMethodDisplayName: 'Card',
        paymentStatus: 'COMPLETED' as PaymentStatus,
        paymentStatusDisplayName: 'Completed',
        transactionId: 'txn-123456',
        gateway: 'Stripe',
        paymentDate: '2024-01-15',
        receivedDate: '2024-01-15',
        referenceNumber: 'REF-001',
        notes: '',
        reversalReason: null,
        createdAt: '2024-01-15T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockPayments,
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        first: true,
        last: true,
        numberOfElements: 1,
        empty: false
      }
    } as any)

    await store.getPaymentsByInvoice('inv-1')

    expect(get).toHaveBeenCalledWith('/payments', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        invoiceId: 'inv-1'
      }
    })
  })

  it('should compute filtered payments correctly', () => {
    const store = usePaymentStore()

    const mockPayments: Payment[] = [
      {
        id: '1',
        paymentNumber: 'PAY-001',
        customerId: 'cust-1',
        invoiceId: 'inv-1',
        amount: 123.00,
        currency: 'PLN',
        paymentMethod: 'CARD' as PaymentMethod,
        paymentMethodDisplayName: 'Card',
        paymentStatus: 'COMPLETED' as PaymentStatus,
        paymentStatusDisplayName: 'Completed',
        transactionId: 'txn-123',
        gateway: 'Stripe',
        paymentDate: '2024-01-15',
        receivedDate: '2024-01-15',
        referenceNumber: 'REF-001',
        notes: '',
        reversalReason: null,
        createdAt: '2024-01-15T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        paymentNumber: 'PAY-002',
        customerId: 'cust-2',
        invoiceId: 'inv-2',
        amount: 246.00,
        currency: 'PLN',
        paymentMethod: 'BANK_TRANSFER' as PaymentMethod,
        paymentMethodDisplayName: 'Bank Transfer',
        paymentStatus: 'PENDING' as PaymentStatus,
        paymentStatusDisplayName: 'Pending',
        transactionId: null,
        gateway: 'Bank',
        paymentDate: '2024-01-16',
        receivedDate: null,
        referenceNumber: 'REF-002',
        notes: '',
        reversalReason: null,
        createdAt: '2024-01-16T00:00:00Z',
        updatedAt: '2024-01-16T00:00:00Z',
        version: 1
      },
      {
        id: '3',
        paymentNumber: 'PAY-003',
        customerId: 'cust-3',
        invoiceId: 'inv-3',
        amount: 50.00,
        currency: 'PLN',
        paymentMethod: 'CASH' as PaymentMethod,
        paymentMethodDisplayName: 'Cash',
        paymentStatus: 'FAILED' as PaymentStatus,
        paymentStatusDisplayName: 'Failed',
        transactionId: null,
        gateway: 'Office',
        paymentDate: '2024-01-17',
        receivedDate: null,
        referenceNumber: 'REF-003',
        notes: 'Payment failed',
        reversalReason: 'Insufficient funds',
        createdAt: '2024-01-17T00:00:00Z',
        updatedAt: '2024-01-17T00:00:00Z',
        version: 1
      }
    ]

    store.payments = mockPayments

    expect(store.completedPayments).toHaveLength(1)
    expect(store.pendingPayments).toHaveLength(1)
    expect(store.processingPayments).toHaveLength(0)
    expect(store.failedPayments).toHaveLength(1)
    expect(store.refundedPayments).toHaveLength(0)
    expect(store.cardPayments).toHaveLength(1)
    expect(store.bankTransferPayments).toHaveLength(1)
    expect(store.cashPayments).toHaveLength(1)
  })

  it('should calculate total amounts correctly', () => {
    const store = usePaymentStore()

    const mockPayments: Payment[] = [
      {
        id: '1',
        paymentNumber: 'PAY-001',
        customerId: 'cust-1',
        invoiceId: 'inv-1',
        amount: 123.00,
        currency: 'PLN',
        paymentMethod: 'CARD' as PaymentMethod,
        paymentMethodDisplayName: 'Card',
        paymentStatus: 'COMPLETED' as PaymentStatus,
        paymentStatusDisplayName: 'Completed',
        transactionId: 'txn-123',
        gateway: 'Stripe',
        paymentDate: '2024-01-15',
        receivedDate: '2024-01-15',
        referenceNumber: 'REF-001',
        notes: '',
        reversalReason: null,
        createdAt: '2024-01-15T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        paymentNumber: 'PAY-002',
        customerId: 'cust-2',
        invoiceId: 'inv-2',
        amount: 246.00,
        currency: 'PLN',
        paymentMethod: 'BANK_TRANSFER' as PaymentMethod,
        paymentMethodDisplayName: 'Bank Transfer',
        paymentStatus: 'PENDING' as PaymentStatus,
        paymentStatusDisplayName: 'Pending',
        transactionId: null,
        gateway: 'Bank',
        paymentDate: '2024-01-16',
        receivedDate: null,
        referenceNumber: 'REF-002',
        notes: '',
        reversalReason: null,
        createdAt: '2024-01-16T00:00:00Z',
        updatedAt: '2024-01-16T00:00:00Z',
        version: 1
      }
    ]

    store.payments = mockPayments

    expect(store.totalPaidAmount).toBe(123.00)
    expect(store.totalPendingAmount).toBe(246.00)
  })

  it('should reset store state', () => {
    const store = usePaymentStore()

    store.payments = [
      {
        id: '1',
        paymentNumber: 'PAY-001',
        customerId: 'cust-1',
        invoiceId: 'inv-1',
        amount: 123.00,
        currency: 'PLN',
        paymentMethod: 'CARD' as PaymentMethod,
        paymentMethodDisplayName: 'Card',
        paymentStatus: 'COMPLETED' as PaymentStatus,
        paymentStatusDisplayName: 'Completed',
        transactionId: 'txn-123',
        gateway: 'Stripe',
        paymentDate: '2024-01-15',
        receivedDate: '2024-01-15',
        referenceNumber: 'REF-001',
        notes: '',
        reversalReason: null,
        createdAt: '2024-01-15T00:00:00Z',
        updatedAt: '2024-01-15T00:00:00Z',
        version: 1
      }
    ]

    store.pagination.totalElements = 1
    store.currentPayment = {
      id: '1',
      paymentNumber: 'PAY-001',
      customerId: 'cust-1',
      invoiceId: 'inv-1',
      amount: 123.00,
      currency: 'PLN',
      paymentMethod: 'CARD' as PaymentMethod,
      paymentMethodDisplayName: 'Card',
      paymentStatus: 'COMPLETED' as PaymentStatus,
      paymentStatusDisplayName: 'Completed',
      transactionId: 'txn-123',
      gateway: 'Stripe',
      paymentDate: '2024-01-15',
      receivedDate: '2024-01-15',
      referenceNumber: 'REF-001',
      notes: '',
      reversalReason: null,
      createdAt: '2024-01-15T00:00:00Z',
      updatedAt: '2024-01-15T00:00:00Z',
      version: 1
    }

    store.reset()

    expect(store.payments).toEqual([])
    expect(store.currentPayment).toBeNull()
    expect(store.pagination.totalElements).toBe(0)
  })
})
