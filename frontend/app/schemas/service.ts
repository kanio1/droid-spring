import { z } from 'zod'

// Service Type Enum
export const serviceTypeEnum = z.enum([
  'INTERNET',
  'VOICE',
  'TELEVISION',
  'MOBILE',
  'CLOUD_SERVICES',
  'IoT',
  'VPN',
  'CDN',
  'SECURITY',
  'CONSULTING'
])

// Service Status Enum
export const serviceStatusEnum = z.enum(['ACTIVE', 'INACTIVE', 'PLANNED', 'DEPRECATED', 'SUSPENDED'])

// Service Category Enum
export const serviceCategoryEnum = z.enum([
  'BROADBAND',
  'VOICE',
  'VIDEO',
  'MOBILE',
  'CLOUD',
  'ENTERPRISE',
  'EMERGING'
])

// Technology Enum (reuse from coverage-node, but define here for independence)
export const serviceTechnologyEnum = z.enum([
  'DSL',
  'FIBER',
  'CABLE',
  '4G',
  '5G',
  'WIFI',
  'SATELLITE',
  'ETHERNET',
  'VOIP',
  'CLOUD_NATIVE'
])

// Billing Cycle Enum
export const billingCycleEnum = z.enum(['MONTHLY', 'QUARTERLY', 'YEARLY', 'ONE_TIME'])

// Service Entity
export const serviceSchema = z.object({
  id: z.string(),
  name: z.string().min(1, 'Name is required').max(100, 'Name must not exceed 100 characters'),
  code: z.string().regex(/^[A-Z0-9-_]+$/, 'Code must be uppercase letters, numbers, hyphens or underscores').max(20, 'Code must not exceed 20 characters'),
  type: serviceTypeEnum,
  typeDisplayName: z.string(),
  category: serviceCategoryEnum,
  categoryDisplayName: z.string(),
  status: serviceStatusEnum,
  statusDisplayName: z.string(),
  technology: serviceTechnologyEnum,
  technologyDisplayName: z.string(),

  // Description
  description: z.string().max(1000, 'Description must not exceed 1000 characters').optional().or(z.literal('')),

  // Pricing
  price: z.number().min(0, 'Price must be positive').max(999999.99, 'Price must not exceed 999999.99'),
  currency: z.string().length(3, 'Currency must be 3-letter code'),
  billingCycle: billingCycleEnum,

  // Service Details
  dataLimit: z.number().min(0).optional(), // GB/month, null for unlimited
  speed: z.number().min(0).optional(), // Mbps
  voiceMinutes: z.number().min(0).optional(), // minutes/month
  smsCount: z.number().min(0).optional(), // SMS/month

  // Coverage Requirements
  requiredCoverageNodes: z.array(z.string()).min(0),
  coverageNodeCount: z.number().min(0).default(0),

  // Features
  features: z.array(z.string()).max(50, 'Maximum 50 features'),

  // Customer Count
  activeCustomerCount: z.number().min(0).default(0),
  maxCustomerCount: z.number().min(0).optional(),

  // SLA
  slaUptime: z.number().min(0).max(100).optional(), // percentage
  supportLevel: z.enum(['BASIC', 'STANDARD', 'PREMIUM', 'ENTERPRISE']).default('STANDARD'),

  // Technical
  bandwidth: z.number().min(0).optional(), // Mbps
  latency: z.number().min(0).optional(), // ms
  coverageArea: z.number().optional(), // km²

  // Metadata
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  version: z.number()
})

// Create Service Command
export const createServiceSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100, 'Name must not exceed 100 characters'),
  code: z.string().regex(/^[A-Z0-9-_]+$/, 'Code must be uppercase letters, numbers, hyphens or underscores').max(20, 'Code must not exceed 20 characters'),
  type: serviceTypeEnum,
  category: serviceCategoryEnum,
  technology: serviceTechnologyEnum,
  description: z.string().max(1000, 'Description must not exceed 1000 characters').optional().or(z.literal('')),
  price: z.number().min(0, 'Price must be positive').max(999999.99, 'Price must not exceed 999999.99'),
  currency: z.string().length(3, 'Currency must be 3-letter code'),
  billingCycle: billingCycleEnum,
  dataLimit: z.number().min(0).optional(),
  speed: z.number().min(0).optional(),
  voiceMinutes: z.number().min(0).optional(),
  smsCount: z.number().min(0).optional(),
  requiredCoverageNodes: z.array(z.string()).optional().default([]),
  features: z.array(z.string()).max(50, 'Maximum 50 features').optional().default([]),
  maxCustomerCount: z.number().min(0).optional(),
  slaUptime: z.number().min(0).max(100).optional(),
  supportLevel: z.enum(['BASIC', 'STANDARD', 'PREMIUM', 'ENTERPRISE']).default('STANDARD'),
  bandwidth: z.number().min(0).optional(),
  latency: z.number().min(0).optional(),
  coverageArea: z.number().optional()
})

