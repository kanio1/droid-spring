import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { id: '1' }
  })
}))

const mockFetchInvoiceById = vi.fn()
const mockChangeInvoiceStatus = vi.fn()

vi.mock('~/stores/invoice', () => ({
  useInvoiceStore: () => ({
    currentInvoice: {
      id: '1',
      invoiceNumber: 'INV-001',
      customerId: 'cust-1',
      customerName: 'John Doe',
      invoiceType: 'ONE_TIME',
      status: 'DRAFT',
      totalAmount: 1000,
      subtotal: 850,
      taxRate: 23,
      taxAmount: 150,
      currency: 'USD',
      invoiceDate: '2024-01-01',
      dueDate: '2024-02-01',
      items: [
        {
          description: 'Service A',
          quantity: 1,
          unitPrice: 850,
          total: 850
        }
      ],
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1
    },
    loading: false,
    fetchInvoiceById: mockFetchInvoiceById,
    changeInvoiceStatus: mockChangeInvoiceStatus
  })
}))

vi.mock('~/composables/useToast', () => ({
  useToast: () => ({
    showToast: vi.fn()
  })
}))

vi.mock('~/schemas/invoice', () => ({
  isInvoiceOverdue: () => false,
  canCancelInvoice: () => true,
  canSendInvoice: () => true
}))

import InvoicesDetail from '~/pages/invoices/[id].vue'

describe('InvoicesDetail', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the invoice number', () => {
    const wrapper = mount(InvoicesDetail)
    expect(wrapper.find('h1.page-title').text()).toContain('Invoice INV-001')
  })

  it('displays invoice summary', () => {
    const wrapper = mount(InvoicesDetail)
    expect(wrapper.find('.invoice-summary').exists()).toBe(true)
  })

  it('displays invoice items', () => {
    const wrapper = mount(InvoicesDetail)
    expect(wrapper.find('.invoice-items').exists()).toBe(true)
  })

  it('displays totals', () => {
    const wrapper = mount(InvoicesDetail)
    expect(wrapper.find('.invoice-totals').exists()).toBe(true)
    expect(wrapper.text()).toContain('1000 USD')
  })

  it('fetches invoice on mount', () => {
    mount(InvoicesDetail)
    expect(mockFetchInvoiceById).toHaveBeenCalledWith('1')
  })
})
