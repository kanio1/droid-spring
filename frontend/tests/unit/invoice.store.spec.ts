import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useInvoiceStore } from '~/stores/invoice'
import type { Invoice, InvoiceStatus, InvoiceType } from '~/schemas/invoice'

// Mock useApi composable
const mockUseApi = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: mockUseApi
}))

describe('Invoice Store', () => {
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
    const store = useInvoiceStore()

    expect(store.invoices).toEqual([])
    expect(store.currentInvoice).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.pagination.page).toBe(0)
    expect(store.pagination.size).toBe(20)
    expect(store.pagination.totalElements).toBe(0)
  })

  it('should fetch invoices successfully', async () => {
    const store = useInvoiceStore()
    const mockInvoices: Invoice[] = [
      {
        id: '1',
        invoiceNumber: 'INV-2024-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'RECURRING' as InvoiceType,
        invoiceTypeDisplayName: 'Recurring',
        status: 'ISSUED' as InvoiceStatus,
        statusDisplayName: 'Issued',
        issueDate: '2024-01-01',
        dueDate: '2024-01-31',
        paidDate: null,
        billingPeriodStart: '2024-01-01',
        billingPeriodEnd: '2024-01-31',
        subtotal: 100.00,
        discountAmount: 0,
        taxAmount: 23.00,
        totalAmount: 123.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: 'Monthly service invoice',
        pdfUrl: null,
        sentToEmail: 'john@example.com',
        sentAt: '2024-01-02T00:00:00Z',
        isUnpaid: true,
        isOverdue: false,
        isPaid: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockInvoices,
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

    await store.fetchInvoices()

    expect(store.invoices).toEqual(mockInvoices)
    expect(store.pagination.totalElements).toBe(1)
  })

  it('should filter invoices by status', async () => {
    const store = useInvoiceStore()
    const mockInvoices: Invoice[] = [
      {
        id: '1',
        invoiceNumber: 'INV-2024-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'RECURRING' as InvoiceType,
        invoiceTypeDisplayName: 'Recurring',
        status: 'PAID' as InvoiceStatus,
        statusDisplayName: 'Paid',
        issueDate: '2024-01-01',
        dueDate: '2024-01-31',
        paidDate: '2024-01-15',
        billingPeriodStart: '2024-01-01',
        billingPeriodEnd: '2024-01-31',
        subtotal: 100.00,
        discountAmount: 0,
        taxAmount: 23.00,
        totalAmount: 123.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'john@example.com',
        sentAt: '2024-01-02T00:00:00Z',
        isUnpaid: false,
        isOverdue: false,
        isPaid: true,
        canBeCancelled: false,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockInvoices,
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

    await store.getInvoicesByStatus('PAID')

    expect(get).toHaveBeenCalledWith('/invoices', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        status: 'PAID'
      }
    })
  })

  it('should create an invoice', async () => {
    const store = useInvoiceStore()
    const newInvoice = {
      invoiceNumber: 'INV-2024-002',
      customerId: 'cust-2',
      invoiceType: 'ONE_TIME' as InvoiceType,
      issueDate: '2024-01-15',
      dueDate: '2024-02-15',
      billingPeriodStart: '2024-01-15',
      billingPeriodEnd: '2024-01-15',
      subtotal: 200.00,
      discountAmount: 20.00,
      taxAmount: 41.40,
      totalAmount: 221.40,
      currency: 'PLN',
      paymentTerms: 30,
      lateFee: 0,
      notes: 'One-time service',
      sentToEmail: 'jane@example.com'
    }

    const mockInvoice: Invoice = {
      id: '2',
      ...newInvoice,
      status: 'DRAFT' as InvoiceStatus,
      statusDisplayName: 'Draft',
      paidDate: null,
      isUnpaid: true,
      isOverdue: false,
      isPaid: false,
      canBeCancelled: true,
      itemCount: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    const { post } = mockUseApi()
    vi.mocked(post).mockResolvedValueOnce({
      data: mockInvoice
    } as any)

    await store.createInvoice(newInvoice)

    expect(store.invoices).toHaveLength(1)
    expect(store.invoices[0]).toEqual(mockInvoice)
  })

  it('should change invoice status', async () => {
    const store = useInvoiceStore()

    const existingInvoice: Invoice = {
      id: '1',
      invoiceNumber: 'INV-2024-001',
      customerId: 'cust-1',
      customerName: 'John Doe',
      invoiceType: 'RECURRING' as InvoiceType,
      invoiceTypeDisplayName: 'Recurring',
      status: 'DRAFT' as InvoiceStatus,
      statusDisplayName: 'Draft',
      issueDate: '2024-01-01',
      dueDate: '2024-01-31',
      paidDate: null,
      billingPeriodStart: '2024-01-01',
      billingPeriodEnd: '2024-01-31',
      subtotal: 100.00,
      discountAmount: 0,
      taxAmount: 23.00,
      totalAmount: 123.00,
      currency: 'PLN',
      paymentTerms: 14,
      lateFee: 0,
      notes: '',
      pdfUrl: null,
      sentToEmail: 'john@example.com',
      sentAt: null,
      isUnpaid: true,
      isOverdue: false,
      isPaid: false,
      canBeCancelled: true,
      itemCount: 1,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      version: 1
    }

    store.invoices = [existingInvoice]

    const statusData = {
      id: '1',
      status: 'ISSUED' as InvoiceStatus
    }

    const mockUpdatedInvoice: Invoice = {
      ...existingInvoice,
      status: 'ISSUED' as InvoiceStatus,
      statusDisplayName: 'Issued',
      sentAt: '2024-01-02T00:00:00Z',
      updatedAt: '2024-01-02T00:00:00Z',
      version: 2
    }

    const { put } = mockUseApi()
    vi.mocked(put).mockResolvedValueOnce({
      data: mockUpdatedInvoice
    } as any)

    await store.changeInvoiceStatus(statusData)

    expect(store.invoices[0].status).toBe('ISSUED')
    expect(store.invoices[0].version).toBe(2)
  })

  it('should get unpaid invoices', async () => {
    const store = useInvoiceStore()
    const mockInvoices: Invoice[] = [
      {
        id: '1',
        invoiceNumber: 'INV-2024-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'RECURRING' as InvoiceType,
        invoiceTypeDisplayName: 'Recurring',
        status: 'ISSUED' as InvoiceStatus,
        statusDisplayName: 'Issued',
        issueDate: '2024-01-01',
        dueDate: '2024-01-31',
        paidDate: null,
        billingPeriodStart: '2024-01-01',
        billingPeriodEnd: '2024-01-31',
        subtotal: 100.00,
        discountAmount: 0,
        taxAmount: 23.00,
        totalAmount: 123.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'john@example.com',
        sentAt: '2024-01-02T00:00:00Z',
        isUnpaid: true,
        isOverdue: false,
        isPaid: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockInvoices,
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

    await store.getUnpaidInvoices()

    expect(get).toHaveBeenCalledWith('/invoices', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        unpaid: true
      }
    })
  })

  it('should get overdue invoices', async () => {
    const store = useInvoiceStore()
    const mockInvoices: Invoice[] = [
      {
        id: '1',
        invoiceNumber: 'INV-2024-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'RECURRING' as InvoiceType,
        invoiceTypeDisplayName: 'Recurring',
        status: 'OVERDUE' as InvoiceStatus,
        statusDisplayName: 'Overdue',
        issueDate: '2023-12-01',
        dueDate: '2023-12-31',
        paidDate: null,
        billingPeriodStart: '2023-12-01',
        billingPeriodEnd: '2023-12-31',
        subtotal: 100.00,
        discountAmount: 0,
        taxAmount: 23.00,
        totalAmount: 123.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'john@example.com',
        sentAt: '2023-12-02T00:00:00Z',
        isUnpaid: true,
        isOverdue: true,
        isPaid: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2023-12-01T00:00:00Z',
        updatedAt: '2023-12-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockInvoices,
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

    await store.getOverdueInvoices()

    expect(get).toHaveBeenCalledWith('/invoices', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        overdue: true
      }
    })
  })

  it('should compute filtered invoices correctly', () => {
    const store = useInvoiceStore()

    const mockInvoices: Invoice[] = [
      {
        id: '1',
        invoiceNumber: 'INV-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'RECURRING' as InvoiceType,
        invoiceTypeDisplayName: 'Recurring',
        status: 'ISSUED' as InvoiceStatus,
        statusDisplayName: 'Issued',
        issueDate: '2024-01-01',
        dueDate: '2024-01-31',
        paidDate: null,
        billingPeriodStart: '2024-01-01',
        billingPeriodEnd: '2024-01-31',
        subtotal: 100.00,
        discountAmount: 0,
        taxAmount: 23.00,
        totalAmount: 123.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'john@example.com',
        sentAt: null,
        isUnpaid: true,
        isOverdue: false,
        isPaid: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        invoiceNumber: 'INV-002',
        customerId: 'cust-2',
        customerName: 'Jane Doe',
        invoiceType: 'ONE_TIME' as InvoiceType,
        invoiceTypeDisplayName: 'One Time',
        status: 'PAID' as InvoiceStatus,
        statusDisplayName: 'Paid',
        issueDate: '2024-01-01',
        dueDate: '2024-01-31',
        paidDate: '2024-01-15',
        billingPeriodStart: '2024-01-01',
        billingPeriodEnd: '2024-01-31',
        subtotal: 200.00,
        discountAmount: 0,
        taxAmount: 46.00,
        totalAmount: 246.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'jane@example.com',
        sentAt: null,
        isUnpaid: false,
        isOverdue: false,
        isPaid: true,
        canBeCancelled: false,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '3',
        invoiceNumber: 'INV-003',
        customerId: 'cust-3',
        customerName: 'Bob Doe',
        invoiceType: 'ADJUSTMENT' as InvoiceType,
        invoiceTypeDisplayName: 'Adjustment',
        status: 'OVERDUE' as InvoiceStatus,
        statusDisplayName: 'Overdue',
        issueDate: '2023-12-01',
        dueDate: '2023-12-31',
        paidDate: null,
        billingPeriodStart: '2023-12-01',
        billingPeriodEnd: '2023-12-31',
        subtotal: 50.00,
        discountAmount: 0,
        taxAmount: 11.50,
        totalAmount: 61.50,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'bob@example.com',
        sentAt: null,
        isUnpaid: true,
        isOverdue: true,
        isPaid: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2023-12-01T00:00:00Z',
        updatedAt: '2023-12-01T00:00:00Z',
        version: 1
      }
    ]

    store.invoices = mockInvoices

    expect(store.draftInvoices).toHaveLength(0)
    expect(store.issuedInvoices).toHaveLength(1)
    expect(store.sentInvoices).toHaveLength(0)
    expect(store.paidInvoices).toHaveLength(1)
    expect(store.overdueInvoices).toHaveLength(1)
    expect(store.cancelledInvoices).toHaveLength(0)
    expect(store.unpaidInvoices).toHaveLength(2)
    expect(store.recurringInvoices).toHaveLength(1)
    expect(store.oneTimeInvoices).toHaveLength(1)
    expect(store.adjustmentInvoices).toHaveLength(1)
  })

  it('should calculate total outstanding amount', () => {
    const store = useInvoiceStore()

    const mockInvoices: Invoice[] = [
      {
        id: '1',
        invoiceNumber: 'INV-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'RECURRING' as InvoiceType,
        invoiceTypeDisplayName: 'Recurring',
        status: 'ISSUED' as InvoiceStatus,
        statusDisplayName: 'Issued',
        issueDate: '2024-01-01',
        dueDate: '2024-01-31',
        paidDate: null,
        billingPeriodStart: '2024-01-01',
        billingPeriodEnd: '2024-01-31',
        subtotal: 100.00,
        discountAmount: 0,
        taxAmount: 23.00,
        totalAmount: 123.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'john@example.com',
        sentAt: null,
        isUnpaid: true,
        isOverdue: false,
        isPaid: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      },
      {
        id: '2',
        invoiceNumber: 'INV-002',
        customerId: 'cust-2',
        customerName: 'Jane Doe',
        invoiceType: 'ONE_TIME' as InvoiceType,
        invoiceTypeDisplayName: 'One Time',
        status: 'PAID' as InvoiceStatus,
        statusDisplayName: 'Paid',
        issueDate: '2024-01-01',
        dueDate: '2024-01-31',
        paidDate: '2024-01-15',
        billingPeriodStart: '2024-01-01',
        billingPeriodEnd: '2024-01-31',
        subtotal: 200.00,
        discountAmount: 0,
        taxAmount: 46.00,
        totalAmount: 246.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'jane@example.com',
        sentAt: null,
        isUnpaid: false,
        isOverdue: false,
        isPaid: true,
        canBeCancelled: false,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    store.invoices = mockInvoices

    expect(store.totalOutstandingAmount).toBe(123.00)
  })

  it('should get invoices by customer', async () => {
    const store = useInvoiceStore()
    const mockInvoices: Invoice[] = [
      {
        id: '1',
        invoiceNumber: 'INV-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'RECURRING' as InvoiceType,
        invoiceTypeDisplayName: 'Recurring',
        status: 'ISSUED' as InvoiceStatus,
        statusDisplayName: 'Issued',
        issueDate: '2024-01-01',
        dueDate: '2024-01-31',
        paidDate: null,
        billingPeriodStart: '2024-01-01',
        billingPeriodEnd: '2024-01-31',
        subtotal: 100.00,
        discountAmount: 0,
        taxAmount: 23.00,
        totalAmount: 123.00,
        currency: 'PLN',
        paymentTerms: 14,
        lateFee: 0,
        notes: '',
        pdfUrl: null,
        sentToEmail: 'john@example.com',
        sentAt: null,
        isUnpaid: true,
        isOverdue: false,
        isPaid: false,
        canBeCancelled: true,
        itemCount: 1,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
        version: 1
      }
    ]

    const { get } = mockUseApi()
    vi.mocked(get).mockResolvedValueOnce({
      data: {
        content: mockInvoices,
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

    await store.getInvoicesByCustomer('cust-1')

    expect(get).toHaveBeenCalledWith('/invoices', {
      query: {
        page: 0,
        size: 20,
        sort: 'createdAt,desc',
        customerId: 'cust-1'
      }
    })
  })
})
