// Export all schemas and types
export * from './customer'
export * from './product'
export * from './order'
export * from './invoice'
export * from './payment'
export * from './subscription'

// Common validation utilities
import { z } from 'zod'

// Pagination schema
export const paginationSchema = z.object({
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// Common response wrapper
export const apiResponseSchema = <T extends z.ZodType>(dataSchema: T) => z.object({
  data: dataSchema,
  ok: z.boolean(),
  status: z.number(),
  statusText: z.string(),
  headers: z.instanceof(Headers)
})

// Common error response
export const errorResponseSchema = z.object({
  message: z.string(),
  error: z.string().optional(),
  status: z.number().optional(),
  timestamp: z.string().optional(),
  path: z.string().optional()
})

export type PaginationParams = z.infer<typeof paginationSchema>
