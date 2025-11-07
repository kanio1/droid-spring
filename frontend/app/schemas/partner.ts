import { z } from 'zod'

// Partner Type Enum
export const partnerTypeEnum = z.enum([
  'RESELLER',
  'DISTRIBUTOR',
  'MVNO',
  'FRANCHISEE',
  'AGENT',
  'SYSTEM_INTEGRATOR',
  'VALUE_ADDED_RESELLER'
])

// Partner Status Enum
export const partnerStatusEnum = z.enum([
  'ACTIVE',
  'SUSPENDED',
  'TERMINATED',
  'PENDING_APPROVAL',
  'ON_HOLD'
])

// Partner Entity
export const partnerSchema = z.object({
  id: z.string(),
  partnerCode: z.string(),
  name: z.string(),
  description: z.string().optional(),
  partnerType: partnerTypeEnum,
  status: partnerStatusEnum,
  contactPerson: z.string().optional(),
  email: z.string().email().optional(),
  phone: z.string().optional(),
  address: z.string().optional(),
  taxId: z.string().optional(),
  registrationNumber: z.string().optional(),
  contractStartDate: z.string().optional(),
  contractEndDate: z.string().optional(),
  commissionRate: z.number(),
  settlementFrequency: z.string().optional(),
  paymentTerms: z.string().optional(),
  creditLimit: z.number(),
  currentBalance: z.number(),
  totalSales: z.number(),
  totalCommission: z.number(),
  territory: z.string().optional(),
  marketSegment: z.string().optional(),
  contractValue: z.number(),
  servicesProvided: z.string().optional(),
  notes: z.string().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime()
})

// Create Partner Command
export const createPartnerSchema = z.object({
  partnerCode: z.string().min(1, 'Partner code is required').max(50, 'Partner code must not exceed 50 characters'),
  name: z.string().min(2, 'Name must be at least 2 characters').max(255, 'Name must not exceed 255 characters'),
  description: z.string().max(1000, 'Description must not exceed 1000 characters').optional(),
  partnerType: partnerTypeEnum,
  contactPerson: z.string().max(255, 'Contact person must not exceed 255 characters').optional(),
  email: z.string().email('Invalid email address').optional(),
  phone: z.string().max(50, 'Phone must not exceed 50 characters').optional(),
  address: z.string().max(1000, 'Address must not exceed 1000 characters').optional(),
  taxId: z.string().max(100, 'Tax ID must not exceed 100 characters').optional(),
  registrationNumber: z.string().max(100, 'Registration number must not exceed 100 characters').optional(),
  contractStartDate: z.string().optional(),
  contractEndDate: z.string().optional(),
  commissionRate: z.number().min(0, 'Commission rate must be non-negative').max(100, 'Commission rate must not exceed 100'),
  settlementFrequency: z.string().optional(),
  paymentTerms: z.string().optional(),
  creditLimit: z.number().min(0, 'Credit limit must be non-negative').default(0),
  territory: z.string().max(500, 'Territory must not exceed 500 characters').optional(),
  marketSegment: z.string().max(100, 'Market segment must not exceed 100 characters').optional(),
  contractValue: z.number().min(0, 'Contract value must be non-negative').default(0),
  servicesProvided: z.string().optional(),
  notes: z.string().optional()
})

// Update Partner Command
export const updatePartnerSchema = z.object({
  id: z.string(),
  name: z.string().min(2, 'Name must be at least 2 characters').max(255, 'Name must not exceed 255 characters'),
  description: z.string().max(1000, 'Description must not exceed 1000 characters').optional(),
  contactPerson: z.string().max(255, 'Contact person must not exceed 255 characters').optional(),
  email: z.string().email('Invalid email address').optional(),
  phone: z.string().max(50, 'Phone must not exceed 50 characters').optional(),
  address: z.string().max(1000, 'Address must not exceed 1000 characters').optional(),
  taxId: z.string().max(100, 'Tax ID must not exceed 100 characters').optional(),
  registrationNumber: z.string().max(100, 'Registration number must not exceed 100 characters').optional(),
  contractStartDate: z.string().optional(),
  contractEndDate: z.string().optional(),
  commissionRate: z.number().min(0, 'Commission rate must be non-negative').max(100, 'Commission rate must not exceed 100'),
  settlementFrequency: z.string().optional(),
  paymentTerms: z.string().optional(),
  creditLimit: z.number().min(0, 'Credit limit must be non-negative'),
  territory: z.string().max(500, 'Territory must not exceed 500 characters').optional(),
  marketSegment: z.string().max(100, 'Market segment must not exceed 100 characters').optional(),
  contractValue: z.number().min(0, 'Contract value must be non-negative'),
  servicesProvided: z.string().optional(),
  notes: z.string().optional()
})

// Suspend Partner Command
export const suspendPartnerSchema = z.object({
  id: z.string(),
  reason: z.string().min(1, 'Reason is required').max(1000, 'Reason must not exceed 1000 characters')
})

// Terminate Partner Command
export const terminatePartnerSchema = z.object({
  id: z.string(),
  reason: z.string().min(1, 'Reason is required').max(1000, 'Reason must not exceed 1000 characters')
})

// Partner Search Params
export const partnerSearchSchema = z.object({
  type: partnerTypeEnum.optional(),
  status: partnerStatusEnum.optional(),
  searchTerm: z.string().optional()
})

// Partner Summary
export const partnerSummarySchema = z.object({
  partnerCode: z.string(),
  name: z.string(),
  type: partnerTypeEnum,
  status: partnerStatusEnum,
  totalSales: z.number(),
  totalCommission: z.number(),
  currentBalance: z.number(),
  commissionRate: z.number(),
  contractStartDate: z.string().optional(),
  contractEndDate: z.string().optional()
})

// Partner Statistics
export const partnerStatisticsSchema = z.object({
  total: z.number(),
  active: z.number(),
  suspended: z.number(),
  other: z.number()
})

// Partner Settlement
export const partnerSettlementSchema = z.object({
  id: z.string(),
  partnerId: z.string(),
  settlementDate: z.string(),
  amount: z.number(),
  commissionAmount: z.number(),
  status: z.string(),
  notes: z.string().optional(),
  createdAt: z.string().datetime()
})

// Form Data for UI
export const partnerFormDataSchema = createPartnerSchema

// Type exports
export type Partner = z.infer<typeof partnerSchema>
export type CreatePartnerCommand = z.infer<typeof createPartnerSchema>
export type UpdatePartnerCommand = z.infer<typeof updatePartnerSchema>
export type SuspendPartnerCommand = z.infer<typeof suspendPartnerSchema>
export type TerminatePartnerCommand = z.infer<typeof terminatePartnerSchema>
export type PartnerSearchParams = z.infer<typeof partnerSearchSchema>
export type PartnerSummary = z.infer<typeof partnerSummarySchema>
export type PartnerStatistics = z.infer<typeof partnerStatisticsSchema>
export type PartnerSettlement = z.infer<typeof partnerSettlementSchema>
export type PartnerType = z.infer<typeof partnerTypeEnum>
export type PartnerStatus = z.infer<typeof partnerStatusEnum>

// Validation helpers
export function validatePartner(data: unknown) {
  return partnerSchema.parse(data)
}

export function validateCreatePartner(data: unknown) {
  return createPartnerSchema.parse(data)
}

export function validateUpdatePartner(data: unknown) {
  return updatePartnerSchema.parse(data)
}

export function validateSuspendPartner(data: unknown) {
  return suspendPartnerSchema.parse(data)
}

export function validateTerminatePartner(data: unknown) {
  return terminatePartnerSchema.parse(data)
}
