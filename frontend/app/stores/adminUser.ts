import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  User,
  CreateUserCommand,
  UpdateUserCommand,
  AssignRolesCommand,
  ChangeUserStatusCommand,
  UserSearchParams,
  UserListResponse,
  UserStatus
} from '~/schemas/adminUser'

export const useAdminUserStore = defineStore('adminUser', () => {
  // State
  const users = ref<User[]>([])
  const currentUser = ref<User | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const pagination = reactive({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: false,
    numberOfElements: 0,
    empty: true
  })

  // Getters
  const userCount = computed(() => users.value.length)
  const activeUsers = computed(() => users.value.filter(u => u.status === 'ACTIVE'))
  const inactiveUsers = computed(() => users.value.filter(u => u.status === 'INACTIVE'))
  const suspendedUsers = computed(() => users.value.filter(u => u.status === 'SUSPENDED'))
  const pendingUsers = computed(() => users.value.filter(u => u.status === 'PENDING_VERIFICATION'))
  const terminatedUsers = computed(() => users.value.filter(u => u.status === 'TERMINATED'))

  const getUserById = (id: string) => computed(() =>
    users.value.find(u => u.id === id)
  )

  // Actions
  async function fetchUsers(params: Partial<UserSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const query = {
        page: params.page ?? pagination.page,
        size: params.size ?? pagination.size,
        sort: params.sort ?? 'createdAt,desc'
      }

      if (params.searchTerm) {
        query.searchTerm = params.searchTerm
      }

      if (params.status) {
        query.status = params.status
      }

      const response = await get<UserListResponse>('/api/admin/users', { params: query })

      users.value = response.content
      pagination.page = response.page
      pagination.size = response.size
      pagination.totalElements = response.totalElements
      pagination.totalPages = response.totalPages
      pagination.first = response.first
      pagination.last = response.last
      pagination.numberOfElements = response.numberOfElements
      pagination.empty = response.empty

      return response
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch users'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchUserById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const user = await get<User>(`/api/admin/users/${id}`)

      const index = users.value.findIndex(u => u.id === id)
      if (index !== -1) {
        users.value[index] = user
      } else {
        users.value.push(user)
      }

      currentUser.value = user
      return user
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch user'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createUser(command: CreateUserCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const user = await post<User>('/api/admin/users', command)

      users.value.unshift(user)
      return user
    } catch (err: any) {
      error.value = err.message || 'Failed to create user'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateUser(id: string, command: UpdateUserCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const user = await put<User>(`/api/admin/users/${id}`, command)

      const index = users.value.findIndex(u => u.id === id)
      if (index !== -1) {
        users.value[index] = user
      }

      if (currentUser.value?.id === id) {
        currentUser.value = user
      }

      return user
    } catch (err: any) {
      error.value = err.message || 'Failed to update user'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function assignRoles(id: string, command: AssignRolesCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const user = await post<User>(`/api/admin/users/${id}/assign-roles`, command)

      const index = users.value.findIndex(u => u.id === id)
      if (index !== -1) {
        users.value[index] = user
      }

      if (currentUser.value?.id === id) {
        currentUser.value = user
      }

      return user
    } catch (err: any) {
      error.value = err.message || 'Failed to assign roles'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function changeUserStatus(id: string, command: ChangeUserStatusCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const user = await post<User>(`/api/admin/users/${id}/status`, { status: command.status })

      const index = users.value.findIndex(u => u.id === id)
      if (index !== -1) {
        users.value[index] = user
      }

      if (currentUser.value?.id === id) {
        currentUser.value = user
      }

      return user
    } catch (err: any) {
      error.value = err.message || 'Failed to change user status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteUser(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { del } = useApi()
      await del(`/api/admin/users/${id}`)

      const index = users.value.findIndex(u => u.id === id)
      if (index !== -1) {
        users.value.splice(index, 1)
      }

      if (currentUser.value?.id === id) {
        currentUser.value = null
      }

      return true
    } catch (err: any) {
      error.value = err.message || 'Failed to delete user'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentUser() {
    currentUser.value = null
  }

  return {
    // State
    users,
    currentUser,
    loading,
    error,
    pagination,

    // Getters
    userCount,
    activeUsers,
    inactiveUsers,
    suspendedUsers,
    pendingUsers,
    terminatedUsers,
    getUserById,

    // Actions
    fetchUsers,
    fetchUserById,
    createUser,
    updateUser,
    assignRoles,
    changeUserStatus,
    deleteUser,
    clearError,
    clearCurrentUser
  }
})
