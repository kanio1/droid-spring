/**
 * CloudEvents integration for Payment Store
 * Automatically updates store state when payment events arrive from backend
 */

import type { Payment } from '~/schemas/payment'

export function setupPaymentEventListeners(paymentStore: ReturnType<typeof usePaymentStore>) {
  const {
    onPaymentCreated,
    onPaymentProcessing,
    onPaymentCompleted,
    onPaymentFailed,
    onPaymentRefunded
  } = useCloudEvents()

  // Payment created - add to store
  onPaymentCreated((data) => {
    console.log('Payment created via event:', data.paymentId)
    // Refresh payments list
    paymentStore.fetchPayments()
  })

  // Payment processing - update status
  onPaymentProcessing((data) => {
    console.log('Payment processing via event:', data.paymentId)
    const index = paymentStore.payments.findIndex(p => p.id === data.paymentId)
    if (index !== -1) {
      paymentStore.payments[index] = {
        ...paymentStore.payments[index],
        status: 'PROCESSING',
        updatedAt: data.occurredAt
      }
    }
    if (paymentStore.currentPayment?.id === data.paymentId) {
      paymentStore.currentPayment = {
        ...paymentStore.currentPayment,
        status: 'PROCESSING',
        updatedAt: data.occurredAt
      }
    }
  })

  // Payment completed - update to completed
  onPaymentCompleted((data) => {
    console.log('Payment completed via event:', data.paymentId)
    const index = paymentStore.payments.findIndex(p => p.id === data.paymentId)
    if (index !== -1) {
      paymentStore.payments[index] = {
        ...paymentStore.payments[index],
        status: 'COMPLETED',
        updatedAt: data.occurredAt
      }
    }
    if (paymentStore.currentPayment?.id === data.paymentId) {
      paymentStore.currentPayment = {
        ...paymentStore.currentPayment,
        status: 'COMPLETED',
        updatedAt: data.occurredAt
      }
    }
  })

  // Payment failed - update to failed
  onPaymentFailed((data) => {
    console.log('Payment failed via event:', data.paymentId, data.failureReason)
    const index = paymentStore.payments.findIndex(p => p.id === data.paymentId)
    if (index !== -1) {
      paymentStore.payments[index] = {
        ...paymentStore.payments[index],
        status: 'FAILED',
        errorMessage: data.failureReason,
        updatedAt: data.occurredAt
      }
    }
    if (paymentStore.currentPayment?.id === data.paymentId) {
      paymentStore.currentPayment = {
        ...paymentStore.currentPayment,
        status: 'FAILED',
        errorMessage: data.failureReason,
        updatedAt: data.occurredAt
      }
    }
  })

  // Payment refunded - update to refunded
  onPaymentRefunded((data) => {
    console.log('Payment refunded via event:', data.paymentId, data.refundReason)
    const index = paymentStore.payments.findIndex(p => p.id === data.paymentId)
    if (index !== -1) {
      paymentStore.payments[index] = {
        ...paymentStore.payments[index],
        status: 'REFUNDED',
        updatedAt: data.occurredAt
      }
    }
    if (paymentStore.currentPayment?.id === data.paymentId) {
      paymentStore.currentPayment = {
        ...paymentStore.currentPayment,
        status: 'REFUNDED',
        updatedAt: data.occurredAt
      }
    }
  })
}
