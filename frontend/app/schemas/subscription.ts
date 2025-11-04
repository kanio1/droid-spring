import { z } from 'zod'

// Subscription Enums
export const subscriptionStatusEnum = z.enum(['ACTIVE', 'SUSPENDED', 'CANCELLED', 'EXPIRED'])

// Subscription Entity
export const subscriptionSchema = z.object({
  id: z.string().uuid(),
  subscriptionNumber: z.string().min(3, 'Subscription number must be at least 3 characters').max(50, 'Subscription number must not exceed 50 characters'),
  customerId: z.string().uuid(),
  productId: z.string().uuid(),
  orderId: z.string().uuid().optional().nullable(),
  status: subscriptionStatusEnum,
  startDate: z.string().date(),
  endDate: z.string().date().optional().nullable(),
  billingStart: z.string().date(),
  nextBillingDate: z.string().date().optional().nullable(),
  billingPeriod: z.string().min(1, 'Billing period is required').max(20, 'Billing period must not exceed 20 characters'),
  price: z.number().min(0, 'Price must be positive'),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  discountAmount: z.number().min(0, 'Discount must be positive').default(0),
  netAmount: z.number().min(0, 'Net amount must be positive').optional().nullable(),
  configuration: z.record(z.any()).optional().nullable(),
  autoRenew: z.boolean().default(true),
  renewalNoticeSent: z.boolean().default(false),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  createdBy: z.string().optional(),
  updatedBy: z.string().optional(),
  version: z.number()
})

// Create Subscription Command
export const createSubscriptionSchema = z.object({
  subscriptionNumber: z.string().min(3, 'Subscription number must be at least 3 characters').max(50, 'Subscription number must not exceed 50 characters'),
  customerId: z.string().uuid('Invalid customer ID'),
  productId: z.string().uuid('Invalid product ID'),
  orderId: z.string().uuid('Invalid order ID').optional().nullable(),
  status: subscriptionStatusEnum.default('ACTIVE'),
  startDate: z.string().date(),
  endDate: z.string().date().optional().or(z.literal('')),
  billingStart: z.string().date(),
  nextBillingDate: z.string().date().optional().or(z.literal('')),
  billingPeriod: z.string().min(1, 'Billing period is required').max(20, 'Billing period must not exceed 20 characters'),
  price: z.number().min(0, 'Price must be positive'),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  discountAmount: z.number().min(0, 'Discount must be positive').default(0),
  netAmount: z.number().min(0, 'Net amount must be positive').optional().nullable(),
  configuration: z.record(z.any()).optional(),
  autoRenew: z.boolean().default(true)
})

// Update Subscription Command
export const updateSubscriptionSchema = z.object({
  id: z.string().uuid(),
  endDate: z.string().date().optional().or(z.literal('')),
  nextBillingDate: z.string().date().optional().or(z.literal('')),
  price: z.number().min(0, 'Price must be positive').optional(),
  discountAmount: z.number().min(0, 'Discount must be positive').default(0),
  netAmount: z.number().min(0, 'Net amount must be positive').optional().nullable(),
  configuration: z.record(z.any()).optional(),
  autoRenew: z.boolean().optional(),
  version: z.number()
})

// Change Subscription Status Command
export const changeSubscriptionStatusSchema = z.object({
  id: z.string().uuid(),
  status: subscriptionStatusEnum
})

// Subscription Search Params
export const subscriptionSearchSchema = z.object({
  searchTerm: z.string().optional(),
  status: subscriptionStatusEnum.optional(),
  customerId: z.string().uuid().optional(),
  productId: z.string().uuid().optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Subscription List Response
export const subscriptionListResponseSchema = z.object({
  content: z.array(subscriptionSchema),
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
export type Subscription = z.infer<typeof subscriptionSchema>
export type CreateSubscriptionCommand = z.infer<typeof createSubscriptionSchema>
export type UpdateSubscriptionCommand = z.infer<typeof updateSubscriptionSchema>
export type ChangeSubscriptionStatusCommand = z.infer<typeof changeSubscriptionStatusSchema>
export type SubscriptionSearchParams = z.infer<typeof subscriptionSearchSchema>
export type SubscriptionListResponse = z.infer<typeof subscriptionListResponseSchema>
export type SubscriptionStatus = z.infer<typeof subscriptionStatusEnum>

// Status labels
export const SUBSCRIPTION_STATUS_LABELS: Record<SubscriptionStatus, string> = {
  ACTIVE: 'Active',
  SUSPENDED: 'Suspended',
  CANCELLED: 'Cancelled',
  EXPIRED: 'Expired'
}

export const SUBSCRIPTION_STATUS_COLORS: Record<SubscriptionStatus, string> = {
  ACTIVE: 'success',
  SUSPENDED: 'warning',
  CANCELLED: 'danger',
  EXPIRED: 'neutral'
}

// Utility functions
export function isSubscriptionActive(subscription: Subscription): boolean {
  return subscription.status === 'ACTIVE'
}

export function isSubscriptionExpiringSoon(subscription: Subscription, days: number = 30): boolean {
  if (!subscription.nextBillingDate) return false

  const nextBilling = new Date(subscription.nextBillingDate)
  const today = new Date()
  const diffTime = nextBilling.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  return diffDays <= days && diffDays >= 0
}

export function getSubscriptionDaysRemaining(subscription: Subscription): number {
  if (!subscription.endDate) return -1

  const endDate = new Date(subscription.endDate)
  const today = new Date()
  const diffTime = endDate.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  return diffDays
}

export function getStatusVariant(status: SubscriptionStatus): 'success' | 'danger' | 'warning' | 'neutral' {
  return SUBSCRIPTION_STATUS_COLORS[status]
}
