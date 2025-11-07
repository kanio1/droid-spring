import { z } from 'zod'

// User Status Enum
export const userStatusEnum = z.enum(['ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION', 'TERMINATED'])

// User Entity
export const userSchema = z.object({
  id: z.string(),
  keycloakId: z.string(),
  firstName: z.string().min(2, 'First name must be at least 2 characters').max(100, 'First name must not exceed 100 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters').max(100, 'Last name must not exceed 100 characters'),
  email: z.string().email('Invalid email address'),
  fullName: z.string(),
  status: userStatusEnum,
  roles: z.array(z.string()),
  createdAt: z.string().datetime(),
  updatedAt: z.string().datetime(),
  isActive: z.boolean(),
  isTerminated: z.boolean()
})

// Create User Command
export const createUserSchema = z.object({
  firstName: z.string().min(2, 'First name must be at least 2 characters').max(100, 'First name must not exceed 100 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters').max(100, 'Last name must not exceed 100 characters'),
  email: z.string().email('Invalid email address'),
  keycloakId: z.string().min(1, 'Keycloak ID is required')
})

// Update User Command
export const updateUserSchema = z.object({
  id: z.string(),
  firstName: z.string().min(2, 'First name must be at least 2 characters').max(100, 'First name must not exceed 100 characters'),
  lastName: z.string().min(2, 'Last name must be at least 2 characters').max(100, 'Last name must not exceed 100 characters'),
  email: z.string().email('Invalid email address')
})

// Assign Roles Command
export const assignRolesSchema = z.object({
  id: z.string(),
  roles: z.array(z.string()).min(1, 'At least one role is required')
})

// Change User Status Command
export const changeUserStatusSchema = z.object({
  id: z.string(),
  status: userStatusEnum
})

// User Search Params
export const userSearchSchema = z.object({
  searchTerm: z.string().optional(),
  status: userStatusEnum.optional(),
  page: z.number().min(0).default(0),
  size: z.number().min(1).max(100).default(20),
  sort: z.string().default('createdAt,desc')
})

// User List Response
export const userListResponseSchema = z.object({
  content: z.array(userSchema),
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
export const userFormDataSchema = createUserSchema

// Type exports
export type User = z.infer<typeof userSchema>
export type CreateUserCommand = z.infer<typeof createUserSchema>
export type UpdateUserCommand = z.infer<typeof updateUserSchema>
export type AssignRolesCommand = z.infer<typeof assignRolesSchema>
export type ChangeUserStatusCommand = z.infer<typeof changeUserStatusSchema>
export type UserSearchParams = z.infer<typeof userSearchSchema>
export type UserListResponse = z.infer<typeof userListResponseSchema>
export type UserStatus = z.infer<typeof userStatusEnum>

// Validation helpers
export function validateUser(data: unknown) {
  return userSchema.parse(data)
}

export function validateCreateUser(data: unknown) {
  return createUserSchema.parse(data)
}

export function validateUpdateUser(data: unknown) {
  return updateUserSchema.parse(data)
}

export function validateAssignRoles(data: unknown) {
  return assignRolesSchema.parse(data)
}

export function validateChangeUserStatus(data: unknown) {
  return changeUserStatusSchema.parse(data)
}
