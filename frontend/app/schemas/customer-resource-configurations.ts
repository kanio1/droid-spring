import { z } from 'zod'

// Customer Resource Configuration Entity
export const customerResourceConfigurationSchema = z.object({
  id: z.number(),
  customerId: z.number(),
  resourceType: z.string(),
  resourceId: z.string(),
  resourceName: z.string().optional(),
  region: z.string().optional(),
  maxLimit: z.number().optional(),
  warningThreshold: z.number(),
  criticalThreshold: z.number(),
  budgetLimit: z.number().optional(),
  budgetCurrency: z.string().optional(),
  alertEmail: z.string().email().optional(),
  alertPhone: z.string().optional(),
  alertSlackWebhook: z.string().optional(),
  autoScalingEnabled: z.boolean(),
  scaleUpThreshold: z.number().optional(),
  scaleDownThreshold: z.number().optional(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED']),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  tags: z.string().optional()
})

// Create Configuration Command
export const createConfigurationSchema = z.object({
  customerId: z.number().positive('Customer ID must be positive'),
  resourceType: z.string().min(1, 'Resource type is required').max(100, 'Resource type must not exceed 100 characters'),
  resourceId: z.string().min(1, 'Resource ID is required').max(255, 'Resource ID must not exceed 255 characters'),
  resourceName: z.string().max(255, 'Resource name must not exceed 255 characters').optional(),
  region: z.string().max(100, 'Region must not exceed 100 characters').optional(),
  maxLimit: z.number().positive('Max limit must be positive').optional(),
  warningThreshold: z.number().min(0).max(100).default(80),
  criticalThreshold: z.number().min(0).max(100).default(95),
  budgetLimit: z.number().positive('Budget limit must be positive').optional(),
  budgetCurrency: z.string().max(10, 'Budget currency must not exceed 10 characters').optional(),
  alertEmail: z.string().email('Invalid email address').optional(),
  alertPhone: z.string().max(50, 'Alert phone must not exceed 50 characters').optional(),
  alertSlackWebhook: z.string().url('Invalid Slack webhook URL').optional(),
  autoScalingEnabled: z.boolean().default(false),
  scaleUpThreshold: z.number().min(0).max(100).optional(),
  scaleDownThreshold: z.number().min(0).max(100).optional(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED']).default('ACTIVE')
})

// Update Configuration Command
export const updateConfigurationSchema = z.object({
  id: z.number().positive('ID must be positive'),
  resourceName: z.string().max(255, 'Resource name must not exceed 255 characters').optional(),
  region: z.string().max(100, 'Region must not exceed 100 characters').optional(),
  maxLimit: z.number().positive('Max limit must be positive').optional(),
  warningThreshold: z.number().min(0).max(100).optional(),
  criticalThreshold: z.number().min(0).max(100).optional(),
  budgetLimit: z.number().positive('Budget limit must be positive').optional(),
  budgetCurrency: z.string().max(10, 'Budget currency must not exceed 10 characters').optional(),
  alertEmail: z.string().email('Invalid email address').optional(),
  alertPhone: z.string().max(50, 'Alert phone must not exceed 50 characters').optional(),
  alertSlackWebhook: z.string().url('Invalid Slack webhook URL').optional(),
  autoScalingEnabled: z.boolean().optional(),
  scaleUpThreshold: z.number().min(0).max(100).optional(),
  scaleDownThreshold: z.number().min(0).max(100).optional(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED']).optional()
})

// Delete Configuration Command
export const deleteConfigurationSchema = z.object({
  id: z.number().positive('ID must be positive')
})

// Configuration Search Params
export const configurationSearchSchema = z.object({
  customerId: z.number().positive().optional(),
  resourceType: z.string().optional(),
  resourceId: z.string().optional(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED']).optional()
})

// Form Data for UI
export const configurationFormDataSchema = createConfigurationSchema

// Type exports
export type CustomerResourceConfiguration = z.infer<typeof customerResourceConfigurationSchema>
export type CreateConfigurationCommand = z.infer<typeof createConfigurationSchema>
export type UpdateConfigurationCommand = z.infer<typeof updateConfigurationSchema>
export type DeleteConfigurationCommand = z.infer<typeof deleteConfigurationSchema>
export type ConfigurationSearchParams = z.infer<typeof configurationSearchSchema>

// Validation helpers
export function validateCustomerResourceConfiguration(data: unknown) {
  return customerResourceConfigurationSchema.parse(data)
}

export function validateCreateConfiguration(data: unknown) {
  return createConfigurationSchema.parse(data)
}

export function validateUpdateConfiguration(data: unknown) {
  return updateConfigurationSchema.parse(data)
}

export function validateDeleteConfiguration(data: unknown) {
  return deleteConfigurationSchema.parse(data)
}
