import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import OrdersIndex from '~/pages/orders/index.vue'

// Mock the store
const mockFetchOrders = vi.fn()
const mockSearchOrders = vi.fn()
const mockGetOrdersByStatus = vi.fn()
const mockUpdateOrderStatus = vi.fn()

vi.mock('~/stores/order', () => ({
  useOrderStore: () => ({
    orders: [
      {
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
        createdAt: '2024-01-01'
      }
    ],
    loading: false,
    pagination: {
      page: 0,
      size: 20,
      totalElements: 1
    },
    fetchOrders: mockFetchOrders,
    searchOrders: mockSearchOrders,
    getOrdersByStatus: mockGetOrdersByStatus,
    updateOrderStatus: mockUpdateOrderStatus
  })
}))

// Mock the composables
vi.mock('~/composables/useToast', () => ({
  useToast: () => ({
    showToast: vi.fn()
  })
}))

vi.mock('~/schemas/order', () => ({
  formatCurrency: (amount: number, currency: string) => `${amount} ${currency}`,
  isOrderActive: () => true,
  canCancelOrder: () => true
}))

describe('OrdersIndex', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders the page title', () => {
    const wrapper = mount(OrdersIndex)
    expect(wrapper.find('h1.page-title').text()).toBe('Orders')
  })

  it('renders the create order button', () => {
    const wrapper = mount(OrdersIndex)
    expect(wrapper.find('button[label="Create Order"]').exists()).toBe(true)
  })

  it('renders the search input', () => {
    const wrapper = mount(OrdersIndex)
    expect(wrapper.find('input[placeholder="Search orders..."]').exists()).toBe(true)
  })

  it('renders order filters', () => {
    const wrapper = mount(OrdersIndex)
    expect(wrapper.find('div.payments-filters').exists()).toBe(true)
  })

  it('fetches orders on mount', async () => {
    mount(OrdersIndex)
    expect(mockFetchOrders).toHaveBeenCalled()
  })

  it('calls search when search term changes', async () => {
    const wrapper = mount(OrdersIndex)
    const searchInput = wrapper.find('input[placeholder="Search orders..."]')

    await searchInput.setValue('test')

    // Wait for debounce
    await new Promise(resolve => setTimeout(resolve, 350))

    expect(mockSearchOrders).toHaveBeenCalledWith('test', expect.any(Object))
  })

  it('renders order table', () => {
    const wrapper = mount(OrdersIndex)
    expect(wrapper.findComponent({ name: 'AppTable' }).exists()).toBe(true)
  })

  it('renders order data in table', () => {
    const wrapper = mount(OrdersIndex)
    const table = wrapper.findComponent({ name: 'AppTable' })
    expect(table.props().data).toHaveLength(1)
    expect(table.props().data[0].orderNumber).toBe('ORD-001')
  })

  it('navigates to create order page when button is clicked', async () => {
    const wrapper = mount(OrdersIndex, {
      global: {
        mocks: {
          navigateTo: vi.fn()
        }
      }
    })

    await wrapper.find('button[label="Create Order"]').trigger('click')
    expect(wrapper.vm.navigateTo).toHaveBeenCalledWith('/orders/create')
  })

  it('navigates to order details when order is clicked', async () => {
    const mockNavigateTo = vi.fn()
    const wrapper = mount(OrdersIndex, {
      global: {
        mocks: {
          navigateTo: mockNavigateTo
        }
      }
    })

    const table = wrapper.findComponent({ name: 'AppTable' })
    await table.vm.$emit('row-click', { id: '1' })

    expect(mockNavigateTo).toHaveBeenCalledWith('/orders/1')
  })

  it('handles order status filter change', async () => {
    const wrapper = mount(OrdersIndex)
    const statusDropdown = wrapper.findAllComponents({ name: 'Dropdown' })[0]

    await statusDropdown.vm.$emit('change', 'PENDING')

    expect(mockFetchOrders).toHaveBeenCalled()
  })

  it('displays empty state when no orders', () => {
    const wrapper = mount(OrdersIndex, {
      global: {
        mocks: {
          '$pinia': {
            state: {
              order: {
                orders: []
              }
            }
          }
        }
      }
    })

    expect(wrapper.find('.empty-state').exists()).toBe(true)
    expect(wrapper.find('.empty-state__title').text()).toBe('No orders found')
  })
})
