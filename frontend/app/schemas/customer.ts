import { z } from 'zod'

// Customer Status Enum
export const customerStatusEnum = z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED', 'TERMINATED'])

// Customer Entity
export const customerSchema = z.object({
  id: z.string(),
  firstName: z.string().min(2, 'First name must be at least 2 characters').max(50, 'First name must not exceed 50 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters').max(50, 'Last name must not exceed 50 characters'),
  pesel: z.string().regex(/^\d{11}$/, 'PESEL must be exactly 11 digits').optional().or(z.literal('')),
  nip: z.string().regex(/^\d{10}$/, 'NIP must be exactly 10 digits').optional().or(z.literal('')),
  email: z.string().email('Invalid email address'),
  phone: z.string().regex(/^\+?[1-9]\d{1,14}$/, 'Invalid phone number format').optional().or(z.literal('')),
  status: customerStatusEnum,
  statusDisplayName: z.string(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  version: z.number()
})

// Create Customer Command
export const createCustomerSchema = z.object({
  firstName: z.string().min(2, 'First name must be at least 2 characters').max(50, 'First name must not exceed 50 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters').max(50, 'Last name must not exceed 50 characters'),
  pesel: z.string().regex(/^\d{11}$/, 'PESEL must be exactly 11 digits').optional().or(z.literal('')),
  nip: z.string().regex(/^\d{10}$/, 'NIP must be exactly 10 digits').optional().or(z.literal('')),
  email: z.string().email('Invalid email address'),
  phone: z.string().regex(/^\+?[1-9]\d{1,14}$/, 'Invalid phone number format').optional().or(z.literal(''))
})

// Update Customer Command
export const updateCustomerSchema = createCustomerSchema.extend({
  id: z.string(),
  version: z.number()
})

// Change Customer Status Command
export const changeCustomerStatusSchema = z.object({
  id: z.string(),
  status: customerStatusEnum
})

// Customer Search Params
export const customerSearchSchema = z.object({
  searchTerm: z.string().optional(),
  status: customerStatusEnum.optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Customer List Response
export const customerListResponseSchema = z.object({
  content: z.array(customerSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
  first: z.boolean(),
  last: z.boolean(),
  numberOfElements: z.number(),
  empty: z.boolean()
})

// Form Data for UI
export const customerFormDataSchema = createCustomerSchema

// Validation helper
export function validateCustomer(data: unknown) {
  return customerSchema.parse(data)
}

export function validateCreateCustomer(data: unknown) {
  return createCustomerSchema.parse(data)
}

export function validateUpdateCustomer(data: unknown) {
  return updateCustomerSchema.parse(data)
}

// Export types
export type Customer = z.infer<typeof customerSchema>
export type CreateCustomerCommand = z.infer<typeof createCustomerSchema>
export type UpdateCustomerCommand = z.infer<typeof updateCustomerSchema>
export type ChangeCustomerStatusCommand = z.infer<typeof changeCustomerStatusSchema>
export type CustomerSearchParams = z.infer<typeof customerSearchSchema>
export type CustomerListResponse = z.infer<typeof customerListResponseSchema>
export type CustomerFormData = z.infer<typeof customerFormDataSchema>
export type CustomerStatus = z.infer<typeof customerStatusEnum>

// Status labels
export const CUSTOMER_STATUS_LABELS: Record<CustomerStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  SUSPENDED: 'Suspended',
  TERMINATED: 'Terminated'
}

export const CUSTOMER_STATUS_COLORS: Record<CustomerStatus, string> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  SUSPENDED: 'warning',
  TERMINATED: 'danger'
}

// Utility functions
export function formatCustomerName(customer: Customer): string {
  return `${customer.firstName} ${customer.lastName}`
}

export function formatCustomerDisplay(customer: Customer): string {
  return `${formatCustomerName(customer)} (${customer.email})`
}

export function getInitials(customer: Customer): string {
  return `${customer.firstName.charAt(0)}${customer.lastName.charAt(0)}`.toUpperCase()
}

export function getStatusVariant(status: CustomerStatus): 'success' | 'neutral' | 'warning' | 'danger' {
  return CUSTOMER_STATUS_COLORS[status]
}

// Custom PESEL validation
export function validatePesel(pesel: string): boolean {
  if (!pesel || pesel.length !== 11) return false

  const weights = [1, 3, 7, 9, 1, 3, 7, 9, 1, 3]
  let sum = 0

  for (let i = 0; i < 10; i++) {
    sum += parseInt(pesel[i]) * weights[i]
  }

  const checksum = (10 - (sum % 10)) % 10
  return checksum === parseInt(pesel[10])
}
