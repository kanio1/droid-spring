import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { id: '1' }
  })
}))

const mockFetchPaymentById = vi.fn()
const mockChangePaymentStatus = vi.fn()

vi.mock('~/stores/payment', () => ({
  usePaymentStore: () => ({
    currentPayment: {
      id: '1',
      paymentNumber: 'PAY-001',
      customerId: 'cust-1',
      customerName: 'John Doe',
      paymentMethod: 'CARD',
      paymentStatus: 'COMPLETED',
      amount: 1000,
      currency: 'USD',
      paymentDate: '2024-01-01',
      transactionId: 'TXN-12345',
      reference: 'REF-001',
      gatewayResponse: {
        gateway: 'Stripe',
        status: 'SUCCESS',
        authCode: 'AUTH123',
        lastFour: '1234'
      },
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1
    },
    loading: false,
    fetchPaymentById: mockFetchPaymentById,
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

import PaymentsDetail from '~/pages/payments/[id].vue'

describe('PaymentsDetail', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the payment number', () => {
    const wrapper = mount(PaymentsDetail)
    expect(wrapper.find('h1.page-title').text()).toContain('Payment PAY-001')
  })

  it('displays payment summary', () => {
    const wrapper = mount(PaymentsDetail)
    expect(wrapper.find('.payment-summary').exists()).toBe(true)
  })

  it('displays payment details', () => {
    const wrapper = mount(PaymentsDetail)
    expect(wrapper.find('.payment-details').exists()).toBe(true)
    expect(wrapper.text()).toContain('Stripe')
  })

  it('displays billing address if present', () => {
    const wrapper = mount(PaymentsDetail)
    expect(wrapper.find('.billing-address').exists()).toBe(false)
  })

  it('fetches payment on mount', () => {
    mount(PaymentsDetail)
    expect(mockFetchPaymentById).toHaveBeenCalledWith('1')
  })
})
