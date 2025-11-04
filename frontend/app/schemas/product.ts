import { z } from 'zod'

// Product Enums
export const productTypeEnum = z.enum(['SERVICE', 'TARIFF', 'BUNDLE', 'ADDON'])
export const productCategoryEnum = z.enum(['MOBILE', 'BROADBAND', 'TV', 'CLOUD'])
export const productStatusEnum = z.enum(['ACTIVE', 'INACTIVE', 'DEPRECATED'])

// Product Entity
export const productSchema = z.object({
  id: z.string().uuid(),
  productCode: z.string().min(3, 'Product code must be at least 3 characters').max(50, 'Product code must not exceed 50 characters'),
  name: z.string().min(1, 'Name is required').max(200, 'Name must not exceed 200 characters'),
  description: z.string().optional(),
  productType: productTypeEnum,
  category: productCategoryEnum,
  price: z.number().min(0, 'Price must be positive'),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  billingPeriod: z.string().min(1, 'Billing period is required').max(20, 'Billing period must not exceed 20 characters'),
  status: productStatusEnum,
  validityStart: z.string().date().optional().nullable(),
  validityEnd: z.string().date().optional().nullable(),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  createdBy: z.string().optional(),
  updatedBy: z.string().optional(),
  version: z.number()
})

// Create Product Command
export const createProductSchema = z.object({
  productCode: z.string().min(3, 'Product code must be at least 3 characters').max(50, 'Product code must not exceed 50 characters'),
  name: z.string().min(1, 'Name is required').max(200, 'Name must not exceed 200 characters'),
  description: z.string().optional(),
  productType: productTypeEnum,
  category: productCategoryEnum,
  price: z.number().min(0, 'Price must be positive'),
  currency: z.string().length(3, 'Currency must be 3 characters').default('PLN'),
  billingPeriod: z.string().min(1, 'Billing period is required').max(20, 'Billing period must not exceed 20 characters'),
  status: productStatusEnum.default('ACTIVE'),
  validityStart: z.string().date().optional().or(z.literal('')),
  validityEnd: z.string().date().optional().or(z.literal(''))
})

// Update Product Command
export const updateProductSchema = createProductSchema.extend({
  id: z.string().uuid(),
  version: z.number()
})

// Change Product Status Command
export const changeProductStatusSchema = z.object({
  id: z.string().uuid(),
  status: productStatusEnum
})

// Product Search Params
export const productSearchSchema = z.object({
  searchTerm: z.string().optional(),
  status: productStatusEnum.optional(),
  type: productTypeEnum.optional(),
  category: productCategoryEnum.optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Product List Response
export const productListResponseSchema = z.object({
  content: z.array(productSchema),
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
export const productFormDataSchema = createProductSchema

// Validation helpers
export function validateProduct(data: unknown) {
  return productSchema.parse(data)
}

export function validateCreateProduct(data: unknown) {
  return createProductSchema.parse(data)
}

export function validateUpdateProduct(data: unknown) {
  return updateProductSchema.parse(data)
}

// Export types
export type Product = z.infer<typeof productSchema>
export type CreateProductCommand = z.infer<typeof createProductSchema>
export type UpdateProductCommand = z.infer<typeof updateProductSchema>
export type ChangeProductStatusCommand = z.infer<typeof changeProductStatusSchema>
export type ProductSearchParams = z.infer<typeof productSearchSchema>
export type ProductListResponse = z.infer<typeof productListResponseSchema>
export type ProductFormData = z.infer<typeof productFormDataSchema>
export type ProductType = z.infer<typeof productTypeEnum>
export type ProductCategory = z.infer<typeof productCategoryEnum>
export type ProductStatus = z.infer<typeof productStatusEnum>

// Status labels
export const PRODUCT_TYPE_LABELS: Record<ProductType, string> = {
  SERVICE: 'Service',
  TARIFF: 'Tariff',
  BUNDLE: 'Bundle',
  ADDON: 'Add-on'
}

export const PRODUCT_CATEGORY_LABELS: Record<ProductCategory, string> = {
  MOBILE: 'Mobile',
  BROADBAND: 'Broadband',
  TV: 'TV',
  CLOUD: 'Cloud'
}

export const PRODUCT_STATUS_LABELS: Record<ProductStatus, string> = {
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  DEPRECATED: 'Deprecated'
}

export const PRODUCT_STATUS_COLORS: Record<ProductStatus, string> = {
  ACTIVE: 'success',
  INACTIVE: 'neutral',
  DEPRECATED: 'warning'
}

// Utility functions
export function formatPrice(product: Product): string {
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency',
    currency: product.currency
  }).format(product.price)
}

export function getProductValidity(product: Product): string {
  const start = product.validityStart ? new Date(product.validityStart) : null
  const end = product.validityEnd ? new Date(product.validityEnd) : null

  if (!start && !end) return 'No expiry'

  const formatDate = (date: Date) => date.toLocaleDateString('pl-PL')

  if (start && end) {
    return `${formatDate(start)} - ${formatDate(end)}`
  }

  return start ? `From ${formatDate(start)}` : `Until ${formatDate(end!)}`
}

export function isProductActive(product: Product): boolean {
  if (product.status !== 'ACTIVE') return false

  const now = new Date()
  const start = product.validityStart ? new Date(product.validityStart) : null
  const end = product.validityEnd ? new Date(product.validityEnd) : null

  if (start && now < start) return false
  if (end && now > end) return false

  return true
}

export function getStatusVariant(status: ProductStatus): 'success' | 'neutral' | 'warning' {
  return PRODUCT_STATUS_COLORS[status]
}
