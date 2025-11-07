<template>
  <AppTable
    :columns="columns"
    :data="customers"
    :loading="loading"
    :show-pagination="true"
    :pagination="pagination"
    @page="onPage"
    @sort="onSort"
    @update:sortField="sortField = $event"
    @update:sortOrder="sortOrder = $event"
  >
    <!-- Name column -->
    <template #cell-name="{ row }">
      <div class="customer-name">
        <div class="customer-avatar">
          {{ getInitials(row) }}
        </div>
        <div class="customer-info">
          <div class="customer-full-name">{{ formatCustomerName(row) }}</div>
          <div class="customer-id">ID: {{ row.id }}</div>
        </div>
      </div>
    </template>

    <!-- Email column -->
    <template #cell-email="{ value }">
      <span class="customer-email">{{ value }}</span>
    </template>

    <!-- Phone column -->
    <template #cell-phone="{ value }">
      <span v-if="value" class="customer-phone">{{ value }}</span>
      <span v-else class="customer-phone--empty">—</span>
    </template>

    <!-- PESEL/NIP column -->
    <template #cell-pesel="{ row }">
      <div class="customer-identifiers">
        <div v-if="row.pesel" class="customer-id-item">
          <span class="id-label">PESEL:</span> {{ row.pesel }}
        </div>
        <div v-if="row.nip" class="customer-id-item">
          <span class="id-label">NIP:</span> {{ row.nip }}
        </div>
        <div v-if="!row.pesel && !row.nip" class="customer-id-item--empty">—</div>
      </div>
    </template>

    <!-- Status column -->
    <template #cell-status="{ row }">
      <StatusBadge :status="row.status" type="customer" size="small" />
    </template>

    <!-- Created Date column -->
    <template #cell-createdAt="{ value }">
      <span class="customer-date">{{ formatDate(value) }}</span>
    </template>

    <!-- Actions column -->
    <template #cell-actions="{ row }">
      <div class="customer-actions">
        <Button
          icon="pi pi-eye"
          text
          rounded
          @click="$emit('view', row)"
          v-tooltip.top="'View details'"
        />
        <Button
          icon="pi pi-pencil"
          text
          rounded
          @click="$emit('edit', row)"
          v-tooltip.top="'Edit customer'"
        />
        <Button
          icon="pi pi-trash"
          text
          rounded
          severity="danger"
          @click="$emit('delete', row)"
          v-tooltip.top="'Delete customer'"
        />
      </div>
    </template>

    <!-- Empty state -->
    <template #empty>
      <slot name="empty">
        <div class="empty-state">
          <i class="pi pi-users empty-state__icon"></i>
          <h3 class="empty-state__title">No customers found</h3>
          <p class="empty-state__description">
            {{ searchTerm || statusFilter ?
              'Try adjusting your search criteria' :
              'Get started by adding your first customer'
            }}
          </p>
          <Button
            v-if="!searchTerm && !statusFilter"
            label="Add First Customer"
            icon="pi pi-plus"
            severity="primary"
            @click="$emit('create')"
          />
        </div>
      </slot>
    </template>
  </AppTable>
</template>

<script setup lang="ts">
import { formatCustomerName, getInitials, CUSTOMER_STATUS_LABELS } from '~/schemas/customer'
import type { Customer } from '~/schemas/customer'

interface Props {
  customers: Customer[]
  loading?: boolean
  pagination: any
  searchTerm?: string
  statusFilter?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  view: [customer: Customer]
  edit: [customer: Customer]
  delete: [customer: Customer]
  create: []
  page: [{ page: number; rows: number }]
  sort: [{ field: string; order: number }]
}>()

const sortField = ref('createdAt')
const sortOrder = ref(-1)

const columns = [
  {
    key: 'name',
    label: 'Name',
    sortable: false,
    style: 'width: 20%'
  },
  {
    key: 'email',
    label: 'Email',
    sortable: true,
    style: 'width: 20%'
  },
  {
    key: 'phone',
    label: 'Phone',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'pesel',
    label: 'Identifiers',
    sortable: false,
    style: 'width: 15%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'createdAt',
    label: 'Created',
    sortable: true,
    style: 'width: 13%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 10%'
  }
]

const onPage = (event: any) => {
  emit('page', { page: event.page, rows: event.rows })
}

const onSort = (event: any) => {
  emit('sort', { field: event.sortField, order: event.sortOrder })
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}
</script>

<style scoped>
.customer-name {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.customer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  flex-shrink: 0;
}

.customer-info {
  flex: 1;
  min-width: 0;
}

.customer-full-name {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.customer-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.customer-email {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.customer-phone {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.customer-phone--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

.customer-identifiers {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.customer-id-item {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.customer-id-item--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

.id-label {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.customer-date {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.customer-actions {
  display: flex;
  gap: var(--space-1);
  align-items: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-12);
  text-align: center;
}

.empty-state__icon {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
  color: var(--color-text-muted);
}

.empty-state__title {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.empty-state__description {
  margin: 0 0 var(--space-6) 0;
  color: var(--color-text-secondary);
  max-width: 400px;
}
</style>
