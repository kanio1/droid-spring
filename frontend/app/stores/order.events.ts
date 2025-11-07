/**
 * CloudEvents integration for Order Store
 * Automatically updates store state when order events arrive from backend
 */

import type { Order } from '~/schemas/order'

export function setupOrderEventListeners(orderStore: ReturnType<typeof useOrderStore>) {
  const {
    onOrderCreated,
    onOrderCompleted,
    onOrderFailed
  } = useCloudEvents()

  // Order created - add to store
  onOrderCreated((data) => {
    console.log('Order created via event:', data.orderId)
    // Refresh orders list
    orderStore.fetchOrders()
  })

  // Order completed - update status
  onOrderCompleted((data) => {
    console.log('Order completed via event:', data.orderId)
    const index = orderStore.orders.findIndex(o => o.id === data.orderId)
    if (index !== -1) {
      orderStore.orders[index] = {
        ...orderStore.orders[index],
        status: 'COMPLETED',
        updatedAt: data.occurredAt
      }
    }
    if (orderStore.currentOrder?.id === data.orderId) {
      orderStore.currentOrder = {
        ...orderStore.currentOrder,
        status: 'COMPLETED',
        updatedAt: data.occurredAt
      }
    }
  })

  // Order failed - update status
  onOrderFailed((data) => {
    console.log('Order failed via event:', data.orderId)
    const index = orderStore.orders.findIndex(o => o.id === data.orderId)
    if (index !== -1) {
      orderStore.orders[index] = {
        ...orderStore.orders[index],
        status: 'FAILED',
        updatedAt: data.occurredAt
      }
    }
    if (orderStore.currentOrder?.id === data.orderId) {
      orderStore.currentOrder = {
        ...orderStore.currentOrder,
        status: 'FAILED',
        updatedAt: data.occurredAt
      }
    }
  })
}
