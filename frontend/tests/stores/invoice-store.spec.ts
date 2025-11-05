/**
 * InvoiceStore Tests
 *
 * Comprehensive test coverage for invoice store
 * Tests status tracking, payment updates, PDF generation, and invoice lifecycle
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useInvoiceStore } from '@/stores/invoice'
import type { Invoice, InvoiceStatus, Payment } from '@/types/invoice'

// Mock API client
vi.mock('@/services/api', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn()
  }
}))

// Mock WebSocket
vi.mock('@/services/websocket', () => ({
  websocket: {
    connect: vi.fn(),
    disconnect: vi.fn(),
    emit: vi.fn(),
    on: vi.fn(),
    off: vi.fn(),
    isConnected: vi.fn().mockReturnValue(true)
  }
}))

// Mock PDF Service
vi.mock('@/services/pdf', () => ({
  pdfService: {
    generateInvoice: vi.fn(),
    downloadInvoice: vi.fn(),
    sendInvoice: vi.fn()
  }
}))

const mockInvoice: Invoice = {
  id: 'inv-123',
  invoiceNumber: 'INV-2024-001',
  customerId: 'cust-123',
  customerName: 'John Doe',
  status: 'PENDING' as InvoiceStatus,
  subtotal: 1000.00,
  tax: 230.00,
  total: 1230.00,
  currency: 'USD',
  dueDate: '2024-12-05T00:00:00Z',
  issuedDate: '2024-11-05T10:00:00Z',
  items: [
    {
      productId: 'prod-001',
      productName: '5G Router',
      quantity: 2,
      unitPrice: 500.00,
      total: 1000.00
    }
  ],
  payments: [],
  notes: 'Thank you for your business'
}

const mockInvoices: Invoice[] = [
  {
    id: 'inv-123',
    invoiceNumber: 'INV-2024-001',
    customerId: 'cust-123',
    customerName: 'John Doe',
    status: 'PENDING' as InvoiceStatus,
    subtotal: 1000.00,
    tax: 230.00,
    total: 1230.00,
    currency: 'USD',
    dueDate: '2024-12-05T00:00:00Z',
    issuedDate: '2024-11-05T10:00:00Z',
    items: [],
    payments: []
  },
  {
    id: 'inv-456',
    invoiceNumber: 'INV-2024-002',
    customerId: 'cust-456',
    customerName: 'Jane Smith',
    status: 'PAID' as InvoiceStatus,
    subtotal: 599.99,
    tax: 138.00,
    total: 737.99,
    currency: 'USD',
    dueDate: '2024-11-15T00:00:00Z',
    issuedDate: '2024-11-01T10:00:00Z',
    items: [],
    payments: [
      {
        id: 'pay-001',
        amount: 737.99,
        method: 'CARD',
        status: 'COMPLETED',
        date: '2024-11-02T10:00:00Z'
      }
    ]
  }
]

describe('InvoiceStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const invoiceStore = useInvoiceStore()

      expect(invoiceStore.invoices).toEqual([])
      expect(invoiceStore.currentInvoice).toBeNull()
      expect(invoiceStore.totalCount).toBe(0)
      expect(invoiceStore.currentPage).toBe(1)
      expect(invoiceStore.pageSize).toBe(20)
      expect(invoiceStore.filters).toEqual({
        status: [],
        dateFrom: null,
        dateTo: null,
        customerId: null,
        minAmount: null,
        maxAmount: null,
        overdue: null
      })
      expect(invoiceStore.searchQuery).toBe('')
      expect(invoiceStore.sortBy).toBe('issuedDate')
      expect(invoiceStore.sortOrder).toBe('desc')
      expect(invoiceStore.isLoading).toBe(false)
      expect(invoiceStore.error).toBeNull()
      expect(invoiceStore.selectedInvoices).toEqual([])
      expect(invoiceStore.realTimeUpdates).toBe(true)
    })
  })

  describe('fetchInvoices', () => {
    it('should fetch invoices successfully', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: {
          content: mockInvoices,
          totalElements: 2,
          totalPages: 1,
          number: 0,
          size: 20
        }
      })

      await invoiceStore.fetchInvoices()

      expect(apiClient.get).toHaveBeenCalledWith('/api/invoices', {
        params: expect.objectContaining({
          page: 1,
          size: 20,
          sortBy: 'issuedDate',
          sortOrder: 'desc'
        })
      })

      expect(invoiceStore.invoices).toEqual(mockInvoices)
      expect(invoiceStore.totalCount).toBe(2)
      expect(invoiceStore.error).toBeNull()
    })

    it('should respect filters when fetching', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      invoiceStore.setFilter('status', ['PENDING', 'PAID'])
      invoiceStore.setFilter('overdue', true)

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockInvoice], totalElements: 1 }
      })

      await invoiceStore.fetchInvoices()

      expect(apiClient.get).toHaveBeenCalledWith('/api/invoices', {
        params: expect.objectContaining({
          status: ['PENDING', 'PAID'],
          overdue: true
        })
      })
    })

    it('should handle API errors', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('API error'))

      await invoiceStore.fetchInvoices()

      expect(invoiceStore.error).toBe('Failed to fetch invoices: API error')
      expect(invoiceStore.invoices).toEqual([])
    })
  })

  describe('fetchInvoice', () => {
    it('should fetch single invoice successfully', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({ data: mockInvoice })

      const invoice = await invoiceStore.fetchInvoice('inv-123')

      expect(apiClient.get).toHaveBeenCalledWith('/api/invoices/inv-123')
      expect(invoice).toEqual(mockInvoice)
      expect(invoiceStore.currentInvoice).toEqual(mockInvoice)
    })

    it('should handle not found error', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('Not found'))

      const invoice = await invoiceStore.fetchInvoice('inv-999')

      expect(invoice).toBeNull()
      expect(invoiceStore.error).toBe('Invoice not found')
    })
  })

  describe('createInvoice', () => {
    it('should create invoice successfully', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      const newInvoice = {
        customerId: 'cust-123',
        items: [
          {
            productId: 'prod-001',
            quantity: 2,
            unitPrice: 500.00
          }
        ],
        dueDate: '2024-12-05',
        notes: 'Thank you'
      }

      vi.mocked(apiClient.post).mockResolvedValue({ data: mockInvoice })

      const invoice = await invoiceStore.createInvoice(newInvoice)

      expect(apiClient.post).toHaveBeenCalledWith('/api/invoices', newInvoice)
      expect(invoice).toEqual(mockInvoice)
      expect(invoiceStore.invoices).toContain(mockInvoice)
    })

    it('should calculate invoice totals automatically', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      const invoiceData = {
        customerId: 'cust-123',
        items: [
          { productId: 'prod-001', quantity: 2, unitPrice: 100 },
          { productId: 'prod-002', quantity: 3, unitPrice: 50 }
        ],
        dueDate: '2024-12-05'
      }

      const expectedInvoice = {
        ...mockInvoice,
        subtotal: 350, // 200 + 150
        tax: 80.5, // 23% of 350
        total: 430.5
      }

      vi.mocked(apiClient.post).mockResolvedValue({ data: expectedInvoice })

      await invoiceStore.createInvoice(invoiceData)

      const createdInvoice = await invoiceStore.createInvoice(invoiceData)
      expect(createdInvoice?.subtotal).toBe(350)
      expect(createdInvoice?.tax).toBe(80.5)
      expect(createdInvoice?.total).toBe(430.5)
    })

    it('should validate required fields', async () => {
      const invoiceStore = useInvoiceStore()

      const invalidInvoice = {
        customerId: '',
        items: [],
        dueDate: ''
      }

      await expect(invoiceStore.createInvoice(invalidInvoice))
        .rejects.toThrow('Validation failed')

      expect(invoiceStore.error).toBeDefined()
    })

    it('should validate minimum items requirement', async () => {
      const invoiceStore = useInvoiceStore()

      const invalidInvoice = {
        customerId: 'cust-123',
        items: [], // Empty items
        dueDate: '2024-12-05'
      }

      await expect(invoiceStore.createInvoice(invalidInvoice))
        .rejects.toThrow('Invoice must have at least one item')

      expect(invoiceStore.error).toBeDefined()
    })
  })

  describe('updateInvoiceStatus', () => {
    it('should update invoice status successfully', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      invoiceStore.invoices = [...mockInvoices]
      const updatedInvoice = { ...mockInvoice, status: 'PAID' as InvoiceStatus }

      vi.mocked(apiClient.patch).mockResolvedValue({ data: updatedInvoice })

      const invoice = await invoiceStore.updateInvoiceStatus('inv-123', 'PAID')

      expect(apiClient.patch).toHaveBeenCalledWith('/api/invoices/inv-123/status', {
        status: 'PAID'
      })
      expect(invoice?.status).toBe('PAID')
      expect(invoiceStore.invoices[0].status).toBe('PAID')
    })

    it('should handle invalid status transition', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.patch).mockRejectedValue(new Error('Invalid transition'))

      await expect(invoiceStore.updateInvoiceStatus('inv-123', 'DRAFT'))
        .rejects.toThrow('Invalid transition')
    })

    it('should validate status transition rules', async () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.invoices = [{ ...mockInvoice, status: 'PAID' }]

      // Cannot change status from PAID back to PENDING
      await expect(invoiceStore.updateInvoiceStatus('inv-123', 'PENDING'))
        .rejects.toThrow('Cannot change status from PAID to PENDING')
    })

    it('should update with optimistic update', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      invoiceStore.invoices = [...mockInvoices]
      const originalStatus = invoiceStore.invoices[0].status

      vi.mocked(apiClient.patch).mockResolvedValue({ data: { ...mockInvoice, status: 'PAID' } })

      await invoiceStore.updateInvoiceStatus('inv-123', 'PAID', { optimistic: true })

      expect(invoiceStore.invoices[0].status).toBe('PAID')
    })
  })

  describe('sendInvoice', () => {
    it('should send invoice successfully', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.post).mockResolvedValue({ data: { success: true } })

      await invoiceStore.sendInvoice('inv-123', { method: 'EMAIL' })

      expect(apiClient.post).toHaveBeenCalledWith('/api/invoices/inv-123/send', {
        method: 'EMAIL'
      })
    })

    it('should send invoice with custom message', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      const customMessage = 'Please find your invoice attached'
      vi.mocked(apiClient.post).mockResolvedValue({ data: { success: true } })

      await invoiceStore.sendInvoice('inv-123', {
        method: 'EMAIL',
        message: customMessage
      })

      expect(apiClient.post).toHaveBeenCalledWith('/api/invoices/inv-123/send', {
        method: 'EMAIL',
        message: customMessage
      })
    })

    it('should not send paid invoice', async () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.invoices = [{ ...mockInvoice, status: 'PAID' }]

      await expect(invoiceStore.sendInvoice('inv-123', { method: 'EMAIL' }))
        .rejects.toThrow('Cannot send paid invoice')
    })
  })

  describe('generatePDF', () => {
    it('should generate PDF successfully', async () => {
      const invoiceStore = useInvoiceStore()
      const { pdfService } = await import('@/services/pdf')

      vi.mocked(pdfService.generateInvoice).mockResolvedValue({
        url: '/pdfs/inv-123.pdf',
        downloadUrl: '/downloads/inv-123.pdf'
      })

      const result = await invoiceStore.generatePDF('inv-123')

      expect(pdfService.generateInvoice).toHaveBeenCalledWith('inv-123')
      expect(result.url).toBe('/pdfs/inv-123.pdf')
    })

    it('should download PDF directly', async () => {
      const invoiceStore = useInvoiceStore()
      const { pdfService } = await import('@/services/pdf')

      vi.mocked(pdfService.downloadInvoice).mockResolvedValue(undefined)

      await invoiceStore.downloadPDF('inv-123')

      expect(pdfService.downloadInvoice).toHaveBeenCalledWith('inv-123')
    })
  })

  describe('Payment Processing', () => {
    it('should add payment to invoice', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      const payment: Payment = {
        id: 'pay-001',
        amount: 1230.00,
        method: 'CARD',
        status: 'PENDING',
        date: '2024-11-05T10:00:00Z'
      }

      const updatedInvoice = {
        ...mockInvoice,
        status: 'PENDING',
        payments: [payment]
      }

      vi.mocked(apiClient.post).mockResolvedValue({ data: updatedInvoice })

      const result = await invoiceStore.addPayment('inv-123', payment)

      expect(apiClient.post).toHaveBeenCalledWith('/api/invoices/inv-123/payments', payment)
      expect(result?.payments).toContainEqual(payment)
    })

    it('should mark invoice as paid when full payment received', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      const payment: Payment = {
        id: 'pay-001',
        amount: 1230.00,
        method: 'CARD',
        status: 'COMPLETED',
        date: '2024-11-05T10:00:00Z'
      }

      const updatedInvoice = {
        ...mockInvoice,
        status: 'PAID',
        payments: [payment]
      }

      vi.mocked(apiClient.post).mockResolvedValue({ data: updatedInvoice })

      const result = await invoiceStore.addPayment('inv-123', payment)

      expect(result?.status).toBe('PAID')
    })

    it('should handle partial payments', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      const payment: Payment = {
        id: 'pay-001',
        amount: 500.00,
        method: 'BANK_TRANSFER',
        status: 'COMPLETED',
        date: '2024-11-05T10:00:00Z'
      }

      vi.mocked(apiClient.post).mockResolvedValue({
        data: { ...mockInvoice, payments: [payment], status: 'PARTIALLY_PAID' }
      })

      const result = await invoiceStore.addPayment('inv-123', payment)

      expect(result?.status).toBe('PARTIALLY_PAID')
      expect(result?.payments).toHaveLength(1)
    })

    it('should validate payment amount', async () => {
      const invoiceStore = useInvoiceStore()

      const invalidPayment: Payment = {
        id: 'pay-001',
        amount: -100, // Negative amount
        method: 'CARD',
        status: 'COMPLETED',
        date: '2024-11-05T10:00:00Z'
      }

      await expect(invoiceStore.addPayment('inv-123', invalidPayment))
        .rejects.toThrow('Payment amount must be greater than 0')
    })

    it('should process refund', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.post).mockResolvedValue({
        data: { ...mockInvoice, status: 'PARTIALLY_REFUNDED' }
      })

      const result = await invoiceStore.refundPayment('inv-123', 'pay-001', 100.00)

      expect(apiClient.post).toHaveBeenCalledWith('/api/invoices/inv-123/payments/pay-001/refund', {
        amount: 100.00
      })
      expect(result?.status).toBe('PARTIALLY_REFUNDED')
    })
  })

  describe('Filtering', () => {
    it('should set individual filter', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setFilter('status', ['PENDING', 'OVERDUE'])

      expect(invoiceStore.filters.status).toEqual(['PENDING', 'OVERDUE'])
    })

    it('should set multiple filters', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setFilters({
        status: ['PENDING'],
        minAmount: 100,
        overdue: true
      })

      expect(invoiceStore.filters.status).toEqual(['PENDING'])
      expect(invoiceStore.filters.minAmount).toBe(100)
      expect(invoiceStore.filters.overdue).toBe(true)
    })

    it('should clear specific filter', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setFilters({ status: ['PENDING'] })
      invoiceStore.clearFilter('status')

      expect(invoiceStore.filters.status).toEqual([])
    })

    it('should clear all filters', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setFilters({
        status: ['PENDING'],
        minAmount: 100,
        customerId: 'cust-123',
        overdue: true
      })

      invoiceStore.clearAllFilters()

      expect(invoiceStore.filters).toEqual({
        status: [],
        dateFrom: null,
        dateTo: null,
        customerId: null,
        minAmount: null,
        maxAmount: null,
        overdue: null
      })
    })

    it('should filter by overdue status', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setFilter('overdue', true)

      expect(invoiceStore.filters.overdue).toBe(true)
    })

    it('should filter by date range', () => {
      const invoiceStore = useInvoiceStore()

      const dateFrom = new Date('2024-11-01')
      const dateTo = new Date('2024-11-30')

      invoiceStore.setFilter('dateFrom', dateFrom)
      invoiceStore.setFilter('dateTo', dateTo)

      expect(invoiceStore.filters.dateFrom).toBe(dateFrom)
      expect(invoiceStore.filters.dateTo).toBe(dateTo)
    })
  })

  describe('Search', () => {
    it('should set search query', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setSearch('INV-2024-001')

      expect(invoiceStore.searchQuery).toBe('INV-2024-001')
    })

    it('should search by invoice number', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      invoiceStore.setSearch('INV-2024-001')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockInvoice], totalElements: 1 }
      })

      await invoiceStore.fetchInvoices()

      expect(apiClient.get).toHaveBeenCalledWith('/api/invoices', {
        params: expect.objectContaining({
          search: 'INV-2024-001'
        })
      })
    })

    it('should search by customer name', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      invoiceStore.setSearch('John Doe')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [mockInvoice], totalElements: 1 }
      })

      await invoiceStore.fetchInvoices()

      expect(apiClient.get).toHaveBeenCalledWith('/api/invoices', {
        params: expect.objectContaining({
          search: 'John Doe'
        })
      })
    })

    it('should clear search', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setSearch('test')
      invoiceStore.clearSearch()

      expect(invoiceStore.searchQuery).toBe('')
    })
  })

  describe('Sorting', () => {
    it('should set sort parameters', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setSorting('total', 'asc')

      expect(invoiceStore.sortBy).toBe('total')
      expect(invoiceStore.sortOrder).toBe('asc')
    })

    it('should toggle sort order', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setSorting('total', 'asc')
      invoiceStore.toggleSort('total')

      expect(invoiceStore.sortOrder).toBe('desc')
    })

    it('should change sort field', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setSorting('issuedDate', 'desc')
      invoiceStore.setSorting('dueDate', 'asc')

      expect(invoiceStore.sortBy).toBe('dueDate')
      expect(invoiceStore.sortOrder).toBe('asc')
    })
  })

  describe('Pagination', () => {
    it('should set current page', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setPage(3)

      expect(invoiceStore.currentPage).toBe(3)
    })

    it('should set page size', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setPageSize(50)

      expect(invoiceStore.pageSize).toBe(50)
      expect(invoiceStore.currentPage).toBe(1)
    })

    it('should navigate to next page', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setPage(1)
      invoiceStore.nextPage()

      expect(invoiceStore.currentPage).toBe(2)
    })

    it('should navigate to previous page', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setPage(3)
      invoiceStore.prevPage()

      expect(invoiceStore.currentPage).toBe(2)
    })

    it('should calculate total pages', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.totalCount = 100
      invoiceStore.pageSize = 20

      expect(invoiceStore.totalPages).toBe(5)
    })
  })

  describe('Real-time Updates', () => {
    it('should enable real-time updates', async () => {
      const invoiceStore = useInvoiceStore()
      const { websocket } = await import('@/services/websocket')

      invoiceStore.enableRealtimeUpdates()

      expect(invoiceStore.realTimeUpdates).toBe(true)
      expect(websocket.connect).toHaveBeenCalled()
    })

    it('should disable real-time updates', async () => {
      const invoiceStore = useInvoiceStore()
      const { websocket } = await import('@/services/websocket')

      invoiceStore.disableRealtimeUpdates()

      expect(invoiceStore.realTimeUpdates).toBe(false)
      expect(websocket.disconnect).toHaveBeenCalled()
    })

    it('should handle payment update via WebSocket', async () => {
      const invoiceStore = useInvoiceStore()
      const { websocket } = await import('@/services/websocket')

      invoiceStore.invoices = [...mockInvoices]

      vi.mocked(websocket.on).mockImplementation((event, callback) => {
        if (event === 'payment:received') {
          callback({
            invoiceId: 'inv-123',
            payment: {
              id: 'pay-002',
              amount: 615.00,
              method: 'BANK_TRANSFER',
              status: 'COMPLETED'
            }
          })
        }
      })

      invoiceStore.enableRealtimeUpdates()

      expect(invoiceStore.invoices[0].payments).toHaveLength(1)
    })

    it('should handle status update via WebSocket', async () => {
      const invoiceStore = useInvoiceStore()
      const { websocket } = await import('@/services/websocket')

      invoiceStore.invoices = [...mockInvoices]

      vi.mocked(websocket.on).mockImplementation((event, callback) => {
        if (event === 'invoice:updated') {
          callback({
            id: 'inv-123',
            status: 'PAID'
          })
        }
      })

      invoiceStore.enableRealtimeUpdates()

      expect(invoiceStore.invoices[0].status).toBe('PENDING')
    })
  })

  describe('Invoice Statistics', () => {
    it('should calculate invoice statistics', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockResolvedValue({
        data: {
          total: 100,
          pending: 20,
          overdue: 10,
          paid: 65,
          cancelled: 5,
          totalRevenue: 75000,
          averageAmount: 750,
          averagePaymentTime: 5.5
        }
      })

      const stats = await invoiceStore.fetchStatistics()

      expect(stats.total).toBe(100)
      expect(stats.pending).toBe(20)
      expect(stats.overdue).toBe(10)
      expect(stats.totalRevenue).toBe(75000)
      expect(stats.averageAmount).toBe(750)
    })

    it('should get status counts', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.invoices = [
        { ...mockInvoice, status: 'PENDING' },
        { ...mockInvoice, status: 'PENDING' },
        { ...mockInvoice, status: 'PAID' }
      ]

      const counts = invoiceStore.getStatusCounts()

      expect(counts.PENDING).toBe(2)
      expect(counts.PAID).toBe(1)
    })

    it('should calculate overdue invoices', () => {
      const invoiceStore = useInvoiceStore()

      const overdueInvoice = {
        ...mockInvoice,
        status: 'PENDING' as InvoiceStatus,
        dueDate: '2024-10-01T00:00:00Z' // Past date
      }

      invoiceStore.invoices = [overdueInvoice]

      const overdue = invoiceStore.getOverdueInvoices()

      expect(overdue).toHaveLength(1)
    })
  })

  describe('Bulk Operations', () => {
    it('should select multiple invoices', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.selectInvoices(['inv-123', 'inv-456'])

      expect(invoiceStore.selectedInvoices).toEqual(['inv-123', 'inv-456'])
    })

    it('should deselect all invoices', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.selectedInvoices = ['inv-123', 'inv-456']
      invoiceStore.deselectAll()

      expect(invoiceStore.selectedInvoices).toEqual([])
    })

    it('should bulk update status', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      invoiceStore.invoices = [...mockInvoices]
      invoiceStore.selectedInvoices = ['inv-123', 'inv-456']

      vi.mocked(apiClient.patch).mockResolvedValue({})

      await invoiceStore.bulkUpdateStatus('SENT')

      expect(apiClient.patch).toHaveBeenCalledWith('/api/invoices/batch/status', {
        invoiceIds: ['inv-123', 'inv-456'],
        status: 'SENT'
      })
    })

    it('should bulk send invoices', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      invoiceStore.selectedInvoices = ['inv-123']

      vi.mocked(apiClient.post).mockResolvedValue({ success: true })

      await invoiceStore.bulkSend({ method: 'EMAIL' })

      expect(apiClient.post).toHaveBeenCalledWith('/api/invoices/batch/send', {
        invoiceIds: ['inv-123'],
        method: 'EMAIL'
      })
    })

    it('should bulk generate PDFs', async () => {
      const invoiceStore = useInvoiceStore()
      const { pdfService } = await import('@/services/pdf')

      invoiceStore.selectedInvoices = ['inv-123', 'inv-456']

      vi.mocked(pdfService.generateInvoice).mockImplementation((id) => {
        return Promise.resolve({ url: `/pdfs/${id}.pdf` })
      })

      const results = await invoiceStore.bulkGeneratePDF()

      expect(pdfService.generateInvoice).toHaveBeenCalledTimes(2)
      expect(results).toHaveLength(2)
    })
  })

  describe('Computed Properties', () => {
    it('should return filtered invoices', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.invoices = [...mockInvoices]
      invoiceStore.setFilter('status', ['PENDING'])

      const filtered = invoiceStore.filteredInvoices

      expect(filtered.every(i => i.status === 'PENDING')).toBe(true)
    })

    it('should return sorted invoices', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.invoices = [...mockInvoices]
      invoiceStore.setSorting('total', 'desc')

      const sorted = invoiceStore.sortedInvoices

      expect(sorted[0].totalAmount).toBeGreaterThanOrEqual(sorted[1].totalAmount)
    })

    it('should return paged invoices', () => {
      const invoiceStore = useInvoiceStore()

      const manyInvoices = Array.from({ length: 50 }, (_, i) => ({
        ...mockInvoice,
        id: `inv-${i}`,
        total: i * 100
      }))

      invoiceStore.invoices = manyInvoices
      invoiceStore.currentPage = 1
      invoiceStore.pageSize = 20

      const paged = invoiceStore.pagedInvoices

      expect(paged).toHaveLength(20)
    })

    it('should check if has next page', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.totalCount = 50
      invoiceStore.currentPage = 2
      invoiceStore.pageSize = 20

      expect(invoiceStore.hasNextPage).toBe(false)
    })

    it('should return selected invoices data', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.invoices = [...mockInvoices]
      invoiceStore.selectedInvoices = ['inv-123', 'inv-456']

      const selected = invoiceStore.selectedInvoicesData

      expect(selected).toHaveLength(2)
    })

    it('should calculate total amount of selected invoices', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.invoices = [...mockInvoices]
      invoiceStore.selectedInvoices = ['inv-123', 'inv-456']

      const total = invoiceStore.selectedTotalAmount

      expect(total).toBe(1967.99) // 1230 + 737.99
    })
  })

  describe('Error Handling', () => {
    it('should set error state', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.setError('Test error')

      expect(invoiceStore.error).toBe('Test error')
    })

    it('should clear error', () => {
      const invoiceStore = useInvoiceStore()

      invoiceStore.error = 'Some error'
      invoiceStore.clearError()

      expect(invoiceStore.error).toBeNull()
    })

    it('should handle network errors', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('Network error'))

      await invoiceStore.fetchInvoices()

      expect(invoiceStore.error).toContain('Network error')
    })
  })

  describe('Integration Tests', () => {
    it('should handle complete invoice lifecycle', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      // Create invoice
      vi.mocked(apiClient.post).mockResolvedValue({ data: mockInvoice })
      const created = await invoiceStore.createInvoice({
        customerId: 'cust-123',
        items: [{ productId: 'prod-001', quantity: 1, unitPrice: 100 }],
        dueDate: '2024-12-05'
      })
      expect(created).toEqual(mockInvoice)

      // Send invoice
      vi.mocked(apiClient.post).mockResolvedValue({ data: { success: true } })
      await invoiceStore.sendInvoice('inv-123', { method: 'EMAIL' })

      // Add payment
      const payment = {
        id: 'pay-001',
        amount: 1230.00,
        method: 'CARD',
        status: 'COMPLETED',
        date: '2024-11-05T10:00:00Z'
      }
      vi.mocked(apiClient.post).mockResolvedValue({
        data: { ...mockInvoice, status: 'PAID', payments: [payment] }
      })
      const updated = await invoiceStore.addPayment('inv-123', payment)
      expect(updated?.status).toBe('PAID')
    })

    it('should handle filtering and pagination together', async () => {
      const invoiceStore = useInvoiceStore()
      const { apiClient } = await import('@/services/api')

      invoiceStore.setFilter('status', ['PENDING'])
      invoiceStore.setPage(2)
      invoiceStore.setPageSize(10)

      vi.mocked(apiClient.get).mockResolvedValue({
        data: { content: [], totalElements: 25 }
      })

      await invoiceStore.fetchInvoices()

      expect(apiClient.get).toHaveBeenCalledWith('/api/invoices', {
        params: expect.objectContaining({
          status: ['PENDING'],
          page: 2,
          size: 10
        })
      })
    })

    it('should handle overdue notification workflow', async () => {
      const invoiceStore = useInvoiceStore()

      const overdueInvoice = {
        ...mockInvoice,
        status: 'OVERDUE' as InvoiceStatus,
        dueDate: '2024-10-01T00:00:00Z'
      }
      invoiceStore.invoices = [overdueInvoice]

      const overdue = invoiceStore.getOverdueInvoices()
      expect(overdue).toHaveLength(1)

      vi.mocked(apiClient.post).mockResolvedValue({ success: true })
      await invoiceStore.sendReminder('inv-123')

      expect(invoiceStore.invoices[0].status).toBe('OVERDUE')
    })
  })
})
