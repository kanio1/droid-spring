import { z } from 'zod'

// Payment Enums
export const paymentMethodEnum = z.enum(['CARD', 'CREDIT_CARD', 'BANK_TRANSFER', 'CASH', 'DIRECT_DEBIT', 'MOBILE_PAY'])
export const paymentStatusEnum = z.enum(['PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED'])

// Payment Entity
export const paymentSchema = z.object({
  id: z.string().uuid(),
  paymentNumber: z.string().min(3, 'Payment number must be at least 3 characters').max(50, 'Payment number must not exceed 50 characters'),
  customerId: z.string().uuid(),
  invoiceId: z.string().uuid().optional().nullable(),
  amount: z.number().min(0, 'Amount must be positive'),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  paymentMethod: paymentMethodEnum,
  paymentMethodDisplayName: z.string(),
  paymentStatus: paymentStatusEnum,
  paymentStatusDisplayName: z.string(),
  transactionId: z.string().optional(),
  gateway: z.string().optional(),
  paymentDate: z.string().date(),
  receivedDate: z.string().date().optional().nullable(),
  referenceNumber: z.string().optional(),
  notes: z.string().optional(),
  reversalReason: z.string().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  createdBy: z.string().optional(),
  updatedBy: z.string().optional(),
  version: z.number()
})

// Create Payment Command
export const createPaymentSchema = z.object({
  paymentNumber: z.string().min(3, 'Payment number must be at least 3 characters').max(50, 'Payment number must not exceed 50 characters'),
  customerId: z.string().uuid('Invalid customer ID'),
  invoiceId: z.string().uuid('Invalid invoice ID').optional().nullable(),
  amount: z.number().min(0, 'Amount must be positive'),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  paymentMethod: paymentMethodEnum,
  paymentDate: z.string().date(),
  transactionId: z.string().optional(),
  gateway: z.string().optional(),
  referenceNumber: z.string().optional(),
  notes: z.string().optional()
})

// Change Payment Status Command
export const changePaymentStatusSchema = z.object({
  id: z.string().uuid(),
  paymentStatus: paymentStatusEnum
})

// Payment Search Params
export const paymentSearchSchema = z.object({
  searchTerm: z.string().optional(),
  status: paymentStatusEnum.optional(),
  customerId: z.string().uuid().optional(),
  invoiceId: z.string().uuid().optional(),
  paymentMethod: paymentMethodEnum.optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Payment List Response
export const paymentListResponseSchema = z.object({
  content: z.array(paymentSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
  first: z.boolean(),
  last: z.boolean(),
  numberOfElements: z.number(),
  empty: z.boolean()
})

// Export types
export type Payment = z.infer<typeof paymentSchema>
export type CreatePaymentCommand = z.infer<typeof createPaymentSchema>
export type ChangePaymentStatusCommand = z.infer<typeof changePaymentStatusSchema>
export type PaymentSearchParams = z.infer<typeof paymentSearchSchema>
export type PaymentListResponse = z.infer<typeof paymentListResponseSchema>
export type PaymentMethod = z.infer<typeof paymentMethodEnum>
export type PaymentStatus = z.infer<typeof paymentStatusEnum>

// Status labels
export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CARD: 'Card',
  CREDIT_CARD: 'Credit Card',
  BANK_TRANSFER: 'Bank Transfer',
  CASH: 'Cash',
  DIRECT_DEBIT: 'Direct Debit',
  MOBILE_PAY: 'Mobile Pay'
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Pending',
  PROCESSING: 'Processing',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  REFUNDED: 'Refunded'
}

export const PAYMENT_STATUS_COLORS: Record<PaymentStatus, string> = {
  PENDING: 'warning',
  PROCESSING: 'info',
  COMPLETED: 'success',
  FAILED: 'danger',
  REFUNDED: 'neutral'
}

// Utility functions
export function getPaymentIcon(method: PaymentMethod): string {
  const icons: Record<PaymentMethod, string> = {
    CARD: '💳',
    CREDIT_CARD: '💳',
    BANK_TRANSFER: '🏦',
    CASH: '💵',
    DIRECT_DEBIT: '🔄',
    MOBILE_PAY: '📱'
  }
  return icons[method]
}

export function isPaymentCompleted(payment: Payment): boolean {
  return payment.paymentStatus === 'COMPLETED'
}

export function isPaymentFailed(payment: Payment): boolean {
  return payment.paymentStatus === 'FAILED'
}

export function canBeRefunded(payment: Payment): boolean {
  return payment.paymentStatus === 'COMPLETED'
}

export function getStatusVariant(status: PaymentStatus): 'success' | 'danger' | 'warning' | 'info' | 'neutral' {
  return PAYMENT_STATUS_COLORS[status]
}
