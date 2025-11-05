import { z } from 'zod'

// Coverage Node Type Enum
export const coverageNodeTypeEnum = z.enum([
  'CELL_TOWER',
  'SATELLITE',
  'FIBER_HUB',
  'WIFI_HOTSPOT',
  'MICROWAVE',
  'DATA_CENTER',
  'EXCHANGE_POINT'
])

// Coverage Node Status Enum
export const coverageNodeStatusEnum = z.enum(['ACTIVE', 'INACTIVE', 'MAINTENANCE', 'PLANNED', 'DECOMMISSIONED'])

// Technology Enum
export const technologyEnum = z.enum([
  '2G',
  '3G',
  '4G',
  '5G',
  'LTE',
  'WIFI',
  'FIBER',
  'SATELLITE',
  'MICROWAVE'
])

// Coverage Node Entity
export const coverageNodeSchema = z.object({
  id: z.string(),
  name: z.string().min(1, 'Name is required').max(100, 'Name must not exceed 100 characters'),
  code: z.string().regex(/^[A-Z0-9-_]+$/, 'Code must be uppercase letters, numbers, hyphens or underscores').max(20, 'Code must not exceed 20 characters'),
  type: coverageNodeTypeEnum,
  typeDisplayName: z.string(),
  status: coverageNodeStatusEnum,
  statusDisplayName: z.string(),
  technology: technologyEnum,
  technologyDisplayName: z.string(),

  // Location
  latitude: z.number().min(-90, 'Latitude must be between -90 and 90').max(90, 'Latitude must be between -90 and 90'),
  longitude: z.number().min(-180, 'Longitude must be between -180 and 180').max(180, 'Longitude must be between -180 and 180'),
  address: z.string().max(200, 'Address must not exceed 200 characters').optional().or(z.literal('')),
  city: z.string().max(100, 'City must not exceed 100 characters'),
  region: z.string().max(100, 'Region must not exceed 100 characters').optional().or(z.literal('')),
  country: z.string().max(100, 'Country must not exceed 100 characters'),

  // Coverage
  coverageRadius: z.number().min(0.1, 'Coverage radius must be at least 0.1 km').max(1000, 'Coverage radius must not exceed 1000 km'),
  coverageArea: z.number().optional(), // Calculated in km²
  addressCount: z.number().min(0).default(0), // Number of addresses in coverage

  // Capacity
  maxCapacity: z.number().min(1, 'Max capacity must be at least 1'),
  currentLoad: z.number().min(0).default(0),
  capacityPercentage: z.number().min(0).max(100),

  // Technical
  equipmentCount: z.number().min(0).default(0),
  uptime: z.number().min(0).max(100).optional(), // Percentage
  lastMaintenance: z.string().datetime().optional(),
  nextMaintenance: z.string().datetime().optional(),

  // Metadata
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  version: z.number()
})

// Create Coverage Node Command
export const createCoverageNodeSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100, 'Name must not exceed 100 characters'),
  code: z.string().regex(/^[A-Z0-9-_]+$/, 'Code must be uppercase letters, numbers, hyphens or underscores').max(20, 'Code must not exceed 20 characters'),
  type: coverageNodeTypeEnum,
  technology: technologyEnum,
  latitude: z.number().min(-90, 'Latitude must be between -90 and 90').max(90, 'Latitude must be between -90 and 90'),
  longitude: z.number().min(-180, 'Longitude must be between -180 and 180').max(180, 'Longitude must be between -180 and 180'),
  address: z.string().max(200, 'Address must not exceed 200 characters').optional().or(z.literal('')),
  city: z.string().max(100, 'City must not exceed 100 characters'),
  region: z.string().max(100, 'Region must not exceed 100 characters').optional().or(z.literal('')),
  country: z.string().max(100, 'Country must not exceed 100 characters'),
  coverageRadius: z.number().min(0.1, 'Coverage radius must be at least 0.1 km').max(1000, 'Coverage radius must not exceed 1000 km'),
  maxCapacity: z.number().min(1, 'Max capacity must be at least 1')
})

// Update Coverage Node Command
export const updateCoverageNodeSchema = createCoverageNodeSchema.extend({
  id: z.string(),
  version: z.number()
})

// Change Coverage Node Status Command
export const changeCoverageNodeStatusSchema = z.object({
  id: z.string(),
  status: coverageNodeStatusEnum
})