// Update Service Command
export const updateServiceSchema = createServiceSchema.extend({
  id: z.string(),
  version: z.number()
})

// Change Service Status Command
export const changeServiceStatusSchema = z.object({
  id: z.string(),
  status: serviceStatusEnum
})

// Activate Service for Customer Command
export const activateServiceSchema = z.object({
  serviceId: z.string(),
  customerId: z.string(),
  startDate: z.string().datetime().optional(),
  notes: z.string().max(500, 'Notes must not exceed 500 characters').optional().or(z.literal(''))
})

// Deactivate Service Command
export const deactivateServiceSchema = z.object({
  serviceId: z.string(),
  customerId: z.string(),
  endDate: z.string().datetime().optional(),
  reason: z.string().max(500, 'Reason must not exceed 500 characters').optional().or(z.literal(''))
})

// Service Search Params
export const serviceSearchSchema = z.object({
  searchTerm: z.string().optional(),
  type: serviceTypeEnum.optional(),
  category: serviceCategoryEnum.optional(),
  status: serviceStatusEnum.optional(),
  technology: serviceTechnologyEnum.optional(),
  minPrice: z.number().optional(),
  maxPrice: z.number().optional(),
  currency: z.string().optional(),
  billingCycle: billingCycleEnum.optional(),
  hasDataLimit: z.boolean().optional(),
  hasSpeed: z.boolean().optional(),
  hasVoice: z.boolean().optional(),
  hasSms: z.boolean().optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Service List Response
export const serviceListResponseSchema = z.object({
  content: z.array(serviceSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
  first: z.boolean(),
  last: z.boolean(),
  numberOfElements: z.number(),
  empty: z.boolean()
})

// Service Statistics Response
export const serviceStatisticsSchema = z.object({
  totalServices: z.number(),
  activeServices: z.number(),
  inactiveServices: z.number(),
  plannedServices: z.number(),
  averagePrice: z.number(),
  totalRevenue: z.number(),
  categoryBreakdown: z.record(z.string(), z.number()),
  typeBreakdown: z.record(z.string(), z.number()),
  technologyBreakdown: z.record(z.string(), z.number()),
  mostPopularService: z.string().optional(),
  topRevenueService: z.string().optional()
})

// Form Data for UI
export const serviceFormDataSchema = createServiceSchema

// Validation helpers
export function validateService(data: unknown) {
  return serviceSchema.parse(data)
}

export function validateCreateService(data: unknown) {
  return createServiceSchema.parse(data)
}

export function validateUpdateService(data: unknown) {
  return updateServiceSchema.parse(data)
}

// Export types
export type Service = z.infer<typeof serviceSchema>
export type CreateServiceCommand = z.infer<typeof createServiceSchema>
export type UpdateServiceCommand = z.infer<typeof updateServiceSchema>
export type ChangeServiceStatusCommand = z.infer<typeof changeServiceStatusSchema>
export type ActivateServiceCommand = z.infer<typeof activateServiceSchema>
export type DeactivateServiceCommand = z.infer<typeof deactivateServiceSchema>
export type ServiceSearchParams = z.infer<typeof serviceSearchSchema>
export type ServiceListResponse = z.infer<typeof serviceListResponseSchema>
export type ServiceStatistics = z.infer<typeof serviceStatisticsSchema>
export type ServiceFormData = z.infer<typeof serviceFormDataSchema>
export type ServiceType = z.infer<typeof serviceTypeEnum>
export type ServiceStatus = z.infer<typeof serviceStatusEnum>
export type ServiceCategory = z.infer<typeof serviceCategoryEnum>
export type ServiceTechnology = z.infer<typeof serviceTechnologyEnum>
export type BillingCycle = z.infer<typeof billingCycleEnum>

// Status labels
export const SERVICE_STATUS_LABELS: Record<ServiceStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  PLANNED: 'Planned',
  DEPRECATED: 'Deprecated',
  SUSPENDED: 'Suspended'
}

export const SERVICE_STATUS_COLORS: Record<ServiceStatus, string> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  PLANNED: 'info',
  DEPRECATED: 'danger',
  SUSPENDED: 'warning'
}

// Type labels
export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  INTERNET: 'Internet',
  VOICE: 'Voice',
  TELEVISION: 'Television',
  MOBILE: 'Mobile',
  CLOUD_SERVICES: 'Cloud Services',
  IoT: 'IoT',
  VPN: 'VPN',
  CDN: 'CDN',
  SECURITY: 'Security',
  CONSULTING: 'Consulting'
}

