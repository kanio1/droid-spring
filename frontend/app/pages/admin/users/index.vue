<template>
  <div class="admin-users-page">
    <div class="admin-users-page__header">
      <h1>User Management</h1>
      <p class="text-gray-600">Manage system users, roles, and permissions</p>
    </div>

    <div class="admin-users-page__content">
      <div class="admin-users-page__filters">
        <AppSearchBar
          v-model="searchQuery"
          placeholder="Search users by name or email..."
          @search="handleSearch"
        />

        <AppSelect
          v-model="statusFilter"
          :options="statusOptions"
          placeholder="Filter by status"
          @change="handleFilterChange"
        />

        <AppSelect
          v-model="roleFilter"
          :options="roleOptions"
          placeholder="Filter by role"
          @change="handleFilterChange"
        />
      </div>

      <UserTable
        :users="users"
        :loading="loading"
        @create="handleCreate"
        @edit="handleEdit"
        @delete="handleDelete"
        @status-change="handleStatusChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { User, UserStatus } from '~/app/composables/useUserManagement'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

const {
  users,
  loading,
  getUsers,
  deleteUser,
  changeUserStatus,
  getAvailableRoles
} = useUserManagement()

const searchQuery = ref('')
const statusFilter = ref<UserStatus | null>(null)
const roleFilter = ref<string | null>(null)
const availableRoles = ref<string[]>([])

const statusOptions = [
  { label: 'All Statuses', value: null },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' },
  { label: 'Pending', value: 'PENDING_VERIFICATION' },
  { label: 'Terminated', value: 'TERMINATED' }
]

const roleOptions = ref<Array<{ label: string; value: string | null }>>([])

onMounted(async () => {
  await loadUsers()
  await loadRoles()
})

const loadUsers = async () => {
  await getUsers({
    search: searchQuery.value || undefined,
    status: statusFilter.value || undefined,
    role: roleFilter.value || undefined
  })
}

const loadRoles = async () => {
  try {
    const roles = await getAvailableRoles()
    availableRoles.value = roles
    roleOptions.value = [
      { label: 'All Roles', value: null },
      ...roles.map(role => ({ label: role, value: role }))
    ]
  } catch (error) {
    console.error('Failed to load roles:', error)
  }
}

const handleSearch = () => {
  loadUsers()
}

const handleFilterChange = () => {
  loadUsers()
}

const handleCreate = () => {
  navigateTo('/admin/users/create')
}

const handleEdit = (user: User) => {
  navigateTo(`/admin/users/${user.id}/edit`)
}

const handleDelete = async (user: User) => {
  try {
    await deleteUser(user.id)
    await loadUsers()
  } catch (error) {
    console.error('Failed to delete user:', error)
  }
}

const handleStatusChange = async (user: User, newStatus: UserStatus) => {
  try {
    await changeUserStatus(user.id, newStatus)
    await loadUsers()
  } catch (error) {
    console.error('Failed to change user status:', error)
  }
}
</script>

<style scoped>
.admin-users-page {
  @apply p-6;
}

.admin-users-page__header {
  @apply mb-6;
}

.admin-users-page__header h1 {
  @apply text-3xl font-bold text-gray-900;
}

.admin-users-page__content {
  @apply space-y-4;
}

.admin-users-page__filters {
  @apply flex gap-4 p-4 bg-white rounded-lg shadow;
}
</style>
