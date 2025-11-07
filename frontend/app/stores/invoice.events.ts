/**
 * CloudEvents integration for Invoice Store
 * Automatically updates store state when invoice events arrive from backend
 */

import type { Invoice } from '~/schemas/invoice'

export function setupInvoiceEventListeners(invoiceStore: ReturnType<typeof useInvoiceStore>) {
  const {
    onInvoiceCreated,
    onInvoicePaid,
    onInvoiceOverdue
  } = useCloudEvents()

  // Invoice created - add to store
  onInvoiceCreated((data) => {
    console.log('Invoice created via event:', data.invoiceId)
    // Refresh invoices list
    invoiceStore.fetchInvoices()
  })

  // Invoice paid - update status
  onInvoicePaid((data) => {
    console.log('Invoice paid via event:', data.invoiceId)
    const index = invoiceStore.invoices.findIndex(i => i.id === data.invoiceId)
    if (index !== -1) {
      invoiceStore.invoices[index] = {
        ...invoiceStore.invoices[index],
        status: 'PAID',
        updatedAt: data.occurredAt
      }
    }
    if (invoiceStore.currentInvoice?.id === data.invoiceId) {
      invoiceStore.currentInvoice = {
        ...invoiceStore.currentInvoice,
        status: 'PAID',
        updatedAt: data.occurredAt
      }
    }
  })

  // Invoice overdue - update status
  onInvoiceOverdue((data) => {
    console.log('Invoice overdue via event:', data.invoiceId)
    const index = invoiceStore.invoices.findIndex(i => i.id === data.invoiceId)
    if (index !== -1) {
      invoiceStore.invoices[index] = {
        ...invoiceStore.invoices[index],
        status: 'OVERDUE',
        updatedAt: data.occurredAt
      }
    }
    if (invoiceStore.currentInvoice?.id === data.invoiceId) {
      invoiceStore.currentInvoice = {
        ...invoiceStore.currentInvoice,
        status: 'OVERDUE',
        updatedAt: data.occurredAt
      }
    }
  })
}