export const SERVICE_TYPE_ICONS: Record<ServiceType, string> = {
  INTERNET: 'pi pi-globe',
  VOICE: 'pi pi-phone',
  TELEVISION: 'pi pi-video',
  MOBILE: 'pi pi-mobile',
  CLOUD_SERVICES: 'pi pi-cloud',
  IoT: 'pi pi-microchip',
  VPN: 'pi pi-lock',
  CDN: 'pi pi-server',
  SECURITY: 'pi pi-shield',
  CONSULTING: 'pi pi-users'
}

// Category labels
export const SERVICE_CATEGORY_LABELS: Record<ServiceCategory, string> = {
  BROADBAND: 'Broadband',
  VOICE: 'Voice',
  VIDEO: 'Video',
  MOBILE: 'Mobile',
  CLOUD: 'Cloud',
  ENTERPRISE: 'Enterprise',
  EMERGING: 'Emerging'
}

// Technology labels
export const SERVICE_TECHNOLOGY_LABELS: Record<ServiceTechnology, string> = {
  DSL: 'DSL',
  FIBER: 'Fiber',
  CABLE: 'Cable',
  '4G': '4G',
  '5G': '5G',
  WIFI: 'WiFi',
  SATELLITE: 'Satellite',
  ETHERNET: 'Ethernet',
  VOIP: 'VoIP',
  CLOUD_NATIVE: 'Cloud Native'
}

// Billing cycle labels
export const BILLING_CYCLE_LABELS: Record<BillingCycle, string> = {
  MONTHLY: 'Monthly',
  QUARTERLY: 'Quarterly',
  YEARLY: 'Yearly',
  ONE_TIME: 'One-time'
}

// Utility functions
export function formatPrice(price: number, currency: string): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
  }).format(price)
}

export function formatDataLimit(gb: number | undefined): string {
  if (!gb) return 'Unlimited'
  return `${gb.toLocaleString()} GB/month`
}

export function formatSpeed(mbps: number | undefined): string {
  if (!mbps) return 'N/A'
  return `${mbps.toLocaleString()} Mbps`
}

export function formatVoiceMinutes(minutes: number | undefined): string {
  if (!minutes) return 'N/A'
  return `${minutes.toLocaleString()} minutes/month`
}

export function formatSmsCount(count: number | undefined): string {
  if (!count) return 'N/A'
  return `${count.toLocaleString()} SMS/month`
}

export function getStatusVariant(status: ServiceStatus): 'success' | 'neutral' | 'warning' | 'info' | 'danger' {
  return SERVICE_STATUS_COLORS[status]
}

export function getTypeIcon(type: ServiceType): string {
  return SERVICE_TYPE_ICONS[type]
}

export function getTypeLabel(type: ServiceType): string {
  return SERVICE_TYPE_LABELS[type]
}

export function getStatusLabel(status: ServiceStatus): string {
  return SERVICE_STATUS_LABELS[status]
}

export function getCategoryLabel(category: ServiceCategory): string {
  return SERVICE_CATEGORY_LABELS[category]
}

export function getTechnologyLabel(technology: ServiceTechnology): string {
  return SERVICE_TECHNOLOGY_LABELS[technology]
}

export function getBillingCycleLabel(cycle: BillingCycle): string {
  return BILLING_CYCLE_LABELS[cycle]
}

export function isActive(service: Service): boolean {
  return service.status === 'ACTIVE'
}

export function isUnlimited(service: Service): boolean {
  return !service.dataLimit
}

export function calculateAnnualPrice(monthlyPrice: number): number {
  return monthlyPrice * 12
}

export function getSupportLevelColor(level: string): 'success' | 'neutral' | 'warning' | 'info' | 'danger' {
  const colors: Record<string, 'success' | 'neutral' | 'warning' | 'info' | 'danger'> = {
    'BASIC': 'neutral',
    'STANDARD': 'info',
    'PREMIUM': 'success',
    'ENTERPRISE': 'warning'
  }
  return colors[level] || 'neutral'
}
