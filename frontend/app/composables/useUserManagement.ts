// User management composable
import type { ApiListResponse } from './useApi'

export interface User {
  id: string
  keycloakId: string
  firstName: string
  lastName: string
  email: string
  fullName: string
  status: UserStatus
  roles: string[]
  createdAt: string
  updatedAt: string
  isActive: boolean
  isTerminated: boolean
}

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION' | 'TERMINATED'

export interface CreateUserRequest {
  firstName: string
  lastName: string
  email: string
  keycloakId: string
}

export interface UpdateUserRequest {
  firstName: string
  lastName: string
  email: string
}

export interface AssignRolesRequest {
  roles: string[]
}

export interface UserFilters {
  search?: string
  status?: UserStatus
  role?: string
  page?: number
  size?: number
  sort?: string
}

export const useUserManagement = () => {
  const api = useApi()
  const toast = useToast()
  const users = ref<User[]>([])
  const loading = ref(false)
  const currentUser = ref<User | null>(null)

  // Get users with filters
  const getUsers = async (filters: UserFilters = {}) => {
    loading.value = true
    try {
      const query: Record<string, any> = {
        page: filters.page || 0,
        size: filters.size || 20,
        sort: filters.sort || 'createdAt,desc'
      }

      if (filters.search) query.search = filters.search
      if (filters.status) query.status = filters.status
      if (filters.role) query.role = filters.role

      const response = await api.read<ApiListResponse<User>>('/admin/users', query)
      users.value = response.data.content
      return response.data
    } catch (error) {
      console.error('Failed to fetch users:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // Get user by ID
  const getUserById = async (id: string) => {
    loading.value = true
    try {
      const response = await api.read<User>(`/admin/users/${id}`)
      currentUser.value = response.data
      return response.data
    } catch (error) {
      console.error('Failed to fetch user:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // Create user
  const createUser = async (userData: CreateUserRequest): Promise<User> => {
    loading.value = true
    try {
      const response = await api.create<User>('/admin/users', userData)
      api.handleSuccess('User created successfully')
      return response.data
    } catch (error) {
      console.error('Failed to create user:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // Update user
  const updateUser = async (id: string, userData: UpdateUserRequest): Promise<User> => {
    loading.value = true
    try {
      const response = await api.update<User>(`/admin/users/${id}`, userData)
      api.handleSuccess('User updated successfully')
      return response.data
    } catch (error) {
      console.error('Failed to update user:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // Assign roles to user
  const assignRoles = async (id: string, roles: string[]): Promise<User> => {
    loading.value = true
    try {
      const response = await api.update<User>(`/admin/users/${id}/roles`, { roles })
      api.handleSuccess('Roles assigned successfully')
      return response.data
    } catch (error) {
      console.error('Failed to assign roles:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // Change user status
  const changeUserStatus = async (id: string, status: UserStatus): Promise<User> => {
    loading.value = true
    try {
      const response = await api.update<User>(`/admin/users/${id}/status`, null, {
        query: { status }
      })
      api.handleSuccess('User status changed successfully')
      return response.data
    } catch (error) {
      console.error('Failed to change user status:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // Delete user (soft delete - sets status to TERMINATED)
  const deleteUser = async (id: string): Promise<void> => {
    loading.value = true
    try {
      await api.remove(`/admin/users/${id}`)
      api.handleSuccess('User deleted successfully')
    } catch (error) {
      console.error('Failed to delete user:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // Get user statistics
  const getUserStats = async () => {
    try {
      const response = await api.read<any>('/admin/users/stats')
      return response.data
    } catch (error) {
      console.error('Failed to fetch user statistics:', error)
      throw error
    }
  }

  // Get all available roles
  const getAvailableRoles = async (): Promise<string[]> => {
    try {
      const response = await api.read<string[]>('/admin/roles')
      return response.data
    } catch (error) {
      console.error('Failed to fetch roles:', error)
      throw error
    }
  }

  return {
    // State
    users: readonly(users),
    currentUser: readonly(currentUser),
    loading: readonly(loading),

    // Actions
    getUsers,
    getUserById,
    createUser,
    updateUser,
    assignRoles,
    changeUserStatus,
    deleteUser,
    getUserStats,
    getAvailableRoles
  }
}
