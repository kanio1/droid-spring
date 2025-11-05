import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import InvoicesIndex from '~/pages/invoices/index.vue'

// Mock the store
const mockFetchInvoices = vi.fn()
const mockChangeInvoiceStatus = vi.fn()

vi.mock('~/stores/invoice', () => ({
  useInvoiceStore: () => ({
    invoices: [
      {
        id: '1',
        invoiceNumber: 'INV-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        invoiceType: 'ONE_TIME',
        status: 'DRAFT',
        totalAmount: 1000,
        currency: 'USD',
        dueDate: '2024-02-01',
        createdAt: '2024-01-01'
      }
    ],
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
  canCancelInvoice: () => true,
  canSendInvoice: () => true
}))

describe('InvoicesIndex', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the page title', () => {
    const wrapper = mount(InvoicesIndex)
    expect(wrapper.find('h1.page-title').text()).toBe('Invoices')
  })

  it('renders the create invoice button', () => {
    const wrapper = mount(InvoicesIndex)
    expect(wrapper.find('button[label="Create Invoice"]').exists()).toBe(true)
  })

  it('renders invoice table', () => {
    const wrapper = mount(InvoicesIndex)
    expect(wrapper.findComponent({ name: 'AppTable' }).exists()).toBe(true)
  })

  it('fetches invoices on mount', () => {
    mount(InvoicesIndex)
    expect(mockFetchInvoices).toHaveBeenCalled()
  })

  it('filters by status', async () => {
    const wrapper = mount(InvoicesIndex)
    const statusDropdown = wrapper.findAllComponents({ name: 'Dropdown' })[0]
    await statusDropdown.vm.$emit('change', 'PAID')
    expect(mockFetchInvoices).toHaveBeenCalled()
  })

  it('navigates to invoice details', async () => {
    const mockNavigateTo = vi.fn()
    const wrapper = mount(InvoicesIndex, {
      global: {
        mocks: {
          navigateTo: mockNavigateTo
        }
      }
    })

    const table = wrapper.findComponent({ name: 'AppTable' })
    await table.vm.$emit('row-click', { id: '1' })

    expect(mockNavigateTo).toHaveBeenCalledWith('/invoices/1')
  })
})
