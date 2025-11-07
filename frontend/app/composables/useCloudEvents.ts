/**
 * useCloudEvents - CloudEvents consumer composable
 * Handles domain events from backend (Customer, Payment, Invoice, Order, etc.)
 */

import type { CloudEvent } from './useEventSource'

export interface CustomerEventData {
  customerId: string
  firstName: string
  lastName: string
  email: string
  status: string
  occurredAt: string
}

export interface PaymentEventData {
  paymentId: string
  paymentNumber: string
  customerId: string
  invoiceId?: string
  amount: number
  currency: string
  paymentMethod: string
  status: string
  occurredAt: string
}

export interface InvoiceEventData {
  invoiceId: string
  invoiceNumber: string
  customerId: string
  status: string
  totalAmount: number
  currency: string
  occurredAt: string
}

export interface OrderEventData {
  orderId: string
  orderNumber: string
  customerId: string
  status: string
  orderType: string
  priority: string
  occurredAt: string
}

export function useCloudEvents() {
  const config = useRuntimeConfig()
  const { addEventListener, isConnected, connect, disconnect } = useEventSource({
    url: `${config.public.apiBase}/events/stream`,
    eventTypes: [
      'com.droid.bss.customer.*',
      'com.droid.bss.payment.*',
      'com.droid.bss.invoice.*',
      'com.droid.bss.order.*',
      'com.droid.bss.service.*'
    ],
    withCredentials: true,
    autoReconnect: true,
    maxReconnectAttempts: 10
  })

  const toast = useToast()

  // Customer event handlers
  const onCustomerCreated = (callback: (data: CustomerEventData) => void) => {
    return addEventListener('com.droid.bss.customer.created.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
    })
  }

  const onCustomerUpdated = (callback: (data: CustomerEventData) => void) => {
    return addEventListener('com.droid.bss.customer.updated.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
    })
  }

  const onCustomerStatusChanged = (callback: (data: CustomerEventData & { previousStatus: string }) => void) => {
    return addEventListener('com.droid.bss.customer.statusChanged.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
    })
  }

  const onCustomerTerminated = (callback: (data: CustomerEventData & { terminationReason: string }) => void) => {
    return addEventListener('com.droid.bss.customer.terminated.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
    })
  }

  // Payment event handlers
  const onPaymentCreated = (callback: (data: PaymentEventData) => void) => {
    return addEventListener('com.droid.bss.payment.created.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
    })
  }

  const onPaymentProcessing = (callback: (data: PaymentEventData & { transactionId: string }) => void) => {
    return addEventListener('com.droid.bss.payment.processing.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
    })
  }

  const onPaymentCompleted = (callback: (data: PaymentEventData & { receivedDate: string }) => void) => {
    return addEventListener('com.droid.bss.payment.completed.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
      toast.add({
        severity: 'success',
        summary: 'Payment Completed',
        detail: `Payment ${data.data.paymentNumber} of ${data.data.amount} ${data.data.currency} has been processed successfully.`,
        life: 5000
      })
    })
  }

  const onPaymentFailed = (callback: (data: PaymentEventData & { failureReason: string }) => void) => {
    return addEventListener('com.droid.bss.payment.failed.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
      toast.add({
        severity: 'error',
        summary: 'Payment Failed',
        detail: `Payment ${data.data.paymentNumber} failed: ${data.data.failureReason}`,
        life: 8000
      })
    })
  }

  const onPaymentRefunded = (callback: (data: PaymentEventData & { refundReason: string }) => void) => {
    return addEventListener('com.droid.bss.payment.refunded.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
      toast.add({
        severity: 'info',
        summary: 'Payment Refunded',
        detail: `Payment ${data.data.paymentNumber} has been refunded.`,
        life: 5000
      })
    })
  }

  // Invoice event handlers
  const onInvoiceCreated = (callback: (data: InvoiceEventData) => void) => {
    return addEventListener('com.droid.bss.invoice.created.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
    })
  }

  const onInvoicePaid = (callback: (data: InvoiceEventData) => void) => {
    return addEventListener('com.droid.bss.invoice.paid.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
      toast.add({
        severity: 'success',
        summary: 'Invoice Paid',
        detail: `Invoice ${data.data.invoiceNumber} has been paid.`,
        life: 5000
      })
    })
  }

  const onInvoiceOverdue = (callback: (data: InvoiceEventData) => void) => {
    return addEventListener('com.droid.bss.invoice.overdue.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
      toast.add({
        severity: 'warn',
        summary: 'Invoice Overdue',
        detail: `Invoice ${data.data.invoiceNumber} is now overdue.`,
        life: 8000
      })
    })
  }

  // Order event handlers
  const onOrderCreated = (callback: (data: OrderEventData) => void) => {
    return addEventListener('com.droid.bss.order.created.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
    })
  }

  const onOrderCompleted = (callback: (data: OrderEventData) => void) => {
    return addEventListener('com.droid.bss.order.completed.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
      toast.add({
        severity: 'success',
        summary: 'Order Completed',
        detail: `Order ${data.data.orderNumber} has been completed.`,
        life: 5000
      })
    })
  }

  const onOrderFailed = (callback: (data: OrderEventData) => void) => {
    return addEventListener('com.droid.bss.order.failed.v1', (event) => {
      const data = JSON.parse(event.data)
      callback(data.data)
      toast.add({
        severity: 'error',
        summary: 'Order Failed',
        detail: `Order ${data.data.orderNumber} has failed.`,
        life: 8000
      })
    })
  }

  // Generic event handler
  const onAnyEvent = (callback: (event: CloudEvent) => void) => {
    return addEventListener('*', (event) => {
      callback(JSON.parse(event.data))
    })
  }

  return {
    // Connection state
    isConnected,
    connect,
    disconnect,

    // Customer events
    onCustomerCreated,
    onCustomerUpdated,
    onCustomerStatusChanged,
    onCustomerTerminated,

    // Payment events
    onPaymentCreated,
    onPaymentProcessing,
    onPaymentCompleted,
    onPaymentFailed,
    onPaymentRefunded,

    // Invoice events
    onInvoiceCreated,
    onInvoicePaid,
    onInvoiceOverdue,

    // Order events
    onOrderCreated,
    onOrderCompleted,
    onOrderFailed,

    // Generic
    onAnyEvent
  }
}
