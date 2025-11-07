<template>
  <AppTable
    :columns="columns"
    :data="users"
    :loading="loading"
    :clickable="true"
    @row-click="handleRowClick"
  >
    <template #header>
      <div class="user-table__header-actions">
        <AppButton variant="primary" @click="$emit('create')">
          <template #icon>➕</template>
          Create User
        </AppButton>
      </div>
    </template>

    <template #cell-fullName="{ value, row }">
      <div class="user-table__name">
        <div class="user-table__name-main">{{ value }}</div>
        <div class="user-table__name-sub">{{ row.email }}</div>
      </div>
    </template>

    <template #cell-roles="{ value }">
      <div class="user-table__roles">
        <span v-for="role in value" :key="role" class="user-table__role-tag">
          {{ role }}
        </span>
      </div>
    </template>

    <template #cell-status="{ value }">
      <AppBadge
        :variant="getStatusVariant(value)"
        :label="getStatusLabel(value)"
      />
    </template>

    <template #cell-actions="{ row }">
      <div class="user-table__actions">
        <button
          class="user-table__action-btn"
          @click.stop="$emit('edit', row)"
          title="Edit User"
        >
          ✏️
        </button>
        <button
          class="user-table__action-btn"
          @click.stop="handleStatusChange(row)"
          :title="row.isActive ? 'Deactivate User' : 'Activate User'"
        >
          {{ row.isActive ? '⏸️' : '▶️' }}
        </button>
        <button
          class="user-table__action-btn user-table__action-btn--danger"
          @click.stop="handleDelete(row)"
          title="Delete User"
        >
          🗑️
        </button>
      </div>
    </template>

    <template #empty>
      <div class="user-table__empty-state">
        <p>No users found</p>
      </div>
    </template>
  </AppTable>
</template>

<script setup lang="ts">
import type { User, UserStatus } from '~/app/composables/useUserManagement'

interface Props {
  users: User[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  create: []
  edit: [user: User]
  delete: [user: User]
  'status-change': [user: User, status: UserStatus]
}>()

const columns = [
  {
    key: 'fullName',
    label: 'User',
    sortable: true
  },
  {
    key: 'roles',
    label: 'Roles',
    sortable: false
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true
  },
  {
    key: 'createdAt',
    label: 'Created',
    sortable: true
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    width: '120px'
  }
]

const getStatusVariant = (status: UserStatus): string => {
  switch (status) {
    case 'ACTIVE':
      return 'success'
    case 'INACTIVE':
      return 'secondary'
    case 'SUSPENDED':
      return 'warning'
    case 'PENDING_VERIFICATION':
      return 'info'
    case 'TERMINATED':
      return 'danger'
    default:
      return 'secondary'
  }
}

const getStatusLabel = (status: UserStatus): string => {
  return status.replace(/_/g, ' ').toLowerCase()
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

const handleRowClick = (user: User) => {
  // Navigate to user details or emit event
  navigateTo(`/admin/users/${user.id}`)
}

const handleStatusChange = (user: User) => {
  const newStatus: UserStatus = user.isActive ? 'INACTIVE' : 'ACTIVE'
  emit('status-change', user, newStatus)
}

const handleDelete = (user: User) => {
  if (confirm(`Are you sure you want to delete user ${user.fullName}?`)) {
    emit('delete', user)
  }
}
</script>

<style scoped>
.user-table__header-actions {
  @apply flex justify-end;
}

.user-table__name {
  @apply py-2;
}

.user-table__name-main {
  @apply font-medium text-gray-900;
}

.user-table__name-sub {
  @apply text-sm text-gray-500;
}

.user-table__roles {
  @apply flex flex-wrap gap-1;
}

.user-table__role-tag {
  @apply inline-block px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800 rounded;
}

.user-table__actions {
  @apply flex items-center gap-2;
}

.user-table__action-btn {
  @apply p-1 text-lg hover:bg-gray-100 rounded transition-colors;
}

.user-table__action-btn--danger {
  @apply text-red-600 hover:bg-red-50;
}

.user-table__empty-state {
  @apply py-8 text-center text-gray-500;
}
</style>
