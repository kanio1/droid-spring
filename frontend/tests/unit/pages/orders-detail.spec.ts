import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useRoute } from 'vue-router'

// Mock route
vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { id: '1' }
  })
}))

// Mock the store
const mockFetchOrderById = vi.fn()
const mockUpdateOrderStatus = vi.fn()

vi.mock('~/stores/order', () => ({
  useOrderStore: () => ({
    currentOrder: {
      id: '1',
      orderNumber: 'ORD-001',
      customerId: 'cust-1',
      customerName: 'John Doe',
      orderType: 'NEW',
      status: 'PENDING',
      priority: 'HIGH',
      totalAmount: 1000,
      currency: 'USD',
      requestedDate: '2024-01-01',
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1
    },
    loading: false,
    fetchOrderById: mockFetchOrderById,
    updateOrderStatus: mockUpdateOrderStatus
  })
}))

vi.mock('~/composables/useToast', () => ({
  useToast: () => ({
    showToast: vi.fn()
  })
}))

vi.mock('~/schemas/order', () => ({
  isOrderActive: () => true,
  canCancelOrder: () => true
}))

describe('OrderDetail', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the page title', () => {
    const wrapper = mount(OrdersDetail)
    expect(wrapper.find('h1.page-title').text()).toContain('Order ORD-001')
  })

  it('fetches order on mount', () => {
    mount(OrdersDetail)
    expect(mockFetchOrderById).toHaveBeenCalledWith('1')
  })

  it('displays loading state', () => {
    const wrapper = mount(OrdersDetail, {
      global: {
        mocks: {
          '$pinia': {
            state: {
              order: {
                loading: true
              }
            }
          }
        }
      }
    })
    expect(wrapper.find('.loading-state').exists()).toBe(true)
  })

  it('displays order summary card', () => {
    const wrapper = mount(OrdersDetail)
    expect(wrapper.find('.order-summary').exists()).toBe(true)
    expect(wrapper.find('.summary-grid').exists()).toBe(true)
  })

  it('displays customer information', () => {
    const wrapper = mount(OrdersDetail)
    expect(wrapper.text()).toContain('John Doe')
    expect(wrapper.text()).toContain('cust-1')
  })

  it('displays order items table', () => {
    const wrapper = mount(OrdersDetail)
    expect(wrapper.findComponent({ name: 'AppTable' }).exists()).toBe(true)
  })

  it('displays order status badge', () => {
    const wrapper = mount(OrdersDetail)
    expect(wrapper.findComponent({ name: 'StatusBadge' }).props().status).toBe('PENDING')
  })

  it('displays metadata section', () => {
    const wrapper = mount(OrdersDetail)
    expect(wrapper.find('.order-metadata').exists()).toBe(true)
  })
})

// Import the component at the end to ensure mocks are set up
import OrdersDetail from '~/pages/orders/[id].vue'
