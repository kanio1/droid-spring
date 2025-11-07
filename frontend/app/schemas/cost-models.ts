import { z } from 'zod'

// Cost Model Entity
export const costModelSchema = z.object({
  id: z.number(),
  modelName: z.string(),
  description: z.string().optional(),
  billingPeriod: z.string(),
  baseCost: z.number(),
  overageRate: z.number(),
  includedUsage: z.number(),
  currency: z.string(),
  active: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime()
})

// Create Cost Model Command
export const createCostModelSchema = z.object({
  modelName: z.string().min(1, 'Model name is required').max(100, 'Model name must not exceed 100 characters'),
  description: z.string().max(500, 'Description must not exceed 500 characters').optional(),
  billingPeriod: z.enum(['hourly', 'daily', 'monthly', 'yearly']),
  baseCost: z.number().min(0, 'Base cost must be non-negative'),
  overageRate: z.number().min(0, 'Overage rate must be non-negative'),
  includedUsage: z.number().min(0, 'Included usage must be non-negative'),
  currency: z.string().min(1, 'Currency is required').max(10, 'Currency must not exceed 10 characters'),
  active: z.boolean().default(true)
})

// Update Cost Model Command
export const updateCostModelSchema = z.object({
  id: z.number().positive('ID must be positive'),
  description: z.string().max(500, 'Description must not exceed 500 characters').optional(),
  baseCost: z.number().min(0, 'Base cost must be non-negative'),
  overageRate: z.number().min(0, 'Overage rate must be non-negative'),
  includedUsage: z.number().min(0, 'Included usage must be non-negative'),
  currency: z.string().min(1, 'Currency is required').max(10, 'Currency must not exceed 10 characters'),
  active: z.boolean().default(true)
})

// Delete Cost Model Command
export const deleteCostModelSchema = z.object({
  id: z.number().positive('ID must be positive')
})

// Cost Model Search Params
export const costModelSearchSchema = z.object({
  active: z.boolean().optional(),
  billingPeriod: z.enum(['hourly', 'daily', 'monthly', 'yearly']).optional(),
  modelName: z.string().optional()
})

// Form Data for UI
export const costModelFormDataSchema = createCostModelSchema

// Type exports
export type CostModel = z.infer<typeof costModelSchema>
export type CreateCostModelCommand = z.infer<typeof createCostModelSchema>
export type UpdateCostModelCommand = z.infer<typeof updateCostModelSchema>
export type DeleteCostModelCommand = z.infer<typeof deleteCostModelSchema>
export type CostModelSearchParams = z.infer<typeof costModelSearchSchema>

// Validation helpers
export function validateCostModel(data: unknown) {
  return costModelSchema.parse(data)
}

export function validateCreateCostModel(data: unknown) {
  return createCostModelSchema.parse(data)
}

export function validateUpdateCostModel(data: unknown) {
  return updateCostModelSchema.parse(data)
}

export function validateDeleteCostModel(data: unknown) {
  return deleteCostModelSchema.parse(data)
}