// Coverage Node Search Params
export const coverageNodeSearchSchema = z.object({
  searchTerm: z.string().optional(),
  type: coverageNodeTypeEnum.optional(),
  status: coverageNodeStatusEnum.optional(),
  technology: technologyEnum.optional(),
  city: z.string().optional(),
  country: z.string().optional(),
  minCapacity: z.number().optional(),
  maxCapacity: z.number().optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Coverage Node List Response
export const coverageNodeListResponseSchema = z.object({
  content: z.array(coverageNodeSchema),
  page: z.number(),
  size: z.number(),
  totalElements: z.number(),
  totalPages: z.number(),
  first: z.boolean(),
  last: z.boolean(),
  numberOfElements: z.number(),
  empty: z.boolean()
})

// Coverage Statistics Response
export const coverageStatisticsSchema = z.object({
  totalNodes: z.number(),
  activeNodes: z.number(),
  inactiveNodes: z.number(),
  maintenanceNodes: z.number(),
  plannedNodes: z.number(),
  averageCapacity: z.number(),
  totalCoverageArea: z.number(),
  technologyBreakdown: z.record(z.string(), z.number()),
  typeBreakdown: z.record(z.string(), z.number())
})

// Form Data for UI
export const coverageNodeFormDataSchema = createCoverageNodeSchema

// Validation helper
export function validateCoverageNode(data: unknown) {
  return coverageNodeSchema.parse(data)
}

export function validateCreateCoverageNode(data: unknown) {
  return createCoverageNodeSchema.parse(data)
}

export function validateUpdateCoverageNode(data: unknown) {
  return updateCoverageNodeSchema.parse(data)
}

// Export types
export type CoverageNode = z.infer<typeof coverageNodeSchema>
export type CreateCoverageNodeCommand = z.infer<typeof createCoverageNodeSchema>
export type UpdateCoverageNodeCommand = z.infer<typeof updateCoverageNodeSchema>
export type ChangeCoverageNodeStatusCommand = z.infer<typeof changeCoverageNodeStatusSchema>
export type CoverageNodeSearchParams = z.infer<typeof coverageNodeSearchSchema>
export type CoverageNodeListResponse = z.infer<typeof coverageNodeListResponseSchema>
export type CoverageStatistics = z.infer<typeof coverageStatisticsSchema>
export type CoverageNodeFormData = z.infer<typeof coverageNodeFormDataSchema>
export type CoverageNodeType = z.infer<typeof coverageNodeTypeEnum>
export type CoverageNodeStatus = z.infer<typeof coverageNodeStatusEnum>
export type Technology = z.infer<typeof technologyEnum>

// Status labels
export const COVERAGE_NODE_STATUS_LABELS: Record<CoverageNodeStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  MAINTENANCE: 'Maintenance',
  PLANNED: 'Planned',
  DECOMMISSIONED: 'Decommissioned'
}

export const COVERAGE_NODE_STATUS_COLORS: Record<CoverageNodeStatus, string> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  MAINTENANCE: 'warning',
  PLANNED: 'info',
  DECOMMISSIONED: 'danger'
}

// Type labels
export const COVERAGE_NODE_TYPE_LABELS: Record<CoverageNodeType, string> = {
  CELL_TOWER: 'Cell Tower',
  SATELLITE: 'Satellite',
  FIBER_HUB: 'Fiber Hub',
  WIFI_HOTSPOT: 'WiFi Hotspot',
  MICROWAVE: 'Microwave',
  DATA_CENTER: 'Data Center',
  EXCHANGE_POINT: 'Exchange Point'
}

export const COVERAGE_NODE_TYPE_ICONS: Record<CoverageNodeType, string> = {
  CELL_TOWER: 'pi pi-signal',
  SATELLITE: 'pi pi-star',
  FIBER_HUB: 'pi pi-share-alt',
  WIFI_HOTSPOT: 'pi pi-wifi',
  MICROWAVE: 'pi pi-bolt',
  DATA_CENTER: 'pi pi-database',
  EXCHANGE_POINT: 'pi pi-refresh'
}

// Technology labels
export const TECHNOLOGY_LABELS: Record<Technology, string> = {
  '2G': '2G',
  '3G': '3G',
  '4G': '4G',
  '5G': '5G',
  'LTE': 'LTE',
  'WIFI': 'WiFi',
  'FIBER': 'Fiber',
  'SATELLITE': 'Satellite',
  'MICROWAVE': 'Microwave'
}

// Utility functions
export function formatCoordinates(node: CoverageNode): string {
  return `${node.latitude.toFixed(6)}, ${node.longitude.toFixed(6)}`
}

export function formatCoverageArea(radiusKm: number): string {
  const area = Math.PI * Math.pow(radiusKm, 2)
  return `${area.toFixed(2)} km²`
}

export function formatCapacity(load: number, max: number): string {
  const percentage = Math.round((load / max) * 100)
  return `${load}/${max} (${percentage}%)`
}

export function getStatusVariant(status: CoverageNodeStatus): 'success' | 'neutral' | 'warning' | 'info' | 'danger' {
  return COVERAGE_NODE_STATUS_COLORS[status]
}

export function getTypeIcon(type: CoverageNodeType): string {
  return COVERAGE_NODE_TYPE_ICONS[type]
}

export function getTypeLabel(type: CoverageNodeType): string {
  return COVERAGE_NODE_TYPE_LABELS[type]
}

export function getStatusLabel(status: CoverageNodeStatus): string {
  return COVERAGE_NODE_STATUS_LABELS[status]
}

export function getTechnologyLabel(technology: Technology): string {
  return TECHNOLOGY_LABELS[technology]
}

export function calculateCoverageArea(radiusKm: number): number {
  return Math.PI * Math.pow(radiusKm, 2)
}

export function calculateCapacityPercentage(current: number, max: number): number {
  if (max === 0) return 0
  return Math.min(Math.round((current / max) * 100), 100)
}

export function isNodeOverloaded(node: CoverageNode): boolean {
  return node.capacityPercentage > 90
}

export function isNodeHealthy(node: CoverageNode): boolean {
  return node.status === 'ACTIVE' && node.capacityPercentage < 80
}
