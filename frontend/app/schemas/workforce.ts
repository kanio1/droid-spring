import { z } from 'zod'

// Employee Type Enum
export const employeeTypeEnum = z.enum([
  'TECHNICIAN',
  'SENIOR_TECHNICIAN',
  'SUPERVISOR',
  'MANAGER',
  'SPECIALIST',
  'CONTRACTOR',
  'DISPATCHER',
  'INSPECTOR'
])

// Employee Status Enum
export const employeeStatusEnum = z.enum([
  'ACTIVE',
  'INACTIVE',
  'ON_LEAVE',
  'SICK',
  'SUSPENDED',
  'TERMINATED',
  'PENDING'
])

// Work Order Type Enum
export const workOrderTypeEnum = z.enum([
  'INSTALLATION',
  'REPAIR',
  'MAINTENANCE',
  'INSPECTION',
  'DISCONNECT',
  'RELOCATION',
  'UPGRADE',
  'TROUBLESHOOTING',
  'EMERGENCY',
  'SURVEY'
])

// Work Order Status Enum
export const workOrderStatusEnum = z.enum([
  'PENDING',
  'SCHEDULED',
  'ASSIGNED',
  'IN_PROGRESS',
  'ON_HOLD',
  'COMPLETED',
  'CANCELLED',
  'REQUIRES_REVIEW',
  'REQUIRES_PARTS'
])

// Employee Entity
export const employeeSchema = z.object({
  id: z.string(),
  employeeCode: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string().email(),
  phone: z.string().optional(),
  department: z.string().optional(),
  employeeType: employeeTypeEnum,
  status: employeeStatusEnum,
  position: z.string().optional(),
  managerId: z.string().optional(),
  hireDate: z.string().optional(),
  terminationDate: z.string().optional(),
  territory: z.string().optional(),
  serviceArea: z.string().optional(),
  address: z.string().optional(),
  emergencyContact: z.string().optional(),
  emergencyPhone: z.string().optional(),
  notes: z.string().optional(),
  hourlyRate: z.number(),
  maxDailyJobs: z.number(),
  onCall: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime()
})

