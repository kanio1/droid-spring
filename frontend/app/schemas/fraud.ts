import { z } from 'zod'

// Fraud Alert Status Enum
export const fraudAlertStatusEnum = z.enum([
  'NEW',
  'ASSIGNED',
  'IN_REVIEW',
  'ESCALATED',
  'RESOLVED',
  'CLOSED',
  'REJECTED'
])

// Fraud Alert Type Enum
export const fraudAlertTypeEnum = z.enum([
  'UNUSUAL_LOGIN',
  'MULTIPLE_FAILED_LOGINS',
  'HIGH_VALUE_TRANSACTION',
  'RAPID_TRANSACTIONS',
  'LOCATION_ANOMALY',
  'IP_REPUTATION',
  'VELOCITY_CHECK',
  'PATTERN_ANOMALY',
  'DEVICE_MISMATCH',
  'ACCOUNT_TAKEOVER',
  'PAYMENT_FRAUD',
  'IDENTITY_VERIFICATION',
  'BLACKLISTED_ENTITY',
  'CROSS_ACCOUNT_ACTIVITY',
  'MANUAL_REVIEW'
])

// Fraud Alert Entity
export const fraudAlertSchema = z.object({
  id: z.string(),
  alertId: z.string(),
  customerId: z.string(),
  accountId: z.string().optional(),
  status: fraudAlertStatusEnum,
  alertType: fraudAlertTypeEnum,
  title: z.string(),
  description: z.string().optional(),
  severity: z.string(),
  ruleTriggered: z.string().optional(),
  riskScore: z.number(),
  sourceEntityType: z.string().optional(),
  sourceEntityId: z.string().optional(),
  transactionId: z.string().optional(),
  ipAddress: z.string().optional(),
  userAgent: z.string().optional(),
  location: z.string().optional(),
  details: z.string().optional(),
  assignedTo: z.string().optional(),
  assignedAt: z.string().datetime().optional(),
  resolvedBy: z.string().optional(),
  resolvedAt: z.string().datetime().optional(),
  resolutionNotes: z.string().optional(),
  falsePositive: z.boolean(),
  escalatedTo: z.string().optional(),
  escalatedAt: z.string().datetime().optional(),
  reviewCount: z.number(),
  sourceSystem: z.string().optional(),
  createdAt: z.string().datetime()
})

// Create Fraud Alert Command
export const createFraudAlertSchema = z.object({
  customerId: z.string().min(1, 'Customer ID is required'),
  accountId: z.string().optional(),
  alertType: fraudAlertTypeEnum,
  title: z.string().min(1, 'Title is required').max(255, 'Title must not exceed 255 characters'),
  description: z.string().max(2000, 'Description must not exceed 2000 characters').optional(),
  severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']),
  ruleTriggered: z.string().optional(),
  riskScore: z.number().min(0).max(100),
  sourceEntityType: z.string().optional(),
  sourceEntityId: z.string().optional(),
  transactionId: z.string().optional(),
  ipAddress: z.string().optional(),
  userAgent: z.string().optional(),
  location: z.string().optional(),
  details: z.string().optional(),
  sourceSystem: z.string().optional()
})

// Assign Alert Command
export const assignAlertSchema = z.object({
  id: z.string(),
  analystId: z.string().min(1, 'Analyst ID is required')
})

// Resolve Alert Command
export const resolveAlertSchema = z.object({
  id: z.string(),
  resolvedBy: z.string().min(1, 'Resolver ID is required'),
  resolutionNotes: z.string().min(1, 'Resolution notes are required').max(2000, 'Notes must not exceed 2000 characters')
})

// Mark as False Positive Command
export const falsePositiveSchema = z.object({
  id: z.string(),
  notedBy: z.string().min(1, 'Noted by is required'),
  reason: z.string().min(1, 'Reason is required').max(1000, 'Reason must not exceed 1000 characters')
})

// Escalate Alert Command
export const escalateAlertSchema = z.object({
  id: z.string(),
  escalatedTo: z.string().min(1, 'Escalated to is required')
})

// Velocity Check Command
export const velocityCheckSchema = z.object({
  customerId: z.string().min(1, 'Customer ID is required'),
  entityType: z.string().min(1, 'Entity type is required'),
  entityId: z.string().min(1, 'Entity ID is required'),
  transactionCount: z.number().min(1, 'Transaction count must be at least 1'),
  timeWindowMinutes: z.number().min(1, 'Time window must be at least 1 minute'),
  ipAddress: z.string().optional()
})

// High Value Check Command
export const highValueCheckSchema = z.object({
  customerId: z.string().min(1, 'Customer ID is required'),
  transactionId: z.string().min(1, 'Transaction ID is required'),
  amount: z.number().positive('Amount must be positive'),
  location: z.string().optional()
})

// Fraud Search Params
export const fraudSearchSchema = z.object({
  status: fraudAlertStatusEnum.optional(),
  severity: z.string().optional(),
  customerId: z.string().optional()
})

// Fraud Statistics
export const fraudStatisticsSchema = z.object({
  totalAlerts: z.number(),
  openAlerts: z.number(),
  highRiskAlerts: z.number(),
  resolvedToday: z.number(),
  avgResolutionHours: z.number(),
  falsePositives: z.number()
})

// Form Data for UI
export const fraudAlertFormDataSchema = createFraudAlertSchema

// Type exports
export type FraudAlert = z.infer<typeof fraudAlertSchema>
export type CreateFraudAlertCommand = z.infer<typeof createFraudAlertSchema>
export type AssignAlertCommand = z.infer<typeof assignAlertSchema>
export type ResolveAlertCommand = z.infer<typeof resolveAlertSchema>
export type FalsePositiveCommand = z.infer<typeof falsePositiveSchema>
export type EscalateAlertCommand = z.infer<typeof escalateAlertSchema>
export type VelocityCheckCommand = z.infer<typeof velocityCheckSchema>
export type HighValueCheckCommand = z.infer<typeof highValueCheckSchema>
export type FraudSearchParams = z.infer<typeof fraudSearchSchema>
export type FraudStatistics = z.infer<typeof fraudStatisticsSchema>
export type FraudAlertStatus = z.infer<typeof fraudAlertStatusEnum>
export type FraudAlertType = z.infer<typeof fraudAlertTypeEnum>

// Validation helpers
export function validateFraudAlert(data: unknown) {
  return fraudAlertSchema.parse(data)
}

export function validateCreateFraudAlert(data: unknown) {
  return createFraudAlertSchema.parse(data)
}

export function validateAssignAlert(data: unknown) {
  return assignAlertSchema.parse(data)
}

export function validateResolveAlert(data: unknown) {
  return resolveAlertSchema.parse(data)
}

export function validateFalsePositive(data: unknown) {
  return falsePositiveSchema.parse(data)
}

export function validateEscalateAlert(data: unknown) {
  return escalateAlertSchema.parse(data)
}
