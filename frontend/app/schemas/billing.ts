import { z } from 'zod'

// Usage Record Types
export enum UsageType {
  VOICE = 'VOICE',
  SMS = 'SMS',
  DATA = 'DATA',
  SERVICE = 'SERVICE'
}

export enum UsageStatus {
  PENDING = 'PENDING',
  PROCESSED = 'PROCESSED',
  RATED = 'RATED',
  BILLABLE = 'BILLABLE',
  INCLUDED = 'INCLUDED'
}

// Usage Record Entity
export const UsageRecordSchema = z.object({
  id: z.string().uuid(),
  customerId: z.string().uuid(),
  subscriptionId: z.string().uuid().optional(),
  usageType: z.nativeEnum(UsageType),
  usageAmount: z.number().positive(),
  unit: z.string(), // minutes, MB, SMS, etc.
  timestamp: z.string().datetime(),
  source: z.string(), // CDR source identifier
  destination: z.string().optional(),
  isRated: z.boolean(),
  ratingStatus: z.nativeEnum(UsageStatus).optional(),
  ratedAmount: z.number().optional(),
  currency: z.string().length(3),
  cost: z.number().optional(),
  metadata: z.record(z.any()).optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime()
})

export type UsageRecord = z.infer<typeof UsageRecordSchema>

// Create Usage Record Command
export const CreateUsageRecordCommandSchema = z.object({
  customerId: z.string().uuid(),
  subscriptionId: z.string().uuid().optional(),
  usageType: z.nativeEnum(UsageType),
  usageAmount: z.number().positive(),
  unit: z.string(),
  timestamp: z.string().datetime(),
  source: z.string(),
  destination: z.string().optional(),
  currency: z.string().length(3),
  metadata: z.record(z.any()).optional()
})

export type CreateUsageRecordCommand = z.infer<typeof CreateUsageRecordCommandSchema>

// Usage Record Search Params
export const UsageRecordSearchParamsSchema = z.object({
  page: z.number().optional(),
  size: z.number().optional(),
  sort: z.string().optional(),
  unrated: z.boolean().optional(),
  customerId: z.string().uuid().optional(),
  subscriptionId: z.string().uuid().optional(),
  usageType: z.nativeEnum(UsageType).optional(),
  startDate: z.string().datetime().optional(),
  endDate: z.string().datetime().optional()
})

export type UsageRecordSearchParams = z.infer<typeof UsageRecordSearchParamsSchema>

// Usage Record List Response
export const UsageRecordListResponseSchema = z.object({
  content: z.array(UsageRecordSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
  first: z.boolean(),
  last: z.boolean(),
  numberOfElements: z.number(),
  empty: z.boolean()
})

export type UsageRecordListResponse = z.infer<typeof UsageRecordListResponseSchema>

// Billing Cycle Types
export enum BillingCycleStatus {
  PENDING = 'PENDING',
  SCHEDULED = 'SCHEDULED',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

// Billing Cycle Entity
export const BillingCycleSchema = z.object({
  id: z.string().uuid(),
  customerId: z.string().uuid(),
  cycleNumber: z.number(),
  startDate: z.string().datetime(),
  endDate: z.string().datetime(),
  dueDate: z.string().datetime(),
  status: z.nativeEnum(BillingCycleStatus),
  totalUsage: z.number().optional(),
  totalCost: z.number().optional(),
  totalRatedCost: z.number().optional(),
  currency: z.string().length(3),
  invoiceId: z.string().uuid().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime()
})

export type BillingCycle = z.infer<typeof BillingCycleSchema>

// Create Billing Cycle Command
export const CreateBillingCycleCommandSchema = z.object({
  customerId: z.string().uuid(),
  startDate: z.string().datetime(),
  endDate: z.string().datetime(),
  dueDate: z.string().datetime().optional()
})

export type CreateBillingCycleCommand = z.infer<typeof CreateBillingCycleCommandSchema>

// Billing Cycle Search Params
export const BillingCycleSearchParamsSchema = z.object({
  page: z.number().optional(),
  size: z.number().optional(),
  sort: z.string().optional(),
  status: z.nativeEnum(BillingCycleStatus).optional(),
  customerId: z.string().uuid().optional()
})

export type BillingCycleSearchParams = z.infer<typeof BillingCycleSearchParamsSchema>

// Billing Cycle List Response
export const BillingCycleListResponseSchema = z.object({
  content: z.array(BillingCycleSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
  first: z.boolean(),
  last: z.boolean(),
  numberOfElements: z.number(),
  empty: z.boolean()
})

export type BillingCycleListResponse = z.infer<typeof BillingCycleListResponseSchema>

// Utility functions
export const getUsageTypeLabel = (type: UsageType): string => {
  const labels: Record<UsageType, string> = {
    [UsageType.VOICE]: 'Voice',
    [UsageType.SMS]: 'SMS',
    [UsageType.DATA]: 'Data',
    [UsageType.SERVICE]: 'Service'
  }
  return labels[type] || type
}

export const getUsageStatusLabel = (status: UsageStatus): string => {
  const labels: Record<UsageStatus, string> = {
    [UsageStatus.PENDING]: 'Pending',
    [UsageStatus.PROCESSED]: 'Processed',
    [UsageStatus.RATED]: 'Rated',
    [UsageStatus.BILLABLE]: 'Billable',
    [UsageStatus.INCLUDED]: 'Included'
  }
  return labels[status] || status
}

export const getBillingCycleStatusLabel = (status: BillingCycleStatus): string => {
  const labels: Record<BillingCycleStatus, string> = {
    [BillingCycleStatus.PENDING]: 'Pending',
    [BillingCycleStatus.SCHEDULED]: 'Scheduled',
    [BillingCycleStatus.PROCESSING]: 'Processing',
    [BillingCycleStatus.COMPLETED]: 'Completed',
    [BillingCycleStatus.FAILED]: 'Failed',
    [BillingCycleStatus.CANCELLED]: 'Cancelled'
  }
  return labels[status] || status
}

export const formatUsageAmount = (amount: number, unit: string): string => {
  // Format large numbers for data usage
  if (unit.toLowerCase().includes('mb') || unit.toLowerCase().includes('gb')) {
    if (amount >= 1024) {
      return `${(amount / 1024).toFixed(2)} GB`
    }
    return `${amount} MB`
  }

  // Format duration for voice/SMS
  if (unit.toLowerCase().includes('min')) {
    const hours = Math.floor(amount / 60)
    const minutes = amount % 60
    return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
  }

  return `${amount} ${unit}`
}

export const calculateTotalCost = (usageRecords: UsageRecord[]): number => {
  return usageRecords.reduce((total, record) => {
    return total + (record.cost || 0)
  }, 0)
}

export const getUnratedUsageCount = (usageRecords: UsageRecord[]): number => {
  return usageRecords.filter(record => !record.isRated).length
}

export const getRatedUsageCount = (usageRecords: UsageRecord[]): number => {
  return usageRecords.filter(record => record.isRated).length
}
