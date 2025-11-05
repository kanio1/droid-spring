import { z } from 'zod'

// Address Type Enum
export const addressTypeEnum = z.enum(['BILLING', 'SHIPPING', 'SERVICE', 'CORRESPONDENCE'])

// Address Status Enum
export const addressStatusEnum = z.enum(['ACTIVE', 'INACTIVE', 'PENDING'])

// Country Enum (simplified list, extend as needed)
export const countryEnum = z.enum([
  'PL', 'DE', 'FR', 'ES', 'IT', 'UK', 'NL', 'SE', 'NO', 'DK',
  'FI', 'CZ', 'SK', 'AT', 'HU', 'RO', 'BG', 'HR', 'SI', 'EE',
  'LV', 'LT', 'IE', 'PT', 'GR', 'CY', 'MT', 'LU', 'BE'
])

// Address Entity
export const addressSchema = z.object({
  id: z.string(),
  customerId: z.string(),
  customerName: z.string(),
  type: addressTypeEnum,
  typeDisplayName: z.string(),
  status: addressStatusEnum,
  statusDisplayName: z.string(),
  street: z.string().min(1, 'Street is required').max(100, 'Street must not exceed 100 characters'),
  houseNumber: z.string().max(20, 'House number must not exceed 20 characters'),
  apartmentNumber: z.string().max(20, 'Apartment number must not exceed 20 characters').optional().or(z.literal('')),
  postalCode: z.string().regex(/^\d{2}-\d{3}$/, 'Postal code must be in format XX-XXX'),
  city: z.string().min(1, 'City is required').max(100, 'City must not exceed 100 characters'),
  region: z.string().max(100, 'Region must not exceed 100 characters').optional().or(z.literal('')),
  country: countryEnum,
  countryDisplayName: z.string(),
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  isPrimary: z.boolean(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  version: z.number()
})

// Create Address Command
export const createAddressSchema = z.object({
  customerId: z.string().min(1, 'Customer is required'),
  type: addressTypeEnum,
  street: z.string().min(1, 'Street is required').max(100, 'Street must not exceed 100 characters'),
  houseNumber: z.string().max(20, 'House number must not exceed 20 characters'),
  apartmentNumber: z.string().max(20, 'Apartment number must not exceed 20 characters').optional().or(z.literal('')),
  postalCode: z.string().regex(/^\d{2}-\d{3}$/, 'Postal code must be in format XX-XXX'),
  city: z.string().min(1, 'City is required').max(100, 'City must not exceed 100 characters'),
  region: z.string().max(100, 'Region must not exceed 100 characters').optional().or(z.literal('')),
  country: countryEnum,
  latitude: z.number().optional(),
  longitude: z.number().optional(),
  isPrimary: z.boolean().default(false)
})

// Update Address Command
export const updateAddressSchema = createAddressSchema.extend({
  id: z.string(),
  version: z.number()
})

// Change Address Status Command
export const changeAddressStatusSchema = z.object({
  id: z.string(),
  status: addressStatusEnum
})

// Address Search Params
export const addressSearchSchema = z.object({
  searchTerm: z.string().optional(),
  customerId: z.string().optional(),
  type: addressTypeEnum.optional(),
  status: addressStatusEnum.optional(),
  country: countryEnum.optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Address List Response
export const addressListResponseSchema = z.object({
  content: z.array(addressSchema),
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
export const addressFormDataSchema = createAddressSchema

// Validation helper
export function validateAddress(data: unknown) {
  return addressSchema.parse(data)
}

export function validateCreateAddress(data: unknown) {
  return createAddressSchema.parse(data)
}

export function validateUpdateAddress(data: unknown) {
  return updateAddressSchema.parse(data)
}

// Export types
export type Address = z.infer<typeof addressSchema>
export type CreateAddressCommand = z.infer<typeof createAddressSchema>
export type UpdateAddressCommand = z.infer<typeof updateAddressSchema>
export type ChangeAddressStatusCommand = z.infer<typeof changeAddressStatusSchema>
export type AddressSearchParams = z.infer<typeof addressSearchSchema>
export type AddressListResponse = z.infer<typeof addressListResponseSchema>
export type AddressFormData = z.infer<typeof addressFormDataSchema>
export type AddressType = z.infer<typeof addressTypeEnum>
export type AddressStatus = z.infer<typeof addressStatusEnum>
export type Country = z.infer<typeof countryEnum>

// Status labels
export const ADDRESS_STATUS_LABELS: Record<AddressStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  PENDING: 'Pending'
}

export const ADDRESS_STATUS_COLORS: Record<AddressStatus, string> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  PENDING: 'warning'
}

// Type labels
export const ADDRESS_TYPE_LABELS: Record<AddressType, string> = {
  BILLING: 'Billing',
  SHIPPING: 'Shipping',
  SERVICE: 'Service',
  CORRESPONDENCE: 'Correspondence'
}

export const ADDRESS_TYPE_COLORS: Record<AddressType, string> = {
  BILLING: 'primary',
  SHIPPING: 'info',
  SERVICE: 'success',
  CORRESPONDENCE: 'secondary'
}

// Country labels
export const COUNTRY_LABELS: Record<Country, string> = {
  PL: 'Poland',
  DE: 'Germany',
  FR: 'France',
  ES: 'Spain',
  IT: 'Italy',
  UK: 'United Kingdom',
  NL: 'Netherlands',
  SE: 'Sweden',
  NO: 'Norway',
  DK: 'Denmark',
  FI: 'Finland',
  CZ: 'Czech Republic',
  SK: 'Slovakia',
  AT: 'Austria',
  HU: 'Hungary',
  RO: 'Romania',
  BG: 'Bulgaria',
  HR: 'Croatia',
  SI: 'Slovenia',
  EE: 'Estonia',
  LV: 'Latvia',
  LT: 'Lithuania',
  IE: 'Ireland',
  PT: 'Portugal',
  GR: 'Greece',
  CY: 'Cyprus',
  MT: 'Malta',
  LU: 'Luxembourg',
  BE: 'Belgium'
}

// Utility functions
export function formatFullAddress(address: Address): string {
  const parts = [
    `${address.street} ${address.houseNumber}`,
    address.apartmentNumber ? `/${address.apartmentNumber}` : '',
    address.postalCode,
    address.city,
    COUNTRY_LABELS[address.country]
  ]
  return parts.filter(p => p && p.trim()).join(', ')
}

export function formatShortAddress(address: Address): string {
  const parts = [
    `${address.street} ${address.houseNumber}`,
    address.postalCode,
    address.city
  ]
  return parts.filter(p => p && p.trim()).join(', ')
}

export function getStatusVariant(status: AddressStatus): 'success' | 'neutral' | 'warning' | 'primary' | 'info' | 'secondary' {
  return ADDRESS_STATUS_COLORS[status]
}

export function getTypeVariant(type: AddressType): 'primary' | 'success' | 'info' | 'secondary' {
  return ADDRESS_TYPE_COLORS[type]
}

export function getTypeLabel(type: AddressType): string {
  return ADDRESS_TYPE_LABELS[type]
}

export function getStatusLabel(status: AddressStatus): string {
  return ADDRESS_STATUS_LABELS[status]
}

export function getCountryLabel(country: Country): string {
  return COUNTRY_LABELS[country]
}
