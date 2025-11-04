import { z } from 'zod'

// Invoice Enums
export const invoiceTypeEnum = z.enum(['RECURRING', 'ONE_TIME', 'ADJUSTMENT', 'CREDIT_NOTE', 'DEBIT_NOTE'])
export const invoiceStatusEnum = z.enum(['DRAFT', 'ISSUED', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED'])

// Invoice Entity
export const invoiceSchema = z.object({
  id: z.string().uuid(),
  invoiceNumber: z.string().min(3, 'Invoice number must be at least 3 characters').max(50, 'Invoice number must not exceed 50 characters'),
  customerId: z.string().uuid(),
  customerName: z.string().optional(),
  invoiceType: invoiceTypeEnum,
  invoiceTypeDisplayName: z.string(),
  status: invoiceStatusEnum,
  statusDisplayName: z.string(),
  issueDate: z.string().date(),
  dueDate: z.string().date(),
  paidDate: z.string().date().optional().nullable(),
  billingPeriodStart: z.string().date().optional().nullable(),
  billingPeriodEnd: z.string().date().optional().nullable(),
  subtotal: z.number().min(0, 'Subtotal must be positive'),
  discountAmount: z.number().min(0, 'Discount must be positive').default(0),
  taxAmount: z.number().min(0, 'Tax must be positive'),
  totalAmount: z.number().min(0, 'Total must be positive'),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  paymentTerms: z.number().min(0, 'Payment terms must be positive').default(14),
  lateFee: z.number().min(0, 'Late fee must be positive').default(0),
  notes: z.string().optional(),
  pdfUrl: z.string().url('Invalid PDF URL').optional().nullable(),
  sentToEmail: z.string().email('Invalid email').optional().nullable(),
  sentAt: z.string().datetime().optional().nullable(),
  isUnpaid: z.boolean(),
  isOverdue: z.boolean(),
  isPaid: z.boolean(),
  canBeCancelled: z.boolean(),
  itemCount: z.number().min(0),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  createdBy: z.string().optional(),
  updatedBy: z.string().optional(),
  version: z.number()
})

// Create Invoice Command
export const createInvoiceSchema = z.object({
  invoiceNumber: z.string().min(3, 'Invoice number must be at least 3 characters').max(50, 'Invoice number must not exceed 50 characters'),
  customerId: z.string().uuid('Invalid customer ID'),
  invoiceType: invoiceTypeEnum,
  issueDate: z.string().date(),
  dueDate: z.string().date(),
  billingPeriodStart: z.string().date().optional().or(z.literal('')),
  billingPeriodEnd: z.string().date().optional().or(z.literal('')),
  subtotal: z.number().min(0, 'Subtotal must be positive'),
  discountAmount: z.number().min(0, 'Discount must be positive').default(0),
  taxAmount: z.number().min(0, 'Tax must be positive'),
  totalAmount: z.number().min(0, 'Total must be positive'),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  paymentTerms: z.number().min(0, 'Payment terms must be positive').default(14),
  lateFee: z.number().min(0, 'Late fee must be positive').default(0),
  notes: z.string().optional(),
  sentToEmail: z.string().email('Invalid email').optional()
})

// Update Invoice Status Command
export const changeInvoiceStatusSchema = z.object({
  id: z.string().uuid(),
  status: invoiceStatusEnum
})

// Invoice Search Params
export const invoiceSearchSchema = z.object({
  query: z.string().optional(),
  status: invoiceStatusEnum.optional(),
  type: invoiceTypeEnum.optional(),
  customerId: z.string().uuid().optional(),
  startDate: z.string().date().optional(),
  endDate: z.string().date().optional(),
  unpaid: z.boolean().optional(),
  overdue: z.boolean().optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Invoice List Response
export const invoiceListResponseSchema = z.object({
  content: z.array(invoiceSchema),
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
export type Invoice = z.infer<typeof invoiceSchema>
export type CreateInvoiceCommand = z.infer<typeof createInvoiceSchema>
export type ChangeInvoiceStatusCommand = z.infer<typeof changeInvoiceStatusSchema>
export type InvoiceSearchParams = z.infer<typeof invoiceSearchSchema>
export type InvoiceListResponse = z.infer<typeof invoiceListResponseSchema>
export type InvoiceType = z.infer<typeof invoiceTypeEnum>
export type InvoiceStatus = z.infer<typeof invoiceStatusEnum>

// Status labels
export const INVOICE_TYPE_LABELS: Record<InvoiceType, string> = {
  RECURRING: 'Recurring',
  ONE_TIME: 'One Time',
  ADJUSTMENT: 'Adjustment',
  CREDIT_NOTE: 'Credit Note',
  DEBIT_NOTE: 'Debit Note'
}

export const INVOICE_STATUS_LABELS: Record<InvoiceStatus, string> = {
  DRAFT: 'Draft',
  ISSUED: 'Issued',
  SENT: 'Sent',
  PAID: 'Paid',
  OVERDUE: 'Overdue',
  CANCELLED: 'Cancelled'
}

export const INVOICE_STATUS_COLORS: Record<InvoiceStatus, string> = {
  DRAFT: 'neutral',
  ISSUED: 'info',
  SENT: 'info',
  PAID: 'success',
  OVERDUE: 'danger',
  CANCELLED: 'danger'
}

// Utility functions
export function calculateDaysOverdue(invoice: Invoice): number {
  if (!invoice.isOverdue || invoice.isPaid) return 0

  const dueDate = new Date(invoice.dueDate)
  const today = new Date()
  const diffTime = today.getTime() - dueDate.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  return Math.max(0, diffDays)
}

export function getInvoiceAge(invoice: Invoice): number {
  const issueDate = new Date(invoice.issueDate)
  const today = new Date()
  const diffTime = today.getTime() - issueDate.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  return diffDays
}

export function isInvoicePaid(invoice: Invoice): boolean {
  return invoice.status === 'PAID' || invoice.isPaid
}

export function isInvoiceOverdue(invoice: Invoice): boolean {
  if (invoice.isPaid) return false

  const dueDate = new Date(invoice.dueDate)
  const today = new Date()

  return today > dueDate
}

export function getStatusVariant(status: InvoiceStatus): 'success' | 'danger' | 'info' | 'neutral' {
  return INVOICE_STATUS_COLORS[status]
}
