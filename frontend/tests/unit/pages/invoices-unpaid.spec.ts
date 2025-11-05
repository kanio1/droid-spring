import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import InvoicesUnpaid from '~/pages/invoices/unpaid.vue'

const mockFetchInvoices = vi.fn()
const mockChangeInvoiceStatus = vi.fn()

vi.mock('~/stores/invoice', () => ({
  useInvoiceStore: () => ({
    unpaidInvoices: [
      {
        id: '1',
        invoiceNumber: 'INV-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'ONE_TIME',
        status: 'ISSUED',
        totalAmount: 1000,
        currency: 'USD',
        dueDate: '2024-01-15',
        createdAt: '2024-01-01'
      }
    ],
    overdueInvoices: [],
    totalOutstandingAmount: 1000,
    totalOverdueAmount: 0,
    loading: false,
    pagination: {
      page: 0,
      size: 20,
      totalElements: 1
    },
    fetchInvoices: mockFetchInvoices,
    changeInvoiceStatus: mockChangeInvoiceStatus
  })
}))

vi.mock('~/composables/useToast', () => ({
  useToast: () => ({
    showToast: vi.fn()
  })
}))

vi.mock('~/schemas/invoice', () => ({
  formatCurrency: (amount: number, currency: string) => `${amount} ${currency}`,
  isInvoiceOverdue: () => false,
  canSendInvoice: () => true
}))

describe('InvoicesUnpaid', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the page title', () => {
    const wrapper = mount(InvoicesUnpaid)
    expect(wrapper.find('h1.page-title').text()).toBe('Unpaid Invoices')
  })

  it('displays summary cards', () => {
    const wrapper = mount(InvoicesUnpaid)
    expect(wrapper.find('.summary-cards').exists()).toBe(true)
    expect(wrapper.findAll('.summary-card')).toHaveLength(4)
  })

  it('displays outstanding amount', () => {
    const wrapper = mount(InvoicesUnpaid)
    expect(wrapper.text()).toContain('1000 USD')
  })

  it('has quick filter button', () => {
    const wrapper = mount(InvoicesUnpaid)
    expect(wrapper.find('button').text()).toContain('Overdue Only')
  })

  it('fetches unpaid invoices on mount', () => {
    mount(InvoicesUnpaid)
    expect(mockFetchInvoices).toHaveBeenCalledWith({ unpaid: true })
  })
})
