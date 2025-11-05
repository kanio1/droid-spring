import { z } from 'zod'

// Order Enums
export const orderTypeEnum = z.enum(['NEW', 'UPGRADE', 'DOWNGRADE', 'CANCEL', 'RENEW'])
export const orderStatusEnum = z.enum(['PENDING', 'CONFIRMED', 'PROCESSING', 'COMPLETED', 'CANCELLED'])
export const orderPriorityEnum = z.enum(['LOW', 'NORMAL', 'HIGH', 'URGENT'])

// Order Entity
export const orderSchema = z.object({
  id: z.string().uuid(),
  orderNumber: z.string().min(3, 'Order number must be at least 3 characters').max(50, 'Order number must not exceed 50 characters'),
  customerId: z.string().uuid(),
  customerName: z.string().optional(),
  orderType: orderTypeEnum,
  orderTypeDisplayName: z.string(),
  status: orderStatusEnum,
  statusDisplayName: z.string(),
  priority: orderPriorityEnum,
  priorityDisplayName: z.string(),
  totalAmount: z.number().min(0, 'Total amount must be positive').optional().nullable(),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  requestedDate: z.string().date().optional().nullable(),
  promisedDate: z.string().date().optional().nullable(),
  completedDate: z.string().date().optional().nullable(),
  orderChannel: z.string().optional(),
  salesRepId: z.string().optional(),
  salesRepName: z.string().optional(),
  notes: z.string().optional(),
  isPending: z.boolean(),
  isCompleted: z.boolean(),
  canBeCancelled: z.boolean(),
  itemCount: z.number().min(0),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  createdBy: z.string().optional(),
  updatedBy: z.string().optional(),
  version: z.number()
})

// Create Order Command
export const createOrderSchema = z.object({
  orderNumber: z.string().min(3, 'Order number must be at least 3 characters').max(50, 'Order number must not exceed 50 characters'),
  customerId: z.string().uuid('Invalid customer ID'),
  orderType: orderTypeEnum,
  status: orderStatusEnum.default('PENDING'),
  priority: orderPriorityEnum.default('NORMAL'),
  totalAmount: z.number().min(0, 'Total amount must be positive').optional().nullable(),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  requestedDate: z.string().date().optional().or(z.literal('')),
  promisedDate: z.string().date().optional().or(z.literal('')),
  orderChannel: z.string().optional(),
  salesRepId: z.string().optional(),
  notes: z.string().max(2000, 'Notes must not exceed 2000 characters').optional()
})

// Update Order Status Command
export const updateOrderStatusSchema = z.object({
  id: z.string().uuid(),
  status: orderStatusEnum
})

// Order Search Params
export const orderSearchSchema = z.object({
  searchTerm: z.string().optional(),
  status: orderStatusEnum.optional(),
  type: orderTypeEnum.optional(),
  customerId: z.string().uuid().optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Order List Response
export const orderListResponseSchema = z.object({
  content: z.array(orderSchema),
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
export type Order = z.infer<typeof orderSchema>
export type CreateOrderCommand = z.infer<typeof createOrderSchema>
export type UpdateOrderStatusCommand = z.infer<typeof updateOrderStatusSchema>
export type OrderSearchParams = z.infer<typeof orderSearchSchema>
export type OrderListResponse = z.infer<typeof orderListResponseSchema>
export type OrderType = z.infer<typeof orderTypeEnum>
export type OrderStatus = z.infer<typeof orderStatusEnum>
export type OrderPriority = z.infer<typeof orderPriorityEnum>

// Status labels
export const ORDER_TYPE_LABELS: Record<OrderType, string> = {
  NEW: 'New',
  UPGRADE: 'Upgrade',
  DOWNGRADE: 'Downgrade',
  CANCEL: 'Cancel',
  RENEW: 'Renew'
}

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  PROCESSING: 'Processing',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled'
}

export const ORDER_PRIORITY_LABELS: Record<OrderPriority, string> = {
  LOW: 'Low',
  NORMAL: 'Normal',
  HIGH: 'High',
  URGENT: 'Urgent'
}

export const ORDER_STATUS_COLORS: Record<OrderStatus, string> = {
  PENDING: 'warning',
  CONFIRMED: 'info',
  PROCESSING: 'info',
  COMPLETED: 'success',
  CANCELLED: 'danger'
}

export const ORDER_PRIORITY_COLORS: Record<OrderPriority, string> = {
  LOW: 'neutral',
  NORMAL: 'info',
  HIGH: 'warning',
  URGENT: 'danger'
}

// Utility functions
export function getOrderProgress(order: Order): number {
  const statusOrder = ['PENDING', 'CONFIRMED', 'PROCESSING', 'COMPLETED']
  const currentIndex = statusOrder.indexOf(order.status)
  return ((currentIndex + 1) / statusOrder.length) * 100
}

export function isOrderActive(order: Order): boolean {
  return ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(order.status)
}

export function canCancelOrder(order: Order): boolean {
  return order.canBeCancelled && isOrderActive(order)
}

export function getStatusVariant(status: OrderStatus): 'success' | 'warning' | 'info' | 'danger' {
  return ORDER_STATUS_COLORS[status]
}

export function getPriorityVariant(priority: OrderPriority): 'neutral' | 'info' | 'warning' | 'danger' {
  return ORDER_PRIORITY_COLORS[priority]
}

export function formatCurrency(amount: number | null | undefined): string {
  if (amount === null || amount === undefined) return '---'
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'PLN',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}
