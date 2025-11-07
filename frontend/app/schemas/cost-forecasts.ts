import { z } from 'zod'

// Cost Forecast Entity
export const costForecastSchema = z.object({
  id: z.number(),
  customerId: z.number(),
  resourceType: z.string(),
  billingPeriod: z.string(),
  forecastPeriodStart: z.string().datetime(),
  forecastPeriodEnd: z.string().datetime(),
  predictedCost: z.number(),
  lowerBound: z.number(),
  upperBound: z.number(),
  trendDirection: z.enum(['INCREASING', 'DECREASING', 'STABLE']),
  confidenceLevel: z.number(),
  calculatedAt: z.string().datetime(),
  forecastModel: z.string()
})

// Generate Forecast Command
export const generateForecastSchema = z.object({
  customerId: z.number().positive('Customer ID must be positive'),
  resourceType: z.string().min(1, 'Resource type is required').max(100, 'Resource type must not exceed 100 characters'),
  billingPeriod: z.enum(['hourly', 'daily', 'monthly', 'yearly']),
  forecastStartDate: z.string().datetime('Invalid forecast start date'),
  forecastEndDate: z.string().datetime('Invalid forecast end date'),
  historicalMonths: z.number().min(1).max(24).default(3),
  forecastModel: z.enum(['LINEAR_REGRESSION', 'MOVING_AVERAGE']).default('LINEAR_REGRESSION')
})

// Cost Forecast Search Params
export const costForecastSearchSchema = z.object({
  customerId: z.number().positive().optional(),
  resourceType: z.string().optional(),
  startDate: z.string().datetime().optional(),
  endDate: z.string().datetime().optional(),
  forecastPeriodStart: z.string().datetime().optional()
})

// Form Data for UI
export const costForecastFormDataSchema = generateForecastSchema

// Type exports
export type CostForecast = z.infer<typeof costForecastSchema>
export type GenerateForecastCommand = z.infer<typeof generateForecastSchema>
export type CostForecastSearchParams = z.infer<typeof costForecastSearchSchema>

// Validation helpers
export function validateCostForecast(data: unknown) {
  return costForecastSchema.parse(data)
}

export function validateGenerateForecast(data: unknown) {
  return generateForecastSchema.parse(data)
}
