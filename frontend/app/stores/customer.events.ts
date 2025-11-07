/**
 * CloudEvents integration for Customer Store
 * Automatically updates store state when events arrive from backend
 */

import type { Customer } from '~/schemas/customer'

export function setupCustomerEventListeners(customerStore: ReturnType<typeof useCustomerStore>) {
  const { onCustomerCreated, onCustomerUpdated, onCustomerStatusChanged, onCustomerTerminated } = useCloudEvents()

  // Customer created - add to store
  onCustomerCreated((data) => {
    console.log('Customer created via event:', data.customerId)
    // Refresh customers list to get the new customer
    customerStore.fetchCustomers()
  })

  // Customer updated - update in store
  onCustomerUpdated((data) => {
    console.log('Customer updated via event:', data.customerId)
    const index = customerStore.customers.findIndex(c => c.id === data.customerId)
    if (index !== -1) {
      // Update the customer in the list
      customerStore.customers[index] = {
        ...customerStore.customers[index],
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        status: data.status,
        updatedAt: data.occurredAt
      }
    }
    // Update current customer if it's the same
    if (customerStore.currentCustomer?.id === data.customerId) {
      customerStore.currentCustomer = {
        ...customerStore.currentCustomer,
        firstName: data.firstName,
        lastName: data.lastName,
        email: data.email,
        status: data.status,
        updatedAt: data.occurredAt
      }
    }
  })

  // Customer status changed - update in store
  onCustomerStatusChanged((data) => {
    console.log('Customer status changed via event:', data.customerId, data.previousStatus, '->', data.status)
    const index = customerStore.customers.findIndex(c => c.id === data.customerId)
    if (index !== -1) {
      customerStore.customers[index] = {
        ...customerStore.customers[index],
        status: data.status,
        updatedAt: data.occurredAt
      }
    }
    if (customerStore.currentCustomer?.id === data.customerId) {
      customerStore.currentCustomer = {
        ...customerStore.currentCustomer,
        status: data.status,
        updatedAt: data.occurredAt
      }
    }
  })

  // Customer terminated - remove from store
  onCustomerTerminated((data) => {
    console.log('Customer terminated via event:', data.customerId)
    const index = customerStore.customers.findIndex(c => c.id === data.customerId)
    if (index !== -1) {
      customerStore.customers.splice(index, 1)
    }
    if (customerStore.currentCustomer?.id === data.customerId) {
      customerStore.currentCustomer = null
    }
  })
}
