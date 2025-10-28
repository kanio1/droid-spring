// Customer types - based on backend API

export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'TERMINATED'

export interface Customer {
  id: string
  firstName: string
  lastName: string
  pesel?: string
  nip?: string
  email: string
  phone?: string
  status: CustomerStatus
  statusDisplayName: string
  createdAt: string
  updatedAt: string
  version: number
}

export interface CreateCustomerCommand {
  firstName: string
  lastName: string
  pesel?: string
  nip?: string
  email: string
  phone?: string
}

export interface UpdateCustomerCommand {
  id: string
  firstName: string
  lastName: string
  pesel?: string
  nip?: string
  email: string
  phone?: string
}

export interface ChangeCustomerStatusCommand {
  id: string
  status: CustomerStatus
}

export interface CustomerSearchParams {
  searchTerm?: string
  status?: CustomerStatus
  page?: number
  size?: number
  sort?: string
}

export interface CustomerListResponse {
  content: Customer[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  numberOfElements: number
  empty: boolean
}

export interface CustomerFormData {
  firstName: string
  lastName: string
  pesel: string
  nip: string
  email: string
  phone: string
}

export const CUSTOMER_STATUS_LABELS: Record<CustomerStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  SUSPENDED: 'Suspended',
  TERMINATED: 'Terminated'
}

export const CUSTOMER_STATUS_COLORS: Record<CustomerStatus, 'success' | 'neutral' | 'warning' | 'danger'> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  SUSPENDED: 'warning',
  TERMINATED: 'danger'
}

// Validation schemas (for use with validation libraries)
export const CUSTOMER_VALIDATION_RULES = {
  firstName: {
    required: true,
    minLength: 2,
    maxLength: 100,
    pattern: /^[A-Za-zĄąĆćĘęŁłŃńÓóŚśŹźŻż]+$/
  },
  lastName: {
    required: true,
    minLength: 2,
    maxLength: 100,
    pattern: /^[A-Za-zĄąĆćĘęŁłŃńÓóŚśŹźŻż]+$/
  },
  email: {
    required: true,
    format: 'email',
    maxLength: 255
  },
  phone: {
    format: '^\\+?[1-9]\\d{1,14}$' // E.164 format
  },
  pesel: {
    format: '^\\d{11}$',
    custom: (value: string) => validatePesel(value)
  },
  nip: {
    format: '^\\d{10}$'
  }
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

// Format functions
export function formatCustomerName(customer: Customer): string {
  return `${customer.firstName} ${customer.lastName}`
}

export function formatCustomerDisplay(customer: Customer): string {
  return `${formatCustomerName(customer)} (${customer.email})`
}

export function getInitials(customer: Customer): string {
  return `${customer.firstName.charAt(0)}${customer.lastName.charAt(0)}`.toUpperCase()
}

export function getCustomerStatusVariant(status: CustomerStatus): 'success' | 'neutral' | 'warning' | 'danger' {
  return CUSTOMER_STATUS_COLORS[status]
}