// Work Order Entity
export const workOrderSchema = z.object({
  id: z.string(),
  workOrderNumber: z.string(),
  type: workOrderTypeEnum,
  status: workOrderStatusEnum,
  title: z.string(),
  description: z.string().optional(),
  priority: z.string().optional(),
  customerId: z.string().optional(),
  serviceAddress: z.string().optional(),
  serviceType: z.string().optional(),
  requiredSkill: z.string().optional(),
  estimatedDuration: z.number().optional(),
  scheduledDate: z.string().optional(),
  scheduledStartTime: z.string().datetime().optional(),
  scheduledEndTime: z.string().datetime().optional(),
  requestedDate: z.string().optional(),
  dueDate: z.string().optional(),
  customerContact: z.string().optional(),
  customerPhone: z.string().optional(),
  notes: z.string().optional(),
  attachments: z.string().optional(),
  actualDuration: z.number().optional(),
  completionNotes: z.string().optional(),
  customerSatisfactionRating: z.number().optional(),
  requiresFollowUp: z.boolean(),
  followUpNotes: z.string().optional(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime()
})

// Create Employee Command
export const createEmployeeSchema = z.object({
  employeeCode: z.string().min(1, 'Employee code is required').max(50, 'Employee code must not exceed 50 characters'),
  firstName: z.string().min(2, 'First name must be at least 2 characters').max(100, 'First name must not exceed 100 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters').max(100, 'Last name must not exceed 100 characters'),
  email: z.string().email('Invalid email address'),
  phone: z.string().max(50, 'Phone must not exceed 50 characters').optional(),
  department: z.string().max(100, 'Department must not exceed 100 characters').optional(),
  employeeType: employeeTypeEnum,
  position: z.string().max(100, 'Position must not exceed 100 characters').optional(),
  managerId: z.string().optional(),
  hireDate: z.string().optional(),
  territory: z.string().max(500, 'Territory must not exceed 500 characters').optional(),
  serviceArea: z.string().max(500, 'Service area must not exceed 500 characters').optional(),
  address: z.string().max(1000, 'Address must not exceed 1000 characters').optional(),
  emergencyContact: z.string().max(255, 'Emergency contact must not exceed 255 characters').optional(),
  emergencyPhone: z.string().max(50, 'Emergency phone must not exceed 50 characters').optional(),
  notes: z.string().optional(),
  hourlyRate: z.number().min(0, 'Hourly rate must be non-negative').default(0),
  maxDailyJobs: z.number().min(1, 'Max daily jobs must be at least 1').max(20, 'Max daily jobs must not exceed 20').default(5),
  onCall: z.boolean().default(false)
})

// Update Employee Command
export const updateEmployeeSchema = z.object({
  id: z.string(),
  firstName: z.string().min(2, 'First name must be at least 2 characters').max(100, 'First name must not exceed 100 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters').max(100, 'Last name must not exceed 100 characters'),
  email: z.string().email('Invalid email address'),
  phone: z.string().max(50, 'Phone must not exceed 50 characters').optional(),
  department: z.string().max(100, 'Department must not exceed 100 characters').optional(),
  position: z.string().max(100, 'Position must not exceed 100 characters').optional(),
  managerId: z.string().optional(),
  hireDate: z.string().optional(),
  terminationDate: z.string().optional(),
  territory: z.string().max(500, 'Territory must not exceed 500 characters').optional(),
  serviceArea: z.string().max(500, 'Service area must not exceed 500 characters').optional(),
  address: z.string().max(1000, 'Address must not exceed 1000 characters').optional(),
  emergencyContact: z.string().max(255, 'Emergency contact must not exceed 255 characters').optional(),
  emergencyPhone: z.string().max(50, 'Emergency phone must not exceed 50 characters').optional(),
  notes: z.string().optional(),
  hourlyRate: z.number().min(0, 'Hourly rate must be non-negative'),
  maxDailyJobs: z.number().min(1, 'Max daily jobs must be at least 1').max(20, 'Max daily jobs must not exceed 20'),
  onCall: z.boolean()
})

// Create Work Order Command
export const createWorkOrderSchema = z.object({
  workOrderNumber: z.string().min(1, 'Work order number is required').max(50, 'Work order number must not exceed 50 characters'),
  type: workOrderTypeEnum,
  title: z.string().min(1, 'Title is required').max(255, 'Title must not exceed 255 characters'),
  description: z.string().max(2000, 'Description must not exceed 2000 characters').optional(),
  priority: z.string().optional(),
  customerId: z.string().optional(),
  serviceAddress: z.string().max(1000, 'Service address must not exceed 1000 characters').optional(),
  serviceType: z.string().max(100, 'Service type must not exceed 100 characters').optional(),
  requiredSkill: z.string().max(100, 'Required skill must not exceed 100 characters').optional(),
  estimatedDuration: z.number().positive('Estimated duration must be positive').optional(),
  scheduledDate: z.string().optional(),
  scheduledStartTime: z.string().optional(),
  scheduledEndTime: z.string().optional(),
  requestedDate: z.string().optional(),
  dueDate: z.string().optional(),
  customerContact: z.string().max(255, 'Customer contact must not exceed 255 characters').optional(),
  customerPhone: z.string().max(50, 'Customer phone must not exceed 50 characters').optional(),
  notes: z.string().optional(),
  attachments: z.string().optional(),
  requiresFollowUp: z.boolean().default(false)
})

// Assign Work Order Command
export const assignWorkOrderSchema = z.object({
  id: z.string(),
  employeeId: z.string().min(1, 'Employee ID is required'),
  assignedBy: z.string().min(1, 'Assigned by is required')
})

// Start Work Order Command
export const startWorkOrderSchema = z.object({
  id: z.string(),
  employeeId: z.string().min(1, 'Employee ID is required')
})

// Complete Work Order Command
export const completeWorkOrderSchema = z.object({
  id: z.string(),
  completionNotes: z.string().min(1, 'Completion notes are required').max(2000, 'Notes must not exceed 2000 characters')
})

// Auto Assign Work Order Command
export const autoAssignWorkOrderSchema = z.object({
  id: z.string(),
  performedBy: z.string().min(1, 'Performed by is required')
})

// Employee Search Params
export const employeeSearchSchema = z.object({
  type: employeeTypeEnum.optional(),
  status: employeeStatusEnum.optional()
})

// Work Order Search Params
export const workOrderSearchSchema = z.object({
  type: workOrderTypeEnum.optional(),
  status: workOrderStatusEnum.optional(),
  date: z.string().optional()
})

// Workforce Statistics
export const workforceStatisticsSchema = z.object({
  totalEmployees: z.number(),
  activeEmployees: z.number(),
  technicians: z.number(),
  onCall: z.number(),
  pendingWorkOrders: z.number(),
  inProgressWorkOrders: z.number(),
  overdueWorkOrders: z.number()
})

// Form Data for UI
export const employeeFormDataSchema = createEmployeeSchema
export const workOrderFormDataSchema = createWorkOrderSchema

// Type exports
export type Employee = z.infer<typeof employeeSchema>
export type WorkOrder = z.infer<typeof workOrderSchema>
export type CreateEmployeeCommand = z.infer<typeof createEmployeeSchema>
export type UpdateEmployeeCommand = z.infer<typeof updateEmployeeSchema>
export type CreateWorkOrderCommand = z.infer<typeof createWorkOrderSchema>
export type AssignWorkOrderCommand = z.infer<typeof assignWorkOrderSchema>
export type StartWorkOrderCommand = z.infer<typeof startWorkOrderSchema>
export type CompleteWorkOrderCommand = z.infer<typeof completeWorkOrderSchema>
export type AutoAssignWorkOrderCommand = z.infer<typeof autoAssignWorkOrderSchema>
export type EmployeeSearchParams = z.infer<typeof employeeSearchSchema>
export type WorkOrderSearchParams = z.infer<typeof workOrderSearchSchema>
export type WorkforceStatistics = z.infer<typeof workforceStatisticsSchema>
export type EmployeeType = z.infer<typeof employeeTypeEnum>
export type EmployeeStatus = z.infer<typeof employeeStatusEnum>
export type WorkOrderType = z.infer<typeof workOrderTypeEnum>
export type WorkOrderStatus = z.infer<typeof workOrderStatusEnum>

// Validation helpers
export function validateEmployee(data: unknown) {
  return employeeSchema.parse(data)
}

export function validateWorkOrder(data: unknown) {
  return workOrderSchema.parse(data)
}

export function validateCreateEmployee(data: unknown) {
  return createEmployeeSchema.parse(data)
}

export function validateUpdateEmployee(data: unknown) {
  return updateEmployeeSchema.parse(data)
}

export function validateCreateWorkOrder(data: unknown) {
  return createWorkOrderSchema.parse(data)
}

export function validateAssignWorkOrder(data: unknown) {
  return assignWorkOrderSchema.parse(data)
}

export function validateStartWorkOrder(data: unknown) {
  return startWorkOrderSchema.parse(data)
}

export function validateCompleteWorkOrder(data: unknown) {
  return completeWorkOrderSchema.parse(data)
}

export function validateAutoAssignWorkOrder(data: unknown) {
  return autoAssignWorkOrderSchema.parse(data)
}
