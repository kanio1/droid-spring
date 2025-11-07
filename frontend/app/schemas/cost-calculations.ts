import { z } from 'zod'

// Cost Calculation Entity
export const costCalculationSchema = z.object({
  id: z.number(),
  customerId: z.number(),
  resourceType: z.string(),
  billingPeriod: z.string(),
  periodStart: z.string().datetime(),
  periodEnd: z.string().datetime(),
  totalUsage: z.number(),
  baseCost: z.number(),
  overageCost: z.number(),
  totalCost: z.number(),
  currency: z.string(),
  status: z.enum(['DRAFT', 'FINAL', 'INVOICED']),
  calculatedAt: z.string().datetime()
})

// Calculate Cost Command
export const calculateCostSchema = z.object({
  customerId: z.number().positive('Customer ID must be positive'),
  resourceType: z.string().min(1, 'Resource type is required').max(100, 'Resource type must not exceed 100 characters'),
  billingPeriod: z.enum(['hourly', 'daily', 'monthly', 'yearly']),
  periodStart: z.string().datetime('Invalid period start date'),
  periodEnd: z.string().datetime('Invalid period end date'),
  totalUsage: z.number().min(0, 'Total usage must be non-negative'),
  costModelId: z.number().positive('Cost model ID must be positive'),
  currency: z.string().min(1, 'Currency is required').max(10, 'Currency must not exceed 10 characters')
})

// Recalculate Cost Command
export const recalculateCostSchema = z.object({
  id: z.number().positive('ID must be positive')
})

// Cost Calculation Search Params
export const costCalculationSearchSchema = z.object({
  customerId: z.number().positive().optional(),
  resourceType: z.string().optional(),
  status: z.enum(['DRAFT', 'FINAL', 'INVOICED']).optional(),
  startDate: z.string().datetime().optional(),
  endDate: z.string().datetime().optional()
})

// Form Data for UI
export const costCalculationFormDataSchema = calculateCostSchema

// Type exports
export type CostCalculation = z.infer<typeof costCalculationSchema>
export type CalculateCostCommand = z.infer<typeof calculateCostSchema>
export type RecalculateCostCommand = z.infer<typeof recalculateCostSchema>
export type CostCalculationSearchParams = z.infer<typeof costCalculationSearchSchema>

// Validation helpers
export function validateCostCalculation(data: unknown) {
  return costCalculationSchema.parse(data)
}

export function validateCalculateCost(data: unknown) {
  return calculateCostSchema.parse(data)
}

export function validateRecalculateCost(data: unknown) {
  return recalculateCostSchema.parse(data)
}
