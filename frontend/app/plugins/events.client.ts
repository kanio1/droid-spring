/**
 * Events Plugin - CloudEvents consumer initialization
 * Sets up real-time event listeners for all stores
 */

import { setupCustomerEventListeners } from '~/stores/customer.events'
import { setupPaymentEventListeners } from '~/stores/payment.events'
import { setupInvoiceEventListeners } from '~/stores/invoice.events'
import { setupOrderEventListeners } from '~/stores/order.events'
import { setupServiceEventListeners } from '~/stores/service.events'

export default defineNuxtPlugin(() => {
  // Only run on client side
  if (process.server) {
    return
  }

  const customerStore = useCustomerStore()
  const paymentStore = usePaymentStore()
  const invoiceStore = useInvoiceStore()
  const orderStore = useOrderStore()
  const serviceStore = useServiceStore()

  // Setup event listeners for all stores
  const customerCleanup = setupCustomerEventListeners(customerStore)
  const paymentCleanup = setupPaymentEventListeners(paymentStore)
  const invoiceCleanup = setupInvoiceEventListeners(invoiceStore)
  const orderCleanup = setupOrderEventListeners(orderStore)
  const serviceCleanup = setupServiceEventListeners(serviceStore)

  console.log('[CloudEvents] Real-time event listeners initialized for all stores')

  return {
    provide: {
      events: {
        customer: customerCleanup,
        payment: paymentCleanup,
        invoice: invoiceCleanup,
        order: orderCleanup,
        service: serviceCleanup
      }
    }
  }
})
