import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PaymentsIndex from '~/pages/payments/index.vue'

const mockFetchPayments = vi.fn()
const mockChangePaymentStatus = vi.fn()

vi.mock('~/stores/payment', () => ({
  usePaymentStore: () => ({
    payments: [
      {
        id: '1',
        paymentNumber: 'PAY-001',
        customerId: 'cust-1',
        customerName: 'John Doe',
        paymentMethod: 'CARD',
        paymentStatus: 'COMPLETED',
        amount: 1000,
        currency: 'USD',
        paymentDate: '2024-01-01',
        createdAt: '2024-01-01'
      }
    ],
    loading: false,
    pagination: {
      page: 0,
      size: 20,
      totalElements: 1
    },
    fetchPayments: mockFetchPayments,
    changePaymentStatus: mockChangePaymentStatus
  })
}))

vi.mock('~/composables/useToast', () => ({
  useToast: () => ({
    showToast: vi.fn()
  })
}))

vi.mock('~/schemas/payment', () => ({
  canCancelPayment: () => true,
  canRetryPayment: () => false
}))

describe('PaymentsIndex', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the page title', () => {
    const wrapper = mount(PaymentsIndex)
    expect(wrapper.find('h1.page-title').text()).toBe('Payments')
  })

  it('renders the create payment button', () => {
    const wrapper = mount(PaymentsIndex)
    expect(wrapper.find('button[label="Create Payment"]').exists()).toBe(true)
  })

  it('renders payment table', () => {
    const wrapper = mount(PaymentsIndex)
    expect(wrapper.findComponent({ name: 'AppTable' }).exists()).toBe(true)
  })

  it('fetches payments on mount', () => {
    mount(PaymentsIndex)
    expect(mockFetchPayments).toHaveBeenCalled()
  })

  it('filters by status', async () => {
    const wrapper = mount(PaymentsIndex)
    const statusDropdown = wrapper.findAllComponents({ name: 'Dropdown' })[0]
    await statusDropdown.vm.$emit('change', 'COMPLETED')
    expect(mockFetchPayments).toHaveBeenCalled()
  })

  it('navigates to payment details', async () => {
    const mockNavigateTo = vi.fn()
    const wrapper = mount(PaymentsIndex, {
      global: {
        mocks: {
          navigateTo: mockNavigateTo
        }
      }
    })

    const table = wrapper.findComponent({ name: 'AppTable' })
    await table.vm.$emit('row-click', { id: '1' })

    expect(mockNavigateTo).toHaveBeenCalledWith('/payments/1')
  })
})
